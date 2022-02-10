package io.openems.edge.heatsystem.components.valve;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
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
 * It either works with 1 Aio/Pwm or 1 ChannelAddresses.
 * It can update it's opening/closing state and shows up the percentage value of itself.
 * To check if the output is correct, you can configure the CheckUp.
 * E.g. The Consolinno Leaflet reads the MCP and it's status, this will be send into the {@link AnalogInputOutput#getThousandthCheckChannel()}
 * If the value you read is the expected value, everything is ok, otherwise the Components tries to set the expected values again.
 */
@Designate(ocd = ConfigValveOneOutput.class, factory = true)
@Component(name = "HeatsystemComponent.Valve.OneOutput", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS}

)

public class ValveOneOutput extends AbstractValve implements OpenemsComponent, HydraulicComponent, ExceptionalState, EventHandler {

    private final Logger log = LoggerFactory.getLogger(ValveOneOutput.class);

    private ChannelAddress outputChannel;
    private ChannelAddress optionalCheckOutputChannel;
    private ConfigurationType configurationType;
    private DeviceType deviceType;
    private Pwm valvePwm;
    private AnalogInputOutput valveAio;
    private ConfigValveOneOutput config;
    private boolean validCheckOutputChannel;
    private boolean powerValueReachedBeforeCheck = false;

    @Reference
    ComponentManager cpm;

    AtomicReference<Cycle> cycle = new AtomicReference<>();


    public ValveOneOutput() {
        super(OpenemsComponent.ChannelId.values(),
                HydraulicComponent.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    enum DeviceType {
        PWM, AIO;
    }

    @Activate
    void activate(ComponentContext context, ConfigValveOneOutput config) {
        try {
            super.activate(context, config.id(), config.alias(), config.enabled());
            this.config = config;

            this.activationOrModifiedRoutine(config);
            if (config.shouldCloseOnActivation()) {
                this.getPowerLevelChannel().setNextValue(0);
                this.getPowerLevelChannel().nextProcessImage();
                this.setPointPowerLevelChannel().setNextWriteValueFromObject(0);
                this.forceClose();
            } else if (config.shouldOpenOnActivation()) {
                this.getPowerLevelChannel().setNextValue(0);
                this.getPowerLevelChannel().nextProcessImage();
                this.setPointPowerLevelChannel().setNextWriteValueFromObject(100);
                this.forceOpen();
            }
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configSuccess = false;
            this.log.warn("Couldn't apply Config. Components may not be initialized yet This: "
                    + super.id() + " will try again later.");
        }
    }

    /**
     * Sets the basic Configuration on activation or modification.
     *
     * @param config the config of this component.
     * @throws ConfigurationException             if something is configured in a wrong way (e.g. instanceof is wrong)
     * @throws OpenemsError.OpenemsNamedException if Components with the given ID or ChannelAddresses cannot be found/are wrong.
     */
    private void activationOrModifiedRoutine(ConfigValveOneOutput config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.useCheckOutput = config.useCheckChannel();
        this.configurationType = config.configurationType();
        this.getCycle();
        switch (this.configurationType) {
            case CHANNEL:
                try {
                    this.outputChannel = ChannelAddress.fromString(config.inputChannelOrDevice());

                    if (this.useCheckOutput) {
                        this.optionalCheckOutputChannel = ChannelAddress.fromString(config.checkChannel());
                    } else {
                        this.optionalCheckOutputChannel = null;
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    throw new ConfigurationException("Activate : " + super.id(), "ChannelAddresses are configured in a wrong Way");
                }
                break;
            case DEVICE:
                OpenemsComponent component = this.cpm.getComponent(config.inputChannelOrDevice());
                if (component instanceof Pwm) {
                    this.valvePwm = (Pwm) component;
                    this.deviceType = DeviceType.PWM;
                } else if (component instanceof AnalogInputOutput) {
                    this.valveAio = (AnalogInputOutput) component;
                    this.deviceType = DeviceType.AIO;
                } else {
                    throw new ConfigurationException("Activate : " + super.id(), "OpenEmsComponent "
                            + config.inputChannelOrDevice() + " is not an instance of an expected Device!");
                }
                break;
        }
        this.secondsPerPercentage = ((double) config.timeToOpenValve() / 100.d);
        this.timeChannel().setNextValue(0);
        super.createTimerHandler(config.timerId(), config.maxTime(), this.cpm, this, config.useExceptionalState());
        int deltaMaxTime = Cycle.DEFAULT_CYCLE_TIME;
        if (this.cycle.get() != null) {
            deltaMaxTime = this.cycle.get().getCycleTime();
        }
        super.percentPossiblePerCycle = deltaMaxTime / (this.secondsPerPercentage * MILLI_SECONDS_TO_SECONDS);
        super.createTimerHandler(config.timerId(), config.maxTime(), this.cpm, this, config.useExceptionalState());
        this.configSuccess = true;
    }

    /**
     * Get the Current active {@link Cycle} and set as Reference
     */
    private void getCycle() {
        Optional<OpenemsComponent> cycleOptional = this.cpm.getAllComponents().stream().filter(component -> component instanceof Cycle).findAny();
        cycleOptional.ifPresent(component -> this.cycle.set((Cycle) component));
        super.setCycle(this.cycle.get());
    }

    @Modified
    void modified(ComponentContext context, ConfigValveOneOutput config) throws ConfigurationException {
        this.configSuccess = false;
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        try {
            this.activationOrModifiedRoutine(config);
        } catch (OpenemsError.OpenemsNamedException e) {
            this.configSuccess = false;
            this.log.warn("Couldn't apply Config. Components may not be initialized yet This: "
                    + super.id() + " will try again later.");
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }


    /**
     * Changes Valve Position by incoming percentage.
     * Only executes if valve is not busy! (was not forced to open/close)
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
        double futurePowerLevel;
        if (super.changeInvalid(percentage)) {
            return false;
        } else {
            //Setting the oldPowerLevel and adjust the percentage Value
            futurePowerLevel = super.calculateCurrentPowerLevelAndSetTime(percentage);
            if (futurePowerLevel < 0) {
                return false;
            }
            this.writeToOutputChannel(Math.round(futurePowerLevel));
            boolean prevClosing = this.isClosing;
            this.isClosing = percentage < DEFAULT_MIN_POWER_VALUE;
            this.isChanging = true;
            if (prevClosing != this.isClosing) {
                this.timeStampValveCurrent = -1;
            }
            return true;
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
                this.writeToOutputChannel(DEFAULT_MIN_POWER_VALUE);
            }
        }

    }

    /**
     * Writes a percent Value to an output channel.
     *
     * @param percent the SetPoint
     */
    private void writeToOutputChannel(double percent) {
        try {
            Channel<?> channelToWrite;
            if (this.configurationType.equals(ConfigurationType.CHANNEL)) {
                channelToWrite = this.cpm.getChannel(this.outputChannel);
            } else {
                if (this.deviceType.equals(DeviceType.AIO)) {
                    channelToWrite = this.valveAio.setWriteThousandthChannel();
                } else {
                    channelToWrite = this.valvePwm.getWritePwmPowerLevelChannel();
                }
            }
            int scaleFactor = channelToWrite.channelDoc().getUnit().equals(Unit.THOUSANDTH) ? 10 : 1;
            if (channelToWrite instanceof WriteChannel<?>) {
                ((WriteChannel<?>) channelToWrite).setNextWriteValueFromObject(percent * scaleFactor);
            } else {
                channelToWrite.setNextValue(percent * scaleFactor);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Couldn't write into Channel! Might not exist: " + this.outputChannel);
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
            if (super.parentForceOpen()) {
                this.writeToOutputChannel(DEFAULT_MAX_POWER_VALUE);
            }
        }
    }


    /**
     * Check if the expected Value is almost the same as the written Value, if not -> Set Value again.
     *
     * @param optionalChannel the OptionalChannel to Check.
     * @return true if expected Value is almost the Same (Or if an Error occurred, so the Valve can continue to run)
     */

    private boolean checkOutputIsEqualToGoalPercent(Channel<?> optionalChannel) {

        if (this.useCheckOutput) {
            try {
                //Prepare and get all Values needed.
                optionalChannel = this.getOptionalChannel();
                Value<?> otherChannelValue = optionalChannel.value().isDefined() ? optionalChannel.value() : optionalChannel.getNextValue().isDefined() ? optionalChannel.getNextValue() : null;
                int scaleDownFactorOtherChannel = this.getScaleFactor(optionalChannel);

                Value<Double> goalPowerLevelValue = this.futurePowerLevelChannel().value().isDefined() ? this.futurePowerLevelChannel().value() : null;
                int scaleDownFactorGoalPowerLevel = this.getScaleFactor(this.futurePowerLevelChannel());
                if (otherChannelValue != null && goalPowerLevelValue != null) {
                    if (this.containsOnlyNumbers(otherChannelValue.get().toString()) && this.containsOnlyNumbers(goalPowerLevelValue.get().toString())) {
                        this.validCheckOutputChannel = true;
                        double otherValue = Double.parseDouble(otherChannelValue.get().toString());
                        double goalValue = Double.parseDouble(goalPowerLevelValue.get().toString());
                        if (Math.abs(otherValue / scaleDownFactorOtherChannel - goalValue / scaleDownFactorGoalPowerLevel) > TOLERANCE) {
                            this.getPowerLevelChannel().setNextValue(otherValue / scaleDownFactorOtherChannel);
                            this.setPointPowerLevelChannel().setNextWriteValueFromObject(goalValue / scaleDownFactorGoalPowerLevel);
                            this.isChanging = true;
                            this.timeStampValveCurrent = -1;
                            return false;
                        }
                    } else {
                        this.validCheckOutputChannel = false;
                        this.log.warn("Couldn't compare Values of " + super.id() + " Other Value Channel does not contain only numbers!");
                    }

                } else {
                    return true;
                }
            } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
                this.log.error("Couldn't get or write into Channel: Check channelAddress: " + optionalChannel);
                return true;
            }

        }
        return true;
    }

    /**
     * Gets the check output channel.
     * On Device -> get the ReadChannel of Pwm or AIO. Otherwise return given OutputChannel.
     *
     * @return the Channel for inspection
     * @throws OpenemsError.OpenemsNamedException if channel cannot be found
     * @throws ConfigurationException             this error shouldn't occur and is a "default" value
     */
    private Channel<?> getOptionalChannel() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        switch (this.configurationType) {
            case DEVICE:
                switch (this.deviceType) {
                    case PWM:
                        return this.valvePwm.getReadPwmPowerLevelChannel();

                    case AIO:
                        return this.valveAio.getThousandthCheckChannel();
                }
                break;
            case CHANNEL:
            default:
                return this.cpm.getChannel(this.optionalCheckOutputChannel);
        }
        throw new ConfigurationException("Get Optional Channel", "This Error shouldn't occur, there should always be a configuration Type at this point");
    }

    /**
     * Gets the ScaleFactor of a Channel. Since the Valve works with Percent and another Channel may use
     * Thousandth -> a ScaleFactor of 10 (value*10 not value*10^10) is applied.
     *
     * @param channel the channel.
     * @return the Scalefactor for the value.
     */
    private int getScaleFactor(Channel<?> channel) {
        return channel.channelDoc().getUnit().equals(Unit.THOUSANDTH) ? 10 : 1;
    }

    /**
     * Resets the Valve -> Force Closes it.
     * Can be useful if something weird is happening or fi the Valve was deactivated for some reason and got reactivated again.
     */
    @Override
    public void reset() {
        this.forceClose();
    }

    /**
     * <p> The OneOutputValve works in a similar Way to the ValveTwoOutput.
     * It has a "FuturePowerLevel" That needs to be reached.
     * The Valve checks its output. If it is not reached -> adapt the value.
     * <p>
     * Otherwise the Parent will call the "ChangeByPercentage" Method and this allows the Valve to write a PowerLevel into the outPutAddress.
     * </p>
     *
     * @param event the OpenEMS Event, either After_Process_Image, or After_Controllers
     */
    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            if (this.configSuccess == false) {
                try {
                    this.activationOrModifiedRoutine(this.config);
                    this.configSuccess = true;
                } catch (ConfigurationException | OpenemsError.OpenemsNamedException e) {
                    this.configSuccess = false;
                    this.log.error("Config is Wrong in : " + super.id() + " Please reconfigure!");
                }
            } else if (this.powerValueReachedBeforeCheck) {
                if (this.configurationType.equals(ConfigurationType.DEVICE) && super.timerHandler.checkTimeIsUp(CHECK_COMPONENT_IDENTIFIER)) {
                    this.checkForMissingComponents();
                    this.timerHandler.resetTimer(CHECK_COMPONENT_IDENTIFIER);
                }
                Channel<?> optionalChannel;
                try {
                    optionalChannel = this.getOptionalChannel();
                } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
                    this.log.warn("Couldn't receive optional Check Output Channel! " + super.id());
                    return;
                }
                boolean check = this.checkOutputIsEqualToGoalPercent(optionalChannel);
                if (check == false) {
                    //Set PowerLevel To "correct" Output -> if Output says 10% it is 10%; since it is not timebased in comparison to the ValveTwoRelay
                    if (this.validCheckOutputChannel) {
                        Value<?> captureValueOfChannel = this.getValueOfOptionalChannel(optionalChannel);
                        if (captureValueOfChannel != null) {
                            Unit otherUnit = optionalChannel.channelDoc().getUnit();
                            Unit powerLevelUnit = this.getPowerLevelChannel().channelDoc().getUnit();
                            double scaleDownFactor = 1;
                            double value = Double.parseDouble(captureValueOfChannel.get().toString());
                            if (otherUnit.equals(powerLevelUnit) == false) {
                                //powerLevel is PERCENT but
                                if (powerLevelUnit.equals(Unit.PERCENT) && otherUnit.equals(Unit.THOUSANDTH)) {
                                    scaleDownFactor = 10;
                                } else if (powerLevelUnit.equals(Unit.THOUSANDTH) && otherUnit.equals(Unit.PERCENT)) {
                                    scaleDownFactor = 0.1d;
                                }
                            }
                            this.getPowerLevelChannel().setNextValue(value / scaleDownFactor);
                        }

                    }
                    this.powerValueReachedBeforeCheck = false;
                }
            }

        }
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS) && this.configSuccess) {
            if (this.parentDidRoutine() == false) {
                if (this.powerLevelReached()) {
                    this.powerValueReachedBeforeCheck = true;
                }
            }
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
            switch (this.deviceType) {

                case PWM:
                    if (this.valvePwm != null) {
                        component = this.cpm.getComponent(this.valvePwm.id());
                        if (component instanceof Pwm && !component.equals(this.valvePwm)) {
                            this.valvePwm = (Pwm) component;
                        }
                    }
                    break;
                case AIO:
                default:
                    if (this.valveAio != null) {
                        component = this.cpm.getComponent(this.valveAio.id());
                        if (component instanceof AnalogInputOutput && !component.equals(this.valveAio)) {
                            this.valveAio = (AnalogInputOutput) component;
                        }
                    }
                    break;
            }
            if (this.cycle.get() == null || (this.cycle.get().equals(this.cpm.getComponent(this.cycle.get().id())) == false)) {
                this.getCycle();
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't check for missing Components. Reason: " + e.getMessage());
        }
    }
}
