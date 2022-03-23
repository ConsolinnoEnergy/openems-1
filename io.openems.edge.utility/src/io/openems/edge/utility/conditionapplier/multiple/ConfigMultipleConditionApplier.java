package io.openems.edge.utility.conditionapplier.multiple;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Condition Applier Multiple BooleanChannel", description = "This Controller gets multiple ChannelAddresses (Boolean)")
@interface ConfigMultipleConditionApplier {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the Component.")
    String id() default "NeedHeatConditionApplier";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "ChannelAddresses", description = "Where to get Boolean Values and what Value To Expect")
    String[] channelAddresses() default {"VirtualBooleanChannel0/VirtualBoolean:true", "VirtualBooleanChannel1/VirtualBoolean:false"};

    CheckConditions checkConditions() default CheckConditions.OR;

    boolean useActiveValue() default true;

    boolean activeValueIsChannel() default false;

    String activeValue() default "true";

    boolean useInactiveValue() default true;

    boolean inactiveValueIsChannel() default false;

    String inactiveValue() default "false";

    String answerChannelAddress() default "Pid0/OnOff";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Condition Applier Multiple BooleanChannel {id}";
}
