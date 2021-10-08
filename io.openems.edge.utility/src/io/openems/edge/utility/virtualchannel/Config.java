package io.openems.edge.utility.virtualchannel;


import io.openems.edge.utility.api.VirtualChannelType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Virtual Channel ", description = "A Virtual Channel. Create On the Fly Channel to Write and Read from.")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "VirtualChannel0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    VirtualChannelType channelType() default VirtualChannelType.BOOLEAN;

    @AttributeDefinition(name = "Default Value", description = "Default Value applied to the Channel, depending on the VirtualChannelType")
    String defaultValue() default "true";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Virtual Channel {id}";
}
