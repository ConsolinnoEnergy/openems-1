package io.openems.edge.utility.integerbitconverter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Bits to Integer Converter", description = "Receive an Input of Bits and write the value to own Integer Value.")
@interface BitsToIntegerConverterConfig {

    @AttributeDefinition(name = "Id", description = "Unique Id for the component.")
    String id() default "BitToIntegerConverter";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this component.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility Bits to Integer Converter {id}";
}
