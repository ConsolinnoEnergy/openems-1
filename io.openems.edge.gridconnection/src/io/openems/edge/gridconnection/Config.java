package io.openems.edge.gridconnection;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Generic Modbus GridConnection",
        description = "A module to map Modbus calls to OpenEMS channels for a Generic Modbus gridconnection Module."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Device ID", description = "Unique Id of the gridconnection Module.")
    String id() default "gridconnection0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Modbus Register GridConnection NaturalGas", description = "Modbus Register for GridConnection NaturalGas.")
    int modbusRegisterNaturalGas() default -1;
    @AttributeDefinition(name = "Modbus Register GridConnection ColdWater", description = "Modbus Register for GridConnection ColdWater.")
    int modbusRegisterColdWater() default -1;

    @AttributeDefinition(name = "alias", description = "Human readable name of the gridconnection Module.")
    String alias() default "";

    boolean enabled() default true;

}