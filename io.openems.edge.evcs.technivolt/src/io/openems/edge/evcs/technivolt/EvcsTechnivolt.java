package io.openems.edge.evcs.technivolt;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.AbstractEvcs;
import io.openems.edge.evcs.api.AbstractEvcsModbusChannel;
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
@Component(name = "Evcs.Technivolt", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class EvcsTechnivolt extends AbstractEvcs implements OpenemsComponent, Evcs, ManagedEvcs, AbstractEvcsModbusChannel {

    @Reference
    protected ConfigurationAdmin cm;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    @Reference
    EvcsPower evcsPower;
    
    public EvcsTechnivolt() {
        super(OpenemsComponent.ChannelId.values(),
                ManagedEvcs.ChannelId.values(),
                Evcs.ChannelId.values(),
                AbstractEvcsModbusChannel.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId(), config.minCurrent(), config.maxCurrent(), config.phases(), config.priority(), 1, 1, 1000);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                new FC4ReadInputRegistersTask(122, Priority.HIGH,
                        m(AbstractEvcsModbusChannel.ChannelId.EV_STATUS,
                                new SignedDoublewordElement(122),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(212, Priority.HIGH,
                        m(AbstractEvcsModbusChannel.ChannelId.CURRENT_L1,
                                new SignedDoublewordElement(212),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(214, Priority.HIGH,
                        m(AbstractEvcsModbusChannel.ChannelId.CURRENT_L2,
                                new SignedDoublewordElement(214),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(216, Priority.HIGH,
                        m(AbstractEvcsModbusChannel.ChannelId.CURRENT_L3,
                                new SignedDoublewordElement(216),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC4ReadInputRegistersTask(220, Priority.HIGH,
                        m(AbstractEvcsModbusChannel.ChannelId.INTERNAL_CHARGE_POWER,
                                new SignedDoublewordElement(220),
                                ElementToChannelConverter.DIRECT_1_TO_1)),
                new FC6WriteRegisterTask(715,
                        m(AbstractEvcsModbusChannel.ChannelId.MAXIMUM_CHARGE_POWER,
                                new SignedDoublewordElement(715),
                                ElementToChannelConverter.DIRECT_1_TO_1))

        );
    }


    @Override
    public EvcsPower getEvcsPower() {
        return this.evcsPower;
    }
}
