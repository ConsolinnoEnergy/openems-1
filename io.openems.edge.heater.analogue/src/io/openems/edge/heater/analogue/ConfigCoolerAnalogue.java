package io.openems.edge.heater.analogue;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Cooler Analogue", description = "A Cooler that works with an Analogue Device/Module.")
@interface ConfigCoolerAnalogue {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "AnalogueCooler0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    AnalogueType analogueType() default AnalogueType.LUCID_CONTROL;

    @AttributeDefinition(name = "AnalogueDevice ID", description = "The Device Id that Controls the Cooler in reality")
    String[] analogueId() default {"Relay0"};

    /*boolean useErrorSignals() default true;

    @AttributeDefinition(name = "SignalSensor Ids", description = "If you want to use SignalSensor as ErrorHandling, put them here")
    String[] signalSensors() default {"SignalSensor0"};*/

    @AttributeDefinition(name = "Max Power in KW", description = "Maximum available Power")
    int maxPower() default 100;

    ControlType controlType() default ControlType.PERCENT;

    @AttributeDefinition(name = "Default PowerValue on Activation", description = "Default Power On Activation, this can be changed on RunTime via REST")
    int defaultRunPower() default 80;

    @AttributeDefinition(name = "MinPower", description = "If you want to run the Cooler with a minimum Power, you can set it up here")
    int defaultMinPower() default 0;

    boolean autoRun() default false;

    @AttributeDefinition(name = "TimerId", description = "This Timer will be used for the following 2 Configurations.")
    String timerId() default "TimerByCycles";

    @AttributeDefinition(name = "Max Time Enable Signal", description = "How Long to Wait after EnableSignal is Missing"
            + "and Cooler was Active until it sets itself inactive")
    int maxTimeEnableSignal() default 10;

    @AttributeDefinition(name = "Max Time Power Signal", description = "How Long to Wait after ")
    int maxTimePowerSignal() default 10;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Analogue Cooler {id}";
}