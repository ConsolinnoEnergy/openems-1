package io.openems.edge.storage.heat.modbus;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Storage Heat Modbus Quad",
        description = "A module to map Modbus calls to OpenEMS channels for a Generic Modbus HeatStorageQuad Module."
)
@interface QuadConfig {

    String service_pid();

    @AttributeDefinition(name = "Device ID", description = "Unique Id of the HeatStorageQuad Module.")
    String id() default "heatstorage0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Capacity", description = "Capacity in kWh.")
    String capacity() default "10";

    @AttributeDefinition(name = "Max temperature", description = "Maximum temperature at 100% in celsius.")
    String maxPressure() default "10";

    @AttributeDefinition(name = "Modbus Register first temp", description = "Modbus Register first temp sensor.")
    int modbusRegisterTempFirst() default -1;
    @AttributeDefinition(name = "Modbus Register second temp", description = "Modbus Register second temp sensor.")
    int modbusRegisterTempSecond() default -1;
    @AttributeDefinition(name = "Modbus Register third temp", description = "Modbus Register third temp sensor.")
    int modbusRegisterTempThird() default -1;
    @AttributeDefinition(name = "Modbus Register fourth temp", description = "Modbus Register fourth temp sensor.")
    int modbusRegisterTempFourth() default -1;

    @AttributeDefinition(name = "alias", description = "Human readable name of the heat storage Module.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Generic Modbus Component [{id}]";

}