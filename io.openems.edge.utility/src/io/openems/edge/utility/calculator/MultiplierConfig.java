package io.openems.edge.utility.calculator;

import io.openems.edge.utility.api.InputOutputType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Calculator Multiplier", description = "This Component multiplies a \"x\" amount of inputs. "
        + "Either static numeric Values or Channel Inputs."
        + "Write the result of the calculation to a Channel Output.")
@interface MultiplierConfig {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the Calculator.")
    String id() default "";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Input Values", description = "Multiply static numeric values and/or values from ChannelAddresses (getChannel.value())."
            + "If you want to divide your current result with a follow up Value simply put a \"/\" in front of the input line."
            + "Order does matter! This Calculator does not support Brackets it calculates by InputOrder!")
    String[] values() default {"750", "/200", "VirtualChannel/VirtualInteger", "/VirtualChannel1/VirtualInteger"};

    @AttributeDefinition(name = "Output ChannelAddress", description = "Write the result of the multiplication into this Channel.")
    String outputChannel() default "VirtualChannel/VirtualDouble";

    @AttributeDefinition(name = "OutputType", description = "Set the result to: the nextWrite, nextValue or value "
            + "(nextValue and next ProcessImage)")
    InputOutputType inputOutputType() default InputOutputType.NEXT_VALUE;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility Calculator Multiplier {id}";
}
