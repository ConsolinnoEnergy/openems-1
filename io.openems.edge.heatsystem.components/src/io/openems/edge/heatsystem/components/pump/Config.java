package io.openems.edge.heatsystem.components.pump;

import io.openems.edge.heatsystem.components.ConfigurationType;
import io.openems.edge.heatsystem.components.PumpType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(
        name = "Hydraulic Pump",
        description = "A HydraulicComponent. Allows an analogue Pump to run. With a Relay and or analogueOutputs, either"
                + "with channel Addresses or Components (AIO and Pwm)"
)
@interface Config {


    String service_pid();

    @AttributeDefinition(name = "Pump Name", description = "Unique Id of the Pump.")
    String id() default "Pump0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Configuration Type", description = "Do you want to Configure and Control the Pump via Devices or Channels")
    ConfigurationType configType() default ConfigurationType.CHANNEL;

    @AttributeDefinition(name = "Pump Type", description = "What Kind of Pump is it?")
    PumpType pump_Type() default PumpType.RELAY;

    @AttributeDefinition(name = "BooleanChannel or Id of (Relay) Device", description = "Either the BooleanChannel or the Relay Device.")
    String pump_Relay() default "Relay0/WriteOnOff";

    @AttributeDefinition(name = "PWM Id/ PwmChannel", description = "Either the WriteChannel or the Pwm Device")
    String pump_Pwm_or_Aio() default "PwmDevice0/WritePowerLevel";

    boolean enabled() default true;

    @AttributeDefinition(name = "Use default PowerLevel", description = "Use a default PowerLevel on Activation")
    boolean useDefault() default false;

    @AttributeDefinition(name = "Default PowerLevel", description = "Default PowerLevel Value")
    double defaultPowerLevel() default 100;

    boolean checkPowerLevelIsApplied() default false;

    @AttributeDefinition(name = "Check ChannelAddress Relay", description = "If the Pump PowerValue is applied -> Check via Output if it's set correctly")
    String checkRelayChannelAddress() default "SignalSensorSpi/SignalActive";

    @AttributeDefinition(name = "Check ChannelAddress Pwm", description = "If the Pump PowerValue is applied -> Check via Output if it's set correctly")
    String checkPwmOrAioChannelAddress() default "Pwm/ReadPowerLevel";

    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "TimerId", description = "Timer to check for missing Components")
    String timerIdMissingComponents() default "TimerByCycles";

    @AttributeDefinition(name = "TimerId", description = "The Timer used for the ExceptionalState")
    String timerId() default "TimerByCycles";

    @AttributeDefinition(name = "Timeout ExceptionalState", description = "Time exceptionalState Value stays active after it's enable Signal is missing")
    int maxTime() default 10;

    String webconsole_configurationFactory_nameHint() default "Pump [{id}]";
}
