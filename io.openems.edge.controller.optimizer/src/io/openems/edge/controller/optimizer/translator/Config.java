package io.openems.edge.controller.optimizer.translator;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;



@ObjectClassDefinition(name = "Consolinno Optimizer Translator", description = "Translator for the Broker Json to the Optimizer Json")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Translator.")
    String id() default "Translator";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Sensor.")
    String alias() default "";

    @AttributeDefinition(name = "OptimizerId", description = "Unique Id for the Optimizer that this Json has to go to.")
    String optimizerId() default "Optimizer0";

    @AttributeDefinition(name = "Component", description = "Unique Id of the Component that has to be Optimized.")
    String componentId();

    @AttributeDefinition(name = "ChannelIds", description = "This List will automatically filled with ChannelIds")
    String[] channelIdList() default {};

    @AttributeDefinition(name = "ComponentChannel", description = "Channel of the Component that has to be Optimized.")
    String componentChannel();

    @AttributeDefinition(name = "Fallback", description = "The value the component has to take if there are no new values.")
    String fallback();

    @AttributeDefinition(name = "EnableSignal", description = "Check if the Component needs an additional EnableSignal Channel")
    boolean enableSignal() default false;

    @AttributeDefinition(name = "EnableChannel", description = "Enable Channel of the Component that has to be Optimized.")
    String enableChannel();

    boolean configurationDone() default false;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Translator [{id}]";
}
