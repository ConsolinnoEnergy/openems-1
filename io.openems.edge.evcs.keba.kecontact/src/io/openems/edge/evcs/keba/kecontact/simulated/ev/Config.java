package io.openems.edge.evcs.keba.kecontact.simulated.ev;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Simulated EV", description = "This simulates an electric Vehicle charging at an EVCS.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Phase", description = "Number of Phases the EV should charge from.")
    int phase() default 1;

    @AttributeDefinition(name = "ChargePower", description = "Power in A the EV should charge.")
    int charge() default 10;

    @AttributeDefinition(name = "EVCSId", description = "Unique Id of the Charging station.")
    String EVCSId() default "";


    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
