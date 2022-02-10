package io.openems.edge.meter.virtual.asymmetric.subtract;

import io.openems.edge.meter.api.MeterType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Meter Virtual Asymmetric Subtract",
        description = "This is a virtual meter built from subtracting other meters." //
                + "The logic calculates `Minuend - Subtrahend1 - Subtrahend2 - ...`." //
                + "Example use-case: create a virtual Grid-Meter from Production-Meter and Consumption-Meter by " //
                + "configuring the Consumption-Meter as Minuend and Production-Meter as Subtrahend.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Meter.")
    String id() default "meter1";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Meter.")
    String alias() default "";

    @AttributeDefinition(name = "Meter-Type", description = "Grid, Production (=default), Consumption")
    MeterType type() default MeterType.PRODUCTION;

    @AttributeDefinition(name = "Minuend-ID", description = "Component-ID of the minuend")
    String minuend_id();

    @AttributeDefinition(name = "Subtrahends-IDs", description = "Component-IDs of the subtrahends")
    String[] subtrahends_ids();

    @AttributeDefinition(name = "Add to Sum?", description = "Should the data of this meter be added to the Sum?")
    boolean addToSum() default false;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Meter Virtual Asymmetric Subtract [{id}]";
}
