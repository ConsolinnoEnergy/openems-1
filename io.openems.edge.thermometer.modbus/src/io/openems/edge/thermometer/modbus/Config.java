package io.openems.edge.thermometer.modbus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Thermometer Modbus",
        description = "A module to map Modbus calls to OpenEMS channels for a Generic Modbus WeatherStation."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Device ID", description = "Unique Id of the WeatherStation Module.")
    String id() default "ThermometerModbus0";

    @AttributeDefinition(name = "alias", description = "Human readable name of the natural gas sensor Module.")
    String alias() default "";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Modbus Register currentTemp outside", description = "Modbus Register for currentTemp outside.")
    int address() default -1;

    @AttributeDefinition(name = "AddOrSubtract this Value", description = "Add or Subtract a Certain Value before Converting Unit.")
    int constantValueToAddOrSubtract() default 0;

    @AttributeDefinition(name = "Unit", description = "Unit of the Modbus Thermometer, will be converted to DeciDegree Celsius")
    TEMPERATURE_UNIT temperatureUnit() default TEMPERATURE_UNIT.DEZIDEGREE_CELSIUS;

    @AttributeDefinition(name = "WordElement modbus", description = "The ModbusWordElement. For Temperatures it's usually Float or Signed")
    Word word() default Word.FLOAT;

    @AttributeDefinition(name = "HoldingRegister?", description = "Is the ModbusRegister a Holding Register (FC3)? If false -> InputRegister(Fc4)")
    boolean isHoldingRegister() default false;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Thermometer Modbus[{id}]";

}