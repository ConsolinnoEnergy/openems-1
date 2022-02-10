package io.openems.edge.utility.calculator;

import io.openems.edge.utility.api.InputOutputType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Calculator Adder ", description = "This Component adds Input. Either static Values or Channel Input."
        + "Write the Sum to a Channel Output.")
@interface AdderConfig {

    String service_pid();

    @AttributeDefinition(name = "Id of this Component", description = "Unique Id for the Calculator.")
    String id() default "UtilityAdder";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Input Values", description = "Add static values or ChannelAddresses you want to sum up."
            + "If you want to subtract a Value simply put a \"-\" in front of the input")
    String[] values() default {"750", "-200", "VirtualChannel/VirtualInteger", "-VirtualChannel1/VirtualInteger"};

    @AttributeDefinition(name = "Output ChannelAddress", description = "Write the Value into this Channel")
    String outputChannel() default "VirtualChannel/VirtualDouble";

    @AttributeDefinition(name = "OutputType", description = "Write the Value the nextWrite, nextValue or value (nextValue and next ProcessImage)")
    InputOutputType inputOutputType() default InputOutputType.NEXT_VALUE;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility Calculator Adder {id}";
}
