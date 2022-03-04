package io.openems.edge.utility.calculator;

import io.openems.edge.utility.api.InputOutputType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Calculator Adder", description = "This Component adds up \"x\" amount of inputs."
        + " Either static numeric values or channel inputs."
        + " Write the result of the adding to a channel output.")
@interface AdderConfig {

    String service_pid();

    @AttributeDefinition(name = "Id of this component", description = "Unique Id for the calculator.")
    String id() default "UtilityAdder";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Input Values", description = "Add static values or ChannelAddresses you want to sum up."
            + "If you want to subtract a value simply put a \"-\" in front of the input.")
    String[] values() default {"750", "-200", "VirtualChannel/VirtualInteger", "-VirtualChannel1/VirtualInteger"};

    @AttributeDefinition(name = "Output ChannelAddress", description = "Write the result of the calculation into this channel.")
    String outputChannel() default "VirtualChannel/VirtualDouble";

    @AttributeDefinition(name = "OutputType", description = "Set the result to: the nextWrite, nextValue or value "
            + "(nextValue and next ProcessImage)")
    InputOutputType inputOutputType() default InputOutputType.NEXT_VALUE;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility Calculator Adder {id}";
}
