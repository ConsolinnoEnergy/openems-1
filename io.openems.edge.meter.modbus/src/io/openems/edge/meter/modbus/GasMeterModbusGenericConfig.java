package io.openems.edge.meter.modbus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Modbus GasMeter Generic", description = "A Generic Modbus GasMeter, Configure the Meter, as you please.")
@interface GasMeterModbusGenericConfig {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the .")
    String id() default "";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "ModbusBridgeId", description = "The Modbus Bridge Id")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "Modbus Unit Id", description = "The Modbus Unit Id")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Channel Ids", description = "These will be filled in automatically and shows the supported channel")
    String[] channelIds();

    @AttributeDefinition(name = "Task Types", description = "This will be filled in automatically and shows the available "
            + "Types (Read and Write Coil and Register)")
    String[] TaskType();

    @AttributeDefinition(name = "Configuration", description = "Configuration for this ModbusMeter, Expected Entry is:"
            + "\"Channel:ModbusAddress:TaskType:PRIORITY\" \n please note: Priority is Optional. The Configuration Expects a Splitter, the current Splitter is a ':'"
            + " Please Type in a ChannelId, listed in ChannelIds first, then Type in the ModbusAddress, followed by the TaskType, listed in TaskType and then the Priority, if needed"
            + "Priorities are by default LOW and are only needed by Read Coils and Inputs")
    String[] configurationList() default {"Power:1:READ_REGISTER:HIGH"};

    boolean enabled() default true;

    boolean configurationDone() default false;

    String webconsole_configurationFactory_nameHint() default "GasMeter {id}";
}
