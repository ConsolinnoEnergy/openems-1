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
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.alfen.api.Alfen;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
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
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "AlfenImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AlfenImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, Alfen, ManagedEvcs, Evcs {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }


    public AlfenImpl() {
        super(OpenemsComponent.ChannelId.values(), Alfen.ChannelId.values()
        );
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
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
                                new SignedWordElement(316),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(318, Priority.HIGH,
                        m(Alfen.ChannelId.CURRENT_N,
                                new FloatDoublewordElement(318),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(320, Priority.HIGH,
                        m(Alfen.ChannelId.CURRENT_L1,
                                new SignedWordElement(320),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(322, Priority.HIGH,
                        m(Alfen.ChannelId.CURRENT_L2,
                                new FloatDoublewordElement(322),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(324, Priority.HIGH,
                        m(Alfen.ChannelId.CURRENT_L3,
                                new FloatDoublewordElement(324),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(326, Priority.HIGH,
                        m(Alfen.ChannelId.CURRENT_SUM,
                                new FloatDoublewordElement(326),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(328, Priority.HIGH,
                        m(Alfen.ChannelId.POWER_FACTOR_L1,
                                new FloatDoublewordElement(328),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(330, Priority.HIGH,
                        m(Alfen.ChannelId.POWER_FACTOR_L2,
                                new FloatDoublewordElement(330),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(332, Priority.HIGH,
                        m(Alfen.ChannelId.POWER_FACTOR_L3,
                                new FloatDoublewordElement(332),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(334, Priority.HIGH,
                        m(Alfen.ChannelId.POWER_FACTOR_SUM,
                                new FloatDoublewordElement(334),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(336, Priority.HIGH,
                        m(Alfen.ChannelId.FREQUENCY,
                                new FloatDoublewordElement(336),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(338, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_POWER_L1,
                                new FloatDoublewordElement(338),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(340, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_POWER_L2,
                                new FloatDoublewordElement(340),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(342, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_POWER_L3,
                                new FloatDoublewordElement(342),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(344, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_POWER_SUM,
                                new FloatDoublewordElement(344),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(346, Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_POWER_L1,
                                new FloatDoublewordElement(346),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(348, Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_POWER_L2,
                                new FloatDoublewordElement(348),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(350, Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_POWER_L3,
                                new FloatDoublewordElement(350),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(352, Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_POWER_SUM,
                                new FloatDoublewordElement(352),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(354, Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_POWER_L1,
                                new FloatDoublewordElement(354),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(356, Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_POWER_L2,
                                new FloatDoublewordElement(356),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(358, Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_POWER_L3,
                                new FloatDoublewordElement(358),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(360, Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_POWER_SUM,
                                new FloatDoublewordElement(360),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(362, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_DELIVERED_L1,
                                new FloatQuadrupleWordElement(362),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(366, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_DELIVERED_L2,
                                new FloatQuadrupleWordElement(364),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(370, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_DELIVERED_L3,
                                new FloatQuadrupleWordElement(366),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(374, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_DELIVERED_SUM,
                                new FloatQuadrupleWordElement(374),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(378, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_CONSUMED_L1,
                                new FloatQuadrupleWordElement(378),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(382, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_CONSUMED_L2,
                                new FloatQuadrupleWordElement(382),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(386, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_CONSUMED_L3,
                                new FloatQuadrupleWordElement(386),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(390, Priority.HIGH,
                        m(Alfen.ChannelId.REAL_ENERGY_CONSUMED_SUM,
                                new FloatQuadrupleWordElement(390),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(394, Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_ENERGY_L1,
                                new FloatQuadrupleWordElement(394),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(398, Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_ENERGY_L2,
                                new FloatQuadrupleWordElement(398),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(402, Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_ENERGY_L3,
                                new FloatQuadrupleWordElement(402),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(406, Priority.HIGH,
                        m(Alfen.ChannelId.APPARENT_ENERGY_SUM,
                                new FloatQuadrupleWordElement(406),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(410, Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_ENERGY_L1,
                                new FloatQuadrupleWordElement(410),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(414, Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_ENERGY_L2,
                                new FloatQuadrupleWordElement(414),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(418, Priority.HIGH,
                        m(Alfen.ChannelId.REACTIVE_ENERGY_L3,
                                new FloatQuadrupleWordElement(418),
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
    public String debugLog() {
        return "";
    }

    @Override
    public EvcsPower getEvcsPower() {
        return null;
    }
}
