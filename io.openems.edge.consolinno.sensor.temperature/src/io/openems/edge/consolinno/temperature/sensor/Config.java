package io.openems.edge.consolinno.temperature.sensor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Modbus Temperatur Sensor", description = "Temperatur Sensor that communicates over Modbus.")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Temperature Sensor.")
    String id() default "TemperatureSensor0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Sensor.")
    String alias() default "";

    @AttributeDefinition(name = "Module", description = "ModuleNumber where this Sensor is plugged in.")
    int module() default 1;

    @AttributeDefinition(name = "Position", description = "Pinposition of this sensor.")
    int position() default 1;

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "ModbusUnitId from Configurator.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "ModbusBridgeId", description = "ModbusBridgeId from Configurator.")
    String modbusBridgeId() default "modbus0";

    String webconsole_configurationFactory_nameHint() default "TemperatureSensor [{id}]";
}
