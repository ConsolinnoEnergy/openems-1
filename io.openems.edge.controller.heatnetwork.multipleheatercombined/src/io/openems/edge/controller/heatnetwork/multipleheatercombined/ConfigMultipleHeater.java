package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Multiple Heater",
        description = "This Controller regulates Heater by activation/Deactivation Temperatures and Thermometer."
)
@interface ConfigMultipleHeater {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Controller.")
    String id() default "MultipleHeaterCombined0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Controller.")
    String alias() default "";

    @AttributeDefinition(name = "Heating Device Primary Names", description = "Unique Ids of the primary Heating Devices.")
    String[] heaterIds() default {"Chp0", "WoodChipHeater0", "GasBoiler0"};

    @AttributeDefinition(name = "Heating Device Activation Temperature in dC", description = "Threshold of the primary Heating Devices, when to activate the Heater (in dC --> 1°C == 10°dC). "
            + "If measured Temperature beneath this -> Activate heater. Can also be a ChannelAddress!")
    String[] activationTemperatures() default {"600","600","VirtualThermometer0/Temperature"};

    @AttributeDefinition(name = "Heating Device Deactivation Temperature in dC",
            description = "Threshold of the Heating Devices should be turned OFF(in dC --> 1°C == 10°dC). Can also be a ChannelAddress")
    String[] deactivationTemperatures() default {"800","800", "VirtualThermometer1/Temperature"};

    @AttributeDefinition(name = "Activation Thermometers", description = "The Thermometer measuring the activation Temperatures.")
    String[] activationThermometers() default {"TemperatureSensor0", "TemperatureSensor2", "TemperatureSensor4"};

    @AttributeDefinition(name = "Deactivation Thermometers", description = "The Temperature-Sensor for the Heating Device 1 Temperature MAX.")
    String[] deactivationThermometers() default {"TemperatureSensor1", "TemperatureSensor3", "TemperatureSensor5"};

    @AttributeDefinition(name = "useTimer", description = "Offset the activation of the Relay by time.")
    boolean useTimer() default false;

    @AttributeDefinition(name = "timeDelta", description = "Time (in s or Cycles) between the activation of the Relay/s and reaching of the activationTemperature.")
    int timeDelta() default 900;

    @AttributeDefinition(name = "timerId", description = "Unique Id of the timer")
    String timerId() default "TimerByTime";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Multiple Heater Combined [{id}]";

}