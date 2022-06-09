package io.openems.edge.utility.comparator;

import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.ComparatorType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Comparator",
        description = "This Component allows the comparison of channels and static values the result is written into a given channel")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the component.")
    String id() default "Comparator0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this component.")
    String alias() default "";

    @AttributeDefinition(name = "First compare value", description = "CompareValue or ChannelAddress for first compare value (A)")
    String compareOne() default "VirtualChannel/VirtualLong";

    InputOutputType compareOneInputOutputType() default InputOutputType.VALUE;

    @AttributeDefinition(name = "Second compare value", description = "CompareValue or ChannelAddress for first compare value (B)")
    String compareTwo() default "VirtualChannel/VirtualLong";

    InputOutputType compareTwoInputOutputType() default InputOutputType.VALUE;

    @AttributeDefinition(name = "Result ChannelAddress", description = "Result of comparison is written into this channel")
    String output() default "VirtualChannel0/VirtualBoolean";

    InputOutputType compareResultInputOutputType() default InputOutputType.NEXT_WRITE_VALUE;

    ComparatorType comparatorType() default ComparatorType.EQUALS;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Comparator ChannelValuesStaticNumbers {id}";
}
