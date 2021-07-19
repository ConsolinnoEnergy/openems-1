package io.openems.edge.weatherstation;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.weatherstation.api.WeatherStation;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = Config.class, factory = true)
@Component(name = "WeatherStation",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class WeatherStationImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, WeatherStation, Thermometer, EventHandler {

    @Reference
    protected ConfigurationAdmin cm;

    // This is essential for Modbus to work, but the compiler does not warn you when it is missing!
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    protected Config oConfig;

    public WeatherStationImpl() {
        super(OpenemsComponent.ChannelId.values(),
                WeatherStation.ChannelId.values(),
                Thermometer.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) {
        oConfig = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() {
        if (oConfig.modbusRegisterCurrentTempOutside() >= 0) {
            return new ModbusProtocol(this,
                    new FC3ReadRegistersTask(oConfig.modbusRegisterCurrentTempOutside(), Priority.HIGH,
                            m(WeatherStation.ChannelId.CURRENT_OUTDOOR_TEMP, new FloatDoublewordElement(oConfig.modbusRegisterCurrentTempOutside()), ElementToChannelConverter.DIRECT_1_TO_1)));
        } else {
            return null;
        }
    }

    @Override
    public String debugLog() {
        return String.valueOf(getOutdoorTempChannel());
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            Float temperature;
            temperature = this.getOutdoorTempChannel().value().orElse(this.getOutdoorTempChannel().getNextValue().orElse(null));
            if(temperature != null) {
                this.getTemperature().setNextValue(temperature * OUTDOOR_TO_TEMPERATURE_SCALE);
            }
        }
    }
}
