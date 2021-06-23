package io.openems.edge.hydraulic.minmax;

import io.openems.edge.hydraulic.api.MinMax;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "MinMax To Channel Writer ", description = ".")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "MinMaxChannelWriter";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Channel to read From", description = "Enter Channel to read the Value from.")
    String[] channel() default {"TemperatureSensor0/Temperature", "VirtualThermometer0/Thermometer"};

    @AttributeDefinition(name = "Channel to write into", description = "Write the Max/Min Value into this channel")
    String[] answerChannel() default {"VirtualThermometer/VirtualTemperature"};

    @AttributeDefinition(name = "Min or Max", description = "Min or Max Value from given Channel")
    MinMax minOrMax() default MinMax.MAX;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "{id}";
}
