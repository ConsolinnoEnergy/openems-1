package io.openems.edge.heatsystem.components.pump.old;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.consolinno.pwm.api.Pwm;
import io.openems.edge.consolinno.relay.api.Relay;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heatsystem.components.api.PassingChannel;
import io.openems.edge.heatsystem.components.api.Pump;
import io.openems.edge.pwm.device.api.PwmPowerLevelChannel;
import io.openems.edge.relays.device.api.ActuatorRelaysChannel;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Passing.Pump",
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE})
public class PumpImpl extends AbstractOpenemsComponent implements OpenemsComponent, Pump, EventHandler, ExceptionalState {

    private static final Logger log = LoggerFactory.getLogger(PumpImpl.class);

    private ActuatorRelaysChannel relays;
    private Relay relay;
    private PwmPowerLevelChannel pwm;
    private Pwm pwmConsolinno;
    private boolean isRelays = false;
    private boolean isPwm = false;
    private boolean useCheckChannel;
    private ChannelAddress checkRelayChannel;
    private ChannelAddress checkPwmChannel;
    // 10% tolerance
    private static final int TOLERANCE = 10;
    private ExceptionalStateHandler exceptionalStateHandler;
    private boolean useExceptionalState;
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "EXCEPTIONAL_STATE_IDENTIFIER_HYDRAULIC_PUMP";
    private RelayType relayType = RelayType.UNDEFINED;

    @Reference
    ComponentManager cpm;

    public PumpImpl() {
        super(ChannelId.values(),
                PassingChannel.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    enum RelayType {
        DIRECT, MODBUS, UNDEFINED
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        allocateComponents(config.pump_Type(), config.pump_Relays(), config.pump_Pwm());
        if (config.checkPowerLevelIsApplied()) {
            this.useCheckChannel = true;
            if (this.isRelays) {
                this.checkRelayChannel = ChannelAddress.fromString(config.relayCheckChannelAddress());
            }
            if (this.isPwm) {
                this.checkPwmChannel = ChannelAddress.fromString(config.pwmCheckChannelAddress());
            }
        }
        this.getIsBusy().setNextValue(false);
        if (config.useDefault()) {
            this.getPowerLevel().setNextValue(0);
            this.setPowerLevel(config.defaultPowerLevel());
        }
        this.useExceptionalState = config.useExceptionalState();
        if (this.useExceptionalState) {
            TimerHandler timerHandler = new TimerHandlerImpl(this.id(), this.cpm);
            timerHandler.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, config.timerId(), config.maxTime());
            this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timerHandler, EXCEPTIONAL_STATE_IDENTIFIER);
        }
    }


    /**
     * Allocates the components.
     *
     * @param pump_type   is the pump controlled via realys, pwm or both.
     * @param pump_relays the unique id of the relays controlling the pump.
     * @param pump_pwm    unique id of the pwm controlling the pump.
     *
     *                    <p>Depending if it's a relays/pwm/both the Components will be fetched by the component-manager and allocated.
     *                    The relays and or pump will be off (just in case).
     */
    private void allocateComponents(PumpType pump_type, String pump_relays, String pump_pwm) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        switch (pump_type) {
            case RELAY:
                isRelays = true;
                break;
            case PWM:
                isPwm = true;
                break;
            case RELAY_AND_PWM:
            default:
                isRelays = true;
                isPwm = true;
                break;
        }

        if (isRelays) {
            OpenemsComponent relay = cpm.getComponent(pump_relays);
            if (relay instanceof ActuatorRelaysChannel) {
                this.relayType = RelayType.DIRECT;
                this.relays = (ActuatorRelaysChannel) relay;
            } else if (relay instanceof Relay) {
                this.relay = (Relay) relay;
                this.relayType = RelayType.MODBUS;
            } else {
                throw new ConfigurationException(pump_relays, "Allocated relays not a (configured) relays.");
            }
        }
        if (isPwm) {
            OpenemsComponent component = cpm.getComponent(pump_pwm);
            if (component instanceof PwmPowerLevelChannel) {
                this.pwm = (PwmPowerLevelChannel) component;
                //reset pwm to 0; so pump is on activation off
                this.pwm.getPwmPowerLevelChannel().setNextWriteValue(0.f);
            } else if (component instanceof Pwm) {
                this.pwmConsolinno = (Pwm) component;
            } else {
                throw new ConfigurationException(pump_pwm, "Allocated Pwm, not a (configured) pwm-device.");
            }
        }

    }


    /**
     * Deactivates the pump.
     * if the relays is a closer --> false is written --> open
     * if the relays is an opener --> true is written --> open
     * --> no voltage.
     */
    @Deactivate
    public void deactivate() {
        super.deactivate();
        try {
            if (this.isRelays) {
                if (this.relayType.equals(RelayType.DIRECT) && this.relays != null) {
                    this.relays.getRelaysWriteChannel().setNextWriteValue(false);
                } else if (this.relayType.equals(RelayType.MODBUS) && this.relay != null) {
                    this.relay.getRelaysWriteChannel().setNextWriteValueFromObject(false);
                }
            }
            if (this.isPwm) {
                if (this.pwm != null) {
                    this.pwm.getPwmPowerLevelChannel().setNextWriteValue(0.f);
                } else {
                    this.pwmConsolinno.getWritePwmPowerLevelChannel().setNextWriteValueFromObject(0);
                }
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            log.warn("Couldn't shut down Pump " + super.id() + " Reason: " + e.getMessage());
        }
    }

    @Override
    public boolean readyToChange() {
        //always available
        return true;
    }

    /**
     * Changes the powervalue by percentage.
     *
     * @param percentage to adjust the current powerlevel.
     * @return successful boolean
     * <p>
     * If the Pump is only a relays --> if negative --> controlyRelays false, else true
     * If it's in addition a pwm --> check if the powerlevel - percentage <= 0
     * --> pump is idle --> relays off and pwm is 0.f %
     * Otherwise it's calculating the new Power-level and writing
     * the old power-level in the LastPowerLevel Channel
     * </p>
     */
    @Override
    public boolean changeByPercentage(double percentage) {

        if (percentage == 0) {
            return false;
        }

        if (this.isRelays) {
            if (this.isPwm) {
                if ((this.getPowerLevel().getNextValue().get() + percentage <= 0)) {
                    this.getLastPowerLevel().setNextValue(this.getPowerLevel().getNextValue().get());
                    this.getPowerLevel().setNextValue(0);
                    try {
                        if (this.pwm != null) {
                            this.pwm.getPwmPowerLevelChannel().setNextWriteValue(0.f);
                        } else {
                            this.pwmConsolinno.getWritePwmPowerLevelChannel().setNextWriteValueFromObject(0);
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        log.warn("Couldn't write into Channel, Reason: " + e.getMessage());
                        return false;
                    }
                    controlRelays(false, "");
                    return true;
                } else {
                    controlRelays(true, "");
                }
            } else if (percentage <= 0) {
                this.getPowerLevel().setNextValue(0);
                controlRelays(false, "");
            } else {
                this.getPowerLevel().setNextValue(100);
                controlRelays(true, "");
            }
        }
        if (this.isPwm) {
            double currentPowerLevel;
            currentPowerLevel = this.getPowerLevel().getNextValue().get();
            this.getLastPowerLevel().setNextValue(currentPowerLevel);
            currentPowerLevel += percentage;
            currentPowerLevel = currentPowerLevel > 100 ? 100
                    : currentPowerLevel < 0 ? 0 : currentPowerLevel;

            this.getPowerLevel().setNextValue(currentPowerLevel);
            try {
                this.applyPowerToPwm(currentPowerLevel);

            } catch (OpenemsError.OpenemsNamedException e) {
                log.warn("Couldn't apply Power to Pwm at Pump : " + super.id() + "Reason: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    private void applyPowerToPwm(double currentPowerLevel) throws OpenemsError.OpenemsNamedException {
        int scaleFactor;
        if (this.pwm != null) {
            scaleFactor = this.pwm.getPwmPowerLevelChannel().channelDoc().getUnit().equals(Unit.THOUSANDTH) ? 10 : 1;
            this.pwm.getPwmPowerLevelChannel().setNextWriteValue((float) currentPowerLevel * scaleFactor);
        } else {
            scaleFactor = this.pwmConsolinno.getWritePwmPowerLevelChannel().channelDoc().getUnit().equals(Unit.THOUSANDTH) ? 10 : 1;
            this.pwmConsolinno.getWritePwmPowerLevelChannel().setNextWriteValueFromObject(currentPowerLevel * scaleFactor);
        }
    }

    @Override
    public void controlRelays(boolean activate, String whichRelays) {
        try {
            if (this.relays != null && this.relayType.equals(RelayType.DIRECT)) {
                this.relays.getRelaysWriteChannel().setNextWriteValue(activate);
            } else if (this.relay != null && this.relayType.equals(RelayType.MODBUS)) {
                this.relay.getRelaysWriteChannel().setNextWriteValueFromObject(activate);
            } else {
                log.error("Called Control Relay but no Relay available in " + super.id());
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            log.warn("Couldn't apply pump value in: " + super.id());
        }
    }

    @Override
    public void setPowerLevel(double percent) {
        if (percent >= 0 && percent != getCurrentPowerLevelValue()) {
            double changeByPercent = percent - getCurrentPowerLevelValue();
            this.changeByPercentage(changeByPercent);
        }
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            if (this.useCheckChannel && this.getPowerLevel().value().isDefined()) {
                try {
                    this.checkIfPowerValueIsMatching();
                } catch (OpenemsError.OpenemsNamedException e) {
                    log.warn("Couldn't check if PowerValue is correctly set! " + super.id());
                }
            }
        } else if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            if (this.useExceptionalState && this.exceptionalStateHandler.exceptionalStateActive(this)) {
                int value = this.getExceptionalStateValue();
                if (value >= MIN_EXCEPTIONAL_STATE_VALUE) {
                    this.setPowerLevel(this.getExceptionalStateValue());
                } else {
                    log.warn("Couldn't apply value of Exceptional State Value for : " + super.id() + "ExceptionalState below min Value");
                }
            } else if (this.setPowerLevelPercent().getNextWriteValue().isPresent() && this.setPowerLevelPercent().getNextWriteValue().get() >= 0) {
                this.setPowerLevel(this.setPowerLevelPercent().getNextWriteValue().get());
                try {
                    this.setPowerLevelPercent().setNextWriteValueFromObject(-1);
                } catch (OpenemsError.OpenemsNamedException e) {
                    log.warn("Couldn't set PowerLevelPercent for Pump: " + super.id() + " Reason: " + e.getMessage());
                }

            }
        }
    }

    private void checkIfPowerValueIsMatching() throws OpenemsError.OpenemsNamedException {
        double currentPowerLevel = this.getCurrentPowerLevelValue();
        boolean anticipatedValueCorrect = true;
        if (this.isRelays) {
            boolean expectedBooleanValue = currentPowerLevel > 0;
            Channel<?> channel = this.cpm.getChannel(this.checkRelayChannel);
            if (channel.value().isDefined()) {
                switch (channel.channelDoc().getType()) {
                    case BOOLEAN:
                        anticipatedValueCorrect = expectedBooleanValue == (Boolean) channel.value().get();
                        break;
                    case SHORT:
                    case INTEGER:
                    case LONG:
                    case FLOAT:
                    case DOUBLE:
                        anticipatedValueCorrect = expectedBooleanValue == (Double) channel.value().get() > 0;
                        break;
                    case STRING:
                        if (this.containsOnlyNumbers(channel.value().get().toString())) {
                            anticipatedValueCorrect = expectedBooleanValue == Double.parseDouble(channel.value().get().toString()) > 0;
                        } else {
                            log.warn("Couldn't check if Relay value is set correctly, Channel has non Numeric content " + super.id() + channel.toString());
                        }
                        break;
                }
                //
                if (anticipatedValueCorrect == false) {
                    log.info("Adapted value in " + super.id() + " Value was: " + channel.value().get() + " but expected: " + expectedBooleanValue);
                    this.controlRelays(currentPowerLevel > 0, "");
                }
            } else {
                log.info("Couldn't check for anticipated Value! Value is not defined yet: " + super.id() + channel.toString());
            }
        }
        if (this.isPwm) {
            Channel<?> channel = this.cpm.getChannel(this.checkPwmChannel);
            Unit channelUnit = channel.channelDoc().getUnit();
            Unit ownUnit = this.getPowerLevel().channelDoc().getUnit();
            double scaleFactor = 1;
            if (channelUnit.equals(Unit.THOUSANDTH) && ownUnit.equals(Unit.PERCENT)) {
                scaleFactor = 0.1d;
            } else if (channelUnit.equals(Unit.PERCENT) && ownUnit.equals(Unit.THOUSANDTH)) {
                scaleFactor = 10;
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
                                ? Double.parseDouble(channel.value().get().toString()) : this.getCurrentPowerLevelValue();
                        break;
                    default:
                        log.warn("ChannelType is not supported!" + super.id() + "Channel: " + channel.toString());
                }
                if (Math.abs(value * scaleFactor - currentPowerLevel) > TOLERANCE) {
                    log.info("PowerLevel of Pump: " + super.id() + " incorrect. Was: " + value + " but expected: " + currentPowerLevel);
                    this.applyPowerToPwm(currentPowerLevel);
                }
            } else {
                log.info("Couldn't check for anticipated Value! Value is not defined yet: " + super.id() + channel.toString());
            }
        }

    }

    @Override
    public String debugLog() {
        String powerValue = "not Defined";
        if (this.getCurrentPowerLevelValue() >= 0) {
            powerValue = String.valueOf(this.getCurrentPowerLevelValue());
        }
        return "PowerLevel of Pump " + super.id() + " is : " + powerValue;
    }
}
