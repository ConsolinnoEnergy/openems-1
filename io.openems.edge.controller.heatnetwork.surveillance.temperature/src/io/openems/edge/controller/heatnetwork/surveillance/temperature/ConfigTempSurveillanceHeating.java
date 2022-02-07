package io.openems.edge.controller.heatnetwork.surveillance.temperature;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Controller Temperature Surveillance Heating", description = "Temperature Surveillance Controller: On Certain Temperatures Control Valve/Heater")
@interface ConfigTempSurveillanceHeating {
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


    @AttributeDefinition(name = "Heater Id")
    String heaterId() default "Heater0";

    @AttributeDefinition(name = "Timer for Valve", description = "Timer Id to check For Missing Components and if SurveillanceType is Heater And Valve -> Delay HydraulicController")
    String timerId() default "TimerByCounting";

    @AttributeDefinition(name = "Delta Time Delay HydraulicController Enable", description = "How long to wait unit HydraulicController activates")
    int deltaTimeDelay() default 120;

    @AttributeDefinition(name = "Handle OFFLINE state as \"Blocked or error\"", description = "IF Heater is offline, should the Heater be treated in the same way, as if it was blocked/has an error?"
            + " If this Controller should activate but an error occurred it will stop it's execution of logic.")
    boolean disableLogicIfHeaterOffline() default false;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Temperature Surveillance Heating [{id}]";
}
