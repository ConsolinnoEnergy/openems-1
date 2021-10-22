package io.openems.edge.controller.hydrauliccomponent.controller;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Hydraulic Position PID",
        description = "This Controller regulates the Pump and Valves for Heating via Pid Regulation."
)
@interface PidConfig {

    String service_pid();

    @AttributeDefinition(name = "Controller Id", description = "Unique Name of the Passing Controller.")
    String id() default "PidController0";

    @AttributeDefinition(name = "alias", description = "Human Readable Name of Component.")
    String alias() default "";

    @AttributeDefinition(name = "Allocated Passing Device", description = "Unique Name of the allocated Device.")
    String allocatedHydraulicComponent() default "Valve0";

    @AttributeDefinition(name = "Proportional Gain", description = "The proportional gain value for PID.")
    double proportionalGain() default 2.0;

    @AttributeDefinition(name = "Integral Gain", description = "The integral gain value for PID.")
    double integralGain() default 0.2;

    @AttributeDefinition(name = "Derivative Gain", description = "The derivative gain value for PID.")
    double derivativeGain() default 0.1;

    @AttributeDefinition(name = "Lower Limit", description = "Lower Limit of the PID Controller.")
    int lowerLimit() default -200;

    @AttributeDefinition(name = "Upper Limit", description = "Upper Limit of the PID Controller.")
    int upperLimit() default 200;

    @AttributeDefinition(name = "TimerId", description = "Timer where the WaitTime will be handled.")
    String timerId() default "TimerByCycles";

    @AttributeDefinition(name = "WaitTime", description = "Time to wait, until PID controller calculates a Value again.")
    int waitTime() default 10;

    @AttributeDefinition(name = "Temperature", description = "The Temperature you want to reach (T in dC--> 1°C = 10).")
    int setPoint_Temperature() default 750;

    @AttributeDefinition(name = "Turn off Percentage", description = "If PID is off position of controlled device")
    int offPercentage() default 0;

    @AttributeDefinition(name = "TemperatureSensor", description = "The Temperaturesensor allocated to this controller")
    String temperatureSensorId() default "TemperatureSensor4";

    boolean morePercentEqualsCooling() default false;

    boolean autoRun() default false;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller PID Hydraulic [{id}]";
}