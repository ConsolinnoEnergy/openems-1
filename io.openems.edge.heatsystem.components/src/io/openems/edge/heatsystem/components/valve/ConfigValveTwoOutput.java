package io.openems.edge.heatsystem.components.valve;

import io.openems.edge.heatsystem.components.ConfigurationType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(
        name = "Valve Two Output",
        description = "A valve controlled by two outputs."
)
@interface ConfigValveTwoOutput {


    String service_pid();

    @AttributeDefinition(name = "Valve Name", description = "Unique Id of the Valve.")
    String id() default "Valve0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Configuration Type", description = "Select either Control by Channel or by DeviceType.")
    ConfigurationType configurationType() default ConfigurationType.CHANNEL;

    @AttributeDefinition(name = "Closing Channel or Device", description = "What channel to write True/False if Valve should close OR Device. Depends on configurationType.")
    String close() default "Relay1/WriteOnOff";

    @AttributeDefinition(name = "Opening Channel or Device", description = "What channel to write True/False if Valve should close OR Device. Depends on configurationType.")
    String open() default "Relay2/WriteOnOff";


    @AttributeDefinition(name = "Valve Time", description = "The time needed to Open and Close the valve (t in seconds).")
    int valve_Time() default 30;

    @AttributeDefinition(name = "Should Close on Activation", description = "Should the Valve Close completely if it's "
            + "activated: prevents in flight status due to crashes or restarts etc")
    boolean shouldCloseOnActivation() default true;

    @AttributeDefinition(name = "Should Open on Activation", description = "Should the Valve Open completely if it's "
            + "activated: prevents in flight status due to crashes or restarts etc")
    boolean shouldOpenOnActivation() default false;


    @AttributeDefinition(name = "Self check if Output to Valve is written",
            description = "If the Box is ticked, the Valve will check if it's output is written into devices. "
                    + "If the ConfigurationType is Channel, please type the inputChannelAddresses")
    boolean useCheckChannel() default false;

    @AttributeDefinition(name = "CheckClosingChannelAddress", description = "If Valve should close, check with this channel if it is really closing")
    String checkClosingChannelAddress() default "Relay1/ReadOnOff";

    @AttributeDefinition(name = "CheckOpeningChannelAddress", description = "If Valve should open, check with this channel if it is really opening"
            + "e.g. read from Hardware if Opening channel is set to true")
    String checkOpeningChannelAddress() default "Relay2/ReadOnOff";

    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "TimerId", description = "The Timer used for the ExceptionalState and/or check for missing Components")
    String timerId() default "TimerByCycles";

    @AttributeDefinition(name = "Timeout ExceptionalState", description = "Time exceptionalState Value stays active after it's enable Signal is missing")
    int maxTime() default 10;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Valve Two Output [{id}]";
}
