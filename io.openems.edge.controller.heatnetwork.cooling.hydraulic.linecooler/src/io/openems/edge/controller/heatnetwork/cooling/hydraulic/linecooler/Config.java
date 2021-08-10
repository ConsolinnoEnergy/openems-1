package io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Consolinno Hydraulic Line Cooler",
        description = "This Controller cools down a hydraulic line."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Controller.")
    String id() default "HydraulicLineCooler0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Controller.")
    String alias() default "Hydraulic-Line";



    @AttributeDefinition(name = "Reference Temperature", description = "The Temperature-Sensor for the LineCooler.")
    String tempSensorReference() default "TemperatureSensor0";

    @AttributeDefinition(name = "Default Temperature", description = "How long should we cool? OFF(in dC --> 1°C == 10°dC).")
    int temperatureDefault() default 800;

    @AttributeDefinition(name = "Valve, MultiChannel, OneChannel", description = "Do you want to Access Channel, or multiple Channel directly or access Valve")
    LineCoolerType valveOrChannel() default LineCoolerType.ONE_CHANNEL;


    @AttributeDefinition(name = "Value to Write is Boolean", description = "Is the Value you want to write a Boolean "
            + "otherwise the component calculates the amount of % to open the Valve")
    boolean valueToWriteIsBoolean() default false;

    @AttributeDefinition(name = "ChannelAddress", description = "ChannelAddress, if OneChannel is selected")
    String channelAddress() default "AM_2/ActivateHydraulicMixer";

    @AttributeDefinition(name = "Channels To Read and Write From", description = "First Channel is to Read, Second to Write, Third for Max Value, Fourth for min Value only important if Multiple Channel are controlled")
    String[] channels() default {"valve0/PowerLevel", "valve0/SetPowerLevel", "valve0/MaxValvePower", "valve0/MinValveValue"};

    @AttributeDefinition(name = "Reference Valve", description = "The Valve for the LineCooler.")
    String valveBypass() default "Valve0";

    @AttributeDefinition(name = "TimerId FallbackUse", description = "Timer to check if Time is up for Fallback to use")
    String timerIdFallback() default "TimerByTime";

    @AttributeDefinition(name = "Timeout of Remote signal", description = "Seconds after no signal that the fallback starts")
    int timeoutMaxRemote() default 30;
    @AttributeDefinition(name = "TimerId Restart Cycle", description = "Timer Id for a RestartCycle")
    String timerIdRestartCycle() default "TimerByCycles";

    @AttributeDefinition(name = "Restart cycle after time", description = "if the sensor gets cold again and a new cycle should be started")
    int timeoutRestartCycle() default 600;

    @AttributeDefinition(name = "Use Fallback be activated", description = "If there is no signal, hold the line on temperature")
    boolean shouldFallback() default false;

    @AttributeDefinition(name = "Minute for Fallbackstart", description = "MinuteStamp to start Fallback, e.g. start at x:15")
    int minuteFallbackStart() default 15;

    @AttributeDefinition(name = "Minute for FallbackStop", description = "Minute stamp to stop Fallback, e.g. stop at x:30")
    int minuteFallbackStop() default 30;

    boolean useMinMax() default false;

    @AttributeDefinition(name = "Maximum Power Level for Valve in %", description = "The Valve is not allowed to open more than this value")
    double maxValveValue() default 100;

    @AttributeDefinition(name = "Minimum Power Level for Valve in %", description = "The Valve has to be open for at least this value")
    double minValveValue() default 0;

    @AttributeDefinition(name = "Only regulate Max Min", description = "The LineCooler  will ONLY set the Max and Min% values for the Valve.")
    boolean maxMinOnly() default false;

    boolean useDecentralCooler() default false;

    @AttributeDefinition(name = "optional Decentralcooler", description = "If a Decentralcooler directly needs a communication")
    String decentralcoolerReference() default "Decentralcooler0";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Hydraulic Line Cooler [{id}]";

}