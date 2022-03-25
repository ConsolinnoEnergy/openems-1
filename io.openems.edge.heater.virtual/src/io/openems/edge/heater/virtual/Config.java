package io.openems.edge.heater.virtual;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Heater Virtual ", description = "A Virtual Heater to Simulate a Heater")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the component.")
    String id() default "VirtualHeater0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this component.")
    String alias() default "";

    int defaultPower() default 150;

    int defaultSetPoint() default 100;

    int defaultMaintenanceInterval() default 50;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Virtual Heater{id}";
}
