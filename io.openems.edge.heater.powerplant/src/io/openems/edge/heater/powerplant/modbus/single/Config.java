package io.openems.edge.heater.powerplant.modbus.single;


import io.openems.edge.heater.api.EnergyControlMode;
import io.openems.edge.heater.electrolyzer.api.ControlMode;
import io.openems.edge.heater.electrolyzer.api.HydrogenMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Modbus Power Plant Analog",
        description = "A PowerPlant controlled by an analogue output (e.g. 0-10V)."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Device ID", description = "Unique Id of the CombinedHeatPowerPlant Module.")
    String id() default "chp0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component.")
    int modbusUnitId() default 1;

    ControlMode controlMode() default ControlMode.READ_WRITE;

    HydrogenMode hydrogenMode() default HydrogenMode.DEACTIVATED;

    EnergyControlMode energyControlMode() default EnergyControlMode.KW;

    @AttributeDefinition(name = "Timer for NeedHeatResponse", description = "Timer Id either TimerByTime or TimerByCycles")
    String timerNeedHeatResponse() default "TimerByCycles";

    @AttributeDefinition(name = "Cycles to wait when Need Heat Enable Signal (Central Controller) isn't present", description = "How many Cycles do you wait for the Central Communication Controller if Communication is lost")
    int timeNeedHeatResponse() default 8;

    boolean enableExceptionalStateHandling() default true;

    @AttributeDefinition(name = "Timer for ExceptionalState", description = "Timer Id for ExceptionalState Handling, either TimerByTimer or TimerByCycles")
    String timerExceptionalState() default "TimerByCounting";

    @AttributeDefinition(name = "Wait Time/Cycles for ExceptionalState", description = "How long do you await a new Exceptional State enable Signal.")
    int timeToWaitExceptionalState() default 60;

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
            + "Priorities are by default LOW and are only needed by Read Coils and Inputs"
            + "In any way the last entry should always be the Length of an expected String OR the ScaleFactor. (10^ScaleFactor)"
            + "NOTE: Use only ModbusChannel")
    String[] configurationList() default {"Power:1:READ_REGISTER:WORD_TYPE:HIGH:2", "SetPointPower:2:WRITE_REGISTER:FLOAT_32:MSWLSW:0"};


    @AttributeDefinition(name = "alias", description = "Human readable name of the natural gas sensor Module.")
    String alias() default "";

    boolean autoRun() default false;

    int defaultRunPower() default 50;

    @AttributeDefinition(name = "Active Enable Signal Value", description = "On True EnableSignal what value to write")
    int defaultEnableSignalValue() default 2;

    @AttributeDefinition(name = "Inactive Enable Signal Value", description = "On False/null EnableSignal what value to write")
    int defaultDisableSignalValue() default 1;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Generic Modbus Component [{id}]";
}