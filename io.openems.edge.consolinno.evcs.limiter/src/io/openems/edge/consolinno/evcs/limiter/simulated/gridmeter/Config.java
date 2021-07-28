package io.openems.edge.consolinno.evcs.limiter.simulated.gridmeter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Consolinno Simulated EVCS Limiter Grid Meter", //
        description = "This simulates an 'reacting' Grid meter.")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "meter0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "limiterId", description = "Id of the Evcs Limiter")
    String limiterId() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "ScaleFactor", description = "Scales the Power detected by the meter.")
    int scaleFactor() default 1;

    @AttributeDefinition(name = "Minimum Ever Active Power", description = "This is automatically updated.")
    int minActivePower();

    @AttributeDefinition(name = "Maximum Ever Active Power", description = "This is automatically updated.")
    int maxActivePower();

    String webconsole_configurationFactory_nameHint() default "Simulator GridMeter Reacting [{id}]";
}