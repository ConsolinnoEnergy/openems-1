package io.openems.edge.evcs.alfen;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.FloatQuadrupleWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.alfen.api.Alfen;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.GridVoltage;
import io.openems.edge.evcs.api.ManagedEvcs;
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

import java.util.Arrays;
//TODO This does not support Status apparently. This needs to be addressed by the controller or it wont work correctly
/**
 * This Provides the Alfen NG9xx EVCS Modbus TCP implementation.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "AlfenImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class AlfenImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, Alfen, ManagedEvcs, Evcs, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private int minPower;
    private int maxPower;
    private int[] phases;
    private AlfenReadHandler readHandler;
    private AlfenWriteHandler writeHandler;
    private EvcsPower evcsPower;

    public AlfenImpl() {
        super(OpenemsComponent.ChannelId.values(), Alfen.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        this.minPower = config.minCurrent();
        this.maxPower = config.maxCurrent();
        this.phases = config.phases();
        if (!this.checkPhases()) {
            throw new ConfigurationException("Phase Configuration is not valid!", "Configuration must only contain 1,2 and 3.");
        }
        this._setMinimumHardwarePower(6 * GridVoltage.V_230_HZ_50.getValue());
        this._setMaximumHardwarePower(32 * GridVoltage.V_230_HZ_50.getValue());
        this._setMaximumPower(this.maxPower);
        this._setMinimumPower(this.minPower);
        this._setIsPriority(config.priority());
        this._setPowerPrecision(GridVoltage.V_230_HZ_50.getValue());
        this.readHandler = new AlfenReadHandler(this);
        this.writeHandler = new AlfenWriteHandler(this);
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
    }

    /**
     * Checks if the Phase Configuration of the Config is valid.
     *
     * @return true if valid
     */
    private boolean checkPhases() {
        String phases = Arrays.toString(this.phases);
        return phases.contains("1") && phases.contains("2") && phases.contains("3") && this.phases.length == 3;
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,

                //--------------------Read-Register-Tasks-------------------\\
                new FC4ReadInputRegistersTask(300, Priority.HIGH,
                        m(Alfen.ChannelId.METER_STATE,
                                new UnsignedWordElement(300),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(301, Priority.HIGH,
                        m(Alfen.ChannelId.METER_LAST_VALUE_TIMESTAMP,
                                new UnsignedQuadruplewordElement(301),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(305, Priority.HIGH,
                        m(Alfen.ChannelId.METER_TYPE,
                                new UnsignedWordElement(305),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(306, Priority.HIGH,
                        m(Alfen.ChannelId.VOLTAGE_PHASE_L1_N,
                                new FloatDoublewordElement(306),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(308, Priority.HIGH,
                        m(Alfen.ChannelId.VOLTAGE_PHASE_L2_N,
                                new FloatDoublewordElement(308),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(310, Priority.HIGH,
                        m(Alfen.ChannelId.VOLTAGE_PHASE_L3_N,
                                new FloatDoublewordElement(310),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(312, Priority.HIGH,
                        m(Alfen.ChannelId.VOLTAGE_PHASE_L1_L2,
                                new FloatDoublewordElement(312),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(314, Priority.HIGH,
                        m(Alfen.ChannelId.VOLTAGE_PHASE_L2_L3,
                                new FloatDoublewordElement(314),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(316, Priority.HIGH,
                        m(Alfen.ChannelId.VOLTAGE_PHASE_L3_L1,
                                new FloatDoublewordElement(316),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(318, Priority.HIGH,
                        m(Alfen.ChannelId.CURRENT_N,
                                new FloatDoublewordElement(318),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(320 + ((this.phases[0] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.CURRENT_L1,
                                new FloatDoublewordElement(320 + ((this.phases[0] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(320 + ((this.phases[1] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.CURRENT_L2,
                                new FloatDoublewordElement(320 + ((this.phases[1] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(320 + ((this.phases[2] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.CURRENT_L3,
                                new FloatDoublewordElement(320 + ((this.phases[2] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(326, Priority.HIGH,
                        m(Alfen.ChannelId.CURRENT_SUM,
                                new FloatDoublewordElement(326),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(328 + ((this.phases[0] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.POWER_FACTOR_L1,
                                new FloatDoublewordElement(328 + ((this.phases[0] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(328 + ((this.phases[1] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.POWER_FACTOR_L2,
                                new FloatDoublewordElement(328 + ((this.phases[1] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(328 + ((this.phases[2] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.POWER_FACTOR_L3,
                                new FloatDoublewordElement(328 + ((this.phases[2] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(334, Priority.HIGH,
                        m(Alfen.ChannelId.POWER_FACTOR_SUM,
                                new FloatDoublewordElement(334),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(336, Priority.HIGH,
                        m(Alfen.ChannelId.FREQUENCY,
                                new FloatDoublewordElement(336),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                new FC4ReadInputRegistersTask(338 + ((this.phases[0] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.REAL_POWER_L1,
                                new FloatDoublewordElement(338 + ((this.phases[0] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(338 + ((this.phases[1] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.REAL_POWER_L2,
                                new FloatDoublewordElement(338 + ((this.phases[1] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(338 + ((this.phases[2] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.REAL_POWER_L3,
                                new FloatDoublewordElement(338 + ((this.phases[2] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(344, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_POWER_SUM,
                                new FloatDoublewordElement(344),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(346 + ((this.phases[0] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_POWER_L1,
                                new FloatDoublewordElement(346 + ((this.phases[0] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(346 + ((this.phases[1] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_POWER_L2,
                                new FloatDoublewordElement(346 + ((this.phases[1] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(346 + ((this.phases[2] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_POWER_L3,
                                new FloatDoublewordElement(346 + ((this.phases[2] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(352, Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_POWER_SUM,
                                new FloatDoublewordElement(352),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(354 + ((this.phases[0] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_POWER_L1,
                                new FloatDoublewordElement(354 + ((this.phases[0] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(354 + ((this.phases[1] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_POWER_L2,
                                new FloatDoublewordElement(354 + ((this.phases[1] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(354 + ((this.phases[2] * 2) - 2), Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_POWER_L3,
                                new FloatDoublewordElement(354 + ((this.phases[2] * 2) - 2)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(360, Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_POWER_SUM,
                                new FloatDoublewordElement(360),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(362 + ((this.phases[0] * 4) - 4), Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_DELIVERED_L1,
                                new FloatQuadrupleWordElement(362 + ((this.phases[0] * 4) - 4)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(362 + ((this.phases[1] * 4) - 4), Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_DELIVERED_L2,
                                new FloatQuadrupleWordElement(362 + ((this.phases[1] * 4) - 4)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(362 + ((this.phases[2] * 4) - 4), Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_DELIVERED_L3,
                                new FloatQuadrupleWordElement(362 + ((this.phases[2] * 4) - 4)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(374, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_DELIVERED_SUM,
                                new FloatQuadrupleWordElement(374),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(378 + ((this.phases[0] * 4) - 4), Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_CONSUMED_L1,
                                new FloatQuadrupleWordElement(378 + ((this.phases[0] * 4) - 4)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(378 + ((this.phases[1] * 4) - 4), Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_CONSUMED_L2,
                                new FloatQuadrupleWordElement(378 + ((this.phases[1] * 4) - 4)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(378 + ((this.phases[2] * 4) - 4), Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_CONSUMED_L3,
                                new FloatQuadrupleWordElement(378 + ((this.phases[2] * 4) - 4)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(390, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_CONSUMED_SUM,
                                new FloatQuadrupleWordElement(390),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(394 + ((this.phases[0] * 4) - 4), Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_ENERGY_L1,
                                new FloatQuadrupleWordElement(394 + ((this.phases[0] * 4) - 4)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(394 + ((this.phases[1] * 4) - 4), Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_ENERGY_L2,
                                new FloatQuadrupleWordElement(394 + ((this.phases[1] * 4) - 4)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(394 + ((this.phases[2] * 4) - 4), Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_ENERGY_L3,
                                new FloatQuadrupleWordElement(394 + ((this.phases[2] * 4) - 4)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(406, Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_ENERGY_SUM,
                                new FloatQuadrupleWordElement(406),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(410 + ((this.phases[0] * 4) - 4), Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_ENERGY_L1,
                                new FloatQuadrupleWordElement(410 + ((this.phases[0] * 4) - 4)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(410 + ((this.phases[1] * 4) - 4), Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_ENERGY_L2,
                                new FloatQuadrupleWordElement(410 + ((this.phases[1] * 4) - 4)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(410 + ((this.phases[2] * 4) - 4), Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_ENERGY_L3,
                                new FloatQuadrupleWordElement(410 + ((this.phases[2] * 4) - 4)),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(422, Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_ENERGY_SUM,
                                new FloatQuadrupleWordElement(422),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(1200, Priority.HIGH,
                        m(Alfen.ChannelId.AVAILABILITY,
                                new UnsignedWordElement(1200),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(1201, Priority.HIGH,
                        m(Alfen.ChannelId.MODE_3_STATE,
                                new StringWordElement(1201, 5),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(1206, Priority.HIGH,
                        m(Alfen.ChannelId.ACTUAL_APPLIED_MAX_CURRENT,
                                new FloatDoublewordElement(1206),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(1208, Priority.HIGH,
                        m(Alfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT_VALID_TIME,
                                new UnsignedDoublewordElement(1208),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(1210, Priority.HIGH,
                        m(Alfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT,
                                new FloatDoublewordElement(1210),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(1212, Priority.HIGH,
                        m(Alfen.ChannelId.ACTIVE_LOAD_BALANCING_SAFE_CURRENT,
                                new FloatDoublewordElement(1212),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(1214, Priority.HIGH,
                        m(Alfen.ChannelId.MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR,
                                new UnsignedWordElement(1214),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC3ReadRegistersTask(1215, Priority.HIGH,
                        m(Alfen.ChannelId.CHARGE_PHASES,
                                new SignedWordElement(1215),
                                ElementToChannelConverter.DIRECT_1_TO_1)),

                //----------------Write-Register-Task----------------\\
                new FC16WriteRegistersTask(1210,
                        m(Alfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT,
                                new FloatDoublewordElement(1210),
                                ElementToChannelConverter.DIRECT_1_TO_1))
        );
    }

    @Override
    public int[] getPhaseConfiguration() {
        return this.phases;
    }

    @Override
    public String debugLog() {
        return "Total: " + this.getApparentPowerSum() + " W | L1 " + this.getCurrentL1() + " A | L2 " + this.getCurrentL2() + " A | L3 " + this.getCurrentL3() + " A";
    }

    @Override
    public EvcsPower getEvcsPower() {
        return this.evcsPower;
    }

    /**
     * Returns the minimum Software Power.
     *
     * @return minPower
     */
    public int getMinPower() {
        return this.minPower;
    }

    /**
     * Returns the minimum Software Power.
     *
     * @return minPower
     */
    public int getMaxPower() {
        return this.maxPower;
    }

    @Override
    public void handleEvent(Event event) {
        this.writeHandler.run();
        try {
            this.readHandler.run();
        } catch (Throwable throwable) {
            //
        }
    }
}
