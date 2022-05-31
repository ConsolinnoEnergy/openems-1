package io.openems.edge.utility.toogleswitch;

import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.ValueType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility ToggleSwitch ", description = "Toggle Switch, switches between two states after a signal was active.")
@interface ToggleSwitchConfig {

    @AttributeDefinition(name = "Id", description = "Unique Id for the component.")
    String id() default "ToggleSwitch";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this component.")
    String alias() default "";

    @AttributeDefinition(name = "Toggle state", description = "The starting/current ToggleState.")
    ToggleState defaultState() default ToggleState.B;

    @AttributeDefinition(name = "State A Value", description = "The value for state A.")
    String stateAValue() default "true";

    @AttributeDefinition(name = "ValueType A", description = "Is the value a String, ChannelAddress (value) or a static number.")
    ValueType valueTypeA() default ValueType.STRING;

    @AttributeDefinition(name = "State B Value", description = "The value for state B.")
    String stateBValue() default "false";

    @AttributeDefinition(name = "ValueType B", description = "Is the value a String, ChannelAddress (value) or a static number.")
    ValueType valueTypeB() default ValueType.STRING;

    @AttributeDefinition(name = "Use an Output", description = "Write the current state value to an output ChannelAddress.")
    boolean useOutput() default true;

    @AttributeDefinition(name = "Output ChannelAddress", description = "The output ChannelAddress.")
    String outputChannelAddress() default "VirtualChannel0/VirtualBoolean";

    @AttributeDefinition(name = "InputOutputType", description = "Put the data in the value/nextValue/nextWriteValue of the ChannelAddress.")
    InputOutputType outputType() default InputOutputType.NEXT_WRITE_VALUE;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility ToggleSwitch {id}";
}
