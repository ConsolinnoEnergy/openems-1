package io.openems.edge.heatsystem.components.pump.old;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;


@ObjectClassDefinition(
        name = "Hydraulic Pump Pwm/Relay",
        description = "A Pump mainly used for the Passing Station and Controller"
)
@interface Config {


    String service_pid();

    @AttributeDefinition(name = "Pump Name", description = "Unique Id of the Pump.")
    String id() default "Pump0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Pump Type", description = "How is the Pump Controlled?")
    PumpType pump_Type() default PumpType.RELAY;

    @AttributeDefinition(name = "Relays Id", description = "If the Pump is connected to a relays; type the id.")
    String pump_Relays() default "Relays2";

    @AttributeDefinition(name = "PWM Id", description = "If the Pump is connected as a pwm Device; type in the id.")
    String pump_Pwm() default "PwmDevice0";

    @AttributeDefinition(name = "Use default powerLevel", description = "Use a default PowerLevel on Activation")
    boolean useDefault() default false;

    @AttributeDefinition(name = "Default PowerLevel", description = "Default PowerLevel Value")
    double defaultPowerLevel() default 100;

    boolean checkPowerLevelIsApplied() default false;

    @AttributeDefinition(name = "Check ChannelAddress Relay", description = "If the Pump PowerValue is applied -> Check via Output if it's set correctly")
    String relayCheckChannelAddress() default "SignalSensorSpi/SignalActive";

    @AttributeDefinition(name = "Check ChannelAddress Pwm", description = "If the Pump PowerValue is applied -> Check via Output if it's set correctly")
    String pwmCheckChannelAddress() default "Pwm/ReadPowerLevel";

    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "TimerId", description = "The Timer used for the ExceptionalState")
    String timerId() default "TimerByCycles";

    @AttributeDefinition(name = "Timeout ExceptionalState", description = "Time exceptionalState Value stays active after it's enable Signal is missing")
    int maxTime() default 10;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Pump [{id}]";
}
