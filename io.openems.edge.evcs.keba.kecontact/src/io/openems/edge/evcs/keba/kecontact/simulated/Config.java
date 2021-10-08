package io.openems.edge.evcs.keba.kecontact.simulated;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno EVCS Simulator", description = "This Simulates a Keba EVCS.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Simulated Keba.")
    String id() default "";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "MinimumHwPower", description = "Minimum Power (in A).", required = true)
    int minHwPower() default 6;

    @AttributeDefinition(name = "Phases", description = "If the Phases are physically swapped, change the order here.", required = true)
    int[] phases() default {1,2,3};

    @AttributeDefinition(name = "Charging Priority", description = "Tick if the EVCS should charge with a higher priority.")
    boolean priority() default false;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
