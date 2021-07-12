package io.openems.edge.heatsystem.components.pump.old;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Test Device Pump ConsolinnoPwm", description = "Allows you to test a Pump, simulates Consolinno Pwm")
@interface TestDevicePumpConsolinnoPwmConfig {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "TestDevicePump";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "PumpType", description = "PumpType that will be tested e.g. Relay/Pwm/Both")
    PumpType pumpType() default PumpType.RELAY;

    @AttributeDefinition(name = "Enable Disturbance", description = "Simulates possible disturbance")
    boolean useDisturbance() default false;


    @AttributeDefinition
    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
