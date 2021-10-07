package io.openems.edge.heater.heatpump.heliotherm;

import io.openems.common.exceptions.OpenemsError;
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
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

public class HeatpumpHeliothermImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
        ExceptionalState, HeatpumpHeliotherm {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    protected ComponentManager cpm;

    private final Logger log = LoggerFactory.getLogger(HeatpumpHeliothermImpl.class);
    private boolean debug;
    private boolean componentEnabled;
    private LocalDateTime fiveSecondTimestamp;
    private boolean turnOnHeatpump;
    private int defaultSetPointPowerPercent;
    private int defaultSetPointTemperature;
    private int maxElectricPower;
    private int coefficientOfPerformance = 2;    // Fallback value.
    private boolean heatpumpError = false;
    private boolean readOnly;
    private boolean activeBefore;

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
        componentEnabled = config.enabled();
        debug = config.debug();
        fiveSecondTimestamp = LocalDateTime.now().minusMinutes(5);    // Initialize with past time value so code executes immediately on first run.
        defaultSetPointPowerPercent = config.defaultSetPointPowerPercent();
        defaultSetPointTemperature = config.defaultSetPointTemperature() * 10; // Convert to d°C.
        maxElectricPower = config.maxElectricPower();
        readOnly = config.readOnly();
        this.timer = new TimerHandlerImpl(super.id(), this.cpm);
        this.timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, config.timerIdEnableSignal(), config.waitTimeEnableSignal());
        this.enableSignalHandler = new EnableSignalHandlerImpl(this.timer, ENABLE_SIGNAL_IDENTIFIER);
        if (componentEnabled == false) {
            setState(HeaterState.OFFLINE.name());
        }
        this.getOperatingModeChannel().setNextWriteValue(config.defaultOperatingMode().getValue());

    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {
        return createModbusProtocol();/*
        if (readOnly == false) {
            return new ModbusProtocol(this,
                    // Input register read.
                    new FC4ReadInputRegistersTask(12, Priority.HIGH,
                            // Use SignedWordElement when the number can be negative. Signed 16bit maps every number >32767
                            // to negative. That means if the value you read is positive and <32767, there is no difference
                            // between signed and unsigned.
                            m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(12),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(13),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.IR14_BUFFER_TEMPERATURE, new SignedWordElement(14),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC4ReadInputRegistersTask(25, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.IR25_HEATPUMP_RUNNING, new SignedWordElement(25),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.IR26_ERROR, new SignedWordElement(26),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            new DummyRegisterElement(27),
                            new DummyRegisterElement(28),
                            m(HeatpumpHeliothermChannel.ChannelId.IR29_READ_VERDICHTER_DREHZAHL, new SignedWordElement(29),
                                    ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                            m(HeatpumpHeliothermChannel.ChannelId.IR30_COP, new SignedWordElement(30),
                                    ElementToChannelConverter.SCALE_FACTOR_1),
                            new DummyRegisterElement(31),
                            m(HeatpumpHeliothermChannel.ChannelId.IR32_EVU_FREIGABE, new SignedWordElement(32),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            new DummyRegisterElement(33),
                            m(HeatpumpHeliothermChannel.ChannelId.IR34_READ_TEMP_SET_POINT, new SignedWordElement(32),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC4ReadInputRegistersTask(41, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.IR41_RUN_REQUEST_TYPE, new SignedWordElement(41),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC4ReadInputRegistersTask(70, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.IR70_71_CURRENT_ELECTRIC_POWER, new UnsignedDoublewordElement(70),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC4ReadInputRegistersTask(74, Priority.HIGH,
                            m(Heater.ChannelId.READ_EFFECTIVE_POWER, new UnsignedDoublewordElement(74),
                                    ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
                    ),

                    // Holding register read.
                    new FC3ReadRegistersTask(100, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.HR100_OPERATING_MODE, new UnsignedWordElement(100),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            new DummyRegisterElement(101),
                            m(HeatpumpHeliothermChannel.ChannelId.HR102_SET_POINT_TEMPERATUR, new SignedWordElement(102),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR103_USE_SET_POINT_TEMPERATURE, new UnsignedWordElement(103),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC3ReadRegistersTask(117, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.HR117_USE_POWER_CONTROL, new UnsignedWordElement(117),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC3ReadRegistersTask(125, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION, new UnsignedWordElement(125),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR126_SET_POINT_VERDICHTERDREHZAHL_PERCENT, new SignedWordElement(126),
                                    ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                            new DummyRegisterElement(127),
                            m(HeatpumpHeliothermChannel.ChannelId.HR128_RESET_ERROR, new UnsignedWordElement(128),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR129_OUTSIDE_TEMPERATURE, new SignedWordElement(129),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR130_USE_MODBUS_OUTSIDE_TEMPERATURE, new UnsignedWordElement(130),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR131_BUFFER_TEMPERATURE, new SignedWordElement(131),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR132_USE_MODBUS_BUFFER_TEMPERATURE, new UnsignedWordElement(132),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC3ReadRegistersTask(149, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.HR149_EVU_FREIGABE, new SignedWordElement(149),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR150_USE_MODBUS_EVU_FREIGABE, new SignedWordElement(150),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),

                    // Holding register write.
                    // Modbus write tasks take the "setNextWriteValue" value of a channel and send them to the device.
                    // Modbus read tasks put values in the "setNextValue" field, which get automatically transferred to the
                    // "value" field of the channel. By default, the "setNextWriteValue" field is NOT copied to the
                    // "setNextValue" and "value" field. In essence, this makes "setNextWriteValue" and "setNextValue"/"value"
                    // two separate channels.
                    // That means: Modbus read tasks will not overwrite any "setNextWriteValue" values. You do not have to
                    // watch the order in which you call read and write tasks.
                    // Also: if you do not add a Modbus read task for a write channel, any "setNextWriteValue" values will
                    // not be transferred to the "value" field of the channel, unless you add code that does that.
                    new FC16WriteRegistersTask(100,
                            m(HeatpumpHeliothermChannel.ChannelId.HR100_OPERATING_MODE, new UnsignedWordElement(100),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            new DummyRegisterElement(101),
                            m(HeatpumpHeliothermChannel.ChannelId.HR102_SET_POINT_TEMPERATUR, new SignedWordElement(102),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR103_USE_SET_POINT_TEMPERATURE, new UnsignedWordElement(103),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC16WriteRegistersTask(117,
                            m(HeatpumpHeliothermChannel.ChannelId.HR117_USE_POWER_CONTROL, new UnsignedWordElement(117),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC16WriteRegistersTask(125,
                            m(HeatpumpHeliothermChannel.ChannelId.HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION, new UnsignedWordElement(125),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR126_SET_POINT_VERDICHTERDREHZAHL_PERCENT, new SignedWordElement(126),
                                    ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                            new DummyRegisterElement(127),
                            m(HeatpumpHeliothermChannel.ChannelId.HR128_RESET_ERROR, new UnsignedWordElement(128),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR129_OUTSIDE_TEMPERATURE, new SignedWordElement(129),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR130_USE_MODBUS_OUTSIDE_TEMPERATURE, new UnsignedWordElement(130),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR131_BUFFER_TEMPERATURE, new SignedWordElement(131),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR132_USE_MODBUS_BUFFER_TEMPERATURE, new UnsignedWordElement(132),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC16WriteRegistersTask(149,
                            m(HeatpumpHeliothermChannel.ChannelId.HR149_EVU_FREIGABE, new SignedWordElement(149),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR150_USE_MODBUS_EVU_FREIGABE, new SignedWordElement(150),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    )
            );
        } else {

            // read only
            return new ModbusProtocol(this,
                    // Input register read.
                    new FC4ReadInputRegistersTask(12, Priority.HIGH,
                            // Use SignedWordElement when the number can be negative. Signed 16bit maps every number >32767
                            // to negative. That means if the value you read is positive and <32767, there is no difference
                            // between signed and unsigned.
                            m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(12),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(13),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.IR14_BUFFER_TEMPERATURE, new SignedWordElement(14),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC4ReadInputRegistersTask(25, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.IR25_HEATPUMP_RUNNING, new SignedWordElement(25),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.IR26_ERROR, new SignedWordElement(26),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            new DummyRegisterElement(27),
                            new DummyRegisterElement(28),
                            m(HeatpumpHeliothermChannel.ChannelId.IR29_READ_VERDICHTER_DREHZAHL, new SignedWordElement(29),
                                    ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                            m(HeatpumpHeliothermChannel.ChannelId.IR30_COP, new SignedWordElement(30),
                                    ElementToChannelConverter.SCALE_FACTOR_1),
                            new DummyRegisterElement(31),
                            m(HeatpumpHeliothermChannel.ChannelId.IR32_EVU_FREIGABE, new SignedWordElement(32),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            new DummyRegisterElement(33),
                            m(HeatpumpHeliothermChannel.ChannelId.IR34_READ_TEMP_SET_POINT, new SignedWordElement(32),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC4ReadInputRegistersTask(41, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.IR41_RUN_REQUEST_TYPE, new SignedWordElement(41),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC4ReadInputRegistersTask(70, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.IR70_71_CURRENT_ELECTRIC_POWER, new UnsignedDoublewordElement(70),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC4ReadInputRegistersTask(74, Priority.HIGH,
                            m(Heater.ChannelId.READ_EFFECTIVE_POWER, new UnsignedDoublewordElement(74),
                                    ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
                    ),

                    // Holding register read.
                    new FC3ReadRegistersTask(100, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.HR100_OPERATING_MODE, new UnsignedWordElement(100),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            new DummyRegisterElement(101),
                            m(HeatpumpHeliothermChannel.ChannelId.HR102_SET_POINT_TEMPERATUR, new SignedWordElement(102),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR103_USE_SET_POINT_TEMPERATURE, new UnsignedWordElement(103),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC3ReadRegistersTask(117, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.HR117_USE_POWER_CONTROL, new UnsignedWordElement(117),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC3ReadRegistersTask(125, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION, new UnsignedWordElement(125),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR126_SET_POINT_VERDICHTERDREHZAHL_PERCENT, new SignedWordElement(126),
                                    ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
                            new DummyRegisterElement(127),
                            m(HeatpumpHeliothermChannel.ChannelId.HR128_RESET_ERROR, new UnsignedWordElement(128),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR129_OUTSIDE_TEMPERATURE, new SignedWordElement(129),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR130_USE_MODBUS_OUTSIDE_TEMPERATURE, new UnsignedWordElement(130),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR131_BUFFER_TEMPERATURE, new SignedWordElement(131),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR132_USE_MODBUS_BUFFER_TEMPERATURE, new UnsignedWordElement(132),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC3ReadRegistersTask(149, Priority.HIGH,
                            m(HeatpumpHeliothermChannel.ChannelId.HR149_EVU_FREIGABE, new SignedWordElement(149),
                                    ElementToChannelConverter.DIRECT_1_TO_1),
                            m(HeatpumpHeliothermChannel.ChannelId.HR150_USE_MODBUS_EVU_FREIGABE, new SignedWordElement(150),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    )
            );
        }*/
    }

    private ModbusProtocol createModbusProtocol() {
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
                        m(Heater.ChannelId.READ_EFFECTIVE_POWER, new UnsignedDoublewordElement(74),
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
                ));


        if (this.readOnly == false) {
            protocol.addTasks(
                    new FC16WriteRegistersTask(100,
                            m(HeatpumpHeliotherm.ChannelId.HR100_OPERATING_MODE, new UnsignedWordElement(100),
                                    ElementToChannelConverter.DIRECT_1_TO_1)
                    ),
                    new FC6WriteRegisterTask(102,
                            m(HeatpumpHeliotherm.ChannelId.HR102_SET_POINT_TEMPERATUR, new SignedWordElement(102))),
                    new FC6WriteRegisterTask(103, m(ChannelId.HR103_USE_SET_POINT_TEMPERATURE, new UnsignedWordElement(103))),

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
                    ));
        }
        return protocol;

    }

    @Override
    public void handleEvent(Event event) {
        if (componentEnabled) {
            switch (event.getTopic()) {
                case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
                    channelmapping();
                    break;
            }
        }
    }

    // Put values in channels that are not directly Modbus read values but derivatives.
    protected void channelmapping() {

        if (readOnly == false) {

			turnOnHeatpump = this.enableSignalHandler.deviceShouldBeHeating(this);

            // Map the set points. Set default set point (defined in config) if there is no set point in the channels
            int setPointTemperature = defaultSetPointTemperature;
            double setPointPowerPercent = defaultSetPointPowerPercent;
            if (getSetPointTemperature() >= 0) {    // getSetPointTemperature() returns -1 if there is no value in the channel.
                setPointTemperature = getSetPointTemperature();
            }
            if (getSetPointPowerPercent() >= 0) {    // getSetPointPowerPercent() returns -1 if there is no value in the channel.
                setPointPowerPercent = getSetPointPowerPercent();
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
                        log.warn("Couldn't write in Channel " + e.getMessage());
                    }
                } else {
                    try {
                        _setHr100OperatingMode(0);    // Aus
                    } catch (OpenemsError.OpenemsNamedException e) {
                        log.warn("Couldn't write in Channel " + e.getMessage());
                    }
                }
            }
        }

        // Status and other.
        if (getCurrentElectricPower().isDefined()) {
            // The channel READ_EFFECTIVE_POWER_PERCENT in the Heater interface tracks the heating power. Assume the
            // heating power of the heatpump scales linearly with the electric power draw. Then calculate heating power
            // in percent with currentElectricPower / maxElectricPower.
            double effectivePowerPercent = (getCurrentElectricPower().get() * 1.0 / maxElectricPower) * 100;    // Convert to %.
            setEffectivePowerPercentRead(effectivePowerPercent);
        }
        if (getCop().isDefined()) {
            coefficientOfPerformance = getCop().get();
        }
        boolean connectionAlive = false;
        if (getErrorIndicator().isDefined()) {
            heatpumpError = getErrorIndicator().get();
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
            setState(HeaterState.OFFLINE.name());
        } else if (heatpumpError) {
            setState(HeaterState.ERROR.name());
            //} else if (chpWarning){	// No warning indicator for this device.
            //	setState(HeaterState.WARNING.name());
        } else if (heatpumpRunning) {
            setState(HeaterState.RUNNING.name());
            //} else if (chpStartingUp){	// No preheat on this device
            //	setState(HeaterState.PREHEAT.name());
        } else if (heatpumpReady) {
            setState(HeaterState.AWAIT.name());
        } else {
            // If the code gets to here, the state is undefined.
            setState(HeaterState.UNDEFINED.name());
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
                if (getEffectivePower() > 0) {
                    statusMessage = statusMessage + " Heizleistung " + getEffectivePower() + " kW, ";
                }
            } else {
                statusMessage = statusMessage + " Pumpe steht, ";
            }
            statusMessage = statusMessage.substring(0, statusMessage.length() - 2) + ".";
            _setStatusMessage(statusMessage);
        } else {
            _setStatusMessage("Modbus not connected.");
        }


        if (debug) {
            this.logInfo(this.log, "--Heatpump Heliotherm--");
            this.logInfo(this.log, "Flow temperature: " + getFlowTemperature() + " [d°C]");
            this.logInfo(this.log, "Buffer temperature: " + getBufferTemperature().get() + " [d°C]");
            this.logInfo(this.log, "Return temperature: " + getReturnTemperature() + " [d°C]");
            this.logInfo(this.log, "OutsideTemperature: " + getHr129OutsideTemperature() + " [d°C]");
            this.logInfo(this.log, "SetPoint Temperature " + getSetPointTemperatureIndicator() + "[d°C]");
            this.logInfo(this.log, "Current heating power: " + getEffectivePower() + " [kW]");
            this.logInfo(this.log, "Current electric power consumption: " + getCurrentElectricPower().get() + " [W]");
            this.logInfo(this.log, "Current coefficient of performance: " + getCop().get());

            int heatingPowerFromCop = 0;
            if (getCop().isDefined() && getCurrentElectricPower().isDefined()) {
                heatingPowerFromCop = getCurrentElectricPower().get() * getCop().get() / 1000; // Convert to kilowatt
            }

            this.logInfo(this.log, "Heating power calculated from cop & electric power: " + heatingPowerFromCop + " [kW]");
            this.logInfo(this.log, "Verdichterdrehzahl: " + getVerdichterDrehzahl() + " [%]");
            this.logInfo(this.log, "State enum: " + getCurrentState());
            this.logInfo(this.log, "Status message: " + getStatusMessage().get());
            this.logInfo(this.log, "");
        }

    }

    @Override
    public boolean setPointPowerPercentAvailable() {
        return true;
    }

    @Override
    public boolean setPointPowerAvailable() {
        return false;
    }

    @Override
    public boolean setPointTemperatureAvailable() {
        return true;
    }

    @Override
    public int calculateProvidedPower(int demand, float bufferValue) throws OpenemsError.OpenemsNamedException {
        return getEffectivePower();
    }

    @Override
    public int getMaximumThermalOutput() {
        return (coefficientOfPerformance * maxElectricPower);
    }

    @Override
    public void setOffline() throws OpenemsError.OpenemsNamedException {
        setEnableSignal(false);
    }

    @Override
    public boolean hasError() {
        return heatpumpError;
    }

    @Override
    public void requestMaximumPower() {
        // Set heater to run at 100%, but don't set enableSignal.
        try {
            setOperatingMode(OperatingMode.SET_POINT_POWER_PERCENT.getValue());
            setSetPointPowerPercent(100);
        } catch (OpenemsError.OpenemsNamedException e) {
            log.warn("Couldn't write in Channel " + e.getMessage());
        }
    }

    @Override
    public void setIdle() {
        // Set heater to run at 0%, but don't switch it off.
        try {
            setOperatingMode(OperatingMode.SET_POINT_POWER_PERCENT.getValue());
            setSetPointPowerPercent(0);
        } catch (OpenemsError.OpenemsNamedException e) {
            log.warn("Couldn't write in Channel " + e.getMessage());
        }
    }
}
