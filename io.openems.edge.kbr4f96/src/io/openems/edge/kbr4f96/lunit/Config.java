package io.openems.edge.kbr4f96.lunit;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "Kbr 4F96 L Unit", description = "L Unit of the Kbr 4F96 Unit.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Kbr L Unit.")
    String id() default "KbrL1";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Kbr L Unit.")
    String alias() default "";

    @AttributeDefinition(name = "MainUnitId", description = "Unique Id for the Kbr Main Unit, that this L Unit is part of.")
    String mainUnitId() default "Kbr";

    @AttributeDefinition(name = "Position", description = "Pin Position for the Kbr L Unit.",
    options = {
            @Option(label = "1", value = "1"),
            @Option(label = "2", value = "2"),
            @Option(label = "3", value = "3"),
    })
    int position() default 1;

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "Unique Id for the Modbusunit.")
    int modbusUnitId();
    @AttributeDefinition(name = "ModbusBridgeId", description = "Unique Id for the Modbusbridge")
    String modbusBridgeId();
    String webconsole_configurationFactory_nameHint() default "{id}";
}
