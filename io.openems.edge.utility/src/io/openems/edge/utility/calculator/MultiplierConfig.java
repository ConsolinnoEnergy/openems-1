package io.openems.edge.utility.calculator;

import io.openems.edge.utility.api.InputOutputType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Calculator Multiplier ", description = "This Component multiplies Input. Either static Values or Channel Input."
        + "Write the product to a Channel Output.")
@interface MultiplierConfig {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Input Values", description = "Multiply static values or values from ChannelAddresses (getChannel.value) you want to multiply them."
            + "If you want to divide a Value simply put a \"/\" in front of the input"
            + "Order does matter! This Calculator does not support Brackets it calculates by InputOrder!")
    String[] values() default {"750", "/200", "VirtualChannel/VirtualInteger", "/VirtualChannel1/VirtualInteger"};

    @AttributeDefinition(name = "Output ChannelAddress", description = "Write the Value into this Channel")
    String outputChannel() default "VirtualChannel/VirtualDouble";

    @AttributeDefinition(name = "OutputType", description = "Write the Value the nextWrite, nextValue or value (nextValue and next ProcessImage)")
    InputOutputType inputOutputType() default InputOutputType.NEXT_VALUE;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility Calculator Multiplier {id}";
}
