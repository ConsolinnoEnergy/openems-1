package io.openems.edge.controller.heatnetwork.cooling.surveillancer;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Cooling Surveillancer ", description = "This Controller Checks if there are Cooling requests that can be handled.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Surveillancer.")
    String id() default "Cooling Surveillancer 01";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Inputs", description = "Array of all the Input Request Channels (Cooling Requests).")
    String[] inputRequest() default "";

    @AttributeDefinition(name = "Inputs", description = "Array of all the Input Watchdogs Channels "
            + "(Sensors that say weather or not Cooling is allowed).")
    String[] inputWatchdogs() default "";

    @AttributeDefinition(name = "Outputs", description = "Array of all the Output Channels (e.g. Pump).")
    String[] output() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
