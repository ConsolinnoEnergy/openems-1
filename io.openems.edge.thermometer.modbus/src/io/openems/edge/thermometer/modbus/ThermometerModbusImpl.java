package io.openems.edge.thermometer.modbus;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
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
import org.osgi.service.component.annotations.Modified;
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

/**
 * A Generalized ModbusThermometer Implementation. Map any ModbusAddress to a Temperature.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Thermometer.Modbus",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class ThermometerModbusImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, ThermometerModbus, Thermometer, EventHandler {

    private final Logger log = LoggerFactory.getLogger(ThermometerModbusImpl.class);

    @Reference
    protected ConfigurationAdmin cm;

    private int valueToAddOrSubtract = 0;


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
        this.valueToAddOrSubtract = config.constantValueToAddOrSubtract();
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsException {
        this.config = config;
        super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());
        this.valueToAddOrSubtract = config.constantValueToAddOrSubtract();
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

    /**
     * Creates the ModbusTasks specified by the Config.
     *
     * @return the Task.
     */
    private Task createModbusTask() {
        switch (this.config.word()) {

            case DOUBLE:
            case FLOAT:
                if (this.config.isHoldingRegister()) {
                    return new FC3ReadRegistersTask(this.config.address(), Priority.HIGH,
                            m(ThermometerModbus.ChannelId.TEMPERATURE_MODBUS_FLOAT, new FloatDoublewordElement(this.config.address())).wordOrder(this.config.wordorder()));
                } else {
                    return new FC4ReadInputRegistersTask(this.config.address(), Priority.HIGH,
                            m(ThermometerModbus.ChannelId.TEMPERATURE_MODBUS_FLOAT, new FloatDoublewordElement(this.config.address())).wordOrder(this.config.wordorder()));
                }
            case INTEGER_UNSIGNED:
                if (this.config.isHoldingRegister()) {
                    return new FC3ReadRegistersTask(this.config.address(), Priority.HIGH,
                            m(ThermometerModbus.ChannelId.TEMPERATURE_MODBUS_INTEGER, new UnsignedWordElement(this.config.address())));
                } else {
                    return new FC4ReadInputRegistersTask(this.config.address(), Priority.HIGH,
                            m(ThermometerModbus.ChannelId.TEMPERATURE_MODBUS_INTEGER, new UnsignedWordElement(this.config.address())));
                }
            case INTEGER_SIGNED:
                if (this.config.isHoldingRegister()) {
                    return new FC3ReadRegistersTask(this.config.address(), Priority.HIGH,
                            m(ThermometerModbus.ChannelId.TEMPERATURE_MODBUS_INTEGER, new SignedWordElement(this.config.address())));
                } else {
                    return new FC4ReadInputRegistersTask(this.config.address(), Priority.HIGH,
                            m(ThermometerModbus.ChannelId.TEMPERATURE_MODBUS_INTEGER, new SignedWordElement(this.config.address())));
                }
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
                valueTemperature += this.valueToAddOrSubtract;
                this.getTemperatureChannel().setNextValue(this.calculateTemperature(valueTemperature, unit));
            }
        }
    }

    /**
     * Calculates the Temperature, depending on the TemperatureUnit configured.
     *
     * @param valueTemperature the Value read via Modbus
     * @param unit             the unit expected from the modbus input.
     * @return the temperature Value in dC.
     */
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