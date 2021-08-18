package io.openems.edge.timer.api;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "TimerByTime ", description = ".")
@interface TimerByTimeConfig {

    @AttributeDefinition(name = "Id", description = "Unique Id for the TimerByTime. You only need one though")
    String id() default "TimerByTime";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
