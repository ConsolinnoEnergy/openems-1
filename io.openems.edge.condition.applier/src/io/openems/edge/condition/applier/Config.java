package io.openems.edge.condition.applier;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Condition Applier ", description = "This class gets a ChannelAddress and checks if the given"
        + "condition applies and then answers to a channel depending on the if else value.")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "ConditionApplier";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "NOTE", description = "ATM: Only true False check and write int value to given channel! in Future: More to come")
    String[] supportedOperations();

    String[] supportedDataType();

    @AttributeDefinition(name = "Configuration", description = "The Configuration, example is given Below"
            + "Order is: ChannelAddress:trueOrFalse:TrueValue:FalseValue:AnswerChannelAddress")
    //"Order is: ChannelAddress:SupportedOperation:DataType:ExpectedValueToApplyIfOperation:ValueToDo:ValueToDoIfNotApplied:ChannelAddressToWriteAnswer"
    String answer() default "ControlCenter0/ActivateHeater:true:300:0:ThermometerVirtual0/VirtualTemperature";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
