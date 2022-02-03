package io.openems.edge.bridge.lucidcontrol.module;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "LucidControl Module",
        description = "LucidControl Module, connected via USB."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "LucidControlModule-ID", description = "ID of LucidControlModule.")
    String id() default "LucidControlInputModule0";

    @AttributeDefinition(name = "Alias", description = "Human readable Name.")
    String alias() default "";


    @AttributeDefinition(name = "Voltage", description = "Voltage provided by LucidControl (Written on Module itself).")
    ModuleTypes moduleTypes() default ModuleTypes.VOLTAGE_10;

    @AttributeDefinition(name = "Address via USB", description = "Address of USB Path")
            String path() default "/dev/ttyACM0";


    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "LucidControlModule[{id}]";
}