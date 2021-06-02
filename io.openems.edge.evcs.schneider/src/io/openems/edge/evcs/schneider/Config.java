package io.openems.edge.evcs.schneider;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "EVCS Schneider", description = "Implements the Schneider Electric vehicle charging station.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the EVCS.")
    String id() default "Schneider01";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "Phases", description = "If the Phases are physically swapped, change the order here.", required = true)
    int[] phases() default {1,2,3};

    @AttributeDefinition(name = "ModbusUnitId", description = "Unique Id for the Modbusunit.")
    int modbusUnitId();

    @AttributeDefinition(name = "ModbusBridgeId", description = "Unique Id for the Modbusbridge")
    String modbusBridgeId();

    String webconsole_configurationFactory_nameHint() default "{id}";
}
