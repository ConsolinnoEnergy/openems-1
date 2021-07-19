package io.openems.edge.sensor.gas.natural;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Generic Modbus Natural Gas Sensor",
        description = "A module to map Modbus calls to OpenEMS channels for a Generic Modbus Natural Gas Sensor Module."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Device ID", description = "Unique Id of the Natural Gas Sensor Module.")
    String id() default "naturalgassensor0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Modbus Register hydrogen concentration", description = "Modbus Register for concentration of hydrogen in natural gas mixture.")
    int modbusRegisterConcentration() default 1;

    @AttributeDefinition(name = "alias", description = "Human readable name of the natural gas sensor Module.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Generic Modbus Component [{id}]";

}