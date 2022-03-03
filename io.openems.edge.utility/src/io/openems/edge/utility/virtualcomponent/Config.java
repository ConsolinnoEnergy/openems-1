package io.openems.edge.utility.virtualcomponent;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Virtual Component Optimizer", description = "This Component is a virtual Component to represent an optimized Component.")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the Component.")
    String id() default "VirtualComponentOptimized0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Virtual Component Optimizer {id}";
}
