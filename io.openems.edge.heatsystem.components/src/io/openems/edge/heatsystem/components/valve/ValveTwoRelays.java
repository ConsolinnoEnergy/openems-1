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
import io.openems.edge.heatsystem.components.HeatsystemComponent;
import io.openems.edge.heatsystem.components.Valve;
import io.openems.edge.relay.api.Relay;
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


/**
 * This Component allows a Valve  to be configured and controlled.
 * It either works with 2 Relays or 2 ChannelAddresses.
 * It updates it's opening/closing state and shows up the percentage value of itself.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "HeatsystemComponent.Valve",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS}
)
public class ValveTwoRelays extends AbstractValve implements OpenemsComponent, Valve, ExceptionalState, EventHandler {

    @Reference
    Cycle cycle;

    private final Logger log = LoggerFactory.getLogger(ValveTwoRelays.class);

    private ChannelAddress openAddress;
    private ChannelAddress closeAddress;
    private ChannelAddress inputOpenAddress;
    private ChannelAddress inputClosingAddress;

    private Relay openRelay;
    private Relay closeRelay;

    private boolean useCheckChannel;

    private ConfigurationType configurationType;

    @Reference
    ComponentManager cpm;


    private enum ChannelToGet {
        CLOSING, OPENING;
    }


    public ValveTwoRelays() {
        super(OpenemsComponent.ChannelId.values(), HeatsystemComponent.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activateOrModifiedRoutine(config);
        this.getIsBusyChannel().setNextValue(false);
        this.getPowerLevelChannel().setNextValue(0);
        this.getLastPowerLevelChannel().setNextValue(0);
        this.setPointPowerLevelChannel().setNextValue(-1);
        this.futurePowerLevelChannel().setNextValue(0);
        if (config.shouldCloseOnActivation()) {
            this.forceClose();
        }
    }

    /**
     * This will be called on either Activation or Modification.
     *
     * @param config the Config of the Valve
     * @throws ConfigurationException             if anything is configured Wrong
     * @throws OpenemsError.OpenemsNamedException thrown if configured address or Relay cannot be found at all.
     */

    private void activateOrModifiedRoutine(Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.configurationType = config.configurationType();
        this.useCheckChannel = config.useInputCheck();
        switch (this.configurationType) {
            case CHANNEL:
                this.openAddress = ChannelAddress.fromString(config.open());
                this.closeAddress = ChannelAddress.fromString(config.close());
                if (this.checkChannelOk() == false) {
                    throw new ConfigurationException("ActivateMethod in Valve: " + super.id(), "Given Channels are not ok!");
                }
                if (this.useCheckChannel) {
                    this.inputClosingAddress = ChannelAddress.fromString(config.inputClosingChannelAddress());
                    this.inputOpenAddress = ChannelAddress.fromString(config.inputOpeningChannelAddress());
                } else {
                    this.inputClosingAddress = null;
                    this.inputOpenAddress = null;
                }
                break;
            case DEVICE:
                if (this.checkDevicesOk(config.open(), config.close()) == false) {
                    throw new ConfigurationException("ActivateMethod in Valve: " + super.id(), "Given Devices are not ok!");
                }
                break;
        }
        this.secondsPerPercentage = ((double) config.valve_Time() / 100.d);
        this.timeChannel().setNextValue(0);
        if (config.useExceptionalState()) {
            super.createExcpetionalStateHandler(config.timerId(), config.maxTime(), this.cpm, this);
        }
    }


    /**
     * Called on Activation or Modification. Checks if the Strings match a Relay configured within OpenEms
     *
     * @param open  the String/Id of the Relay that opens the Valve
     * @param close the String/Id of the Relay that closes the Valve
     * @return true if the device is ok, otherwise false (Happens if the OpenEmsComponent is not an instance of a Relay)
     * @throws OpenemsError.OpenemsNamedException if the Id is not found at all
     */
    private boolean checkDevicesOk(String open, String close) throws OpenemsError.OpenemsNamedException {
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


    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activateOrModifiedRoutine(config);
    }


    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            this.lastMaximum = this.maximum;
            this.lastMinimum = this.minimum;
            this.checkValveChannelCorrect();
            this.updatePowerLevel();
            this.checkMaxAndMinAllowed();
            boolean reached = this.powerLevelReached() && this.readyToChange();
            if (reached) {
                this.getIsBusyChannel().setNextValue(false);
                this.isForced = false;
                this.adaptValveValue();
            }
        } else if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            if (this.isExceptionalStateActive()) {
                this.setPowerLevel(this.getExceptionalSateValue());
            } else if (this.shouldReset()) {
                this.reset();
            } else if (this.getForceFullPowerAndResetChannel()) {
                this.forceOpen();
            } else {
                int setPointPowerLevelValue = this.setPointPowerLevelValue();
                if (setPointPowerLevelValue >= DEFAULT_MIN_POWER_VALUE) {
                    this.setPowerLevel(setPointPowerLevelValue);
                } else {
                    if (this.powerLevelReached()) {
                        this.shutdownRelays();
                    }
                    this.updatePowerLevel();
                }
            }
        }
    }

    private void setPowerLevel(double setPoint) {
        setPoint -= this.getPowerLevelValue();
        if (this.changeByPercentage(setPoint)) {
            this.setPointPowerLevelChannel().setNextValue(-1);
        }
    }

    /**
     * Check if the Min and/or Max value is defined and valid.
     */
    private void checkMaxAndMinAllowed() {
        Double maxAllowed = this.getMaxAllowedValue();
        Double minAllowed = this.getMinAllowedValue();
        double futurePowerLevel = this.getFuturePowerLevelValue();
        if (maxAllowed != null && maxAllowed + VALUE_BUFFER < futurePowerLevel) {
            this.changeByPercentage(maxAllowed - this.getPowerLevelValue());
        } else if (minAllowed != null && minAllowed - VALUE_BUFFER > futurePowerLevel) {
            this.changeByPercentage(minAllowed - futurePowerLevel);
        }
    }

    /**
     * Only if Reached! this Method will be called.
     */
    private void adaptValveValue() {
        int cycleTime = this.cycle == null ? Cycle.DEFAULT_CYCLE_TIME : this.cycle.getCycleTime();
        double percentPossiblePerCycle = cycleTime / (this.secondsPerPercentage * MILLI_SECONDS_TO_SECONDS);
        double limit = percentPossiblePerCycle * 2;
        boolean powerLevelOutOfBounce = this.getPowerLevelValue() - limit > this.getFuturePowerLevelValue() || this.getPowerLevelValue() + limit < this.getFuturePowerLevelValue() || this.getPowerLevelValue() == this.getFuturePowerLevelValue();
        if (percentPossiblePerCycle >= 2 && powerLevelOutOfBounce) {
            try {
                this.setPointPowerLevelChannel().setNextWriteValueFromObject(this.getFuturePowerLevelValue());
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't adapt Valve; Value of Valve: " + super.id());
            }
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


        if (this.readyToChange() == false || percentage == DEFAULT_MIN_POWER_VALUE) {
            return false;
        } else {
            Double currentPowerLevel = this.getPowerLevelValue();
            //Setting the oldPowerLevel and adjust the percentage Value
            this.getLastPowerLevelChannel().setNextValue(currentPowerLevel);
            this.maximum = getMaxAllowedValue();
            this.minimum = getMinAllowedValue();
            if (this.maxMinValid() == false) {
                this.minimum = null;
                this.maximum = null;
            }
            currentPowerLevel += percentage;
            if (this.maximum != null && this.maximum < currentPowerLevel) {
                currentPowerLevel = this.maximum;
            } else if (this.lastMaximum != null && this.lastMaximum < currentPowerLevel) {
                currentPowerLevel = this.lastMaximum;
            } else if (currentPowerLevel >= DEFAULT_MAX_POWER_VALUE) {
                currentPowerLevel = (double) DEFAULT_MAX_POWER_VALUE;
            } else if (this.minimum != null && this.minimum > currentPowerLevel) {
                currentPowerLevel = this.minimum;
            } else if (this.lastMinimum != null && this.lastMinimum > currentPowerLevel) {
                currentPowerLevel = this.lastMinimum;
            }
            //Set goal Percentage for future reference
            this.futurePowerLevelChannel().setNextValue(currentPowerLevel);
            //if same power level do not change and return --> relays is not always powered
            Double lastPower = this.getLastPowerLevelValue();
            if (lastPower.equals(currentPowerLevel)) {
                this.isChanging = false;
                this.shutdownRelays();
                return false;
            }
            //Calculate the Time to Change the Valve
            if (Math.abs(percentage) >= DEFAULT_MAX_POWER_VALUE) {
                this.timeChannel().setNextValue(DEFAULT_MAX_POWER_VALUE * this.secondsPerPercentage);
            } else {
                this.timeChannel().setNextValue(Math.abs(percentage) * this.secondsPerPercentage);
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
            this.isForced = true;
            this.isChanging = true;
            this.futurePowerLevelChannel().setNextValue(DEFAULT_MIN_POWER_VALUE);
            this.timeChannel().setNextValue(DEFAULT_MAX_POWER_VALUE * this.secondsPerPercentage);
            this.valveClose();
            this.getIsBusyChannel().setNextValue(true);
            //Making sure to wait the correct time even if it is already closing.
            this.timeStampValveInitial = -1;
            this.updatePowerLevel();

        }

    }

    /**
     * Opens the valve completely, overriding any current valve operation.
     * If an open valve is all you need, better use this instead of changeByPercentage(100) as you do not need
     * to check if the valve is busy or not.
     */
    @Override
    public void forceOpen() {
        if (this.isForced == false || this.isClosing == true) {
            this.isForced = true;
            this.isChanging = true;
            this.futurePowerLevelChannel().setNextValue(DEFAULT_MAX_POWER_VALUE);
            this.timeChannel().setNextValue(DEFAULT_MAX_POWER_VALUE * this.secondsPerPercentage);
            this.valveOpen();
            this.getIsBusyChannel().setNextValue(true);
            //Making sure to wait the correct time even if it is already opening
            this.timeStampValveInitial = -1;
            this.updatePowerLevel();
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
    private void checkValveChannelCorrect() {
        if (this.useCheckChannel) {
            try {
                boolean channelValueIsTrue;
                boolean channelValueIsNotFalse;

                Channel<?> channelToCheckForTrue;
                Channel<?> channelToCheckForFalse;
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
                        this.getPowerLevelChannel().setNextValue(this.powerLevelBeforeUpdate);
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

    private Channel<?> getInputChannel(ChannelToGet channelToGet) throws OpenemsError.OpenemsNamedException {
        switch (this.configurationType) {
            case CHANNEL:
                return this.cpm.getChannel(channelToGet.equals(ChannelToGet.CLOSING)
                        ? this.cpm.getChannel(this.inputClosingAddress) : this.cpm.getChannel(this.inputOpenAddress));
            case DEVICE:
            default:
                return channelToGet.equals(ChannelToGet.CLOSING) ? this.closeRelay.getRelaysReadChannel() : this.openRelay.getRelaysReadChannel();
        }
    }


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
                        throw new ConfigurationException("CheckChannelValue: " + super.id() + channelToCheck.toString(), "Channel Cannot be parsed to a Numeric Value");
                    }
                default:
                    throw new ConfigurationException("CheckChannelValue: " + super.id() + channelToCheck.toString(), "ValueType not supported!");
            }
        } else {
            return false;
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }
}

