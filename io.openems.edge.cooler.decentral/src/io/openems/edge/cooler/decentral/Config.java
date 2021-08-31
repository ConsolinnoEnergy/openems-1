package io.openems.edge.cooler.decentral;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Cooler Decentralize",
        description = "A Cooler that enables an Hydraulic Controller and awaits and EnableSignal."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Valve Name", description = "Unique Id of the Valve.")
    String id() default "DecentralizedCooler0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "ValveOrValveController", description = "Control a Valve directly or via Controller",
            options = {
                    @Option(label = "ValveController", value = "ValveController"),
                    @Option(label = "Valve", value = "Valve")}
    )
    String valveOrController() default "ValveController";

    @AttributeDefinition(name = "ValveOrControllerId", description = "The Valve that will be controlled, or the Controller you want to set")
    String valveOrControllerId() default "StaticValveController0";

    @AttributeDefinition(name = "ThresholdThermometerId", description = "ThresholdThermometer to Check")
    String thresholdThermometerId() default "ThresholdThermometer0";

    @AttributeDefinition(name = "SetPointTemperature", description = "SetPoint to OpenValve, if the Temperature is not reached / Above Value -> Ask for more Cooling, Unit: DeciDegree: 1°C = 10dC")
    int setPointTemperature() default 200;

    int maximumThermalOutputInKw() default 150;

    boolean shouldCloseOnActivation() default true;

    @AttributeDefinition(name = "Force Cooling", description = "On CoolerEnabled Signal -> Force Cooling no matter if the Response/Callback says ok or not")
    boolean forceCooling() default false;

    @AttributeDefinition(name = "Cycles to wait when Need Cool Enable Signal (Central Controller) isn't present", description = "How many Cycles do you wait for the Central Communication Controller if Communication is lost")
    int waitCyclesNeedCoolResponse() default 8;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Cooler Decentral [{id}]";
}