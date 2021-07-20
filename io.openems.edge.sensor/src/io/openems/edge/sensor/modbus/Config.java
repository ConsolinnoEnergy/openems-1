package io.openems.edge.sensor.modbus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "Sensor Signal Modbus ", description = ".")
@interface Config {


    String service_pid();

    @AttributeDefinition(name = "Device ID", description = "Unique Id of the Natural Gas Sensor Module.")
    String id() default "SignalSensor0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Signal Active Register", description = "Modbus Register for Signal Active.")
    int address() default 1;

    @AttributeDefinition(name = "alias", description = "Human readable name of the natural gas sensor Module.")
    String alias() default "";

    @AttributeDefinition(name = "SignalType", description = "Is the Signal an Error/Status")
    SignalType signalType() default SignalType.ERROR;

    @AttributeDefinition(name = "Inverted Logic", description = "Usually ON signal at T. >100°C--> inverted Logic : Signal on at < 100°C")
    boolean inverted() default true;

    boolean useActiveMessage() default true;

    String messageActive() default "Error Occurred!";

    boolean useIdleMessage() default false;

    String messageIdle() default "Everything's ok";

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Modbus Signal Sensor {id}";
}
