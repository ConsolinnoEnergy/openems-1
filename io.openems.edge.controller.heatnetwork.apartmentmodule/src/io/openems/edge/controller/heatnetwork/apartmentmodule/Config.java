package io.openems.edge.controller.heatnetwork.apartmentmodule;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Heating Apartment Module",
        description = "A module to map Modbus calls to OpenEMS channels for a Apartment Module."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "ApartmentModule-Device ID", description = "Unique Id of the Apartment Module.")
    String id() default "ControllerApartmentModuleCord0";

    @AttributeDefinition(name = "alias", description = "Human readable name of the Apartment Module.")
    String alias() default "";

    @AttributeDefinition(name = "ApartmentCords", description = "Enter for each ApartmentCord the ApartmentModules separated by ':' a new Line represents a new Cord")
    String[] apartmentCords() default {"ApartmentModule0:ApartmentModule1:ApartmentModule2", "ApartmentModule3:ApartmentModule4:ApartmentModule5"};

    @AttributeDefinition(name = "ApartmentModule to Thermometer", description = "Map an ApartmentModule to a Thermometer")
    String[] apartmentToThermometer() default {"AM_1:ThermometerVirtual3", "AM_2:ThermometerVirtual4", "AM_3:ThermometerVirtual5",
            "AM_4:ThermometerVirtual6", "AM_5:ThermometerVirtual7"};

    @AttributeDefinition(name = "Response Channel", description = "Enter the ChannelAddress of a Component, "
            + "that reacts to the corresponding Chord, e.g. HydraulicLineHeater0/EnableSignal,"
            + "The Order of entries matters; first Line corresponds to first entered chord, second responds responds to second chord etc etc.")
    String[] apartmentResponse() default {"HydraulicLineHeater0/EnableSignal", "HydraulicLineHeater1/EnableSignal"};

    @AttributeDefinition(name = "SetPoint Temperature", description = "The Temperature that needs to be reached by a Thermometer, otherwise apartment Response starts boosting the Temperature")
    int setPointTemperature() default 500;

    @AttributeDefinition(name = "Threshold Thermometer", description = "The Threshold Thermometer you want to use to check your Temperature.")
    String[] thresholdId() default {"ThresholdThermometer0", "ThresholdThermometer0"};

    @AttributeDefinition(name = "Heatpump to activate on Requests", description = "A Heatpump that activates on Requests")
    String heatPumpId() default "Pump0";

    @AttributeDefinition(name = "Value to Set Pump on Activation", description = "Set the Level of Performance in % of the Heatpump on Activation")
    double powerLevelPump() default 100;

    boolean useHeatBooster() default true;

    @AttributeDefinition(name = "HeatBooster activation Temperature", description = "If the Temperature drops below this "
            + "point when the HeatPump should activate/run, the booster will start")
    int heatBoostTemperature() default 500;

    @AttributeDefinition(name = "HeatBooster Id", description = "The HeatBooster Id")
    String heatBoosterId() default "HeatBooster0";

    @AttributeDefinition(name = "TimerId", description = "Timer to use for checking Missing Components")
    String timerId();

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Consolinno Apartment Module Device [{id}]";

}