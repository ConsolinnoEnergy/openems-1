package io.openems.edge.battery.siemens;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Management for Siemens Battery", //
        description = "This Manager gives you functions to Send to a Siemens Battery over REST.")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "SiemensManager";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "tenant_ID", description = "Tenant_ID provided by Cactus.")
    String tenant_ID();

    @AttributeDefinition(name = "token", description = "Token provided by Cactus.")
    String token();

    @AttributeDefinition(name = "project_ID", description = "Project_ID provided by Cactus.")
    String project_ID();

    @AttributeDefinition(name = "product_ID", description = "Product_ID provided by Cactus.")
    String product_ID();

    @AttributeDefinition(name = "serial", description = "Serial provided by Cactus.")
    String serial();

    @AttributeDefinition(name = "Capacity", description = "Capacity of the Individual Battery.")
    float capacity() default (float) 3.3;


    String webconsole_configurationFactory_nameHint() default "{id}";

}