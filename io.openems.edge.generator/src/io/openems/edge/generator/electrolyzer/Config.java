package io.openems.edge.generator.electrolyzer;

import io.openems.edge.generator.api.ElectrolyzerAccessMode;
import io.openems.edge.generator.api.ControlMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Electrolyzer Modbus",
        description = "A module to map Modbus calls to OpenEMS channels for a Generic Modbus Electrolyzer."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Device ID", description = "Unique Id of the Electrolyzer.")
    String id() default "electrolyzer0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Modbus Register Enable Signal", description = "Modbus Register for Enabling the Electrolyzer")
    int modbusRegisterEnableSignal() default -1;

    @AttributeDefinition(name = "Modbus Register Power Write", description = "Modbus Register for Power Write")
    int modbusRegisterWritePower() default -1;

    @AttributeDefinition(name = "Timer for NeedHeatResponse", description = "Timer Id either TimerByTime or TimerByCycles")
    String timerNeedHeatResponse() default "TimerByCycles";

    @AttributeDefinition(name = "Cycles to wait when Need Heat Enable Signal (Central Controller) isn't present", description = "How many Cycles do you wait for the Central Communication Controller if Communication is lost")
    int timeNeedHeatResponse() default 8;

    boolean enableExceptionalStateHandling() default true;

    @AttributeDefinition(name = "Timer for ExceptionalState", description = "Timer Id for ExceptionalState Handling, either TimerByTimer or TimerByCycles")
    String timerExceptionalState() default "TimerByCycles";

    @AttributeDefinition(name = "Wait Time/Cycles for ExceptionalState", description = "How long do you await a new Exceptional State enable Signal.")
    int timeToWaitExceptionalState() default 60;

    @AttributeDefinition(name = "alias", description = "Human readable name of the Electrolyzer.")
    String alias() default "";

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


    @AttributeDefinition(name = "Configuration", description = "Configuration for this ModbusMeter, Expected Entry is:"
            + "\"Channel:ModbusAddress:TaskType:wordType:Priority:LengthORScaleFactor\" "
            + "\n please note: Priority is Optional. The Configuration Expects a Splitter, the current Splitter is a ':'\n"
            + "Please Type in a ChannelId, listed in ChannelIds first, then Type in the ModbusAddress, followed by the TaskType and the WordType, listed in TaskType and then the Priority, if needed"
            + "Priorities are by default LOW and are only needed by Read Coils and Inputs"
            + "In any way the last entry should always be the Length of an expected String OR the ScaleFactor. (10^ScaleFactor)")
    String[] configurationList() default {"Power:1:READ_REGISTER:WORD_TYPE:HIGH:2"};

    ControlMode controlMode() default ControlMode.READ;

    ElectrolyzerAccessMode accessMode() default ElectrolyzerAccessMode.HEATER;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Generic Modbus Component [{id}]";

}