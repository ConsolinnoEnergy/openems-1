package io.openems.edge.utility.virtualchannel.modbus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Generic Modbus Channel",
        description = "A module to map Modbus calls to OpenEMS channels for a Generic OpenemsChannel."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Device Id", description = "Unique Id for the Component.")
    String id() default "GenericChannel0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Channel Ids", description = "These will be filled in automatically and shows the supported channel")
    String[] channelIds();

    @AttributeDefinition(name = "Task Types", description = "This will be filled in automatically and shows the available "
            + "Types (Read and Write Coil and Register)")
    String[] taskType();

    @AttributeDefinition(name = "Word Types", description = "This will be filled in automatically and shows the available "
            + "WordTypes say if you have a signed/unsigned int of 1(16), 2(32) or 4(64) register size. Same for Float")
    String[] wordType();

    @AttributeDefinition(name = "Priority", description = "This will be filled in automatically and shows the available ")
    String[] priorities();

    @AttributeDefinition(name = "WordOrder", description = "This will be filled in automatically and shows the available ")
    String[] wordOrder();

    @AttributeDefinition(name = "Configuration", description = "Configuration for this ModbusMeter, Expected Entry is:"
            + "\"Channel:ModbusAddress:TaskType:wordType:Priority:LengthORScaleFactor\" "
            + "\n please note: Priority is Optional. The Configuration Expects a Splitter, the current Splitter is a ':'\n"
            + "Please Type in a ChannelId, listed in ChannelIds first, then Type in the ModbusAddress, followed by the TaskType and the WordType, listed in TaskType and then the Priority, if needed"
            + "Priorities are by default LOW and are only needed by Read Coils and Inputs, WRITE_REGISTER using wordOrder Here"
            + "In any way the last entry should always be the Length of an expected String OR the ScaleFactor. (10^ScaleFactor)"
            + "NOTE: Use only ModbusChannel")
    String[] configurationList() default {"ReadLong:1:READ_REGISTER:WORD_TYPE:HIGH:2", "WriteLong:2:WRITE_REGISTER:FLOAT_32:MSWLSW:0"};

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Generic Modbus Component {id}";
}