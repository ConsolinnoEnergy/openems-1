package io.openems.edge.controller.hydrauliccomponent.controller;

import io.openems.edge.controller.hydrauliccomponent.api.ControlType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "Controller Hydraulic Position", description = "Static Valve Controller: Has a Valve and sets by Temperature:Value Mapping")
@interface ConfigHydraulicStaticPosition {
    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for this Controller.")
    String id() default "HydraulicController0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Controller.")
    String alias() default "";

    @AttributeDefinition(name = "ComponentId", description = "The Id of the Valve or Pump you want to Control")
    String componentToControl() default "Valve0";

    @AttributeDefinition(name = "temperatureToPosition", description = "Map in each entry the Temperature to the Position, "
            + "e.g. at 700dC set Valve to 20%, at 500dc to 70% etc")
    String[] temperaturePositionMap() default {"700:100", "500:50"};

    @AttributeDefinition(name = "Tolerance", description = "The amount of leeway the component is allowed to have so its still considered to be at the correct position.")
    double tolerance() default 1.5;

    @AttributeDefinition(name = "Control by", description = "Control this valve by Position or Temperature")
    ControlType controlType() default ControlType.TEMPERATURE;

    @AttributeDefinition(name = "ThermometerId", description = "The Thermometer you want to use for Temperature the ValveController reacts to")
    String thermometerId() default "TemperatureThreshold0";

    @AttributeDefinition(name = "autorun", description = "Should the Controller start automatically/sets it's own enable_signal")
    boolean autorun() default false;

    @AttributeDefinition(name = "Allow Force Open/Close", description = "Should calling Components be able to Allow Force Close/Open")
    boolean allowForcing() default false;

    @AttributeDefinition(name = "Close When No SignalEnable", description = "Should the Valve close when there is neither autorun or EnableSignal")
    boolean shouldCloseWhenNoSignal() default true;

    @AttributeDefinition(name = "Default ValvePosition on EnableSignal", description = "If config temperatures are beneath every setPoint/no positionMap is given: set this to default")
    int defaultPosition() default 100;

    @AttributeDefinition(name = "TimerId for Running Time", description = "Do you want to count Cycles or Time for the \"Running Time\"")
    String timerForRunning() default "TimerByCounting";

    @AttributeDefinition(name = "Running Time or Cycles after Enable and missing signal", description = "Run this amount of Cycles/Time if the controller was activated before AND EnableSignal is missing.")
    int waitForSignalAfterActivation() default 10;

    @AttributeDefinition(name = "ShouldCool", description = "If the controller should choose the lower temperature instead of the higher one.")
    boolean shouldCool() default false;

    @AttributeDefinition(name = "use Fallback", description = "If Enabled Signal isn't arriving (due to error) after activation, wait X amount till Fallback activates")
    boolean useFallback() default false;

    @AttributeDefinition(name = "TimerId for FallbackTime", description = "TimerId for the Fallback time/cycles")
    String timerForFallback() default "TimerByCycles";

    @AttributeDefinition(name = "Activation Time/CycleCount", description = "If an EnableSignal is not Present wait for the time amount previously configured -> activate the ValveController after amount of Time in seconds/Cycles")
    int fallbackRunTime() default 100;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Hydraulic Position Controller [{id}]";
}
