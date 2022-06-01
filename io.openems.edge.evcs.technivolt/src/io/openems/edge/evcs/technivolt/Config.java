package io.openems.edge.evcs.technivolt;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Evcs Technivolt", description = "Technivolt 1100/1100 SMART/2200 SMART.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the EVCS.")
    String id() default "evcs01";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this EVCS.")
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
    int modbusUnitId() default 255;

    @AttributeDefinition(name = "ModbusBridgeId", description = "Unique Id for the Modbusbridge.")
    String modbusBridgeId() default "modbusTechnivolt01";

    String webconsole_configurationFactory_nameHint() default "Evcs Technivolt [{id}]";
}
