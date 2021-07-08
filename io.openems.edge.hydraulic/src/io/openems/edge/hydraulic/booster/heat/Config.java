package io.openems.edge.hydraulic.booster.heat;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "HeatBooster one Channel ", description = "This HeatBooster allows to write true/false into a given Channel, "
        + "if EnableSignal is Available.")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the HeatBooster.")
    String id() default "HeatBooster0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "ChannelAddress to write into", description = "WriteChannel Address to write the Booster into")
    String channelString() default "Relays0/WriteOnOff";

    @AttributeDefinition(name = "Enable Value", description = "Value to write into Channel when Enable Signal is present")
    int value() default 1;

    @AttributeDefinition(name = "Time or Cycles Enable Signal Expiration", description =
            "Amount of Time in seconds or number of cycles, after the Enable Signal for this HeatBooster expires.")
    int expiration() default 60;

    @AttributeDefinition(name = "Timer Id", description = "Timer to use")
    String timerId() default "TimerByCycles";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "HeatBooster {id}";
}
