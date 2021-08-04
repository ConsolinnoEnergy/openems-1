package io.openems.edge.heater.decentral;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Heater Decentral",
        description = "A Valve controlled by 2 relays used in the passing station."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Valve Name", description = "Unique Id of the Valve.")
    String id() default "DecentralHeater0";

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

    @AttributeDefinition(name = "SetPointTemperature", description = "SetPoint to OpenValve, also setPoint To Tell: NeedMoreHeat, Unit: DeciDegree: 1°C = 10dC")
    int setPointTemperature() default 700;

    int maximumThermalOutputInKw() default 150;

    boolean shouldCloseOnActivation() default true;

    @AttributeDefinition(name = "Force Heating", description = "On HeaterEnabled Signal -> Force Heating no matter if the Response/Callback says ok or not")
    boolean forceHeating() default false;

    @AttributeDefinition(name = "Cycles to wait when Need Heat Enable Signal (Central Controller) isn't present", description = "How many Cycles do you wait for the Central Communication Controller if Communication is lost")
    int waitTimeNeedHeatResponse() default 8;

    @AttributeDefinition(name = "NeedHeatResponse timer unit is seconds not cycles", description = "Use seconds instead "
            + "of OpenEMS cycles as the unit for the timer.")
    boolean heatResponseTimerIsSecondsNotCycles() default false;

    @AttributeDefinition(name = "Wait time EnableSignal", description = "How long to wait after the EnableSignal is "
            + "no longer received before the heater is switched off. Unit is seconds, unless cycles option is selected.")
    int waitTimeEnableSignal() default 30;

    @AttributeDefinition(name = "EnableSignal timer unit is cycles not seconds", description = "Use OpenEMS cycles "
            + "instead of seconds as the unit for the timer.")
    boolean enableSignalTimerIsCyclesNotSeconds() default false;

    @AttributeDefinition(name = "Use ExceptionalState", description = "React to commands from the Exceptional State "
            + "interface. When the Exceptional State is active, this will override any other commands.")
    boolean useExceptionalState() default true;

    @AttributeDefinition(name = "Wait time ExceptionalState", description = "How long to wait after the Exceptional "
            + "State Active Signal is no longer received before the heater leaves the Exceptional State. Unit is "
            + "seconds, unless cycles option is selected.")
    int waitTimeExceptionalState() default 60;

    @AttributeDefinition(name = "ExceptionalState timer unit is cycles not seconds", description = "Use OpenEMS cycles "
            + "instead of seconds as the unit for the timer.")
    boolean exceptionalStateTimerIsCyclesNotSeconds() default false;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Heater Decentral [{id}]";
}