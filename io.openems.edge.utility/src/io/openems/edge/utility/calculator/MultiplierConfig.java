package io.openems.edge.utility.calculator;

import io.openems.edge.utility.api.InputOutputType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Calculator Multiplier", description = "This component multiplies \"x\" amount of inputs. "
        + "Either static numeric values or channel inputs."
        + "Write the result of the calculation to a channel output.")
@interface MultiplierConfig {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the calculator.")
    String id() default "";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Input Values", description = "Multiply static numeric values and/or values from ChannelAddresses (getChannel.value())."
            + "If you want to divide your current result with a follow up value simply put a \"/\" in front of the input line."
            + "Order does matter! This calculator does not support brackets it calculates by input order!")
    String[] values() default {"750", "/200", "VirtualChannel/VirtualInteger", "/VirtualChannel1/VirtualInteger"};

    @AttributeDefinition(name = "Output ChannelAddress", description = "Write the result of the multiplication into this channel.")
    String outputChannel() default "VirtualChannel/VirtualDouble";

    @AttributeDefinition(name = "OutputType", description = "Set the result to: the nextWrite, nextValue or value "
            + "(nextValue and next ProcessImage)")
    InputOutputType inputOutputType() default InputOutputType.NEXT_VALUE;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility Calculator Multiplier {id}";
}
