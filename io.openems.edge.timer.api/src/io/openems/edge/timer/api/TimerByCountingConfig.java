package io.openems.edge.timer.api;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Timer By Count Down ", description = "This Timer is used to Count. "
        + " Each time an OpenEMS Component calls this Timer a mapped counter is started. "
        + "When the Counter reached  it's configured Maximum -> The Time is up.")
@interface TimerByCountingConfig {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Timer. You only need one though.")
    String id() default "TimerByCycles";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Timer By Counting {id}";
}
