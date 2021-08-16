package io.openems.edge.heater.decentral;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Heater Decentral",
        description = "A Heater that is controlled, via a HydraulicComponent or a HydraulicComponentController."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Heater Name", description = "Unique Id of the Heater.")
    String id() default "DecentralHeater0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "ComponentOrComponentController", description = "Control a Valve directly or via Controller",
            options = {
                    @Option(label = "ComponentController", value = "ComponentController"),
                    @Option(label = "Component", value = "Component"),
                   }
    )
    String componentOrController() default "ComponentController";

    @AttributeDefinition(name = "ComponentOrControllerId", description = "The Component that will be controlled, or the Controller you want to set")
    String componentOrControllerId() default "HydraulicController0";

    @AttributeDefinition(name = "ThresholdThermometerId", description = "ThresholdThermometer to Check")
    String thresholdThermometerId() default "ThresholdThermometer0";

    @AttributeDefinition(name = "SetPointTemperature", description = "SetPoint to OpenValve, also setPoint To Tell: NeedMoreHeat, Unit: DeciDegree: 1°C = 10dC")
    int setPointTemperature() default 700;

    int maximumThermalOutputInKw() default 150;

    boolean shouldCloseOnActivation() default true;

    @AttributeDefinition(name = "Force Heating", description = "On HeaterEnabled Signal -> Force Heating no matter if the Response/Callback says ok or not")
    boolean forceHeating() default false;

    @AttributeDefinition(name = "Timer for NeedHeatResponse", description = "Timer Id either TimerByTime or TimerByCycles")
    String timerNeedHeatResponse() default "TimerByCycles";

    @AttributeDefinition(name = "Cycles to wait when Need Heat Enable Signal (Central Controller) isn't present", description = "How many Cycles do you wait for the Central Communication Controller if Communication is lost")
    int timeNeedHeatResponse() default 8;

    boolean enableExceptionalStateHandling() default true;

    @AttributeDefinition(name = "Timer for ExceptionalState", description = "Timer Id for ExceptionalState Handling, either TimerByTimer or TimerByCycles")
    String timerExceptionalState() default "TimerByCycles";

    @AttributeDefinition(name = "Wait Time/Cycles for ExceptionalState", description = "How long do you await a new Exceptional State enable Signal.")
    int timeToWaitExceptionalState() default 60;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Heater Decentral [{id}]";
}