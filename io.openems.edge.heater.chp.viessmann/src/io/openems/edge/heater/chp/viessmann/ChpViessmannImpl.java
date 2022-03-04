package io.openems.edge.heater.chp.viessmann;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.io.api.AnalogInputOutput;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.Chp;
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.chp.viessmann.api.ModuleStatus;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import io.openems.edge.heater.chp.viessmann.api.ChpViessmann;
import io.openems.edge.io.api.Relay;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * This module reads the most important variables available via Modbus from a Viessmann chp and maps them to
 * OpenEMS channels.
 * This chp does not support sending commands via Modbus. Instead, an AiO module and a relay (depending on chp model)
 * is needed to control the chp. Just reading the chp values available from Modbus does not need these modules.
 * The module is written to be used with the Heater interface methods.
 * When setEnableSignal() from the Heater interface is set to true with no other parameters like setPointPowerPercent()
 * specified, the CHP will turn on with default settings. The default settings are configurable in the config.
 * The CHP can be controlled with setHeatingPowerSetPoint() or setElectricPowerSetPoint(). However, both are mapped to
 * the same AiO output, so a hierarchy is needed. setHeatingPowerSetPoint() overwrites setElectricPowerSetPoint(), if
 * both are used.
 * setTemperatureSetpoint() and related methods are not supported by this CHP.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Chp.Viessmann",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = { //
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS //
        })
public class ChpViessmannImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
        ExceptionalState, ChpViessmann {

    private final Logger log = LoggerFactory.getLogger(ChpViessmannImpl.class);

    private ViessmannChpType chpType;
    private int electricOutput;
    private double powerPercentSetpoint;
    private Relay relay;
    private boolean useRelay;
    private AnalogInputOutput aioChannel;
    private PercentageRange percentageRange;

    private final String[] errorPossibilities = ErrorPossibilities.STANDARD_ERRORS.getErrorList();

    private boolean printInfoToLog;
    private boolean readOnly = false;
    private boolean startupStateChecked = false;
    private boolean readyForCommands = false;
    private double defaultPower = 0.d;
    private EnableSignalHandler enableSignalHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "CHP_VIESSMANN_ENABLE_SIGNAL_IDENTIFIER";
    private boolean useExceptionalState;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "CHP_VIESSMANN_EXCEPTIONAL_STATE_IDENTIFIER";

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    protected ComponentManager cpm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public ChpViessmannImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ChpViessmann.ChannelId.values(),
                Chp.ChannelId.values(),
                Heater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus", config.modbusBridgeId());

        this.chpType = config.chpType();
        this.printInfoToLog = config.printInfoToLog();
        this.electricOutput = Math.round(this.chpType.getElectricOutput());
        this.readOnly = config.readOnly();
        if (this.isEnabled() == false) {
            this._setHeaterState(HeaterState.OFF.getValue());
        }

        if (this.readOnly == false) {
            this.allocateComponents(config);
            this.percentageRange = config.percentageRange();
            this.startupStateChecked = false;
            this.powerPercentSetpoint = Math.min(100, Math.max(config.defaultSetPointPowerPercent(), 0));
            this.defaultPower = this.powerPercentSetpoint;
            this.initializeTimers(config);
        }


    }

    private void allocateComponents(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (this.cpm.getComponent(config.aioModuleId()) instanceof AnalogInputOutput) {
            this.aioChannel = this.cpm.getComponent(config.aioModuleId());
        } else {
            throw new ConfigurationException("activate", "The Component with id: "
                    + config.aioModuleId() + " is not an AIO module");
        }
        this.useRelay = config.useRelay();
        if (this.useRelay) {
            if (this.cpm.getComponent(config.relayId()) instanceof Relay) {
                this.relay = this.cpm.getComponent(config.relayId());
                //this.relay.getRelaysWriteChannel().setNextWriteValue(false); No need to turn chp off here.
            } else {
                throw new ConfigurationException("activate", "The Component with id: "
                        + config.relayId() + " is not a relay module");
            }
        }
    }

    private void initializeTimers(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
        timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, config.enableSignalTimerId(), config.waitTimeEnableSignal());
        this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
        this.useExceptionalState = config.useExceptionalState();
        if (this.useExceptionalState) {
            timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, config.exceptionalStateTimerId(), config.waitTimeExceptionalState());
            this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                new FC3ReadRegistersTask(0x4000, Priority.HIGH,
                        new DummyRegisterElement(0x4000, 0x4000),
                        m(ChpViessmann.ChannelId.MODE, new UnsignedWordElement(0x4001),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.STATUS, new UnsignedWordElement(0x4002),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.OPERATING_MODE, new UnsignedWordElement(0x4003),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Heater.ChannelId.EFFECTIVE_HEATING_POWER_PERCENT, new SignedWordElement(0x4004),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.ERROR_BITS_1, new UnsignedWordElement(0x4005),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.ERROR_BITS_2, new UnsignedWordElement(0x4006),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.ERROR_BITS_3, new UnsignedWordElement(0x4007),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.ERROR_BITS_4, new UnsignedWordElement(0x4008),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.ERROR_BITS_5, new UnsignedWordElement(0x4009),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.ERROR_BITS_6, new UnsignedWordElement(0x400A),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.ERROR_BITS_7, new UnsignedWordElement(0x400B),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.ERROR_BITS_8, new UnsignedWordElement(0x400C),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.OPERATING_HOURS, new UnsignedWordElement(0x400D),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.OPERATING_MINUTES, new UnsignedWordElement(0x400E),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.START_COUNTER, new UnsignedWordElement(0x400F),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.MAINTENANCE_INTERVAL, new SignedWordElement(0x4010),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.MODULE_LOCK, new SignedWordElement(0x4011),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.WARNING_TIME, new SignedWordElement(0x4012),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.NEXT_MAINTENANCE, new UnsignedWordElement(0x4013),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.EXHAUST_A, new SignedWordElement(0x4014),
                                ElementToChannelConverter.SCALE_FACTOR_1),
                        m(ChpViessmann.ChannelId.EXHAUST_B, new SignedWordElement(0x4015),
                                ElementToChannelConverter.SCALE_FACTOR_1),
                        m(ChpViessmann.ChannelId.EXHAUST_C, new SignedWordElement(0x4016),
                                ElementToChannelConverter.SCALE_FACTOR_1),
                        m(ChpViessmann.ChannelId.EXHAUST_D, new SignedWordElement(0x4017),
                                ElementToChannelConverter.SCALE_FACTOR_1),
                        m(ChpViessmann.ChannelId.PT_100_1, new SignedWordElement(0x4018),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(0x4019),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(0x401A),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.PT_100_4, new SignedWordElement(0x401B),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.PT_100_5, new SignedWordElement(0x401C),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.PT_100_6, new SignedWordElement(0x401D),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.BATTERY_VOLTAGE, new SignedWordElement(0x401E),
                                ElementToChannelConverter.SCALE_FACTOR_2),
                        m(ChpViessmann.ChannelId.OIL_PRESSURE, new SignedWordElement(0x401F),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.LAMBDA_PROBE_VOLTAGE, new SignedWordElement(0x4020),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(0x4025, Priority.HIGH,
                        m(ChpViessmann.ChannelId.ROTATION_PER_MIN, new UnsignedWordElement(0x4025),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.TEMPERATURE_CONTROLLER, new SignedWordElement(0x4026),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.TEMPERATURE_CLEARANCE, new SignedWordElement(0x4027),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.SUPPLY_VOLTAGE_L1, new SignedWordElement(0x4028),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.SUPPLY_VOLTAGE_L2, new SignedWordElement(0x4029),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.SUPPLY_VOLTAGE_L3, new SignedWordElement(0x402A),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.GENERATOR_VOLTAGE_L1, new SignedWordElement(0x402B),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.GENERATOR_VOLTAGE_L2, new SignedWordElement(0x402C),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.GENERATOR_VOLTAGE_L3, new SignedWordElement(0x402D),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.GENERATOR_CURRENT_L1, new SignedWordElement(0x402E),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.GENERATOR_CURRENT_L2, new SignedWordElement(0x402F),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.GENERATOR_CURRENT_L3, new SignedWordElement(0x4030),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.SUPPLY_VOLTAGE_TOTAL, new SignedWordElement(0x4031),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.GENERATOR_VOLTAGE_TOTAL, new SignedWordElement(0x4032),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.GENERATOR_CURRENT_TOTAL, new SignedWordElement(0x4033),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Chp.ChannelId.EFFECTIVE_ELECTRIC_POWER, new SignedWordElement(0x4034),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ChpViessmann.ChannelId.SUPPLY_FREQUENCY, new FloatDoublewordElement(0x4035),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(0x4037, Priority.LOW,
                        m(ChpViessmann.ChannelId.GENERATOR_FREQUENCY, new FloatDoublewordElement(0x4037),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(0x403B, Priority.LOW,
                        m(ChpViessmann.ChannelId.ACTIVE_POWER_FACTOR, new SignedWordElement(0x403B),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
                        m(ChpViessmann.ChannelId.RESERVE, new UnsignedDoublewordElement(0x403C),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(0x403E, 0x403E)));

    }

    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() == false) {
            return;
        }
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            this.channelmapping();
            if (this.printInfoToLog) {
                this.printInfo();
            }
        } else if (this.readOnly == false && this.readyForCommands && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            this.writeCommands();
        }
    }

    /**
     * Put values in channels that are not directly Modbus read values but derivatives.
     */
    protected void channelmapping() {

        // Parse errors.
        List<String> errorSummary = new ArrayList<>();
        char[] allErrorsAsChar = this.generateErrorAsCharArray();
        int errorMax = 80;
        //int errorBitLength = 16;
        for (int i = 0, errorListPosition = 0; i < errorMax; i++) {
            if (allErrorsAsChar[i] == '1') {
                if (this.errorPossibilities[i].toLowerCase().contains("reserve") == false) {
                    errorSummary.add(errorListPosition, this.errorPossibilities[i]);
                }
                errorListPosition++;
            }
        }

        // Calculate values not directly supplied by modbus.
        if (this.getEffectiveHeatingPowerPercent().isDefined()) {
            double powerPercent = this.getEffectiveHeatingPowerPercent().get();
            int heatingPowerEstimate = this.calculateHeatingPower(powerPercent);
            this._setEffectiveHeatingPower(heatingPowerEstimate);
            this.getEffectiveHeatingPowerChannel().nextProcessImage();
        }

        // Set Heater interface STATUS channel
        if (this.getModuleStatus().isDefined()) {
            this.readyForCommands = true;
            ModuleStatus moduleStatus = ModuleStatus.valueOf(this.getModuleStatus().get());
            switch (moduleStatus) {
                case OFF:
                    this._setHeaterState(HeaterState.OFF.getValue());
                    break;
                case DISTURBANCE:
                case READY:
                    this._setHeaterState(HeaterState.STANDBY.getValue());
                    break;
                case START:
                    this._setHeaterState(HeaterState.STARTING_UP_OR_PREHEAT.getValue());
                    break;
                case RUNNING:
                    this._setHeaterState(HeaterState.RUNNING.getValue());
                    break;
                case UNDEFINED:
                default:
                    this._setHeaterState(HeaterState.UNDEFINED.getValue());
                    break;
            }
        } else {
            this._setHeaterState(HeaterState.UNDEFINED.getValue());
            this.readyForCommands = false;
        }
        this.getHeaterStateChannel().nextProcessImage();

        // Check for missing components. This is in ’channelmapping()’ because it may produce an error message.
        if (this.readOnly == false) {
            this.checkMissingComponents(errorSummary);
        }

        // Write errors to error channel.
        if ((errorSummary.size() > 0)) {
            this._setErrorMessage(errorSummary.toString());
        } else {
            this._setErrorMessage("No error");
        }
        this.getErrorMessageChannel().nextProcessImage();
    }

    /**
     * Checks if components required by this module are still available. If not, tries to allocate them again.
     *
     * @param errorSummary List to which error messages are added.
     */
    protected void checkMissingComponents(List<String> errorSummary) {
        try {
            OpenemsComponent componentFetchedByCpm;
            if (this.aioChannel.equals(this.cpm.getComponent(this.aioChannel.id())) == false) {
                componentFetchedByCpm = this.cpm.getComponent(this.aioChannel.id());
                if (componentFetchedByCpm instanceof AnalogInputOutput) {
                    this.aioChannel = (AnalogInputOutput) componentFetchedByCpm;
                }
            }
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.readyForCommands = false;
            errorSummary.add("OpenEMS error: Could not find configured AIO module.");
            this.log.warn("Could not find configured AIO module!");
        }
        if (this.useRelay) {
            try {
                if (this.relay.equals(this.cpm.getComponent(this.relay.id())) == false) {
                    OpenemsComponent componentFetchedByCpm;
                    componentFetchedByCpm = this.cpm.getComponent(this.relay.id());
                    if (componentFetchedByCpm instanceof Relay) {
                        this.relay = (Relay) componentFetchedByCpm;
                    }
                }
            } catch (OpenemsError.OpenemsNamedException ignored) {
                this.readyForCommands = false;
                errorSummary.add("OpenEMS error: Could not find configured relay module.");
                this.log.warn("Could not find configured relay module!");
            }
        }
    }

    /**
     * Determine commands and send them to the heater.
     */
    protected void writeCommands() {

        // Handle EnableSignal.
        boolean turnOnChp = this.enableSignalHandler.deviceShouldBeHeating(this);

        // Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
        int exceptionalStateValue = 0;
        boolean exceptionalStateActive = false;
        if (this.useExceptionalState) {
            exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
            if (exceptionalStateActive) {
                exceptionalStateValue = this.getExceptionalStateValue();
                if (exceptionalStateValue <= this.DEFAULT_MIN_EXCEPTIONAL_VALUE) {
                    turnOnChp = false;
                } else {
                    // When ExceptionalStateValue is between 0 and 100, set Chp to this PowerPercentage.
                    turnOnChp = true;
                    exceptionalStateValue = Math.min(exceptionalStateValue, this.DEFAULT_MAX_EXCEPTIONAL_VALUE);
                }
            }
        }

        /* At startup, check if chp is already running. If yes, keep it running by sending 'EnableSignal = true' to
           yourself once. This gives controllers until the EnableSignal timer runs out to decide the state of the chp.
           This avoids a chp restart if the controllers want the chp to stay on. -> Longer chp lifetime.
           Without this function, the chp will always switch off at startup because EnableSignal starts as ’false’. */
        if (this.startupStateChecked == false) {
            this.startupStateChecked = true;
            turnOnChp = (HeaterState.valueOf(this.getHeaterState().orElse(-1)) == HeaterState.RUNNING);
            if (turnOnChp) {
                try {
                    this.getEnableSignalChannel().setNextWriteValue(true);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }
        }
        int writeToAioValue = 0;
        if (turnOnChp) {
            if (this.useRelay) {
                try {
                    this.relay.getRelaysWriteChannel().setNextWriteValue(true);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }
            double powerPercentToAio = this.defaultPower;
            if (exceptionalStateActive) {
                powerPercentToAio = exceptionalStateValue;
            } else {
                if (this.getHeatingPowerPercentSetpointChannel().getNextWriteValue().isPresent()) {
                    Optional<Double> heatingPowerPercentWrite = this.getHeatingPowerPercentSetpointChannel().getNextWriteValueAndReset();
                    heatingPowerPercentWrite.ifPresent(aDouble -> this.powerPercentSetpoint = aDouble);
                    this.powerPercentSetpoint = Math.min(this.powerPercentSetpoint, 100);
                    this.powerPercentSetpoint = Math.max(this.powerPercentSetpoint, 0);
                    powerPercentToAio = this.powerPercentSetpoint;
                } else {
                    // Check write channels for values. Since there are two channels, need to have a hierarchy.
                    // SET_POINT_HEATING_POWER_PERCENT (heater interface) overwrites EFFECTIVE_ELECTRIC_POWER_SETPOINT (Chp interface).
                    Optional<Double> electricPowerWrite = this.getElectricPowerSetpointChannel().getNextWriteValueAndReset();
                    if (electricPowerWrite.isPresent()) {
                        double electricPowerSetpoint = electricPowerWrite.get();
                        electricPowerSetpoint = Math.min(electricPowerSetpoint, this.electricOutput);
                        electricPowerSetpoint = Math.max(electricPowerSetpoint, 0);
                        this._setElectricPowerSetpoint(electricPowerSetpoint);
                        this.powerPercentSetpoint = 100.0 * electricPowerSetpoint / this.electricOutput;
                    }
                }
            }
            this._setHeatingPowerPercentSetpoint(powerPercentToAio);

            writeToAioValue = (int) Math.round(powerPercentToAio);
            if (this.percentageRange == PercentageRange.RANGE_50_100) {
                // Map to 50-100% range.
                writeToAioValue = (int) Math.round((powerPercentToAio - 50) * 2);
            }
        } else {
            if (this.useRelay) {
                if (this.relay.isEnabled()) {
                    try {
                        this.relay.getRelaysWriteChannel().setNextWriteValue(false);
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Couldn't write in Channel " + e.getMessage());
                    }
                } else {
                    this.log.warn("Relay module " + this.relay.id() + "is not enabled! Sending commands to CHP not possible.");
                }
            }
        }

        // Use AiO to send commands to chp.
        try {
            this.aioChannel.setWritePercent(writeToAioValue);
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't write in Channel " + e.getMessage());
        }
    }

    /**
     * This chp does not provide the current heating power as a readable value. However, an estimate can be calculated
     * from the electric power percent value and the information in the data sheet. For most models, the datasheet lists
     * the heat output at 75% and 50% electric power. We use the 50% and 100% value to do a simple linear fit, then use
     * this fit to calculate a heating power estimate for any % value of electric power.
     * If there is no value for 50% in the datasheet, we assume that at 50% electric power, heating power is at 61% of
     * maximum (average of the available data).
     *
     * @param powerPercent the electric power percent value.
     * @return the estimated heating power in kW
     */
    private int calculateHeatingPower(double powerPercent) {
        float thermalOutput100 = this.chpType.getThermalOutput();
        float thermalOutput50 = this.chpType.getHeatOutputAt50Percent();
        float scaleFactor = 0.61f;
        if (thermalOutput50 > 0) {  // if no data available, thermalOutput50 = -1
            scaleFactor = thermalOutput50 / thermalOutput100;
        }
        float multiplier = (2 - (scaleFactor * 4));
        return (int) Math.round(powerPercent * thermalOutput100 * (1 + (100 - powerPercent) * multiplier));
    }

    /**
     * Collects all the error messages and puts them in a char array.
     *
     * @return the error messages.
     */
    private char[] generateErrorAsCharArray() {
        String errorBitsAsString = "";
        String dummyString = "0000000000000000";
        if (getError1Channel().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getError1Channel().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getError2Channel().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getError2Channel().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getError3Channel().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getError3Channel().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getError4Channel().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getError4Channel().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getError5Channel().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getError5Channel().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getError6Channel().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getError6Channel().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getError7Channel().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getError7Channel().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getError8Channel().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getError8Channel().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }

        return errorBitsAsString.toCharArray();
    }

    /**
     * Information that is printed to the log if ’print info to log’ option is enabled.
     */
    protected void printInfo() {
        this.logInfo(this.log, "--CHP Viessmann Vitobloc " + this.chpType.getName() + "--");
        this.logInfo(this.log, "Engine rpm: " + this.getRotationPerMinute());
        this.logInfo(this.log, "Power percent set point (write mode only): " + this.getHeatingPowerPercentSetpoint());
        this.logInfo(this.log, "Effective electrical power: " + this.getEffectiveElectricPower() + " of max "
                + this.electricOutput + " kW (" + (1.0 * this.getEffectiveElectricPower().orElse(0.0) / this.electricOutput) + "%)");
        this.logInfo(this.log, "Flow temperature: " + this.getFlowTemperature());
        this.logInfo(this.log, "Return temperature: " + this.getReturnTemperature());
        this.logInfo(this.log, "Operating hours: " + this.getOperatingHours());
        this.logInfo(this.log, "Engine start counter: " + this.getStartCounter());
        this.logInfo(this.log, "Hours until next maintenance: " + this.getNextMaintenance());
        this.logInfo(this.log, "Heater state: " + this.getHeaterState());
        this.logInfo(this.log, "Error message: " + this.getErrorMessage().get());
    }

    /**
     * Returns the debug message.
     *
     * @return the debug message.
     */
    public String debugLog() {
        String debugMessage = this.id() + " :" + this.getHeaterState().asEnum().asCamelCase() //
                + "|F:" + this.getFlowTemperature().asString() //
                + "|R:" + this.getReturnTemperature().asString(); //
        if (this.getWarningMessage().isDefined() && this.getWarningMessage().get().equals("No warning") == false) {
            debugMessage = debugMessage + "|Warning";
        }
        if (this.getWarningMessage().isDefined() && this.getErrorMessage().get().equals("No error") == false) {
            debugMessage = debugMessage + "|Error";
        }
        return debugMessage;
    }
}
