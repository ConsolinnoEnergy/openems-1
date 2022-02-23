package io.openems.edge.heatsystem.components.valve;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;

import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.heatsystem.components.ConfigurationType;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.io.api.AnalogInputOutput;
import io.openems.edge.io.api.Pwm;
import io.openems.edge.io.api.Relay;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;


/**
 * This Component allows a Valve  to be configured and controlled.
 * It either works with 2 Relays or 2 ChannelAddresses.
 * It can update it's opening/closing state and shows up the percentage value of itself.
 * To check if the output is correct, you can configure the CheckUp.
 * E.g. The Consolinno Leaflet reads the MCP and it's status, this will be send into the {@link Relay#getRelaysReadChannel()}
 * If the value you read is the expected value, everything is ok, otherwise the Components tries to set the expected values again.
 */
@Designate(ocd = ConfigValveTwoOutput.class, factory = true)
@Component(name = "HeatsystemComponent.Valve.TwoOutput",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS}
)
public class ValveTwoOutput extends AbstractValve implements OpenemsComponent, HydraulicComponent, ExceptionalState, EventHandler {


    private final Logger log = LoggerFactory.getLogger(ValveTwoOutput.class);

    private ChannelAddress openAddress;
    private ChannelAddress closeAddress;
    private ChannelAddress inputOpenAddress;
    private ChannelAddress inputClosingAddress;

    private Relay openRelay;
    private Relay closeRelay;


    private ConfigurationType configurationType;
    private ConfigValveTwoOutput config;

    @Reference
    ComponentManager cpm;

    AtomicReference<Cycle> cycle = new AtomicReference<>();

    private enum ChannelToGet {
        CLOSING, OPENING;
    }


    public ValveTwoOutput() {
        super(OpenemsComponent.ChannelId.values(), HydraulicComponent.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, ConfigValveTwoOutput config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        try {
            this.activateOrModifiedRoutine(config);
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configSuccess = false;
            this.log.warn("Couldn't find Components for " + super.id() + " Component Tries again later.");
        } finally {
            this.getIsBusyChannel().setNextValue(false);
            this.getPowerLevelChannel().setNextValue(0);
            this.getLastPowerLevelChannel().setNextValue(0);
            this.setPointPowerLevelChannel().setNextValue(-1);
            this.futurePowerLevelChannel().setNextValue(0);
            if (config.shouldCloseOnActivation()) {
                this.forceClose();
            } else if (config.shouldOpenOnActivation()) {
                this.getPowerLevelChannel().setNextValue(0);
                this.getPowerLevelChannel().nextProcessImage();
                try {
                    this.setPointPowerLevelChannel().setNextWriteValueFromObject(100);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn(this.id() + ": Couldn't write into own Channel. Reason: " + e.getMessage());
                }
                this.forceOpen();
            }
        }
    }

    @Modified
    void modified(ComponentContext context, ConfigValveTwoOutput config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        try {
            this.activateOrModifiedRoutine(config);
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Couldn't find Components for " + super.id() + " Component Tries again later.");
        }
    }


    /**
     * This will be called on either Activation or Modification.
     *
     * @param config the Config of the Valve
     * @throws ConfigurationException             if anything is configured Wrong
     * @throws OpenemsError.OpenemsNamedException thrown if configured address or Relay cannot be found at all.
     */

    private void activateOrModifiedRoutine(ConfigValveTwoOutput config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.configSuccess = false;
        this.config = config;
        Optional<OpenemsComponent> cycleOptional = this.cpm.getAllComponents().stream().filter(component -> component instanceof Cycle).findAny();
        cycleOptional.ifPresent(component -> this.cycle.set((Cycle) component));
        super.setCycle(this.cycle.get());
        this.configurationType = config.configurationType();
        this.useCheckOutput = config.useCheckChannel();
        switch (this.configurationType) {
            case CHANNEL:
                this.openAddress = ChannelAddress.fromString(config.open());
                this.closeAddress = ChannelAddress.fromString(config.close());
                if (this.checkChannelOk() == false) {
                    throw new ConfigurationException("ActivateMethod in Valve: " + super.id(), "Given Channels are not ok!");
                }
                if (this.useCheckOutput) {
                    this.inputClosingAddress = ChannelAddress.fromString(config.checkClosingChannelAddress());
                    this.inputOpenAddress = ChannelAddress.fromString(config.checkOpeningChannelAddress());
                } else {
                    this.inputClosingAddress = null;
                    this.inputOpenAddress = null;
                }
                break;
            case DEVICE:
                if (this.checkDevicesOkAndApplyThem(config.open(), config.close()) == false) {
                    throw new ConfigurationException("ActivateMethod in Valve: " + super.id(), "Given Devices are not ok!");
                }
                break;
        }
        this.secondsPerPercentage = ((double) config.valve_Time() / 100.d);
        this.timeChannel().setNextValue(0);
        super.createTimerHandler(config.timerId(), config.maxTime(), this.cpm, this, config.useExceptionalState());
        int deltaMaxTime = Cycle.DEFAULT_CYCLE_TIME;
        if (this.cycle.get() != null) {
            deltaMaxTime = this.cycle.get().getCycleTime();
        }
        super.percentPossiblePerCycle = deltaMaxTime / (this.secondsPerPercentage * MILLI_SECONDS_TO_SECONDS);
        this.configSuccess = true;
    }

    /**
     * Get the Current active {@link Cycle} and set as Reference.
     */
    private void getCycle() {
        Optional<OpenemsComponent> cycleOptional = this.cpm.getAllComponents().stream().filter(component -> component instanceof Cycle).findAny();
        cycleOptional.ifPresent(component -> this.cycle.set((Cycle) component));
        super.setCycle(this.cycle.get());
    }


    /**
     * Called on Activation or Modification. Checks if the Strings match a Relay configured within OpenEms
     *
     * @param open  the String/Id of the Relay that opens the Valve
     * @param close the String/Id of the Relay that closes the Valve
     * @return true if the device is ok, otherwise false (Happens if the OpenEmsComponent is not an instance of a Relay)
     * @throws OpenemsError.OpenemsNamedException if the Id is not found at all
     */
    private boolean checkDevicesOkAndApplyThem(String open, String close) throws OpenemsError.OpenemsNamedException {
        OpenemsComponent relayToApply = this.cpm.getComponent(open);
        if (relayToApply instanceof Relay) {
            this.openRelay = (Relay) relayToApply;
        } else {
            return false;
        }
        relayToApply = this.cpm.getComponent(close);
        if (relayToApply instanceof Relay) {
            this.closeRelay = (Relay) relayToApply;
        } else {
            return false;
        }
        return true;
    }

    /**
     * Checks if the Channel are correct.
     *
     * @return if the channel are instances of WriteChannel and Type = Boolean
     * @throws OpenemsError.OpenemsNamedException if Channel could not be found
     */
    private boolean checkChannelOk() throws OpenemsError.OpenemsNamedException {
        Channel<?> openChannel = this.cpm.getChannel(this.openAddress);
        Channel<?> closeChannel = this.cpm.getChannel(this.closeAddress);
        return openChannel instanceof WriteChannel<?> && closeChannel instanceof WriteChannel<?> && openChannel.getType()
                .equals(OpenemsType.BOOLEAN) && closeChannel.getType().equals(OpenemsType.BOOLEAN);
    }

    /**
     * This handles the basic Operation of a Valve, handled at 2 different events.
     *
     * <p>
     * The Valve checks if the Output is active as expected, by reading the CheckChannel, given via Configuration
     * (if Check is enabled).
     * After that update the PowerLevel depending on how much time has passed.
     * Check if the Valve is within the Min and Max Boundaries or else adapt.
     * If the PowerLevel is Reached -> adapt the Valve Value -> e.g. if 60% was the Goal, Valve was 50% before,
     * has 70% now - > adapt if possible (get as close to 60% as possible)
     * </p>
     *
     * <p>
     * After Controllers -> check if a PowerLevel has been Set (Call Parent)
     * Otherwise if the Valve reached it's powerLevel -> deactivate both outputs.
     *
     * </p>
     *
     * @param event the Event, either After_Process_Image or After_Controllers
     */
    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            if (this.configSuccess) {
                this.lastMaximum = this.maximum;
                this.lastMinimum = this.minimum;
                this.checkValveChannelCorrect();
                this.updatePowerLevel();
                this.checkMaxAndMinAllowed();
                boolean reached = this.powerLevelReached() && this.readyToChange();
                if (reached || this.isChanging == false) {
                    this.getIsBusyChannel().setNextValue(false);
                    this.isForced = false;
                    super.adaptValveValue();
                }
                if (this.configurationType.equals(ConfigurationType.DEVICE) && super.timerHandler.checkTimeIsUp(CHECK_COMPONENT_IDENTIFIER)) {
                    this.checkForMissingComponents();
                    this.timerHandler.resetTimer(CHECK_COMPONENT_IDENTIFIER);
                }
            } else {
                try {
                    this.activateOrModifiedRoutine(this.config);
                    this.configSuccess = true;
                } catch (ConfigurationException | OpenemsError.OpenemsNamedException e) {
                    this.configSuccess = false;
                    this.log.error("Config is Wrong in : " + super.id());
                }
            }
        } else if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS) && this.configSuccess) {
            if (parentDidRoutine() == false) {
                if (this.powerLevelReached()) {
                    this.shutdownRelays();
                }
            }
        }

    }

    /**
     * Check if the Min and/or Max value is defined and valid.
     */
    private void checkMaxAndMinAllowed() {
        double futurePowerLevel = this.getFuturePowerLevelValue();
        double currentPowerLevel = this.getPowerLevelValue();
        double powerValueToCompare = this.isChanging ? futurePowerLevel : currentPowerLevel;
        if (this.maximum != null && (this.maximum + TOLERANCE < powerValueToCompare)) {
            this.setPowerLevel(this.maximum);
        } else if (this.minimum != null && (this.minimum - TOLERANCE > powerValueToCompare)) {
            this.setPowerLevel(this.minimum);
        }
    }


    /**
     * Changes Valve Position by incoming percentage.
     * Warning, only executes if valve is not busy! (was not forced to open/close)
     * Depending on + or - it changes the current State to open/close it more. Switching the relays on/off does
     * not open/close the valve instantly but slowly. The time it takes from completely closed to completely
     * open is entered in the config. Partial open state of x% is then archived by switching the relay on for
     * time-to-open * x%, or the appropriate amount of time depending on initial state.
     * Sets the Future PowerLevel; ValveManager calls further Methods to refresh true % state
     *
     * @param percentage adjusting the current powerLevel in % points. Meaning if current state is 10%, requesting
     *                   changeByPercentage(20) will change the state to 30%.
     *                   <p>
     *                   If the Valve is busy return false
     *                   otherwise: save the current PowerLevel to the old one and overwrite the new one.
     *                   Then it will check how much time is needed to adjust the position of the valve.
     *                   If percentage is neg. valve needs to be closed (further)
     *                   else it needs to open (further).
     *                   </p>
     */
    @Override
    public boolean changeByPercentage(double percentage) {

        if (super.changeInvalid(percentage)) {
            return false;
        } else {
            double futurePowerLevel = super.calculateCurrentPowerLevelAndSetTime(percentage);
            if (futurePowerLevel < 0) {
                this.shutdownRelays();
                return false;
            }
            //Close on negative Percentage and Open on Positive
            this.isChanging = true;
            if (percentage < DEFAULT_MIN_POWER_VALUE) {
                this.valveClose();
            } else {
                this.valveOpen();
            }
            return true;
        }
    }
    //------------------------------------------------------ //

    /**
     * IS Changing --> Is closing/Opening.
     *
     * @return isChanging
     */
    @Override
    public boolean isChanging() {
        return this.isChanging;
    }

    //---------------------RESET------------------------- //

    /**
     * Resets the Valve and forces to close.
     * Was Already Reset prevents multiple forceCloses if Channel not refreshed in time.
     */
    @Override
    public void reset() {
        if (this.wasAlreadyReset == false) {
            this.forceClose();
            this.wasAlreadyReset = true;
        }

    }

    // ------------ FORCE OPEN AND CLOSE------------------ //

    /**
     * Closes the valve completely, overriding any current valve operation.
     * If a closed valve is all you need, better use this instead of changeByPercentage(-100) as you do not need
     * to check if the valve is busy or not.
     * Usually called to Reset a Valve or ForceClose the Valve on an Error.
     */
    @Override
    public void forceClose() {
        if (this.isForced == false || this.isClosing == false) {
            if (super.parentForceClose()) {
                this.valveClose();
            }
        }
    }

    /**
     * Opens the valve completely, overriding any current valve operation.
     * If an open valve is all you need, better use this instead of changeByPercentage(100) as you do not need
     * to check if the valve is busy or not.
     */
    @Override
    public void forceOpen() {
        if (this.isForced == false || this.isClosing) {
            if (super.parentForceOpen()) {
                this.valveOpen();
            }
        }
    }

    //-------------------------------------------------------------//


    //---------------------ShutDown Relay-------------------------//

    /**
     * Turn off Relay if PowerLevel is reached.
     */
    private void shutdownRelays() {
        this.controlRelays(false, "Open");
        this.controlRelays(false, "Closed");
    }

    // -------------------------------------- //


    // ---------- CLOSE AND OPEN VALVE ------------ //

    /**
     * Closes the valve and sets a time stamp.
     */
    private void valveClose() {

        this.controlRelays(false, "Open");
        this.controlRelays(true, "Closed");
        if (this.isClosing == false) {
            this.timeStampValveCurrent = -1;
            this.isClosing = true;
        }

    }

    /**
     * Opens the valve and sets a time stamp.
     * DO NOT CALL DIRECTLY! Might not work if called directly as the timer for "readyToChange()" is not
     * set properly. Use either "changeByPercentage()" or forceClose / forceOpen.
     */
    private void valveOpen() {

        this.controlRelays(false, "Closed");
        this.controlRelays(true, "Open");
        if (this.isClosing) {
            this.timeStampValveCurrent = -1;
            this.isClosing = false;
        }
    }
    //-------------------------------------


    /**
     * Controls the relays by typing either activate or not and what relays should be called.
     * If ExceptionHandling --> use forceClose or forceOpen!
     *
     * @param activateOrDeactivate activate or deactivate.
     * @param whichRelays          opening or closing relays ?
     *                             <p>Writes depending if the relays is an opener or closer, the correct boolean.
     *                             if the relays was set false (no power) busy will be false.</p>
     */
    private void controlRelays(boolean activateOrDeactivate, String whichRelays) {
        try {
            switch (whichRelays) {
                case "Open":
                    if (this.configurationType.equals(ConfigurationType.CHANNEL)) {
                        if (this.checkChannelOk()) {
                            WriteChannel<Boolean> openChannel = this.cpm.getChannel(this.openAddress);
                            openChannel.setNextWriteValue(activateOrDeactivate);
                        }
                    } else {
                        this.openRelay.getRelaysWriteChannel().setNextWriteValueFromObject(activateOrDeactivate);
                    }
                    break;

                case "Closed":
                    if (this.configurationType.equals(ConfigurationType.CHANNEL)) {
                        if (this.checkChannelOk()) {
                            WriteChannel<Boolean> closeChannel = this.cpm.getChannel(this.closeAddress);
                            closeChannel.setNextWriteValue(activateOrDeactivate);
                        }
                    } else {
                        this.closeRelay.setRelayStatus(activateOrDeactivate);
                    }
                    break;

            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't write into Channel; Valve: " + super.id());
        }
    }

    //----------PRIVATE VALVE CHECK ----------- //

    /**
     * Checks if the the Valve reacts properly, by checking on closing/opening the Channel Values.
     */
    private void checkValveChannelCorrect() {
        if (this.useCheckOutput) {
            try {
                boolean channelValueIsTrue;
                boolean channelValueIsNotFalse;

                Channel<?> channelToCheckForTrue;
                Channel<?> channelToCheckForFalse;
                //Either it was Closing before but powerLevel fell below future OR it was opening before and not set to closed -> Future < current
                this.isClosing = this.isClosing
                        || this.futurePowerLevelChannel().value().orElse((double) HydraulicComponent.DEFAULT_MAX_POWER_VALUE)
                        < this.getPowerLevelChannel().value().orElse((double) HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
                if (this.isClosing) {
                    channelToCheckForTrue = this.getInputChannel(ChannelToGet.CLOSING);
                    channelToCheckForFalse = this.getInputChannel(ChannelToGet.OPENING);
                } else {
                    channelToCheckForTrue = this.getInputChannel(ChannelToGet.OPENING);
                    channelToCheckForFalse = this.getInputChannel(ChannelToGet.CLOSING);
                }
                channelValueIsTrue = this.checkChannelValueIsTrue(channelToCheckForTrue);
                channelValueIsNotFalse = this.checkChannelValueIsTrue(channelToCheckForFalse);
                if (this.isChanging) {
                    if (channelValueIsTrue == false || channelValueIsNotFalse) {
                        updateOk = false;
                        timeStampValveCurrent = -1;
                        // if channel who needs to be true is NOT true but false and channel which needs to be false is true
                        // Check if closing -> then add percent increase(is Opening) else -> is closing further
                        double powerLevelToApply = super.powerLevelBeforeUpdate;
                        if (!channelValueIsTrue && channelValueIsNotFalse) {
                            if (this.isClosing) {
                                powerLevelToApply += super.percentIncreaseThisRun;
                                powerLevelToApply = Math.min(powerLevelToApply, HydraulicComponent.DEFAULT_MAX_POWER_VALUE);
                            } else {
                                powerLevelToApply -= super.percentIncreaseThisRun;
                                powerLevelToApply = Math.max(powerLevelToApply, HydraulicComponent.DEFAULT_MAX_POWER_VALUE);
                            }
                        }
                        this.getPowerLevelChannel().setNextValue(powerLevelToApply);

                        if (this.isClosing) {
                            this.valveClose();
                        } else {
                            this.valveOpen();
                        }

                    } else {
                        updateOk = true;
                    }
                } else {
                    if (channelValueIsTrue) {
                        this.isChanging = true;
                        this.updatePowerLevel();
                        this.shutdownRelays();
                    }
                }

            } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
                this.log.warn("Attention in Valve: " + super.id() + " ChannelToCheck Method failed: " + e.getMessage());
                this.updateOk = false;
            }
        }
    }

    /**
     * Returns the Channel that are configured for opening or closing.
     *
     * @param channelToGet the {@link ChannelToGet}
     * @return the Channel corresponding to {@link ChannelToGet}.
     * @throws OpenemsError.OpenemsNamedException if Channel not found.
     */
    private Channel<?> getInputChannel(ChannelToGet channelToGet) throws OpenemsError.OpenemsNamedException {
        switch (this.configurationType) {
            case CHANNEL:
                if (channelToGet.equals(ChannelToGet.OPENING)) {
                    return this.cpm.getChannel(this.inputOpenAddress);
                } else {
                    return this.cpm.getChannel(this.inputClosingAddress);
                }
            case DEVICE:
            default:
                if (channelToGet.equals(ChannelToGet.OPENING)) {
                    return this.openRelay.getRelaysReadChannel();
                } else {
                    return this.closeRelay.getRelaysReadChannel();
                }
        }
    }

    /**
     * Checks if the Value of the Channel is equivalent to true.
     *
     * @param channelToCheck the Channel
     * @return true if Value is true or Value > 0.
     * @throws ConfigurationException if e.g. String does not contain only numbers.
     */
    private boolean checkChannelValueIsTrue(Channel<?> channelToCheck) throws ConfigurationException {
        Value<?> value = channelToCheck.value().isDefined() ? channelToCheck.value()
                : channelToCheck.getNextValue().isDefined() ? channelToCheck.getNextValue() : null;
        if (value != null) {
            switch (channelToCheck.getType()) {
                case BOOLEAN:
                    return (Boolean) value.get();
                case SHORT:
                case INTEGER:
                case LONG:
                case FLOAT:
                case DOUBLE:
                    return ((Double) value.get() > 0);
                case STRING:
                    if (this.containsOnlyNumbers(value.get().toString())) {
                        return Double.parseDouble(value.get().toString()) > 0;
                    } else {
                        throw new ConfigurationException("CheckChannelValue: " + super.id() + channelToCheck.toString(),
                                "Channel Cannot be parsed to a Numeric Value");
                    }
                default:
                    throw new ConfigurationException("CheckChannelValue: " + super.id() + channelToCheck.toString(),
                            "ValueType not supported!");
            }
        } else {
            return false;
        }
    }


    /**
     * This Method will only be called, when someone configured the Valve with the {@link ConfigurationType#DEVICE}.
     * It sometimes happens, that devices restart or get deactivated etc. The Vaöve will check for old references and refreshes them
     * every 30 deltaTime (Depends on the Timer).
     */
    private void checkForMissingComponents() {
        OpenemsComponent component;
        try {
            if (this.configurationType.equals(ConfigurationType.DEVICE)) {
                component = this.cpm.getComponent(this.closeRelay.id());
                if (!this.closeRelay.equals(component) && component instanceof Relay) {
                    this.closeRelay = (Relay) component;
                }
                component = this.cpm.getComponent(this.openRelay.id());
                if (!this.openRelay.equals(component) && component instanceof Relay) {
                    this.openRelay = (Relay) component;
                }
            }
            if (this.cycle.get() == null || (!this.cycle.get().equals(this.cpm.getComponent(this.cycle.get().id())))) {
                this.getCycle();
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't check for missing Components. Reason: " + e.getMessage());
        }
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }
}

