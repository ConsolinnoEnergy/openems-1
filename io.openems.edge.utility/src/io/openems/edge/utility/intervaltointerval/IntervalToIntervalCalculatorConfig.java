package io.openems.edge.utility.intervaltointerval;

import io.openems.edge.utility.api.CalculationType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Calculator IntervalToInterval", description = "A Calculator used to Map an Interval [a;b] to another Interval [c;d]."
        + " This Calculator gets an input Channel, a Range of the input Values for Interval [a;b] as well as [c;d]"
        + "and calculates depending on the needed OutputValue an adapted OutputValue to another [a;b] [c;d]")
@interface IntervalToIntervalCalculatorConfig {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Input Channel", description = "The Input Channel Address.")
    String inputChannelAddress() default "VirtualChannel0/VirtualDouble";

    @AttributeDefinition(name = "Input Min Value", description = "The Minimum Value of Interval A.")
    double inputMinIntervalA() default 4;

    @AttributeDefinition(name = "Input Max Value", description = "The Maximum Value of Interval A.")
    double inputMaxIntervalA() default 20;

    @AttributeDefinition(name = "Input Min Percent", description = "The Minimum Value of Interval B")
    double inputMinIntervalB() default 50;
    @AttributeDefinition(name = "Input Min Percent", description = "The Maximum Value of Interval B")
    double inputMaxIntervalB() default 50;

    @AttributeDefinition(name = "Input Calculation Type", description = "Is the Input a Value or a Percentage Value.")
    CalculationType inputCalculationType() default CalculationType.VALUE_FROM_INTERVAL_B;

    @AttributeDefinition(name = "Output Channel", description = "The Output Channel Address.")
    String outputChannelAddress() default "VirtualChannel0/VirtualDouble";

    double outputMin();

    double outputMax();

    double outputMinPercent();

    CalculationType outputCalculationType() default CalculationType.VALUE_FROM_INTERVAL_B;


    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Calculator RuleOfThree {id}";
}
