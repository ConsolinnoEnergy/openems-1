package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Multiple Cooler",
        description = "This Controller regulates Cooler by activation/Deactivation Temperatures and Thermometer."
)
@interface ConfigMultipleCooler {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Controller.")
    String id() default "MultipleCoolerCombined0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Controller.")
    String alias() default "";

    @AttributeDefinition(name = "Cooling Device Primary Names", description = "Unique Ids of the primary Cooling Devices.")
    String[] coolerIds() default {"Chp0", "WoodChipCooler0", "GasBoiler0"};

    @AttributeDefinition(name = "Cooling Device Activation Temperature in dC", description = "Threshold of the primary Cooling Devices, when to activate the Cooler (in dC --> 1°C == 10°dC). "
            + "If measured Temperature above this -> Activate cooler. Can also be a ChannelAddress!")
    String[] activationTemperatures() default {"600","600","VirtualThermometer0/Temperature"};

    @AttributeDefinition(name = "Cooling Device Deactivation Temperature in dC",
            description = "Threshold of the Cooling Devices should be turned OFF(in dC --> 1°C == 10°dC). Can also be a ChannelAddress")
    String[] deactivationTemperatures() default {"200","200", "VirtualThermometer1/Temperature"};

    @AttributeDefinition(name = "Activation Thermometers", description = "The Thermometer measuring the activation Temperatures.")
    String[] activationThermometers() default {"TemperatureSensor0", "TemperatureSensor2", "TemperatureSensor4"};

    @AttributeDefinition(name = "Deactivation Thermometers", description = "The Temperature-Sensor for the Cooling Device 1 Temperature MAX.")
    String[] deactivationThermometers() default {"TemperatureSensor1", "TemperatureSensor3", "TemperatureSensor5"};

    @AttributeDefinition(name = "useTimer", description = "Offset the activation of the Relay by time.")
    boolean useTimer() default false;

    @AttributeDefinition(name = "timeDelta", description = "After Completely Cooling down/Heating up the system, wait for this amount of time, until executing the Logic again.")
    int timeDelta() default 900;

    @AttributeDefinition(name = "timerId", description = "Unique Id of the timer")
    String timerId() default "TimerByTime";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Multiple Cooler Combined [{id}]";

}