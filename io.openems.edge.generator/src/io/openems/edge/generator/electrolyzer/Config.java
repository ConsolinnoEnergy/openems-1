package io.openems.edge.generator.electrolyzer;

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


    @AttributeDefinition(name = "Modbus Register energy produced", description = "Modbus Register for energy produced.")
    int modbusRegisterEnergyProduced() default -1;

    @AttributeDefinition(name = "Modbus Register temp source", description = "Modbus Register for temp source.")
    int modbusRegisterWMZTempSource() default -1;

    @AttributeDefinition(name = "Modbus Register temp sink", description = "Modbus Register for temp sink.")
    int modbusRegisterWMZTempSink() default -1;

    @AttributeDefinition(name = "Modbus Register power Read", description = "Modbus Register for power Read.")
    int modbusRegisterWMZPower() default -1;

    ControlMode controlMode() default ControlMode.READ_WRITE;

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

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Generic Modbus Component [{id}]";

}