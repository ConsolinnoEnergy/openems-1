package io.openems.edge.heatsystem.components.valve.old.test;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "TEST Device for Valve ", description = "Device to activate to Test a ValveOneOutput OR Test Valve 2 Realy ")
@interface TestDeviceValveConfig {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "TestDeviceValve0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enableDisturbance() default true;

    TestDeviceType testForWhichValve() default TestDeviceType.TWO_RELAYS;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
