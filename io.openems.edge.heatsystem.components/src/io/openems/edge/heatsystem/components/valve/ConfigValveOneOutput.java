package io.openems.edge.heatsystem.components.valve;

import io.openems.edge.heatsystem.components.ConfigurationType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Valve One Output", description = "A Valve that write a percent Value into another Output(Channel)")
@interface ConfigValveOneOutput {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "Valve0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "Configuration Type", description = "Select either Control by Channel or by DeviceType.")
    ConfigurationType configurationType() default ConfigurationType.CHANNEL;

    @AttributeDefinition(name = "InputChannelAddress or InputDevice", description = "Device that writes PowerLevel to this Channel or Device.")
    String inputChannelOrDevice() default "Aio1/AioPercentWrite";


    @AttributeDefinition(name = "TimeToOpenValve", description = "Time to open or Close a Valve Completely (T in seconds)")
    int timeToOpenValve() default 30;

    @AttributeDefinition(name = "Self check if Output to Valve is written",
            description = "If the Box is ticked, the Valve will check if it's output is written into devices. "
                    + "If the ConfigurationType is Channel, please type the inputChannelAddresses")
    boolean useCheckChannel() default true;

    @AttributeDefinition(name = "Check Output ChannelAddress", description = "Check if the Output got written, only important if ConfigurationType selected is \"Channel\"")
    String checkChannel() default "Aio1/AioCheckPercent";

    @AttributeDefinition(name = "Should Close on Activation", description = "Should the Valve Close completely if it's "
            + "activated: prevents in flight status due to crashes or restarts etc")
    boolean shouldCloseOnActivation() default true;

    @AttributeDefinition(name = "Should Open on Activation", description = "Should the Valve Close completely if it's "
            + "activated: prevents in flight status due to crashes or restarts etc")
    boolean shouldOpenOnActivation() default false;

    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "TimerId", description = "The Timer used for the ExceptionalState and/or Check for missing Components")
    String timerId() default "TimerByCycles";

    @AttributeDefinition(name = "Timeout ExceptionalState", description = "Time exceptionalState Value stays active after it's enable Signal is missing")
    int maxTime() default 10;

    String webconsole_configurationFactory_nameHint() default "Valve One Output {id}";
}
