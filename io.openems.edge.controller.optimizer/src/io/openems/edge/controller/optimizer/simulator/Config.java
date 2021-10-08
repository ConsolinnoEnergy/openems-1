package io.openems.edge.controller.optimizer.simulator;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(name = "Consolinno EnableSignal Simulator", description = "Simulates a Component with a Write Channel and an Enable Signal")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Simulator.")
    String id() default "Simulator";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Sensor.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Simulator [{id}]";
}
