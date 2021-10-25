package io.openems.edge.controller.heatnetwork.hydraulic.lineheater;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Hydraulic Line Heater",
        description = "This Controller heats ab a hydraulic line."
)
@interface ConfigLineHeater {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Controller.")
    String id() default "HydraulicLineHeater0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Controller.")
    String alias() default "Hydraulic-Line";

    @AttributeDefinition(name = "Reference Temperature", description = "The Temperature-Sensor for the LineHeater.")
    String tempSensorReference() default "TemperatureSensor0";

    @AttributeDefinition(name = "Default Temperature", description = "How long should we heat? OFF(in dC --> 1°C == 10°dC).")
    String temperatureDefault() default "800";

    @AttributeDefinition(name = "Valve, MultiChannel, OneChannel", description = "Do you want to Access Channel, or multiple Channel directly or access Valve")
    LineHeaterType lineHeaterType() default LineHeaterType.ONE_CHANNEL;


    @AttributeDefinition(name = "Value to Write is Boolean", description = "Is the Value you want to write a Boolean "
            + "otherwise the component calculates the amount of % to open the Valve")
    boolean valueToWriteIsBoolean() default false;

    @AttributeDefinition(name = "ChannelAddress", description = "ChannelAddress, if OneChannel is selected")
    String channelAddress() default "AM_2/ActivateHydraulicMixer";

    @AttributeDefinition(name = "Channels To Read and Write From", description = "First Channel is to Read, Second to Write, Third for Max Value, Fourth for min Value only important if Multiple Channel are controlled")
    String[] channels() default {"valve0/CurrentPowerLevel", "valve0/SetPointPowerLevel", "valve0/MaxAllowedValue", "valve0/MinAllowedValue"};

    @AttributeDefinition(name = "Reference Valve", description = "The Valve for the LineHeater.")
    String valveBypass() default "Valve0";

    @AttributeDefinition(name = "TimerId FallbackUse", description = "Timer for Fallback and CycleRestart as well as checking missing/old components.")
    String timerId() default "TimerByTime";

    @AttributeDefinition(name = "Timeout of Remote signal", description = "Seconds after no signal that the fallback starts")
    int timeoutMaxRemote() default 30;

    @AttributeDefinition(name = "Restart cycle after time", description = "if the sensor gets cold again and a new cycle should be started")
    int timeoutRestartCycle() default 600;

    @AttributeDefinition(name = "Use Fallback be activated", description = "If there is no signal, hold the line on temperature")
    boolean shouldFallback() default false;

    @AttributeDefinition(name = "Minute for FallbackStart", description = "MinuteStamp to start Fallback, e.g. start at x:15")
    int minuteFallbackStart() default 15;

    @AttributeDefinition(name = "Minute for FallbackStop", description = "Minute stamp to stop Fallback, e.g. stop at x:30")
    int minuteFallbackStop() default 30;

    boolean useMinMax() default false;

    @AttributeDefinition(name = "Maximum Power Level for Valve in %", description = "The Valve is not allowed to open more than this value")
    double maxValveValue() default 100;

    @AttributeDefinition(name = "Minimum Power Level for Valve in %", description = "The Valve has to be open for at least this value")
    double minValveValue() default 0;

    @AttributeDefinition(name = "Only regulate Max Min", description = "The LineHeater  will ONLY set the Max and Min% values for the Valve.")
    boolean maxMinOnly() default false;

    boolean useDecentralizedHeater() default false;

    @AttributeDefinition(name = "optional DecentralizedHeater", description = "If a DecentralizedHeater directly needs a communication")
    String decentralizedHeaterReference() default "DecentralizedHeater";

    @AttributeDefinition(name = "ReactionType", description = "Should the LineHeater open, when the DecentralizedHeater "
            + "sends a Heat request or MORE Heat request")
    DecentralizedHeaterReactionType reactionType() default DecentralizedHeaterReactionType.NEED_HEAT;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Hydraulic Line Heater [{id}]";

}