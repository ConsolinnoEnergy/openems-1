package io.openems.edge.bridge.genibus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Bridge GeniBus", //
        description = "Provides a service for connecting to, querying and writing to GeniBus devices.")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "genibus0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Port-Name", description = "The name of the serial port - e.g. '/dev/ttyUSB0'")
    String portName() default "/dev/ttyUSB0";

    @AttributeDefinition(name = "Increase telegram timeout time [ms]", description = "Increase the telegram timeout time, "
            + "unit is milliseconds. Improves connection reliability, but may decrease speed. This number needs to be adjusted "
            + "to the computer on which you run OpenEMS. A laptop can work with 0 ms, while a leaflet needs 200 ms.")
    int timeoutIncreaseMs() default 200;

    @AttributeDefinition(name = "Debug", description = "Enable debug mode.")
    boolean debug() default false;

    String webconsole_configurationFactory_nameHint() default "Bridge GeniBus [{id}]";
}
