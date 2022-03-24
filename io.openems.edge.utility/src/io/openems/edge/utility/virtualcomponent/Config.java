package io.openems.edge.utility.virtualcomponent;

import io.openems.edge.utility.api.VirtualChannelType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Virtual Component Optimizer", description = "This component is a virtual component to represent an optimized component.")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the component.")
    String id() default "VirtualComponentOptimized0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this component.")
    String alias() default "";

    @AttributeDefinition(name = "SetPoint determines EnableSignal",
            description = "When a SetPoint is written. Use this to indicate if the component sets it's EnableSignal.")
    boolean useOptimizedValueAsEnableSignalIndicator() default false;

    @AttributeDefinition(name = "Look up",
            description = "When a SetPoint is written. Check this ChannelType to determine if the component should set it's EnableSignal.")
    VirtualChannelType virtualChannelType() default VirtualChannelType.DOUBLE;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Virtual Component Optimizer {id}";
}
