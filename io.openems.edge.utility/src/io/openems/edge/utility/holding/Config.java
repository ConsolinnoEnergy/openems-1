package io.openems.edge.utility.holding;

import io.openems.edge.utility.api.InputOutputType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Information Holder", description = "This Component holds an information, depending on the EnableSignal. "
        + "When the EnableSignal was true, and afterwards false, the activeValue will be stored for a configured deltaTime amount.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the component.")
    String id() default "InformationHolder0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "ActiveValue", description = "The active value of this component.")
    String activeValue() default "true";

    @AttributeDefinition(name = "Value Is Channel", description = "Is the active value a channel? If so the value of the channel will be written into the current value.")
    boolean activeValueIsChannel() default false;

    @AttributeDefinition(name = "InactiveValue", description = "The inactive value of this component.")
    String inactiveValue() default "false";

    @AttributeDefinition(name = "Value Is Channel", description = "Is the inactive value a channel? If so the Value of the Channel will be written into the current value.")
    boolean inactiveValueIsChannel() default false;

    @AttributeDefinition(name = "TimerId", description = "The TimerId you want to use.")
    String timerId() default "TimerByCycles";

    @AttributeDefinition(name = "DeltaTime", description = "How long to wait after EnableSignal was missing/false.")
    int deltaTime() default 10;

    @AttributeDefinition(name = "WriteToOutput", description = "Write the current value to an output.")
    boolean writeToOutput() default false;

    @AttributeDefinition(name = "OutputType", description = "OutputType. Where to write the current value.")
    InputOutputType outputType() default InputOutputType.NEXT_WRITE_VALUE;

    @AttributeDefinition(name = "OutputChannel", description = "Output channel. Where to write the current value")
    String outputAddress() default "VirtualChannel0/VirtualBoolean";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility Information Holder {id}";
}
