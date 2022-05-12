package io.openems.edge.utility.integerbitconverter;

import io.openems.edge.utility.api.InputOutputType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Integer to Bit Converter ", description = "Convert an input integer to a Bit")
@interface IntegerToBitConverterConfig {

    @AttributeDefinition(name = "Id", description = "Unique Id for the component.")
    String id() default "IntegerToBitConverter";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this component.")
    String alias() default "";

    @AttributeDefinition(name = "Other Component", description = "Other Component. Read the ChannelValue from this component")
    boolean useOtherComponent() default true;

    @AttributeDefinition(name = "Other Component", description = "Other Component. Read the ChannelValue from this component")
    String otherComponentId() default "Chp-1";

    @AttributeDefinition(name = "ChannelIds", description = "This list will automatically filled with ChannelIds")
    String[] channelIds();

    @AttributeDefinition(name = "ChannelId", description = "Provides the Integer")
    String inputChannel() default "VirtualChannel";

    @AttributeDefinition(name = "InputType", description = "Input from Value/nextValue/nextWriteValue")
    InputOutputType inputType() default InputOutputType.VALUE;

    @AttributeDefinition(name = "Write to output", description = "Write bits to different outputs")
    boolean writeToOutput();

    @AttributeDefinition(name = "bitMap", description = "Map the bit to a channel or ChannelAddress. Always setNextWriteValue if possible.")
    String[] mapBitToChannel() default {"1:otherChannel", "2:VirtualChannel/VirtualBoolean"};

    boolean configurationDone() default false;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility Integer to Bit Converter{id}";
}
