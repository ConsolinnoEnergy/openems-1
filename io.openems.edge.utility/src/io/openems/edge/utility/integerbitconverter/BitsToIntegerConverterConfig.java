package io.openems.edge.utility.integerbitconverter;

import io.openems.edge.utility.api.InputOutputType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Bits to Integer Converter", description = "Receive an Input of Bits and write the value to own Integer Value.")
@interface BitsToIntegerConverterConfig {

    @AttributeDefinition(name = "Id", description = "Unique Id for the component.")
    String id() default "BitToIntegerConverter";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this component.")
    String alias() default "";

    @AttributeDefinition(name = "Optional out", description = "Write to an optional output the Integer Value.")
    boolean useOptionalOutput() default false;

    String outputChannel() default "VirtualChannel0/VirtualLong";

    InputOutputType output() default InputOutputType.NEXT_WRITE_VALUE;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility Bits to Integer Converter {id}";
}
