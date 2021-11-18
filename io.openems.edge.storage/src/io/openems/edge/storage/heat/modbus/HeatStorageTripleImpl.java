package io.openems.edge.storage.heat.modbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.storage.api.HeatStorageTriple;
import io.openems.edge.storage.api.Storage;
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

@Designate(ocd = TripleConfig.class, factory = true)
@Component(name = "Storage.Heat.Tripple",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class HeatStorageTripleImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, HeatStorageTriple {
    @Reference
    protected ConfigurationAdmin cm;

    // This is essential for Modbus to work, but the compiler does not warn you when it is missing!
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public HeatStorageTripleImpl() {
        super(OpenemsComponent.ChannelId.values(),
                HeatStorageTriple.ChannelId.values(),
                Storage.ChannelId.values());
    }

    protected TripleConfig oConfig;

    @Activate
    public void activate(ComponentContext context, TripleConfig config) throws OpenemsException {
        oConfig = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        return new ModbusProtocol(this,
                new FC4ReadInputRegistersTask(oConfig.modbusRegisterTempFirst(), Priority.HIGH,
                        m(HeatStorageTriple.ChannelId.TEMP_1, new FloatDoublewordElement(oConfig.modbusRegisterTempFirst()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(oConfig.modbusRegisterTempSecond(), Priority.HIGH,
                        m(HeatStorageTriple.ChannelId.TEMP_2, new FloatDoublewordElement(oConfig.modbusRegisterTempSecond()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(oConfig.modbusRegisterTempThird(), Priority.HIGH,
                        m(HeatStorageTriple.ChannelId.TEMP_3, new FloatDoublewordElement(oConfig.modbusRegisterTempThird()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ));
    }

    @Override
    public String debugLog() {
        return ("HeatStorageTripple: " + super.id() + " First Temp: " + getTempSensorFirstChannel())
                + " Second: " + (getTempSensorSecondChannel()) + " Third: " + getTempSensorThirdChannel() + "\n";
    }
}
