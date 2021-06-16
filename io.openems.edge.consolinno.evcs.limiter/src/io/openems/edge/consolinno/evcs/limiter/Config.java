package io.openems.edge.consolinno.evcs.limiter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Evcs Limiter", description = "Limit the power of the EVCS connected in a Cluster.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Limiter.")
    String id() default "ConsolinnoLimiter";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Limiter.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "evcsIDs", description = "Ids of the EVCS that have to be managed.")
    String[] evcss() default {"evcs0","evcs1"};

    @AttributeDefinition(name = "MeterID", description = "Id of the Meter.")
    boolean useMeter() default true;
    @AttributeDefinition(name = "MeterID", description = "Id of the Meter.")
    String meter() default "";

    @AttributeDefinition(name = "symmetry", description = "Check if the EVCS should stay balanced in their load.")
    boolean symmetry() default true;

    @AttributeDefinition(name = "offTime", description = "Time (in minutes) how long an EVCS should be turned off if the Power has to be balanced.")
    int offTime() default 0;

    @AttributeDefinition(name = "phaseLimit", description = "Maximum Power (W) one Phase can pull from the grid.")
    int phaseLimit() default 0;

    @AttributeDefinition(name = "powerLimit", description = "Maximum Power (W) the entire EVCS cluster can pull from the grid.")
    int powerLimit() default 0;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
