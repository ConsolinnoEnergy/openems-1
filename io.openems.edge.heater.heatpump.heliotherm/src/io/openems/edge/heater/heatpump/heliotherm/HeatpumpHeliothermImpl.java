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
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.heatpump.heliotherm.api.HeatpumpHeliotherm;
import io.openems.edge.heater.heatpump.heliotherm.api.OperatingMode;
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


/**
 * This module reads the most important variables available via Modbus from a Heliotherm heatpump and maps them to OpenEMS
 * channels. The module is written to be used with the Heater interface methods.
 * When setEnableSignal() from the Heater interface is set to true with no other parameters like temperature specified,
 * the heatpump will turn on with default settings. The default settings are configurable in the config.
 * The heatpump can be controlled with setSetPointTemperature() and/or setSetPointPowerPercent().
 * A certain configuration of the pump is required for setSetPointPowerPercent() to work.
 * setSetPointPower() and related methods are not supported by this heater.
 */

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
    private LocalDateTime fiveSecondTimestamp;
    private int defaultSetPointPowerPercent;
    private int defaultSetPointTemperature;
    private int maxElectricPower;
    private boolean heatpumpError = false;
    private boolean readOnly;

    private TimerHandler timer;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "HEAT_PUMP_HELIOTHERM_ENABLE_SIGNAL_IDENTIFIER";
    private EnableSignalHandler enableSignalHandler;

    // This is essential for Modbus to work, but the compiler does not warn you when it is missing!
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public HeatpumpHeliothermImpl() {
        super(OpenemsComponent.ChannelId.values(),
                HeatpumpHeliotherm.ChannelId.values(),
                Heater.ChannelId.values());    // Even though ChpKwEnergySmartblockChannel extends this channel, it needs to be added separately.
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());

        this.printInfoToLog = config.debug();
        this.fiveSecondTimestamp = LocalDateTime.now().minusMinutes(5);    // Initialize with past time value so code executes immediately on first run.
        this.defaultSetPointPowerPercent = config.defaultSetPointPowerPercent();
        this.defaultSetPointTemperature = config.defaultSetPointTemperature() * 10; // Convert to d°C.
        this.maxElectricPower = config.maxElectricPower();
        this.readOnly = config.readOnly();
        this.timer = new TimerHandlerImpl(super.id(), this.cpm);
        this.timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, config.timerIdEnableSignal(), config.waitTimeEnableSignal());
        this.enableSignalHandler = new EnableSignalHandlerImpl(this.timer, ENABLE_SIGNAL_IDENTIFIER);
        if (this.isEnabled() == false) {
            this._setHeaterState(HeaterState.OFF.getValue());
        }
        this.getOperatingModeChannel().setNextWriteValue(config.defaultOperatingMode().getValue());

    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        ModbusProtocol protocol = new ModbusProtocol(this,
                // Input register read.
                new FC4ReadInputRegistersTask(12, Priority.HIGH,
                        // Use SignedWordElement when the number can be negative. Signed 16bit maps every number >32767
                        // to negative. That means if the value you read is positive and <32767, there is no difference
                        // between signed and unsigned.
                        m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(12),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(13),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.IR14_BUFFER_TEMPERATURE, new SignedWordElement(14),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(25, Priority.HIGH,
                        m(HeatpumpHeliotherm.ChannelId.IR25_HEATPUMP_RUNNING, new SignedWordElement(25),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.IR26_ERROR, new SignedWordElement(26),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        new DummyRegisterElement(27),
                        new DummyRegisterElement(28),
                        m(HeatpumpHeliotherm.ChannelId.IR29_READ_VERDICHTER_DREHZAHL, new SignedWordElement(29),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        m(HeatpumpHeliotherm.ChannelId.IR30_COP, new SignedWordElement(30),
                                ElementToChannelConverter.SCALE_FACTOR_1),
                        new DummyRegisterElement(31),
                        m(HeatpumpHeliotherm.ChannelId.IR32_EVU_FREIGABE, new SignedWordElement(32),
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
                        m(HeatpumpHeliotherm.ChannelId.IR70_71_CURRENT_ELECTRIC_POWER, new UnsignedDoublewordElement(70),
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
                        m(HeatpumpHeliotherm.ChannelId.HR102_SET_POINT_TEMPERATUR, new SignedWordElement(102),
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
                        m(HeatpumpHeliotherm.ChannelId.HR126_SET_POINT_VERDICHTERDREHZAHL_PERCENT, new SignedWordElement(126),
                                ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                        new DummyRegisterElement(127),
                        m(HeatpumpHeliotherm.ChannelId.HR128_RESET_ERROR, new UnsignedWordElement(128),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.HR129_OUTSIDE_TEMPERATURE, new SignedWordElement(129),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.HR130_USE_MODBUS_OUTSIDE_TEMPERATURE, new UnsignedWordElement(130),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.HR131_BUFFER_TEMPERATURE, new SignedWordElement(131),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.HR132_USE_MODBUS_BUFFER_TEMPERATURE, new UnsignedWordElement(132),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC3ReadRegistersTask(149, Priority.HIGH,
                        m(HeatpumpHeliotherm.ChannelId.HR149_EVU_FREIGABE, new SignedWordElement(149),
                                ElementToChannelConverter.DIRECT_1_TO_1),
                        m(HeatpumpHeliotherm.ChannelId.HR150_USE_MODBUS_EVU_FREIGABE, new SignedWordElement(150),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                )
        );

        if (this.readOnly == false) {
            protocol.addTasks(
                    new FC16WriteRegistersTask(100,
                            m(HeatpumpHeliotherm.ChannelId.HR100_OPERATING_MODE, new UnsignedWordElement(100),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC6WriteRegisterTask(102,
                            m(HeatpumpHeliotherm.ChannelId.HR102_SET_POINT_TEMPERATUR, new SignedWordElement(102))),
                    new FC6WriteRegisterTask(103,
                            m(HeatpumpHeliotherm.ChannelId.HR103_USE_SET_POINT_TEMPERATURE, new UnsignedWordElement(103))),

                    new FC16WriteRegistersTask(117,
                            m(HeatpumpHeliotherm.ChannelId.HR117_USE_POWER_CONTROL, new UnsignedWordElement(117),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC16WriteRegistersTask(125,
                            m(HeatpumpHeliotherm.ChannelId.HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION, new UnsignedWordElement(125),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliotherm.ChannelId.HR126_SET_POINT_VERDICHTERDREHZAHL_PERCENT, new SignedWordElement(126),
                                    ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                            new DummyRegisterElement(127),
                            m(HeatpumpHeliotherm.ChannelId.HR128_RESET_ERROR, new UnsignedWordElement(128),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliotherm.ChannelId.HR129_OUTSIDE_TEMPERATURE, new SignedWordElement(129),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliotherm.ChannelId.HR130_USE_MODBUS_OUTSIDE_TEMPERATURE, new UnsignedWordElement(130),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliotherm.ChannelId.HR131_BUFFER_TEMPERATURE, new SignedWordElement(131),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliotherm.ChannelId.HR132_USE_MODBUS_BUFFER_TEMPERATURE, new UnsignedWordElement(132),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC16WriteRegistersTask(149,
                            m(HeatpumpHeliotherm.ChannelId.HR149_EVU_FREIGABE, new SignedWordElement(149),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliotherm.ChannelId.HR150_USE_MODBUS_EVU_FREIGABE, new SignedWordElement(150),
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
        } else if (this.readOnly == false && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            this.writeCommands();
        }
    }

    // Put values in channels that are not directly Modbus read values but derivatives.
    protected void channelmapping() {

        if (readOnly == false) {

            boolean turnOnHeatpump = this.enableSignalHandler.deviceShouldBeHeating(this);

            // Map the set points. Set default set point (defined in config) if there is no set point in the channels
            int setPointTemperature = defaultSetPointTemperature;
            double setPointPowerPercent = defaultSetPointPowerPercent;
            if (this.getTemperatureSetpoint().isDefined()) {
                setPointTemperature = this.getTemperatureSetpoint().get();
            }
            if (this.getHeatingPowerPercentSetpoint().isDefined()) {
                setPointPowerPercent = this.getHeatingPowerPercentSetpoint().get();
            }
            if (setPointPowerPercent > 100) {
                setPointPowerPercent = 100;
            } else if (setPointPowerPercent < 0) {
                setPointPowerPercent = 0;
            }
            int setPointElectricPower = (int) Math.round((setPointPowerPercent * maxElectricPower) / 100);


            // Handbuch: "Die Register sollten prinzipiell zyklisch, aber nicht schneller als in einem 5 Sekunden-Intervall
            // beschrieben werden."
            // -> Modbus Writes machen get and reset vom nextWriteValue, senden nur wenn dort ein Wert steht.
            // Deswegen: sämtliche Write Channel die auf Holding Register gemapped sind haben Setter als interne Methode
            // gekennzeichnet. Diese Setter dürfen nur von diesem Modul benutzt werden. Ihre Verwendung ist beschränkt auf
            // einen Codebereich der nur alle 5s ausgeführt wird.

            // All Modbus writes are only allowed in this if statement. You should not send writes to the heatpump faster
            // than every 5 seconds.
            if (ChronoUnit.SECONDS.between(fiveSecondTimestamp, LocalDateTime.now()) >= 6) {
                fiveSecondTimestamp = LocalDateTime.now();

                // Turn on heater when enableSignal == true.
                if (turnOnHeatpump) {
                    // If nothing is in the channel yet, take set point power percent as default behavior.
                    boolean useSetPointTemperature = getOperatingMode().isDefined() && (getOperatingMode().asEnum() == OperatingMode.SET_POINT_TEMPERATURE);

                    try {
                        if (useSetPointTemperature) {
                            _setHr100OperatingMode(1);    // Automatic
                            _setHr102SetPointTemperature(setPointTemperature);
                            _setHr103UseSetPointTemperature(1);
                            //_setHr117UsePowerControl(0);
                            // Set point temperature mappen
                        } else {
                            _setHr100OperatingMode(4);    // Dauerbetrieb
                            _setHr125SetPointElectricPower(setPointElectricPower);
                            _setHr103UseSetPointTemperature(0);
                            _setHr117UsePowerControl(1);
                            // Set point power mappen
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Couldn't write in Channel " + e.getMessage());
                    }
                } else {
                    try {
                        _setHr100OperatingMode(0);    // Aus
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Couldn't write in Channel " + e.getMessage());
                    }
                }
            }
        }

        // Status and other.
        if (this.getCurrentElectricPower().isDefined()) {
            // The channel READ_EFFECTIVE_POWER_PERCENT in the Heater interface tracks the heating power. Assume the
            // heating power of the heatpump scales linearly with the electric power draw. Then calculate heating power
            // in percent with currentElectricPower / maxElectricPower.
            double effectivePowerPercent = (this.getCurrentElectricPower().get() * 1.0 / this.maxElectricPower) * 100;    // Convert to %.
            this._setEffectiveHeatingPowerPercent(effectivePowerPercent);
        }
        boolean connectionAlive = false;
        if (this.getErrorIndicator().isDefined()) {
            this.heatpumpError = getErrorIndicator().get();
            connectionAlive = true;        // ToDo: testen ob getErrorIndicator() auf not defined geht wenn die Verbindung weg ist.
        } else {
            connectionAlive = false;
        }

        boolean heatpumpRunning = getHeatpumpRunningIndicator().isDefined() && getHeatpumpRunningIndicator().get();
        boolean heatpumpEvuIndicator = getEvuFreigabeIndicator().isDefined() && getEvuFreigabeIndicator().get();
        boolean heatpumpReady = false;
        int operatingModeRegister = 0;
        if (getHr100OperatingMode().isDefined()) {
            operatingModeRegister = getHr100OperatingMode().get();
            if (operatingModeRegister < 8) {
                heatpumpReady = true;
            }
        }

        // Set Heater interface STATUS channel
        if (connectionAlive == false || heatpumpEvuIndicator == false) {
            this._setHeaterState(HeaterState.OFF.getValue());
        } else if (heatpumpRunning) {
            this._setHeaterState(HeaterState.RUNNING.getValue());
        } else if (heatpumpReady) {
            this._setHeaterState(HeaterState.STANDBY.getValue());
        } else {
            // If the code gets to here, the state is undefined.
            this._setHeaterState(HeaterState.UNDEFINED.getValue());
        }

        // Parse status, fill status channel.
        if (connectionAlive) {
            String statusMessage = "";
            switch (operatingModeRegister) {
                case 0:
                    statusMessage = "Heatpump status: off, ";
                    break;
                case 1:
                    statusMessage = "Heatpump operating mode: Automatik, ";
                    break;
                case 2:
                    statusMessage = "Heatpump operating mode: Kühlen, ";
                    break;
                case 3:
                    statusMessage = "Heatpump operating mode: Sommer, ";
                    break;
                case 4:
                    statusMessage = "Heatpump operating mode: Dauerbetrieb, ";
                    break;
                case 5:
                    statusMessage = "Heatpump operating mode: Absenkung, ";
                    break;
                case 6:
                    statusMessage = "Heatpump operating mode: Urlaub, ";
                    break;
                case 7:
                    statusMessage = "Heatpump operating mode: Party, ";
                    break;
                case 8:
                    statusMessage = "Heatpump status: Ausheizen, ";
                    break;
                case 9:
                    statusMessage = "Heatpump status: EVU Sperre, ";
                    break;
                case 10:
                    statusMessage = "Heatpump status: Hauptschalter aus, ";
                    break;
            }
            if (heatpumpError) {
                statusMessage = statusMessage + " Störung, ";
            }
            if (heatpumpRunning) {
                statusMessage = statusMessage + " Pumpe läuft, ";
                if (getEffectiveHeatingPower().isDefined() && getEffectiveHeatingPower().get() > 0) {
                    statusMessage = statusMessage + " Heizleistung " + getEffectiveHeatingPower().get() + " kW, ";
                }
            } else {
                statusMessage = statusMessage + " Pumpe steht, ";
            }
            statusMessage = statusMessage.substring(0, statusMessage.length() - 2) + ".";
            _setStatusMessage(statusMessage);
        } else {
            _setStatusMessage("Modbus not connected.");
        }


        if (printInfoToLog) {
            this.logInfo(this.log, "--Heatpump Heliotherm--");
            this.logInfo(this.log, "Flow temperature: " + getFlowTemperature() + " [d°C]");
            this.logInfo(this.log, "Buffer temperature: " + getBufferTemperature().get() + " [d°C]");
            this.logInfo(this.log, "Return temperature: " + getReturnTemperature() + " [d°C]");
            this.logInfo(this.log, "OutsideTemperature: " + getHr129OutsideTemperature() + " [d°C]");
            this.logInfo(this.log, "SetPoint Temperature " + getSetPointTemperatureIndicator() + "[d°C]");
            this.logInfo(this.log, "Current heating power: " + this.getEffectiveHeatingPower() + " [kW]");
            this.logInfo(this.log, "Current electric power consumption: " + getCurrentElectricPower().get() + " [W]");
            this.logInfo(this.log, "Current coefficient of performance: " + getCop().get());

            double heatingPowerFromCop = 0;
            if (getCop().isDefined() && getCurrentElectricPower().isDefined()) {
                // ToDo: check if cop has the right dimension.
                heatingPowerFromCop = getCurrentElectricPower().get() * (getCop().get() / 10.0) / 1000; // Convert to kilowatt
            }

            this.logInfo(this.log, "Heating power calculated from cop & electric power: " + heatingPowerFromCop + " [kW]");
            this.logInfo(this.log, "Verdichterdrehzahl: " + getVerdichterDrehzahl() + " [%]");
            this.logInfo(this.log, "State enum: " + this.getHeaterState());
            this.logInfo(this.log, "Status message: " + getStatusMessage().get());
            this.logInfo(this.log, "");
        }

    }

    /**
     * Determine commands and send them to the heater.
     */
    protected void writeCommands() {

    }

    /**
     * Information that is printed to the log if ’print info to log’ option is enabled.
     */
    protected void printInfo() {

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
