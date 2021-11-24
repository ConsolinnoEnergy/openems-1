package io.openems.edge.heater.heatpump.heliotherm;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.heatpump.heliotherm.api.HeatpumpHeliotherm;
import io.openems.edge.heater.heatpump.heliotherm.api.ControlMode;
import io.openems.edge.heater.heatpump.heliotherm.api.OperatingMode;
import io.openems.edge.heater.heatpump.heliotherm.api.PowerControlSetting;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;


/**
 * This module reads the most important variables available via Modbus from a Heliotherm heat pump and maps them to
 * OpenEMS channels. WriteChannels can be used to send commands to the heat pump via setter methods in
 * HeatpumpHeliotherm and Heater.
 */

// ToDo: Add smart grid functionality using AiO module.

@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.HeatPump.Heliotherm",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = { //
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS //
        })
public class HeatpumpHeliothermImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
        ExceptionalState, HeatpumpHeliotherm {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    protected ComponentManager cpm;

    private final Logger log = LoggerFactory.getLogger(HeatpumpHeliothermImpl.class);
    private boolean printInfoToLog;
    private boolean readOnly;
    private boolean connectionAlive;
    private LocalDateTime fiveSecondTimestamp;
    private static final int sendIntervalSeconds = 6; // How often to send commands to the heat pump. Allowed minimum is 5.
    private int maxElectricPower;
    private int maxCompressorSpeed;
    private ControlMode controlModeSetting;
    private OperatingMode operatingModeSetting;
    private PowerControlSetting powerControlSetting;
    private boolean mapPowerPercentToConsumption;
    private int lastTemperatureSetPoint;
    private double lastPowerPercentSetPoint;
    private int lastConsumptionSetPoint;

    private EnableSignalHandler enableSignalHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "HEAT_PUMP_HELIOTHERM_ENABLE_SIGNAL_IDENTIFIER";
    private boolean useExceptionalState;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "HEAT_PUMP_HELIOTHERM_EXCEPTIONAL_STATE_IDENTIFIER";

    // This is essential for Modbus to work, but the compiler does not warn you when it is missing!
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public HeatpumpHeliothermImpl() {
        super(OpenemsComponent.ChannelId.values(),
                HeatpumpHeliotherm.ChannelId.values(),
                Heater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());

        this.printInfoToLog = config.printInfoToLog();
        this.readOnly = config.readOnly();
        if (this.isEnabled() == false) {
            this._setHeaterState(HeaterState.OFF.getValue());
        }

        if (this.readOnly == false) {
            this.controlModeSetting = config.defaultControlMode();
            this.operatingModeSetting = this.parseConfigOperatingMode(config.defaultOperatingMode());
            this.lastTemperatureSetPoint = config.defaultSetPointTemperature() * 10; // Convert to d°C.
            this.setTemperatureSetpoint(this.lastTemperatureSetPoint);
            this.powerControlSetting = config.powerControlSetting();
            this.maxElectricPower = config.maxElectricPower();
            this.mapPowerPercentToConsumption = config.mapPowerPercentToConsumption();
            this.maxCompressorSpeed = config.maxCompressorSpeed();
            this.fiveSecondTimestamp = LocalDateTime.now().minusSeconds(sendIntervalSeconds);    // Initialize with past time value so code executes immediately on first run.
            this.setHeatingPowerPercentSetpoint(config.defaultSetPointPowerPercent());
            this.initializeTimers(config);
        }
    }

    private OperatingMode parseConfigOperatingMode(String string) {
        switch (string) {
            case "Cooling":
                return OperatingMode.COOLING;
            case "Summer":
                return OperatingMode.SUMMER;
            case "Always on (Dauerbetrieb)":
                return OperatingMode.ALWAYS_ON;
            case "Setback mode (Absenkung)":
                return OperatingMode.SETBACK;
            case "Holidays, full time setback (Urlaub)":
                return OperatingMode.VACATION;
            case "No night setback (Party)":
                return OperatingMode.PARTY;
            case "Automatic":
            default:
                return OperatingMode.AUTOMATIC;
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
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        ModbusProtocol protocol = new ModbusProtocol(this,
                // Input register read.
                new FC4ReadInputRegistersTask(10, Priority.HIGH,
                        m(HeatpumpHeliotherm.ChannelId.IR10_OUTSIDE_TEMPERATURE, new SignedWordElement(10),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(11),
                        m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(12),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(13),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.IR14_STORAGE_TANK_TEMPERATURE, new SignedWordElement(14),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(25, Priority.HIGH,
                        m(HeatpumpHeliotherm.ChannelId.IR25_HEATPUMP_RUNNING, new SignedWordElement(25),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.IR26_ERROR, new SignedWordElement(26),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(27),
                        new DummyRegisterElement(28),
                        m(HeatpumpHeliotherm.ChannelId.IR29_READ_COMPRESSOR_SPEED, new SignedWordElement(29),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        m(HeatpumpHeliotherm.ChannelId.IR30_COP, new SignedWordElement(30),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(31),
                        m(HeatpumpHeliotherm.ChannelId.IR32_DSM_INDICATOR, new SignedWordElement(32),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(33),
                        m(HeatpumpHeliotherm.ChannelId.IR34_READ_TEMP_SET_POINT, new SignedWordElement(34),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(41, Priority.HIGH,
                        m(HeatpumpHeliotherm.ChannelId.IR41_RUN_REQUEST_TYPE, new SignedWordElement(41),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(70, Priority.HIGH,
                        m(HeatpumpHeliotherm.ChannelId.IR70_71_ELECTRIC_POWER_CONSUMPTION, new UnsignedDoublewordElement(70),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(74, Priority.HIGH,
                        m(Heater.ChannelId.EFFECTIVE_HEATING_POWER, new UnsignedDoublewordElement(74),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
                ),

                // Holding register read.
                new FC3ReadRegistersTask(100, Priority.HIGH,
                        m(HeatpumpHeliotherm.ChannelId.HR100_OPERATING_MODE, new UnsignedWordElement(100),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(101),
                        m(Heater.ChannelId.SET_POINT_TEMPERATURE, new SignedWordElement(102),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.HR103_USE_SET_POINT_TEMPERATURE, new UnsignedWordElement(103),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC3ReadRegistersTask(117, Priority.HIGH,
                        m(HeatpumpHeliotherm.ChannelId.HR117_USE_POWER_CONTROL, new UnsignedWordElement(117),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC3ReadRegistersTask(125, Priority.HIGH,
                        m(HeatpumpHeliotherm.ChannelId.HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION, new UnsignedWordElement(125),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.HR126_MODBUS, new SignedWordElement(126),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(127),
                        m(HeatpumpHeliotherm.ChannelId.HR128_RESET_ERROR, new UnsignedWordElement(128),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.HR129_OUTSIDE_TEMPERATURE_SEND, new SignedWordElement(129),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.HR130_USE_MODBUS_OUTSIDE_TEMPERATURE, new UnsignedWordElement(130),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.HR131_STORAGE_TANK_TEMPERATURE_SEND, new SignedWordElement(131),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.HR132_USE_MODBUS_SENT_STORAGE_TANK_TEMP, new UnsignedWordElement(132),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC3ReadRegistersTask(149, Priority.HIGH,
                        m(HeatpumpHeliotherm.ChannelId.HR149_DSM_SWITCH, new SignedWordElement(149),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.HR150_USE_MODBUS_DSM_SWITCH, new SignedWordElement(150),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                )
        );

        if (this.readOnly == false) {
            protocol.addTasks(
                    new FC16WriteRegistersTask(100,
                            m(HeatpumpHeliotherm.ChannelId.HR100_MODBUS, new UnsignedWordElement(100),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC6WriteRegisterTask(102,
                            m(HeatpumpHeliotherm.ChannelId.HR102_MODBUS, new SignedWordElement(102))),
                    new FC6WriteRegisterTask(103,
                            m(HeatpumpHeliotherm.ChannelId.HR103_MODBUS, new UnsignedWordElement(103))),

                    new FC16WriteRegistersTask(117,
                            m(HeatpumpHeliotherm.ChannelId.HR117_MODBUS, new UnsignedWordElement(117),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC16WriteRegistersTask(125,
                            m(HeatpumpHeliotherm.ChannelId.HR125_MODBUS, new UnsignedWordElement(125),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliotherm.ChannelId.HR126_MODBUS, new SignedWordElement(126),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            new DummyRegisterElement(127),
                            m(HeatpumpHeliotherm.ChannelId.HR128_MODBUS, new UnsignedWordElement(128),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliotherm.ChannelId.HR129_MODBUS, new SignedWordElement(129),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliotherm.ChannelId.HR130_MODBUS, new UnsignedWordElement(130),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliotherm.ChannelId.HR131_MODBUS, new SignedWordElement(131),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliotherm.ChannelId.HR132_MODBUS, new UnsignedWordElement(132),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC16WriteRegistersTask(149,
                            m(HeatpumpHeliotherm.ChannelId.HR149_MODBUS, new SignedWordElement(149),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliotherm.ChannelId.HR150_MODBUS, new SignedWordElement(150),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    )
            );
        }
        return protocol;
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
        } else if (this.readOnly == false && this.connectionAlive && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            this.writeCommands();
        }
    }

    // Put values in channels that are not directly Modbus read values but derivatives.
    protected void channelmapping() {

        // Map heatingPowerPercent
        if (this.getElectricPowerConsumption().isDefined()) {
            // The channel READ_EFFECTIVE_POWER_PERCENT in the Heater interface tracks the heating power. Assume the
            // heating power of the heatpump scales linearly with the electric power draw. Then calculate heating power
            // in percent with currentElectricPower / maxElectricPower.
            double effectivePowerPercent = (this.getElectricPowerConsumption().get() * 1.0 / this.maxElectricPower) * 100;    // Convert to %.
            this._setEffectiveHeatingPowerPercent(effectivePowerPercent);
        }

        // Evaluate error indicator
        boolean heatpumpError = false;
        if (this.getErrorIndicator().isDefined()) {
            this.connectionAlive = true; // ToDo: test if getErrorIndicator() goes back to ’not defined’ on connection loss.
            heatpumpError = getErrorIndicator().get();
            if (heatpumpError) {
                this._setErrorMessage("An unknown error occurred.");
            } else {
                this._setErrorMessage("No error");
            }
        } else {
            this.connectionAlive = false;
            this._setErrorMessage("No Modbus connection.");
        }
        this.getErrorMessageChannel().nextProcessImage();

        // Get some status parameters
        boolean heatpumpRunning = getHeatpumpRunningIndicator().isDefined() && getHeatpumpRunningIndicator().get();
        boolean heatpumpDsmIndicator = getDsmIndicator().isDefined() && getDsmIndicator().get();
        boolean runRequestActive = getRunRequestType().isDefined() && getRunRequestType().get() > 0;
        boolean heatpumpReady = false;
        OperatingMode operatingModeEnum = OperatingMode.UNDEFINED;
        if (getHr100OperatingMode().isDefined()) {
            operatingModeEnum = getHr100OperatingMode().asEnum();
            if (operatingModeEnum.getValue() < 8 && operatingModeEnum.getValue() >= 0) {
                heatpumpReady = true;
            }
        }

        // Set Heater interface STATUS channel
        if (this.connectionAlive) {
            if (heatpumpRunning) {
                this._setHeaterState(HeaterState.RUNNING.getValue());
            } else if (heatpumpDsmIndicator || heatpumpError) {
                this._setHeaterState(HeaterState.BLOCKED_OR_ERROR.getValue());
            } else if (runRequestActive) {
                this._setHeaterState(HeaterState.STARTING_UP_OR_PREHEAT.getValue());
            } else if (heatpumpReady) {
                this._setHeaterState(HeaterState.STANDBY.getValue());
            } else if (operatingModeEnum == OperatingMode.OFF || operatingModeEnum == OperatingMode.MAIN_SWITCH_OFF) {
                this._setHeaterState(HeaterState.OFF.getValue());
            } else {
                this._setHeaterState(HeaterState.UNDEFINED.getValue());
            }
        } else {
            this._setHeaterState(HeaterState.UNDEFINED.getValue());
        }
        this.getHeaterStateChannel().nextProcessImage();
    }

    /**
     * Determine commands and send them to the heater.
     * From the manual:
     * "The registers should be written to cyclically, but not faster than every 5 seconds."
     * ("Die Register sollten prinzipiell zyklisch, aber nicht schneller als in einem 5 Sekunden-Intervall beschrieben
     * werden.")
     * Testing suggests writing to a register once is enough.
     * The OpenEMS Modbus implementation can potentially send a write every cycle if there is a ’nextWrite’ available in
     * the channel (it does ’getAndReset’). This is faster than the heat pump allows. The code takes care of this
     * limitation and hides it from the user in the following way:
     * The heat pump has ’public’ write channels that do not have their ’nextWrite’ mapped to Modbus registers. Every
     * ’public’ write channel has a non public duplicate (marked ’internal use only’) that has it's ’nextWrite’ mapped
     * to Modbus. The non public duplicate channel gets the ’nextWrite’ of the ’public’ channel every 6 seconds and
     * sends it to the heat pump. The ’public’ channels can then be used without worrying about the 5 sec rule.
     */
    protected void writeCommands() {

        // Handle OperatingMode channel
        Optional<Integer> operatingModeOptional = this.getHr100OperatingModeChannel().getNextWriteValueAndReset();
        if (operatingModeOptional.isPresent()) {
            int enumAsInt = operatingModeOptional.get();
            // Restrict to valid write values
            if (enumAsInt >= 0 && enumAsInt <= 7) {
                this.operatingModeSetting = OperatingMode.valueOf(enumAsInt);
            }
        }

        // Handle set point power percent, if heat pump is in a configuration that allows it.
        int powerPercentToModbus = 0;
        boolean powerSetPointAvailable = false;
        Optional<Double> heatingPowerPercentSetPointOptional = this.getHeatingPowerPercentSetpointChannel().getNextWriteValueAndReset();
        if (heatingPowerPercentSetPointOptional.isPresent()
                && (this.powerControlSetting == PowerControlSetting.COMPRESSOR_SPEED || this.mapPowerPercentToConsumption)) {
            this.lastPowerPercentSetPoint = heatingPowerPercentSetPointOptional.get();
            this.lastPowerPercentSetPoint = Math.min(this.lastPowerPercentSetPoint, 100);
            this.lastPowerPercentSetPoint = Math.max(this.lastPowerPercentSetPoint, 0);
            if (this.mapPowerPercentToConsumption && this.lastPowerPercentSetPoint > 1) {
                this.lastConsumptionSetPoint = (int) Math.round((this.lastPowerPercentSetPoint * this.maxElectricPower) / 100);
            }
            this._setHeatingPowerPercentSetpoint(this.lastPowerPercentSetPoint);
        }
        if (this.powerControlSetting == PowerControlSetting.COMPRESSOR_SPEED && this.lastPowerPercentSetPoint > 1) {
            powerSetPointAvailable = true;
            // Modbus unit is per mill, so for 100% send 1000 to Modbus.
            powerPercentToModbus = (int) Math.round(this.lastPowerPercentSetPoint * 10 * (this.maxCompressorSpeed / 100.0));
        }

        // Handle set point power consumption, if heat pump is in a configuration that allows it.
        int consumptionToModbus = 0;
        Optional<Integer> powerConsumptionSetPointOptional = this.getHr125SetPointElectricPowerChannel().getNextWriteValueAndReset();
        if (powerConsumptionSetPointOptional.isPresent()
                && this.powerControlSetting == PowerControlSetting.CONSUMPTION && this.mapPowerPercentToConsumption == false) {
            this.lastConsumptionSetPoint = powerConsumptionSetPointOptional.get();
        }
        if (this.powerControlSetting == PowerControlSetting.CONSUMPTION && this.lastConsumptionSetPoint > 1) {
            powerSetPointAvailable = true;
            consumptionToModbus = this.lastConsumptionSetPoint;
        }

        // Determine if heat pump is set to temperature mode or EnableSignal. This can change at runtime.
        Optional<Boolean> useTemperatureControlModeOptional = this.getHr103UseSetPointTemperatureChannel().getNextWriteValueAndReset();
        if (useTemperatureControlModeOptional.isPresent()) {
            if (useTemperatureControlModeOptional.get()) {
                this.controlModeSetting = ControlMode.TEMPERATURE_SET_POINT;
            } else {
                this.controlModeSetting = ControlMode.ENABLE_SIGNAL;
            }
        }

        // Logic code of operating modes temperature and EnableSignal.
        int operatingModeToModbus = 0;
        if (this.controlModeSetting == ControlMode.TEMPERATURE_SET_POINT) {
            Optional<Integer> temperatureSetPointOptional = this.getTemperatureSetpointChannel().getNextWriteValueAndReset();
            temperatureSetPointOptional.ifPresent(integer -> this.lastTemperatureSetPoint = integer);
            this.lastTemperatureSetPoint = Math.min(this.lastTemperatureSetPoint, 1200);
            this.lastTemperatureSetPoint = Math.max(this.lastTemperatureSetPoint, 0);
            operatingModeToModbus = this.operatingModeSetting.getValue();
        } else {

            // Handle EnableSignal.
            boolean turnOnHeatpump = this.enableSignalHandler.deviceShouldBeHeating(this);

            // Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
            if (this.useExceptionalState) {
                boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
                if (exceptionalStateActive) {
                    int exceptionalStateValue = this.getExceptionalStateValue();
                    if (exceptionalStateValue <= this.DEFAULT_MIN_EXCEPTIONAL_VALUE) {
                        turnOnHeatpump = false;
                    } else {
                        powerSetPointAvailable = true;
                        turnOnHeatpump = true;
                        exceptionalStateValue = Math.min(exceptionalStateValue, this.DEFAULT_MAX_EXCEPTIONAL_VALUE);

                        // Calculate a power set point from the exceptionalStateValue, depending on heat pump config.
                        if (this.powerControlSetting == PowerControlSetting.COMPRESSOR_SPEED) {
                            // Modbus unit is per mill, so 100% = 1000.
                            powerPercentToModbus = (int) Math.round(exceptionalStateValue * 10 * (this.maxCompressorSpeed / 100.0));
                        } else {
                            consumptionToModbus = (exceptionalStateValue * this.maxElectricPower) / 100;
                        }
                    }
                }
            }

            // Turn on heater when enableSignal == true.
            if (turnOnHeatpump) {
                // Warning: operatingModeSetting can be set to OFF, meaning the heat pump won't switch on!
                operatingModeToModbus = this.operatingModeSetting.getValue();
            } else {
                operatingModeToModbus = OperatingMode.OFF.getValue();
            }
        }

        // This part handles the 5 second rule. Write values are only sent every ’sendIntervalSeconds’ seconds.
        if (ChronoUnit.SECONDS.between(this.fiveSecondTimestamp, LocalDateTime.now()) >= sendIntervalSeconds) {
            this.fiveSecondTimestamp = LocalDateTime.now();

            try {
                this.getHr100ModbusChannel().setNextWriteValue(operatingModeToModbus);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't write in Channel " + e.getMessage());
            }

            if (this.controlModeSetting == ControlMode.TEMPERATURE_SET_POINT) {
                try {
                    this.getHr102ModbusChannel().setNextWriteValue(this.lastTemperatureSetPoint);
                    this.getHr103ModbusChannel().setNextWriteValue(true);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            } else {
                try {
                    this.getHr103ModbusChannel().setNextWriteValue(false);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }

            try {
                this.getHr117ModbusChannel().setNextWriteValue(powerSetPointAvailable);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't write in Channel " + e.getMessage());
            }
            if (this.powerControlSetting == PowerControlSetting.CONSUMPTION) {
                try {
                    this.getHr125ModbusChannel().setNextWriteValue(consumptionToModbus);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            } else {
                try {
                    this.getHr126ModbusChannel().setNextWriteValue(powerPercentToModbus);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }

            Optional<Boolean> writeValueHr128 = this.getHr128ResetErrorChannel().getNextWriteValueAndReset();
            if (writeValueHr128.isPresent()) {
                try {
                    this.getHr128ModbusChannel().setNextWriteValue(writeValueHr128.get());
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }

            Optional<Integer> writeValueHr129 = this.getHr129OutsideTemperatureSendChannel().getNextWriteValueAndReset();
            if (writeValueHr129.isPresent()) {
                try {
                    this.getHr129ModbusChannel().setNextWriteValue(writeValueHr129.get());
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }
            Optional<Boolean> writeValueHr130 = this.getHr130UseModbusOutsideTemperatureSettingChannel().getNextWriteValueAndReset();
            if (writeValueHr130.isPresent()) {
                try {
                    this.getHr130ModbusChannel().setNextWriteValue(writeValueHr130.get());
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }

            Optional<Integer> writeValueHr131 = this.getHr131StorageTankTemperatureSendChannel().getNextWriteValueAndReset();
            if (writeValueHr131.isPresent()) {
                try {
                    this.getHr131ModbusChannel().setNextWriteValue(writeValueHr131.get());
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }
            Optional<Boolean> writeValueHr132 = this.getHr132UseModbusStorageTankTemperatureSettingChannel().getNextWriteValueAndReset();
            if (writeValueHr132.isPresent()) {
                try {
                    this.getHr132ModbusChannel().setNextWriteValue(writeValueHr132.get());
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }

            Optional<Boolean> writeValueHr149 = this.getHr149DsmSwitchChannel().getNextWriteValueAndReset();
            if (writeValueHr149.isPresent()) {
                try {
                    this.getHr149ModbusChannel().setNextWriteValue(writeValueHr149.get());
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }
            Optional<Boolean> writeValueHr150 = this.getHr150UseModbusDsmSwitchChannel().getNextWriteValueAndReset();
            if (writeValueHr150.isPresent()) {
                try {
                    this.getHr150ModbusChannel().setNextWriteValue(writeValueHr150.get());
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel " + e.getMessage());
                }
            }

        }
    }

    /**
     * Information that is printed to the log if ’print info to log’ option is enabled.
     */
    protected void printInfo() {
        this.logInfo(this.log, "--Heatpump Heliotherm--");
        this.logInfo(this.log, "State enum: " + this.getHeaterState());
        this.logInfo(this.log, "Operating mode: " + getHr100OperatingMode().asEnum().getName());
        this.logInfo(this.log, "Compressor speed: " + getCompressorSpeed());
        this.logInfo(this.log, "OutsideTemperature: " + getOutsideTemperature());
        this.logInfo(this.log, "Flow temperature: " + getFlowTemperature());
        this.logInfo(this.log, "Return temperature: " + getReturnTemperature());
        this.logInfo(this.log, "Buffer temperature: " + getStorageTankTemperature());
        this.logInfo(this.log, "SetPoint Temperature " + getTemperatureSetPointIndicator());
        this.logInfo(this.log, "Current heating power: " + this.getEffectiveHeatingPower());
        /*
        this.logInfo(this.log, "Current electric power consumption: " + getElectricPowerConsumption());
        this.logInfo(this.log, "Current coefficient of performance: " + getCop());

        double heatingPowerFromCop = 0;
        if (getCop().isDefined() && getElectricPowerConsumption().isDefined()) {
            // ToDo: check if cop has the right dimension.
            heatingPowerFromCop = getElectricPowerConsumption().get() * (getCop().get() / 10.0) / 1000; // Convert to kilowatt
        }
        this.logInfo(this.log, "Heating power calculated from cop & electric power: " + heatingPowerFromCop + " [kW]");
        */

        this.logInfo(this.log, "Run request code: " + getRunRequestType().get());
        this.logInfo(this.log, "Error message: " + getErrorMessage().get());
        this.logInfo(this.log, "");
    }

    /**
     * Returns the debug message.
     *
     * @return the debug message.
     */
    public String debugLog() {
        String debugMessage = this.getHeaterState().asEnum().asCamelCase() //
                + "|F:" + this.getFlowTemperature().asString() //
                + "|R:" + this.getReturnTemperature().asString(); //
        if (this.getErrorMessage().get().equals("No error") == false) {
            debugMessage = debugMessage + "|Error";
        }
        return debugMessage;
    }
}
