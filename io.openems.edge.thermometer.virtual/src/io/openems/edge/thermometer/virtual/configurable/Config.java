package io.openems.edge.thermometer.virtual.configurable;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Thermometer Virtual Configurable", description = "This Thermometer works almost "
        + "the same as the Virtual Thermometer. The only difference is, "
        + "that you can adapt the Active and Inactive Temperature and it will update the config")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the Component.")
    String id() default "ThermometerVirtualConfigurable";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "AutoEnable", description = "If this is on, the activeValue is always written, otherwise this requires an \"EnableSignal\"")
    boolean autoApply() default false;

    @AttributeDefinition(name = "Active Temperature", description = "If the Thermometer is active this Temperature will be written")
    int activeTemperature() default 400;

    boolean useInactiveTemperature() default true;

    @AttributeDefinition(name = "Inactive Temperature", description = "If the Thermometer is not active -> this temperature will be written")
    int inactiveTemperature() default 0;

    @AttributeDefinition(name = "TimerId", description = "The Timer to use")
    String timerID() default "TimerByCycles";

    @AttributeDefinition(name = "Max WaitTime", description = "How long to wait after enableSignal is Missing till Thermometer deactivates")
    int waitTime() default 1;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Thermometer Virtual Configurable {id}";
}
