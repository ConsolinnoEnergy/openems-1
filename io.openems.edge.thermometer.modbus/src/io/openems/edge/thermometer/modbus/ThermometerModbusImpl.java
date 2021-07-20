package io.openems.edge.thermometer.modbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerModbus;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Thermometer.Modbus",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class ThermometerModbusImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, ThermometerModbus, Thermometer, EventHandler {

    private final Logger log = LoggerFactory.getLogger(ThermometerModbusImpl.class);

    protected ConfigurationAdmin cm;


    // This is essential for Modbus to work, but the compiler does not warn you when it is missing!
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    protected Config config;

    public ThermometerModbusImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ThermometerModbus.ChannelId.values(),
                Thermometer.ChannelId.values());
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
        ModbusProtocol protocol = new ModbusProtocol(this);
        Task task = this.createModbusTask();
        if (task != null) {
            protocol.addTask(task);
        }
        return protocol;
    }

    private Task createModbusTask() {
        switch (this.config.word()) {

            case DOUBLE:
            case FLOAT:
                return new FC4ReadInputRegistersTask(this.config.address(), Priority.HIGH,
                        m(ThermometerModbus.ChannelId.TEMPERATURE_MODBUS_FLOAT, new FloatDoublewordElement(this.config.address())));
            case INTEGER_UNSIGNED:
                return new FC4ReadInputRegistersTask(this.config.address(), Priority.HIGH,
                        m(ThermometerModbus.ChannelId.TEMPERATURE_MODBUS_INTEGER, new UnsignedWordElement(this.config.address())));
            case INTEGER_SIGNED:
                return new FC4ReadInputRegistersTask(this.config.address(), Priority.HIGH,
                        m(ThermometerModbus.ChannelId.TEMPERATURE_MODBUS_INTEGER, new SignedWordElement(this.config.address())));
        }
        this.log.warn("Couldn't apply Config for Temperature Sensor. No Task will be added: " + super.id());
        return null;
    }


    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            //Convert Temperature to Thermometer Channel
            TEMPERATURE_UNIT unit = this.config.temperatureUnit();
            Word word = this.config.word();
            float valueTemperature = 0;
            boolean isDefined = false;
            switch (word) {

                case DOUBLE:
                case FLOAT:
                    if (this.getTemperatureModbusFloat().value().isDefined()) {
                        isDefined = true;
                        valueTemperature = this.getTemperatureModbusFloatValue();
                    }
                    break;
                case INTEGER_UNSIGNED:
                case INTEGER_SIGNED:
                    if (this.getTemperatureModbusInteger().value().isDefined()) {
                        isDefined = true;
                        valueTemperature = this.getTemperatureModbusIntValue();
                    }
                    break;
            }
            if (isDefined) {
                this.getTemperatureChannel().setNextValue(this.calculateTemperature(valueTemperature, unit));
            }
        }
    }

    private Object calculateTemperature(float valueTemperature, TEMPERATURE_UNIT unit) {
        switch (unit) {

            case CELSIUS:
                return (int) (valueTemperature * 10);
            case DEZIDEGREE_CELSIUS:
                return (int) valueTemperature;

            case FAHRENHEIT:
                return (int) ((valueTemperature - 32) * (5 / 9));
            case KELVIN:
                return (int) (valueTemperature - 273.15f);
        }
        return null;
    }


}
