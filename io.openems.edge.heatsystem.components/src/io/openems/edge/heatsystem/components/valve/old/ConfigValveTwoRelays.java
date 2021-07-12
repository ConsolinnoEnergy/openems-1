package io.openems.edge.heatsystem.components.valve.old;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(
        name = "Valve Two Relays",
        description = "A Valve controlled by 2 relays used in the passing station."
)
@interface ConfigValveTwoRelays {


    String service_pid();

    @AttributeDefinition(name = "Valve Name", description = "Unique Id of the Valve.")
    String id() default "Valve0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Closing Channel", description = "What channel to write True/False if Valve should close")
    String closeChannelAddress() default "Relays0/WriteOnOff";

    @AttributeDefinition(name = "Opening Channel", description = "What channel to write True/False if Valve should close")
    String openChannelAddress() default "Relays1/WriteOnOff";

    @AttributeDefinition(name = "Valve Time", description = "The time needed to Open and Close the valve (t in seconds).")
    int valve_Time() default 30;

    @AttributeDefinition(name = "Should Close on Activation", description = "Should the Valve Close completely if it's "
            + "activated: prevents in flight status due to crashes or restarts etc")
    boolean shouldCloseOnActivation() default true;

    boolean useOpeningAndClosingCheck() default false;

    @AttributeDefinition(name = "CheckClosingChannelAddress", description = "If Valve should close, check with this channel if it is really closing")
    String inputClosingChannelAddress() default "Relay1/ReadOnOff";

    @AttributeDefinition(name = "CheckOpeningChannelAddress", description = "If Valve should open, check with this channel if it is really opening"
            + "e.g. read from Hardware if Opening channel is set to true")
    String inputOpeningChannelAddress() default "Relay0/ReadOnOff";

    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "TimerId", description = "The Timer used for the ExceptionalState")
    String timerId() default "TimerByCycles";

    @AttributeDefinition(name = "Timeout ExceptionalState", description = "Time exceptionalState Value stays active after it's enable Signal is missing")
    int maxTime() default 10;


    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Valve Two Relays [{id}]";
}
