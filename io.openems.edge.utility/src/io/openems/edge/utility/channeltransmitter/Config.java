package io.openems.edge.utility.channeltransmitter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility ChannelTransmitter ", description = "Write the value of one Channel to another.")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id of the Component.")
    String id() default "ChannelTransmitter0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this.")
    String alias() default "";

    @AttributeDefinition(name = "InputChannelAddress", description = "ChannelAddress to get the Information from.")
    String inputChannelAddress() default "RestRemoteDevice0/ValueRead";

    @AttributeDefinition(name = "InputType", description = "Get the Value, nextValue or nextWriteValue")
    InputOutputType inputType() default InputOutputType.VALUE;

    @AttributeDefinition(name = "OutputChannelAddress", description = "ChannelAddress to write/setNextValue the Information to.")
    String outputChannelAddress() default "VirtualChannel0/VirtualBoolean";

    @AttributeDefinition(name = "OutputType", description = "Write to the nextValue or nextWriteValue")
    InputOutputType outputType() default InputOutputType.NEXT_WRITE_VALUE;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility ChannelTransmitter{id}";
}
