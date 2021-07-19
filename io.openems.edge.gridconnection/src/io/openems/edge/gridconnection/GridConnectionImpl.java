package io.openems.edge.gridconnection;

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
import io.openems.edge.gridconnection.api.GridConnection;
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
@Component(name = "Grid.Connection",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class GridConnectionImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, GridConnection {
    @Reference
    protected ConfigurationAdmin cm;

    // This is essential for Modbus to work, but the compiler does not warn you when it is missing!
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public GridConnectionImpl() {
        super(OpenemsComponent.ChannelId.values(),
                GridConnection.ChannelId.values());
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
                new FC4ReadInputRegistersTask(this.config.modbusRegisterNaturalGas(), Priority.HIGH,
                        m(GridConnection.ChannelId.GRID_METER_NATURAL_GAS, new UnsignedWordElement(this.config.modbusRegisterNaturalGas()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ), new FC4ReadInputRegistersTask(this.config.modbusRegisterColdWater(), Priority.HIGH,
                m(GridConnection.ChannelId.GRID_METER_COLD_WATER, new UnsignedWordElement(this.config.modbusRegisterColdWater()),
                        ElementToChannelConverter.DIRECT_1_TO_1)
        ));
    }
}
