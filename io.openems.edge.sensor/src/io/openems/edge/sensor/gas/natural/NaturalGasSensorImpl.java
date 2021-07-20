package io.openems.edge.sensor.gas.natural;

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
import io.openems.edge.sensor.gas.api.NaturalGasSensor;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Sensor.Gas.Natural",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class NaturalGasSensorImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, NaturalGasSensor {

    @Reference
    protected ConfigurationAdmin cm;
    private Config config;

    // This is essential for Modbus to work, but the compiler does not warn you when it is missing!
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public NaturalGasSensorImpl() {
        super(OpenemsComponent.ChannelId.values(),
                NaturalGasSensor.ChannelId.values());
    }

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
                new FC4ReadInputRegistersTask(this.config.address(), Priority.HIGH,
                        m(NaturalGasSensor.ChannelId.HYDROGEN_CONCENTRATION, new UnsignedWordElement(this.config.address()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ));
    }
}
