package io.openems.edge.heatsystem.components.pump;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heatsystem.components.ConfigurationType;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.heatsystem.components.PumpType;
import io.openems.edge.io.api.AnalogInputOutput;
import io.openems.edge.io.api.Pwm;
import io.openems.edge.io.api.Relay;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
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
 * This simple Pump can be configured and used by either a Pwm/Aio or a Relay or Both.
 * It works with Channels as well. You still need to configure if the Pump is controlled by a Relay, Pwm/aio or Both.
 * It provides the ability to check it's output and apply an Output again if the output does not match.
 * Additionally you can use the ExceptionalState to apply an Output.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "HeatsystemComponent.Hydraulic.Pump",
        property = {
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS
        })
public class PumpImpl extends AbstractOpenemsComponent implements OpenemsComponent, HydraulicComponent, ExceptionalState, EventHandler {
    private final Logger log = LoggerFactory.getLogger(PumpImpl.class);

    private Relay relay;
    private Pwm pwm;
    private AnalogInputOutput aio;

    private ChannelAddress relayChannel;
    private ChannelAddress percentageChannel;
    private boolean isRelay = false;
    private boolean isPwmOrAio = false;
    private ConfigurationType configurationType;
    private DeviceType deviceType;
    private boolean shouldCheckOutput;
    private ChannelAddress checkRelayChannel;
    private ChannelAddress checkPwmOrAioChannel;
    // 10% tolerance
    private static final int TOLERANCE = 10;
    TimerHandler timerHandler;
    private ExceptionalStateHandler exceptionalStateHandler;
    private boolean useExceptionalState;
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "EXCEPTIONAL_STATE_IDENTIFIER_HYDRAULIC_PUMP";
    private boolean configSuccess;
    Config config;

    enum DeviceType {
        PWM, AIO
    }

    enum AvailableDevices {
        PWM, AIO, RELAY
    }

    @Reference
    ComponentManager cpm;

    public PumpImpl() {
        super(OpenemsComponent.ChannelId.values(),
                HydraulicComponent.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        try {
            this.activateOrModifiedRoutine(config);
            this.configSuccess = true;
            this.deactivateDevices();
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configSuccess = false;
        }
        this.getPowerLevelChannel().setNextValue(0);
        this.getPowerLevelChannel().nextProcessImage();
        this.getIsBusyChannel().setNextValue(false);
        if (config.useDefault()) {
            this.setPowerLevel(config.defaultPowerLevel());
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        this.isPwmOrAio = false;
        this.isRelay = false;
        this.pwm = null;
        this.relay = null;
        this.activateOrModifiedRoutine(config);
    }

    /**
     * Deactivates the Devices. It sets the Relay to false and the Pwm/aio to 0.
     */
    private void deactivateDevices() {
        if (this.isRelay) {
            this.controlRelay(false);
        }
        if (this.isPwmOrAio) {
            this.controlPercentDevice(DEFAULT_MIN_POWER_VALUE);
        }
    }


    /**
     * This method is called by the activate and modified method.
     * It allocates the configurationType, tells the Class if it uses a Relay and/or Pwm/Aio
     * Configures the ExceptionalStateHandler (if needed).
     *
     * @param config The Config of this Component.
     */
    private void activateOrModifiedRoutine(Config config)
            throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.checkPwmOrAioChannel = null;
        this.checkRelayChannel = null;
        this.configurationType = config.configType();
        PumpType pumpType = config.pump_Type();
        this.shouldCheckOutput = config.checkPowerLevelIsApplied();
        switch (pumpType) {
            case RELAY:
                this.isRelay = true;
                break;
            case PWM_OR_AIO:
                this.isPwmOrAio = true;
                break;

            case RELAY_AND_PWM_OR_AIO:
            default:
                this.isRelay = true;
                this.isPwmOrAio = true;
                break;
        }

        if (this.isRelay) {
            this.configureRelay(config);
        }
        if (this.isPwmOrAio) {
            this.configurePwmOrAio(config);
        }
        if (this.timerHandler != null) {
            this.timerHandler.removeComponent();
        }
        this.useExceptionalState = config.useExceptionalState();
        this.timerHandler = new TimerHandlerImpl(this.id(), this.cpm);
        this.timerHandler.addOneIdentifier(CHECK_COMPONENT_IDENTIFIER, config.timerIdMissingComponents(), WAIT_TIME_CHECK_COMPONENTS);
        if (this.useExceptionalState) {
            this.timerHandler.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, config.timerId(), config.maxTime());
            this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(this.timerHandler, EXCEPTIONAL_STATE_IDENTIFIER);
        }
    }

    /**
     * Configures the Pwm either by Device or Channel.
     *
     * @param config the Config of this Component
     * @throws OpenemsError.OpenemsNamedException if either the Address or Device by the pump_pwm couldn't be found
     * @throws ConfigurationException             if either the Channel is not a WriteChannel or the Device is not a Pwm.
     */
    private void configurePwmOrAio(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        switch (this.configurationType) {
            case CHANNEL:
                ChannelAddress channelAddress = ChannelAddress.fromString(config.pump_Pwm_or_Aio());
                Channel<?> pwmChannelByAddress = this.cpm.getChannel(channelAddress);
                if (pwmChannelByAddress instanceof WriteChannel<?>) {
                    this.percentageChannel = channelAddress;
                } else {
                    throw new ConfigurationException("Configure Pump in : " + super.id(), "Channel is not a WriteChannel");
                }
                if (this.shouldCheckOutput) {
                    this.checkPwmOrAioChannel = ChannelAddress.fromString(config.checkPwmOrAioChannelAddress());
                }
                break;
            case DEVICE:
                OpenemsComponent openemsComponent = this.cpm.getComponent(config.pump_Pwm_or_Aio());
                if (openemsComponent instanceof Pwm) {
                    this.pwm = (Pwm) openemsComponent;
                    this.deviceType = DeviceType.PWM;
                    //reset pwm to 0; so pump is on activation off
                    this.pwm.getWritePwmPowerLevelChannel().setNextWriteValueFromObject(HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
                } else if (openemsComponent instanceof AnalogInputOutput) {
                    this.aio = (AnalogInputOutput) openemsComponent;
                    this.deviceType = DeviceType.AIO;
                    this.aio.setWriteThousandthChannel().setNextWriteValueFromObject(HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
                } else {
                    throw new ConfigurationException("ConfigurePwmOrAio in " + super.id(), "Component instance is not an "
                            + "expected device. Make sure to configure a Pwm or Aio Device.");
                }
                break;
        }
    }

    /**
     * Configures the Relay either by Device or Channel (Boolean).
     *
     * @param config the Config of this Component.
     * @throws OpenemsError.OpenemsNamedException thrown if the Id of Device/Channel couldn't be found.
     * @throws ConfigurationException             if the id of the Device is not an instance of a relay.
     */

    private void configureRelay(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        switch (this.configurationType) {
            case CHANNEL:
                ChannelAddress relayAddress = ChannelAddress.fromString(config.pump_Relay());
                Channel<?> relayChannelByAddress = this.cpm.getChannel(relayAddress);
                if (relayChannelByAddress instanceof WriteChannel<?>) {
                    this.relayChannel = relayAddress;
                } else {
                    throw new ConfigurationException("Configure Relay in : " + super.id(), "Channel is not a WriteChannel");
                }
                if (this.shouldCheckOutput) {
                    this.checkRelayChannel = ChannelAddress.fromString(config.checkRelayChannelAddress());
                }
                break;
            case DEVICE:
                OpenemsComponent relayComponent = this.cpm.getComponent(config.pump_Relay());
                if (relayComponent instanceof Relay) {
                    this.relay = (Relay) relayComponent;
                } else {
                    throw new ConfigurationException("Configure Relay in " + super.id(), "Allocated relay, not a (configured) relay-device.");
                }
                break;
        }
    }


    /**
     * Deactivates the Pump.
     */
    @Deactivate
    protected void deactivate() {
        super.deactivate();
        this.deactivateDevices();
    }

    /**
     * Called internally or by other Components.
     * Tells the calling device if the HeatsystemComponent is ready to apply any Changes.
     *
     * @return true, since a Pump is always allowed to change.
     */

    @Override
    public boolean readyToChange() {
        return true;
    }

    /**
     * Changes the power value by percentage.
     * <p>
     * If the Pump is only a relays --> if negative --> controlRelays false, else true
     * If it's in addition a pwm/aio --> check if the PowerLevel - percentage <= 0
     * --> pump is idle --> relays off and Pwm/Aio is 0 %
     * Otherwise it's calculating the new Power-Level and writing
     * the old power-level in the LastPowerLevel Channel and apply the new % value to the Pwm/Aio
     * </p>
     *
     * @param percentage to adjust the current powerLevel.
     * @return successful boolean
     */
    @Override
    public boolean changeByPercentage(double percentage) {
        double powerLevel = this.getPowerLevelValue();
        boolean changeSuccess = false;
        //Return if no percentage Change is made while PowerLevelValue is Defined -> no changes made
        if (percentage == 0 && this.getPowerLevelChannel().value().isDefined()) {
            return false;
        }
        if (this.isRelay) {
            if (this.isPwmOrAio) {

                //deactivate
                if ((powerLevel + percentage <= DEFAULT_MIN_POWER_VALUE)) {
                    if (this.controlRelay(false) && this.controlPercentDevice(DEFAULT_MIN_POWER_VALUE)) {
                        this.getLastPowerLevelChannel().setNextValue(powerLevel);
                        this.getPowerLevelChannel().setNextValue(DEFAULT_MIN_POWER_VALUE);
                        changeSuccess = true;
                    } else {
                        changeSuccess = false;
                    }
                } else {
                    //activate relay, set Pwm Later
                    this.controlRelay(true);
                }
            } else {
                changeSuccess = this.controlRelay((powerLevel + percentage <= DEFAULT_MIN_POWER_VALUE) == false);
            }
        }
        powerLevel += percentage;
        powerLevel = Math.max(DEFAULT_MIN_POWER_VALUE, powerLevel);
        powerLevel = Math.min(DEFAULT_MAX_POWER_VALUE, powerLevel);
        //sets pwm/aio
        if (this.isPwmOrAio && (this.isRelay == false || changeSuccess == false)) {
            changeSuccess = this.controlPercentDevice(powerLevel);
        }
        if (changeSuccess) {
            this.getLastPowerLevelChannel().setNextValue(this.getPowerLevelValue());
            this.getPowerLevelChannel().setNextValue(powerLevel);
        }
        return changeSuccess;
    }

    /**
     * Sets the Relay(Channel) to either true or false, depending on the {@link #changeByPercentage(double percentage)}.
     *
     * @param activate if the relay should be active or not
     * @return true on success.
     */
    private boolean controlRelay(boolean activate) {
        if (this.isPwmOrAio == false) {
            if (activate) {
                this.getPowerLevelChannel().setNextValue(DEFAULT_MAX_POWER_VALUE);
            } else {
                this.getPowerLevelChannel().setNextValue(DEFAULT_MIN_POWER_VALUE);
            }
        }
        switch (this.configurationType) {
            case CHANNEL:
                try {
                    Channel<?> channel = this.cpm.getChannel(this.relayChannel);
                    if (channel instanceof WriteChannel<?>) {
                        ((WriteChannel<?>) channel).setNextWriteValueFromObject(activate);
                    } else {
                        channel.setNextValue(activate);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write into Channel; Pump: " + super.id() + "Channel : " + this.relayChannel.toString());
                    return false;
                }
                break;
            case DEVICE:
                try {
                    this.relay.getRelaysWriteChannel().setNextWriteValueFromObject(activate);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write into Channel; Pump: " + super.id() + "Device: " + this.relay.id());
                    return false;
                }
                break;
        }
        return true;
    }

    /**
     * Sets the Pwm Value Depending on the Unit of the Channel. Called by {@link #changeByPercentage(double)}
     *
     * @param percent the Percent set to this Pump
     * @return true on success
     */
    private boolean controlPercentDevice(double percent) {
        int multiplier = 1;
        Unit unit;
        if (this.configurationType.equals(ConfigurationType.CHANNEL)) {
            try {
                unit = this.cpm.getChannel(this.percentageChannel).channelDoc().getUnit();
            } catch (OpenemsError.OpenemsNamedException e) {
                unit = Unit.THOUSANDTH;
            }
        } else {
            switch (this.deviceType) {
                case PWM:
                    unit = this.pwm.getWritePwmPowerLevelChannel().channelDoc().getUnit();
                    break;
                case AIO:
                default:
                    unit = this.aio.setWriteThousandthChannel().channelDoc().getUnit();
                    break;
            }
        }
        if (unit == Unit.THOUSANDTH) {
            multiplier = 10;
        }
        int percentToApply = (int) (percent * multiplier);
        percentToApply = percentToApply > DEFAULT_MAX_POWER_VALUE * multiplier ? DEFAULT_MAX_POWER_VALUE : Math.max(percentToApply, 0);

        switch (this.configurationType) {
            case CHANNEL:
                try {
                    Channel<?> channel = this.cpm.getChannel(this.percentageChannel);
                    if (channel instanceof WriteChannel<?>) {
                        ((WriteChannel<?>) channel).setNextWriteValueFromObject(percentToApply);
                    } else {
                        channel.setNextValue(percentToApply);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't apply PwmValue for Pump: " + super.id() + " Value: " + percentToApply);
                    return false;
                }
                break;
            case DEVICE:
                try {
                    switch (this.deviceType) {

                        case PWM:
                            this.pwm.getWritePwmPowerLevelChannel().setNextWriteValueFromObject(percentToApply);
                            break;
                        case AIO:
                            this.aio.setWriteThousandthChannel().setNextWriteValueFromObject(percentToApply);
                            break;
                    }

                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't apply percentValue for Pump: " + super.id() + " Value: " + percentToApply);
                    return false;
                }
                break;
        }
        return true;
    }

    @Override
    public void forceClose() {
        this.setPowerLevel(HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
    }

    @Override
    public void forceOpen() {
        this.setPowerLevel(HydraulicComponent.DEFAULT_MAX_POWER_VALUE);
    }

    @Override
    public boolean powerLevelReached() {
        return false;
    }

    @Override
    public boolean isChanging() {
        return false;
    }

    @Override
    public void reset() {
        this.setPowerLevel(HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
    }

    /**
     * Sets the PowerLevel of the Pump. Values between 0-100% can be applied.
     *
     * @param powerLevelToApply the PowerLevel the Pump should be set to.
     */

    @Override
    public boolean setPowerLevel(double powerLevelToApply) {
        if (powerLevelToApply >= DEFAULT_MIN_POWER_VALUE && powerLevelToApply != this.getPowerLevelValue()) {
            double changeByPercent = powerLevelToApply - getPowerLevelValue();
            this.changeByPercentage(changeByPercent);
            return true;
        }
        return false;
    }


    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled()) {
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
                if (this.configSuccess) {
                    if (this.shouldCheckOutput && this.getPowerLevelChannel().value().isDefined()) {
                        try {
                            this.checkIfPowerValueIsMatching();
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.warn("Couldn't check if PowerValue is correctly set! " + super.id());
                        }
                    }
                    if (this.configurationType.equals(ConfigurationType.DEVICE) && this.timerHandler.checkTimeIsUp(CHECK_COMPONENT_IDENTIFIER)) {
                        this.checkForMissingComponents();
                        this.timerHandler.resetTimer(CHECK_COMPONENT_IDENTIFIER);
                    }
                } else {
                    try {
                        this.activateOrModifiedRoutine(this.config);
                        this.configSuccess = true;
                    } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
                        this.log.warn("Couldn't apply Config yet!");
                        this.configSuccess = false;
                    }
                }

            } else if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS) && this.configSuccess) {
                if (this.useExceptionalState && this.exceptionalStateHandler.exceptionalStateActive(this)) {
                    int value = this.getExceptionalStateValue();
                    value = Math.max(value, DEFAULT_MIN_EXCEPTIONAL_VALUE);
                    this.setPowerLevel(value);
                } else if (this.getResetValueAndResetChannel()) {
                    this.setPowerLevel(DEFAULT_MIN_POWER_VALUE);
                } else if (this.getForceFullPowerAndResetChannel()) {
                    this.setPowerLevel(DEFAULT_MAX_POWER_VALUE);
                    //next Value bc Controller will set the next Value -> no need to wait a full cycle
                } else if (this.setPointPowerLevelChannel().getNextValue().isDefined()) {
                    this.setPowerLevel(this.setPointPowerLevelChannel().getNextValue().get());
                    this.setPointPowerLevelChannel().setNextValue(null);
                }
            }
        }
    }

    /**
     * This Method will only be called, when someone configured the Pump with the {@link ConfigurationType#DEVICE}.
     * It sometimes happens, that devices restart or get deactivated etc. The Pump will check for old references and refreshes them
     * every 30 deltaTime (Depends on the Timer).
     */
    private void checkForMissingComponents() {
        if (this.isRelay) {
            try {
                OpenemsComponent component = this.cpm.getComponent(this.relay.id());
                if (component instanceof Relay) {
                    Relay otherRelay = (Relay) component;
                    if (!otherRelay.equals(this.relay)) {
                        this.relay = otherRelay;
                    }
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't find Relay with id: " + this.relay.id());
            }
            if (this.isPwmOrAio) {
                OpenemsComponent foundComponent;
                try {
                    switch (this.deviceType) {

                        case PWM:
                            foundComponent = this.cpm.getComponent(this.pwm.id());
                            if (foundComponent instanceof Pwm && !foundComponent.equals(this.pwm)) {
                                this.pwm = (Pwm) foundComponent;
                            }
                            break;
                        case AIO:
                        default:
                            foundComponent = this.cpm.getComponent(this.aio.id());
                            if (foundComponent instanceof AnalogInputOutput && !foundComponent.equals(this.aio)) {
                                this.aio = (AnalogInputOutput) foundComponent;
                            }
                            break;
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't find PWM or AIO");
                }
            }
        }
    }

    /**
     * This method is called if the CheckOutput was set in the config {@link Config#checkPowerLevelIsApplied()} was set.
     * The Component checks depending on it's type the expected Values.
     * If not, it applies / controls the devices depending on the expected value.
     *
     * @throws OpenemsError.OpenemsNamedException if Channel couldn't be found.
     */
    private void checkIfPowerValueIsMatching() throws OpenemsError.OpenemsNamedException {
        double currentPowerLevel = this.getPowerLevelValue();
        boolean anticipatedValueCorrect = true;
        if (this.isRelay) {
            boolean expectedBooleanValue = currentPowerLevel > DEFAULT_MIN_EXCEPTIONAL_VALUE;
            Channel<?> channel = this.getChannelForCheckup(AvailableDevices.RELAY);
            if (channel != null && channel.value().isDefined()) {
                switch (channel.channelDoc().getType()) {
                    case BOOLEAN:
                        anticipatedValueCorrect = expectedBooleanValue == (Boolean) channel.value().get();
                        break;
                    case SHORT:
                    case INTEGER:
                    case LONG:
                    case FLOAT:
                    case DOUBLE:
                        anticipatedValueCorrect = expectedBooleanValue == (Double) channel.value().get() > DEFAULT_MIN_POWER_VALUE;
                        break;
                    case STRING:
                        if (this.containsOnlyNumbers(channel.value().get().toString())) {
                            anticipatedValueCorrect = expectedBooleanValue == Double.parseDouble(channel.value().get().toString()) > DEFAULT_MIN_POWER_VALUE;
                        } else {
                            this.log.warn("Couldn't check if Relay value is set correctly, Channel has non Numeric content "
                                    + super.id() + channel.toString());
                        }
                        break;
                }
                //
                if (anticipatedValueCorrect == false) {
                    this.log.info("Adapted value in " + super.id() + " Value was: " + channel.value().get() + " but expected: " + expectedBooleanValue);
                    this.controlRelay(currentPowerLevel > 0);
                }
            } else {
                this.log.info("Couldn't check for anticipated Value! Value is not defined yet: " + super.id() + channel);
            }
        }
        if (this.isPwmOrAio) {
            Channel<?> channel = this.getChannelForCheckup(this.deviceType);
            if (channel != null) {
                Unit channelUnit = channel.channelDoc().getUnit();
                Unit ownUnit = this.getPowerLevelChannel().channelDoc().getUnit();
                double scaleFactorForOtherChannel = 1;
                if (channelUnit.equals(Unit.THOUSANDTH) && ownUnit.equals(Unit.PERCENT)) {
                    scaleFactorForOtherChannel = 0.1d;
                } else if (channelUnit.equals(Unit.PERCENT) && ownUnit.equals(Unit.THOUSANDTH)) {
                    scaleFactorForOtherChannel = 10;
                }
                Double value = null;
                if (channel.value().isDefined()) {
                    switch (channel.channelDoc().getType()) {
                        case BOOLEAN:
                            value = (Boolean) channel.value().get() ? 100.d : 0.d;
                            break;
                        case SHORT:
                        case INTEGER:
                        case LONG:
                        case FLOAT:
                        case DOUBLE:
                        case STRING:
                            value = this.containsOnlyNumbers(channel.value().get().toString())
                                    ? Double.parseDouble(channel.value().get().toString()) : this.getPowerLevelValue();
                            break;
                        default:
                            this.log.warn("ChannelType is not supported!" + super.id() + "Channel: " + channel.toString());
                    }
                    if (Math.abs(value * scaleFactorForOtherChannel - currentPowerLevel) > TOLERANCE) {
                        this.log.info("PowerLevel of Pump: " + super.id() + " incorrect. Was: " + value + " but expected: " + currentPowerLevel);
                        this.controlPercentDevice(currentPowerLevel);
                    }
                } else {
                    this.log.info("Couldn't check for anticipated Value! Value is not defined yet: " + super.id() + channel.toString());
                }
            }
        }

    }

    /**
     * Overload method.
     * This Method calls {@link #getChannelForCheckup(AvailableDevices)} depending on the {@link DeviceType}
     *
     * @param deviceType the DeviceType, usually determined by Config and called in {@link #checkIfPowerValueIsMatching()}
     * @return the channel
     * @throws OpenemsError.OpenemsNamedException if channel cannot be found
     */

    private Channel<?> getChannelForCheckup(DeviceType deviceType) throws OpenemsError.OpenemsNamedException {
        switch (deviceType) {
            case PWM:
                return this.getChannelForCheckup(AvailableDevices.PWM);

            case AIO:
                return this.getChannelForCheckup(AvailableDevices.AIO);
        }
        return null;
    }

    /**
     * This method provides the Channel you need to check for the anticipated Value.
     * Depends on the {@link ConfigurationType} and the {@link AvailableDevices}.
     *
     * @param availableDevice the device that determines the Channel selected.
     * @return a Channel
     * @throws OpenemsError.OpenemsNamedException if channel cannot be found.
     */
    private Channel<?> getChannelForCheckup(AvailableDevices availableDevice) throws OpenemsError.OpenemsNamedException {

        switch (this.configurationType) {
            case CHANNEL:
                switch (availableDevice) {
                    case PWM:
                    case AIO:
                        return this.cpm.getChannel(this.checkPwmOrAioChannel);
                    case RELAY:
                        return this.cpm.getChannel(this.checkRelayChannel);
                }
                break;
            case DEVICE:
                switch (availableDevice) {
                    case PWM:
                        return this.pwm.getReadPwmPowerLevelChannel();
                    case AIO:
                        return this.aio.getThousandthCheckChannel();
                    case RELAY:
                        return this.relay.getRelaysReadChannel();
                }
                break;
        }
        return null;
    }

    @Override
    public String debugLog() {
        return super.id() + " PowerLevel is: " + this.getPowerLevelValue() + this.getPowerLevelChannel().channelDoc().getUnit().getSymbol()
                + " and should be around: " + this.getFuturePowerLevelValue() + this.futurePowerLevelChannel().channelDoc().getUnit().getSymbol() + "\n";
    }
}
