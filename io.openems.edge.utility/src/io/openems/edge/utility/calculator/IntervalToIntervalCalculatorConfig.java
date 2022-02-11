package io.openems.edge.utility.calculator;

import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.IntervalToIntervalCalculator;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Calculator IntervalToInterval", description = "A Calculator used to map a Value from an Interval A"
        + " to another Interval B. Or Vice Versa depending on Configuration -> RepresentationType")
@interface IntervalToIntervalCalculatorConfig {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the Component.")
    String id() default "IntervalToIntervalCalculator";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Input Channel Address", description = "The Input Channel Address.")
    String inputChannelAddress() default "Aio0/AioInput";

    @AttributeDefinition(name = "InputType", description = "Get the Value, nextValue or nextWriteValue")
    InputOutputType inputType() default InputOutputType.VALUE;

    @AttributeDefinition(name = "Interval A Min Value", description = "The Minimum Value of Interval A.")
    double inputMinIntervalA() default 4;

    @AttributeDefinition(name = "Interval A Max Value", description = "The Maximum Value of Interval A")
    double inputMaxIntervalA() default 20;

    @AttributeDefinition(name = "Interval B Min Value", description = "The Minimum Value of Interval B")
    double inputMinIntervalB() default -5;

    @AttributeDefinition(name = "Interval B Max Value", description = "The Maximum Value of Interval B")
    double inputMaxIntervalB() default 5;

    @AttributeDefinition(name = "Output Channel Address", description = "The Output Channel Address where the Result of the Calculation will be stored.")
    String outputChannelAddress() default "VirtualChannel0/VirtualDouble";

    @AttributeDefinition(name = "InputType", description = "Get the Value, nextValue or nextWriteValue")
    InputOutputType outputType() default InputOutputType.NEXT_WRITE_VALUE;

    @AttributeDefinition(name = "InputValue belongs to Interval A_Or_B", description = "Is the InputValue from Interval A or B.")
    IntervalToIntervalCalculator.RepresentationType representationType() default IntervalToIntervalCalculator.RepresentationType.VALUE_FROM_INTERVAL_A;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Calculator IntervalToInterval {id}";
}
