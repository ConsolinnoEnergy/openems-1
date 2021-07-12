package io.openems.edge.heatsystem.components.valve.old;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Valve OneOutput", description = "A Valve that write a percent Value into another Output(Channel)")
@interface ConfigValveOneOutput {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "ValveOneOutput";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "OutputChannelAddress", description = "Write the ValvePosition to this Channel: ")
    String outputChannel() default "Aio0/AioPercentWrite";

    boolean useCheckOutput() default true;

    @AttributeDefinition(name = "Check Output ChannelAddress", description = "Check if the Output got written")
    String checkOutputChannel() default "Aio0/AioCheckPercent";

    @AttributeDefinition(name = "Should Close on Activation", description = "Should the Valve Close completely if it's "
            + "activated: prevents in flight status due to crashes or restarts etc")
    boolean shouldCloseOnActivation() default true;

    @AttributeDefinition(name = "TimeToOpenValve", description = "Time to open or Close a Valve Completely (T in seconds)")
    int timeToOpenValve() default 30;

    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "TimerId", description = "The Timer used for the ExceptionalState")
    String timerId() default "TimerByCycles";

    @AttributeDefinition(name = "Timeout ExceptionalState", description = "Time exceptionalState Value stays active after it's enable Signal is missing")
    int maxTime() default 10;

    String webconsole_configurationFactory_nameHint() default "ValveOneOutput {id}";
}
