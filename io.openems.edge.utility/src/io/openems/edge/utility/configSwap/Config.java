package io.openems.edge.utility.configSwap;

import org.joda.time.DateTime;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility Config Swap TimeBased ", description = "This utility Class adapts Configs. One Component is active, the other Components are inactive."
        + "Inactive Components gets a ConfigUpdate with inactive KeyValue pairs, while active Components receive active Key Value pairs."
        + "The Interval to swap components can be set in seconds. Optional use of ErrorChannel is allowed -> when Swapping to next Component -> has an Error? ignore this one.")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the Config Swapper.")
    String id() default "ConfigSwap0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Component ids", description = "Components to swap. Where 1 is always active and the other are inactive.")
    String[] components() default {"Component0", "Component1"};

    @AttributeDefinition(name = "Swap on error", description = "When error occurred swap components.")
    boolean useSwapOnError() default false;

    @AttributeDefinition(name = "Error Channel", description = "When the Component should be swapped in check this channel.")
    String[] errorChannel() default {"Component0/hasError", "Component1/hasError"};

    @AttributeDefinition(name = "Error occurred when this value is met", description = "Error occurred when this value is met.")
    String errorOccurredValue() default "true";

    @AttributeDefinition(name = "Swap Interval", description = "The next Component will be swapped in after this time Interval."
            + "On -1 : Never Change")
    int swapInterval() default 86400;

    @AttributeDefinition(name = "Timer Id", description = "The Timer, checking when the interval time is up.")
    String timerHandler() default "TimerByTime";

    @AttributeDefinition(name = "Active Key Value Pairs", description = "Pair a configuration Key to an active Value.")
    String[] activeKeyValuePairs() default {"configEntry:ActiveValue", "configEntry2:ActiveValue"};

    @AttributeDefinition(name = "Inactive Key Value Pairs", description = "Pair a configuration Key to an inactive Value.")
    String[] inactiveKeyValuePairs() default {"configEntry:InactiveValue", "configEntry2:InactiveValue"};

    @AttributeDefinition(name = "DateTime", description = "This will be filled by the Component. Do not put anything in here")
    String dateTime() default "";

    @AttributeDefinition(name = "Active Component", description = "Filled in by the Component itself.")
    String activeComponent() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "ConfigSwap Time Based{id}";
}
