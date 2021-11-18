package io.openems.edge.storage.hydrogen.modbus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Generic Modbus HydrogenStorage Module",
        description = "A module to map Modbus calls to OpenEMS channels for a Generic Modbus HydrogenStorage Module."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Device ID", description = "Unique Id of the Hydrogen Storage Module.")
    String id() default "hydrogenstorage0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Capacity", description = "Capacity in kWh.")
    String capacity() default "10";

    @AttributeDefinition(name = "Max pressure", description = "Maximum pressure at 100% fill in bar.")
    String maxPressure() default "10";

    @AttributeDefinition(name = "Modbus Register capacity", description = "Modbus Register capacity.")
    int modbusRegisterCapacity() default 1;

    @AttributeDefinition(name = "alias", description = "Human readable name of the hydrogen storage Module.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Generic Modbus Component [{id}]";

}