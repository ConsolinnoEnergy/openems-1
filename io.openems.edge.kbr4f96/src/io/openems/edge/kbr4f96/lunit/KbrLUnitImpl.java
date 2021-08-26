package io.openems.edge.kbr4f96.lunit;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.FloatQuadrupleWordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.kbr4f96.lunit.api.KbrLUnit;
import io.openems.edge.kbr4f96.api.KbrBaseModule;
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
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;

/**
 * This represents on of the Phase inputs.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.kbr4f96.LUnit", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)
public class KbrLUnitImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, KbrLUnit {
    private KbrBaseModule baseModule;
    private int position;
    private int offset;

    public KbrLUnitImpl() {
        super(OpenemsComponent.ChannelId.values(), KbrLUnit.ChannelId.values());
    }

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }


    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    protected ComponentManager cpm;

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.baseModule = this.cpm.getComponent(config.mainUnitId());
        this.position = config.position();
        this.offset = (this.position - 1) * 2;
        if (!this.baseModule.moduleCheckout(this.position)) {
            throw new ConfigurationException("Error while Creating L Unit", "This Pin is already occupied");
        }
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
    }

    @Override
    public String debugLog() {
        return getActivePower() + "W";
    }

    @Deactivate
    public void deactivate() {
        if (!this.baseModule.moduleRemove(this.position))
            super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                new FC4ReadInputRegistersTask(2 + this.offset - 1, Priority.HIGH,
                        m(KbrLUnit.ChannelId.VOLTAGE_PH_N, new FloatDoublewordElement(
                                        2 + this.offset - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(8 + this.offset - 1, Priority.HIGH,
                        m(KbrLUnit.ChannelId.VOLTAGE_PH_PH, new FloatDoublewordElement(
                                        8 + this.offset - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(14 + this.offset - 1, Priority.HIGH,
                        m(KbrLUnit.ChannelId.RAW_CURRENT, new FloatDoublewordElement(
                                        14 + this.offset - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(20 + this.offset - 1, Priority.HIGH,
                        m(KbrLUnit.ChannelId.AVERAGE_CURRENT, new FloatDoublewordElement(
                                        20 + this.offset - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(26 + this.offset - 1, Priority.HIGH,
                        m(KbrLUnit.ChannelId.APPARENT_POWER, new FloatDoublewordElement(
                                        26 + this.offset - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(32 + this.offset - 1, Priority.HIGH,
                        m(KbrLUnit.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(
                                        32 + this.offset - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(38 + this.offset - 1, Priority.HIGH,
                        m(KbrLUnit.ChannelId.IDLE_POWER, new FloatDoublewordElement(
                                        38 + this.offset - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(44 + this.offset - 1, Priority.HIGH,
                        m(KbrLUnit.ChannelId.COS_PHI, new FloatDoublewordElement(
                                        44 + this.offset - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(50 + this.offset - 1, Priority.HIGH,
                        m(KbrLUnit.ChannelId.POWER_FACTOR, new FloatDoublewordElement(
                                        50 + this.offset - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(56 + this.offset - 1, Priority.HIGH,
                        m(KbrLUnit.ChannelId.SPGS_THD, new FloatDoublewordElement(
                                        56 + this.offset - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(794 + this.offset - 1, Priority.HIGH,
                        m(KbrLUnit.ChannelId.PHASE_ANGLE_U, new FloatDoublewordElement(
                                        794 + this.offset - 1),
                                ElementToChannelConverter.DIRECT_1_TO_1)));
    }
}
