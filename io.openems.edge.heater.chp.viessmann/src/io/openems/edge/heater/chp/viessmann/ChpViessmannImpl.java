package io.openems.edge.heater.chp.viessmann;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.consolinno.aio.api.AioChannel;
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
import io.openems.edge.heater.chp.viessmann.api.ViessmannInformation;
import io.openems.edge.relay.api.Relay;
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


@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Chp.Viessmann",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class ChpViessmannImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
        ExceptionalState, ViessmannInformation {

    private final Logger log = LoggerFactory.getLogger(ChpViessmannImpl.class);

    private boolean componentEnabled;
    private ViessmannChpType chpType;
    private int thermicalOutput;
    private int electricalOutput;
    private double powerPercentSetpoint;
    private Relay relay;
    private boolean useRelay;
    private AioChannel aioChannel;
    private int minValue;
    private int maxValue;
    private int percentageRange;

    private final String[] errorPossibilities = ErrorPossibilities.STANDARD_ERRORS.getErrorList();

    private boolean printInfoToLog;
    private boolean readOnly = false;
    private boolean startupStateChecked = false;

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
                ViessmannInformation.ChannelId.values(),
                Chp.ChannelId.values(),
                Heater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus", config.modbusBridgeId());
        this.componentEnabled = config.enabled();
        this.setChpType(config.chpType());
        this.printInfoToLog = config.printInfoToLog();
        this.thermicalOutput = Math.round(this.chpType.getThermalOutput());
        this.electricalOutput = Math.round(this.chpType.getElectricalOutput());
        this.readOnly = config.readOnly();

        if (this.readOnly == false) {
            if (this.cpm.getComponent(config.aioModuleId()) instanceof AioChannel) {
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

            this.minValue = config.minLimit();
            this.maxValue = config.maxLimit();
            this.percentageRange = config.percentageRange();
            this.startupStateChecked = false;
            this.powerPercentSetpoint = config.defaultSetPointPowerPercent();
            TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
            String timerTypeEnableSignal;
            if (config.enableSignalTimerIsCyclesNotSeconds()) {
                timerTypeEnableSignal = "TimerByCycles";
            } else {
                timerTypeEnableSignal = "TimerByTime";
            }
            timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, timerTypeEnableSignal, config.waitTimeEnableSignal());
            this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
            this.useExceptionalState = config.useExceptionalState();
            if (this.useExceptionalState) {
                String timerTypeExceptionalState;
                if (config.exceptionalStateTimerIsCyclesNotSeconds()) {
                    timerTypeExceptionalState = "TimerByCycles";
                } else {
                    timerTypeExceptionalState = "TimerByTime";
                }
                timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerTypeExceptionalState, config.waitTimeExceptionalState());
                this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
            }
        }

        if (this.componentEnabled == false) {
            this._setHeaterState(HeaterState.OFF.getValue());
        }
    }

    private void setChpType(String chpType) throws ConfigurationException {
        switch (chpType) {
            case "EM_6_15":
                this.chpType = ViessmannChpType.Vito_EM_6_15;
                break;
            case "EM_9_20":
                this.chpType = ViessmannChpType.Vito_EM_9_20;
                break;
            case "EM_20_39":
                this.chpType = ViessmannChpType.Vito_EM_20_39;
                break;
            case "EM_20_39_70":
                this.chpType = ViessmannChpType.Vito_EM_20_39_RL_70;
                break;
            case "EM_50_81":
                this.chpType = ViessmannChpType.Vito_EM_50_81;
                break;
            case "EM_70_115":
                this.chpType = ViessmannChpType.Vito_EM_70_115;
                break;
            case "EM_100_167":
                this.chpType = ViessmannChpType.Vito_EM_100_167;
                break;
            case "EM_140_207":
                this.chpType = ViessmannChpType.Vito_EM_140_207;
                break;
            case "EM_199_263":
                this.chpType = ViessmannChpType.Vito_EM_199_263;
                break;
            case "EM_199_293":
                this.chpType = ViessmannChpType.Vito_EM_199_293;
                break;
            case "EM_238_363":
                this.chpType = ViessmannChpType.Vito_EM_238_363;
                break;
            case "EM_363_498":
                this.chpType = ViessmannChpType.Vito_EM_363_498;
                break;
            case "EM_401_549":
                this.chpType = ViessmannChpType.Vito_EM_401_549;
                break;
            case "EM_530_660":
                this.chpType = ViessmannChpType.Vito_EM_530_660;
                break;
            case "BM_36_66":
                this.chpType = ViessmannChpType.Vito_BM_36_66;
                break;
            case "BM_55_88":
                this.chpType = ViessmannChpType.Vito_BM_55_88;
                break;
            case "BM_190_238":
                this.chpType = ViessmannChpType.Vito_BM_190_238;
                break;
            case "BM_366_437":
                this.chpType = ViessmannChpType.Vito_BM_366_437;
                break;
            default:
                throw new ConfigurationException("chpType", "No valid chp selected.");
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        /*  Don't turn off chp. Restarting the component should not also restart the chp.
        if (this.readOnly == false) {
            if (this.useRelay) {
                try {
                    this.relay.getRelaysWriteChannel().setNextWriteValue(false);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }
        }
        */
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                new FC3ReadRegistersTask(0x4000, Priority.LOW,
                        new DummyRegisterElement(0x4000, 0x4000),
                        m(ViessmannInformation.ChannelId.MODE, new UnsignedWordElement(0x4001),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.STATUS, new UnsignedWordElement(0x4002),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.OPERATING_MODE, new UnsignedWordElement(0x4003),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Heater.ChannelId.EFFECTIVE_HEATING_POWER_PERCENT, new SignedWordElement(0x4004),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_1, new UnsignedWordElement(0x4005),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_2, new UnsignedWordElement(0x4006),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_3, new UnsignedWordElement(0x4007),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_4, new UnsignedWordElement(0x4008),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_5, new UnsignedWordElement(0x4009),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_6, new UnsignedWordElement(0x400A),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_7, new UnsignedWordElement(0x400B),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.ERROR_BITS_8, new UnsignedWordElement(0x400C),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.OPERATING_HOURS, new UnsignedWordElement(0x400D),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.OPERATING_MINUTES, new UnsignedWordElement(0x400E),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.START_COUNTER, new UnsignedWordElement(0x400F),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.MAINTENANCE_INTERVAL, new SignedWordElement(0x4010),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.MODULE_LOCK, new SignedWordElement(0x4011),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.WARNING_TIME, new SignedWordElement(0x4012),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.NEXT_MAINTENANCE, new UnsignedWordElement(0x4013),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.EXHAUST_A, new SignedWordElement(0x4014),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.EXHAUST_B, new SignedWordElement(0x4015),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.EXHAUST_C, new SignedWordElement(0x4016),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.EXHAUST_D, new SignedWordElement(0x4017),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.PT_100_1, new SignedWordElement(0x4018),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(0x4019),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(0x401A),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.PT_100_4, new SignedWordElement(0x401B),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.PT_100_5, new SignedWordElement(0x401C),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.PT_100_6, new SignedWordElement(0x401D),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.BATTERY_VOLTAGE, new SignedWordElement(0x401E),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.OIL_PRESSURE, new SignedWordElement(0x401F),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.LAMBDA_PROBE_VOLTAGE, new SignedWordElement(0x4020),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(0x4025, Priority.LOW,
                        m(ViessmannInformation.ChannelId.ROTATION_PER_MIN, new UnsignedWordElement(0x4025),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.TEMPERATURE_CONTROLLER, new SignedWordElement(0x4026),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.TEMPERATURE_CLEARANCE, new SignedWordElement(0x4027),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        m(ViessmannInformation.ChannelId.SUPPLY_VOLTAGE_L1, new SignedWordElement(0x4028),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.SUPPLY_VOLTAGE_L2, new SignedWordElement(0x4029),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.SUPPLY_VOLTAGE_L3, new SignedWordElement(0x402A),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_VOLTAGE_L1, new SignedWordElement(0x402B),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_VOLTAGE_L2, new SignedWordElement(0x402C),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_VOLTAGE_L3, new SignedWordElement(0x402D),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_ELECTRICITY_L1, new SignedWordElement(0x402E),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_ELECTRICITY_L2, new SignedWordElement(0x402F),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_ELECTRICITY_L3, new SignedWordElement(0x4030),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.SUPPLY_VOLTAGE_TOTAL, new SignedWordElement(0x4031),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_VOLTAGE_TOTAL, new SignedWordElement(0x4032),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.GENERATOR_ELECTRICITY_TOTAL, new SignedWordElement(0x4033),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Chp.ChannelId.EFFECTIVE_ELECTRIC_POWER, new SignedWordElement(0x4034),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(ViessmannInformation.ChannelId.SUPPLY_FREQUENCY, new FloatDoublewordElement(0x4035),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(0x4037, Priority.LOW,
                        m(ViessmannInformation.ChannelId.GENERATOR_FREQUENCY, new FloatDoublewordElement(0x4037),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(0x403B, Priority.LOW,
                        m(ViessmannInformation.ChannelId.ACTIVE_POWER_FACTOR, new SignedWordElement(0x403B),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
                        m(ViessmannInformation.ChannelId.RESERVE, new UnsignedDoublewordElement(0x403C),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(0x403E, 0x403E)));

    }

    @Override
    public void handleEvent(Event event) {
        if (this.componentEnabled && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            this.channelmapping();
        }
    }

    protected void setPowerPercentWithAio(boolean turnOnChp) {
        int writeToAioValue = 0;
        if (turnOnChp) {
            if (this.useRelay) {
                try {
                    this.relay.getRelaysWriteChannel().setNextWriteValue(true);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }

            // Check write channels for values. Since there are two channels, need to have a hierarchy.
            // SET_POINT_HEATING_POWER_PERCENT (heater interface) overwrites EFFECTIVE_ELECTRIC_POWER_SETPOINT (Chp interface).
            Optional<Double> electricPowerWrite = this.getElectricPowerSetpointChannel().getNextWriteValueAndReset();
            if (electricPowerWrite.isPresent()) {
                double electricPowerSetpoint = electricPowerWrite.get();
                if (electricPowerSetpoint > this.electricalOutput) {
                    electricPowerSetpoint = this.electricalOutput;
                } else if (electricPowerSetpoint < 0) {
                    electricPowerSetpoint = 0;
                }
                this._setElectricPowerSetpoint(electricPowerSetpoint);
                this.powerPercentSetpoint = 100.0 * electricPowerSetpoint / this.electricalOutput;
            }
            Optional<Double> heatingPowerPercentWrite = this.getHeatingPowerPercentSetpointChannel().getNextWriteValueAndReset();
            if (heatingPowerPercentWrite.isPresent()) {
                this.powerPercentSetpoint = heatingPowerPercentWrite.get();
            }
            if (this.powerPercentSetpoint > 100) {
                this.powerPercentSetpoint = 100;
            } else if (this.powerPercentSetpoint < 0) {
                this.powerPercentSetpoint = 0;
            }
            this._setHeatingPowerPercentSetpoint(this.powerPercentSetpoint);

            // Maps setpoint % to a mA value on the range minValue to maxValue.
            writeToAioValue = (int)Math.round(this.minValue + ((powerPercentSetpoint - this.percentageRange)
                    / ((100.f - this.percentageRange) / (this.maxValue - this.minValue))));
        } else {
            if (this.useRelay) {
                try {
                    this.relay.getRelaysWriteChannel().setNextWriteValue(false);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }
        }
        try {
            this.aioChannel.getWriteChannel().setNextWriteValue(writeToAioValue);
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't write in Channel " + e.getMessage());
        }
    }

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

        // Check for missing components.
        if (this.readOnly == false) {
            try {
                OpenemsComponent componentFetchedByCpm;
                if (this.aioChannel.isEnabled() == false) {
                    componentFetchedByCpm = this.cpm.getComponent(this.aioChannel.id());
                    if (componentFetchedByCpm instanceof AioChannel) {
                        this.aioChannel = (AioChannel) componentFetchedByCpm;
                    }
                }
            } catch (OpenemsError.OpenemsNamedException ignored) {
                errorSummary.add("OpenEMS error: Could not find configured AIO module.");
                this.log.warn("Could not find configured AIO module!");
            }
                if (this.useRelay) {
                    try {
                        if (this.relay.isEnabled() == false) {
                            OpenemsComponent componentFetchedByCpm;
                            componentFetchedByCpm = this.cpm.getComponent(this.relay.id());
                            if (componentFetchedByCpm instanceof Relay) {
                                this.relay = (Relay) componentFetchedByCpm;
                            }
                        }
                    } catch (OpenemsError.OpenemsNamedException ignored) {
                        errorSummary.add("OpenEMS error: Could not find configured relay module.");
                        this.log.warn("Could not find configured relay module!");
                    }
                }
        }

        // Write errors to error channel.
        if ((errorSummary.size() > 0)) {
            this._setErrorMessage(errorSummary.toString());
        } else {
            this._setErrorMessage("No error");
        }

        // Calculate values not directly supplied by modbus.
        if (this.getEffectiveHeatingPowerPercentChannel().value().isDefined()) {
            double powerPercent = this.getEffectiveHeatingPowerPercent().get();
            // Heating power does not scale linearly with powerPercent (= electric power percent).
            // Datasheet of EM_140_207 says at 50% electric power, heating power is at 62% of maximum.
            // This formula is an estimate based on that scaling.
            int heatingPowerEstimate = (int)Math.round(powerPercent * this.thermicalOutput * (1 + (100 - powerPercent) * 0.48));
            this._setEffectiveHeatingPower(heatingPowerEstimate);
        }

        // Set Heater interface STATUS channel
        boolean statusInfoReceived = false;
        boolean chpEngineRunning = false;
        if (this.getStatus().value().isDefined()) {
            statusInfoReceived = true;
            ModuleStatus moduleStatus = ModuleStatus.valueOf(this.getStatus().value().get());
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
                    chpEngineRunning = true;
                    this._setHeaterState(HeaterState.HEATING.getValue());
                    break;
                case UNDEFINED:
                default:
                    this._setHeaterState(HeaterState.UNDEFINED.getValue());
                    break;
            }
        } else {
            this._setHeaterState(HeaterState.UNDEFINED.getValue());
        }


        if (this.readOnly == false && statusInfoReceived) {

            // Handle EnableSignal.
            boolean turnOnChp = this.enableSignalHandler.deviceShouldBeHeating(this);

            // Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
            int exceptionalStateValue = 0;
            boolean exceptionalStateActive = false;
            if (this.useExceptionalState) {
                exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
                if (exceptionalStateActive) {
                    exceptionalStateValue = this.getExceptionalStateValue();
                    if (exceptionalStateValue <= 0) {
                        // Turn off Chp when ExceptionalStateValue = 0.
                        turnOnChp = false;
                    } else {
                        // When ExceptionalStateValue is between 0 and 100, set Chp to this PowerPercentage.
                        turnOnChp = true;
                        if (exceptionalStateValue > 100) {
                            exceptionalStateValue = 100;
                        }
                        try {
                            this.setHeatingPowerPercentSetpoint(exceptionalStateValue);
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.warn("Couldn't write in Channel " + e.getMessage());
                        }
                    }
                }
            }

            // If the component has just been started, it will most likely take a few cycles before a controller
            // sends an EnableSignal (assuming the CHP should be running). Since no EnableSignal means ’turn off the
            // CHP’, the component will always turn off the CHP during the first few cycles. If the CHP is already
            // on, this would turn the CHP off and on again, which is bad for the lifetime. A scenario where this
            // would happen is if the component or OpenEMS is restarted while the CHP is running.
            // To avoid that, check the CHP status at the startup of the component. If it is on, the component sends
            // the EnableSignal to itself once to keep the CHP on until the timer runs out. This gives any
            // controllers enough time to send the EnableSignal themselves.
            if (this.startupStateChecked == false) {
                this.startupStateChecked = true;
                turnOnChp = chpEngineRunning;
                if (turnOnChp) {
                    try {
                        this.getEnableSignalChannel().setNextWriteValue(true);
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Couldn't write in Channel " + e.getMessage());
                    }
                }
            }

            this.setPowerPercentWithAio(turnOnChp);
        }

        if (this.printInfoToLog) {
            this.logInfo(this.log, "--CHP Viessmann Vitobloc " + this.chpType.getName() + "--");
            this.logInfo(this.log, "Engine rpm: " + this.getRotationPerMinute().value().get());
            this.logInfo(this.log, "Power percent set point (write mode only): " + this.getHeatingPowerPercentSetpoint());
            this.logInfo(this.log, "Effective electrical power: " + this.getEffectiveElectricPower() + " of max "
                    + this.electricalOutput + " kW (" + (1.0 * this.getEffectiveElectricPower().get() / this.electricalOutput) + "%)");
            this.logInfo(this.log, "Flow temperature: " + this.getFlowTemperature());
            this.logInfo(this.log, "Return temperature: " + this.getReturnTemperature());
            this.logInfo(this.log, "Operating hours: " + this.getOperatingHours().value());
            this.logInfo(this.log, "Engine start counter: " + this.getStartCounter().value());
            this.logInfo(this.log, "Maintenance interval: " + this.getMaintenanceInterval().value());
            this.logInfo(this.log, "Heater state: " + this.getHeaterState());
            this.logInfo(this.log, "Error message: " + this.getErrorMessage().get());
        }
    }

    private char[] generateErrorAsCharArray() {

        String errorBitsAsString = "";
        String dummyString = "0000000000000000";
        if (getErrorOne().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorOne().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorTwo().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorTwo().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorThree().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorThree().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorFour().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorFour().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorFive().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorFive().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorSix().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorSix().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorSeven().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorSeven().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }
        if (getErrorEight().getNextValue().isDefined()) {
            errorBitsAsString += String.format("%16s", Integer.toBinaryString(getErrorEight().getNextValue().get())).replace(' ', '0');
        } else {
            errorBitsAsString += dummyString;
        }

        return errorBitsAsString.toCharArray();
    }
}
