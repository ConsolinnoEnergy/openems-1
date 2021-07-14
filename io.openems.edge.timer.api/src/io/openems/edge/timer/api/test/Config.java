package io.openems.edge.timer.api.test;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Timer test ", description = ".")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "TimerTest";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enabled() default true;

    String timer() default "TimerByTime";

    @AttributeDefinition(name = "Identifier:Value", description = "Identifiers")
    String[] identifier() default {"foo:20", "foo2:10", "foo3:5", "foo4:50", "foo5:2"};

    String webconsole_configurationFactory_nameHint() default "{id}";
}
