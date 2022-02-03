package io.openems.edge.evcs.mennekes;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "EVCS Mennekes", description = "Implements the Mennekes Charging Station.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the EVCS.")
    String id() default "Mennekes1";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "Charging Priority", description = "Tick if the EVCS should charge with a higher priority.")
    boolean priority() default false;

    @AttributeDefinition(name = "Minimum power", description = "Minimum current of the Charger in A.", required = true)
    int minCurrent() default 8;

    @AttributeDefinition(name = "Maximum power", description = "Maximum current of the Charger in A.", required = true)
    int maxCurrent() default 32;

    @AttributeDefinition(name = "Phases", description = "If the Phases are physically swapped, change the order here.", required = true)
    int[] phases() default {1,2,3};

    @AttributeDefinition(name = "ModbusUnitId", description = "Unique Id for the Modbusunit.")
    int modbusUnitId();

    @AttributeDefinition(name = "ModbusBridgeId", description = "Unique Id for the Modbusbridge")
    String modbusBridgeId();

    String webconsole_configurationFactory_nameHint() default "{id}";
}
