package io.openems.edge.heater.decentralized;

import io.openems.edge.heater.api.ComponentType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Cooler Decentralize",
        description = "A Cooler that enables an Hydraulic Controller and awaits and EnableSignal. Symbolizes e.g. a cooling Storage"
)
@interface ConfigDecentralizedCooler {

    String service_pid();

    @AttributeDefinition(name = "Cooler Name", description = "Unique Id of the Cooler.")
    String id() default "DecentralizedCooler0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "ComponentOrComponentController", description = "Control a Valve directly or via Controller")
    ComponentType componentOrController() default ComponentType.CONTROLLER;

    @AttributeDefinition(name = "ComponentOrControllerId", description = "The Component that will be controlled, or the Controller you want to set")
    String componentOrControllerId() default "HydraulicController0";

    @AttributeDefinition(name = "ThresholdThermometerId", description = "ThresholdThermometer to Check")
    String thresholdThermometerId() default "ThresholdThermometer0";

    @AttributeDefinition(name = "SetPointTemperature", description = "SetPoint to OpenValve, also setPoint To Tell: NeedMoreCool, Unit: DeciDegree: 1°C = 10dC")
    int setPointTemperature() default 300;


    @AttributeDefinition(name = "Force Cooling", description = "On CoolerEnabled Signal -> Force Cooling no matter if the Response/Callback says ok or not")
    boolean forceCooling() default false;

    @AttributeDefinition(name = "Timer Id for NeedCoolResponse", description = "Timer Id either TimerByTime or TimerByCycles")
    String timerIdNeedCoolResponse() default "TimerByCycles";

    @AttributeDefinition(name = "Cycles to wait when Need Cool Enable Signal (Central Controller) isn't present", description = "How many Cycles do you wait for the Central Communication Controller if Communication is lost")
    int timeNeedCoolResponse() default 8;

    boolean enableExceptionalStateHandling() default true;

    @AttributeDefinition(name = "Timer Id for ExceptionalState", description = "Timer Id for ExceptionalState Handling, either TimerByTimer or TimerByCycles")
    String timerIdExceptionalState() default "TimerByCycles";

    @AttributeDefinition(name = "Wait Time/Cycles for ExceptionalState", description = "How long do you await a new Exceptional State enable Signal.")
    int timeToWaitExceptionalState() default 60;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Cooler Decentralized [{id}]";
}