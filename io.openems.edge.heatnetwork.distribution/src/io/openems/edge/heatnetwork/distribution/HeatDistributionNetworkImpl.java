package io.openems.edge.heatnetwork.distribution;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.heatnetwork.distribution.api.HeatDistributionNetwork;
import org.osgi.service.cm.ConfigurationAdmin;
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


@Designate(ocd = Config.class, factory = true)
@Component(name = "Heatnetwork.Distribution",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class HeatDistributionNetworkImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, HeatDistributionNetwork {

    @Reference
    protected ConfigurationAdmin cm;

    // This is essential for Modbus to work, but the compiler does not warn you when it is missing!
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public HeatDistributionNetworkImpl() {
        super(OpenemsComponent.ChannelId.values(),
                HeatDistributionNetwork.ChannelId.values());
    }

    protected Config config;

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsException {
        this.config = config;

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
                new FC4ReadInputRegistersTask(this.config.modbusRegisterWMZEnergyAmount(), Priority.HIGH,
                        m(HeatDistributionNetwork.ChannelId.WMZ_ENERGY_AMOUNT, new UnsignedWordElement(this.config.modbusRegisterWMZEnergyAmount()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterWMZTempSource(), Priority.HIGH,
                        m(HeatDistributionNetwork.ChannelId.WMZ_TEMP_SOURCE, new UnsignedWordElement(this.config.modbusRegisterWMZTempSource()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterWMZTempSink(), Priority.HIGH,
                        m(HeatDistributionNetwork.ChannelId.WMZ_TEMP_SINK, new UnsignedWordElement(this.config.modbusRegisterWMZTempSink()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterWMZPower(), Priority.HIGH,
                        m(HeatDistributionNetwork.ChannelId.WMZ_POWER, new UnsignedWordElement(this.config.modbusRegisterWMZPower()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                )
        );
    }
}
