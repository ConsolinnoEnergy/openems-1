package io.openems.edge.utility.calculator;

import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.IntervalToIntervalCalculator;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Calculator IntervalToInterval", description = "A calculator used to map a value from an interval A"
        + " to another interval B. Or vice versa depending on configuration -> RepresentationType")
@interface IntervalToIntervalCalculatorConfig {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the Component.")
    String id() default "IntervalToIntervalCalculator";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Input Channel Address", description = "The input ChannelAddress.")
    String inputChannelAddress() default "Aio0/AioInput";

    @AttributeDefinition(name = "InputType", description = "Get the value, nextValue or nextWriteValue")
    InputOutputType inputType() default InputOutputType.VALUE;

    @AttributeDefinition(name = "Interval A Min Value", description = "The minimum value of interval A.")
    double inputMinIntervalA() default 4;

    @AttributeDefinition(name = "Interval A Max Value", description = "The maximum value of interval A")
    double inputMaxIntervalA() default 20;

    @AttributeDefinition(name = "Interval B Min Value", description = "The minimum value of interval B")
    double inputMinIntervalB() default -5;

    @AttributeDefinition(name = "Interval B Max Value", description = "The maximum value of interval B")
    double inputMaxIntervalB() default 5;

    @AttributeDefinition(name = "Output Channel Address", description = "The output channel Address where the result of the calculation will be stored.")
    String outputChannelAddress() default "VirtualChannel0/VirtualDouble";

    @AttributeDefinition(name = "InputType", description = "Get the Value, nextValue or nextWriteValue")
    InputOutputType outputType() default InputOutputType.NEXT_WRITE_VALUE;

    @AttributeDefinition(name = "InputValue belongs to interval A_Or_B", description = "Is the input value from interval A or B.")
    IntervalToIntervalCalculator.RepresentationType representationType() default IntervalToIntervalCalculator.RepresentationType.VALUE_FROM_INTERVAL_A;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Calculator IntervalToInterval {id}";
}
