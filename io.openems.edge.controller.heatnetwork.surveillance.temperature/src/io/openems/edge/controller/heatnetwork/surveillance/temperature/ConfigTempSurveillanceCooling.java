package io.openems.edge.controller.heatnetwork.surveillance.temperature;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Controller Temperature Surveillance Cooling", description = "Temperature Surveillance Controller: On Certain Temperatures Control Valve/Cooler")
@interface ConfigTempSurveillanceCooling {
    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for this Controller.")
    String id() default "TemperatureSurveillance0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Controller.")
    String alias() default "";

    @AttributeDefinition(name = "ReferenceThermometer", description = "Own Thermometer to compare itself to other Thermometer")
    String referenceThermometerId() default "TemperatureSensor0";

    @AttributeDefinition(name = "Thermometer Activate Id", description = "Activation Thermometer, if reference beneath this Thermometer -> activate")
    String thermometerActivateId() default "ThermometerVirtual0";

    @AttributeDefinition(name = "TemperatureOffset Activate", description = "If Reference < ActivateThermometer + Offset --> Control Components; Offset in dC")
    int offsetActivate() default 100;

    @AttributeDefinition(name = "Thermometer Deactivate", description = "Deactivation Thermometer")
    String thermometerDeactivateId() default "ThresholdThermometer1";

    @AttributeDefinition(name = "TemperatureOffset Deactivate", description = "If Reference > Deactivate thermometer + Offset --> Deactivate: Unit dC")
    int offsetDeactivate() default -100;

    @AttributeDefinition(name = "SurveillanceType", description = "Set the Surveillance Type")
    SurveillanceType surveillanceType() default SurveillanceType.HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER;

    @AttributeDefinition(name = "ValveControllerId", description = "Unique Id of the ValveController you want to use")
    String hydraulicControllerId() default "ValveController0";


    @AttributeDefinition(name = "Cooler Id")
    String coolerId() default "Cooler0";

    @AttributeDefinition(name = "Timer for Valve", description = "Timer Id to check For Missing Components and if SurveillanceType is Cooler And Valve -> Delay HydraulicController")
    String timerId() default "TimerByCycles";

    @AttributeDefinition(name = "Delta Time Delay HydraulicController Enable", description = "How long to wait unit HydraulicController activates")
    int deltaTimeDelay() default 120;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Temperature Surveillance Cooling [{id}]";
}
