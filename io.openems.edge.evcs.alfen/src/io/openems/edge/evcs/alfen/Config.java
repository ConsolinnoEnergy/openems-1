package io.openems.edge.evcs.alfen;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Alfen EVCS", description = ".")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "Unique Id for the Modbusunit.")
    int modbusUnitId();

    @AttributeDefinition(name = "ModbusBridgeId", description = "Unique Id for the Modbusbridge")
    String modbusBridgeId();

    String webconsole_configurationFactory_nameHint() default "{id}";
}
