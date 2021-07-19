package io.openems.edge.powerplant.combined;

import io.openems.edge.generator.api.ControlMode;
import io.openems.edge.generator.api.HydrogenMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Generic Modbus CombinedHeatPowerPlant Module",
        description = "A module to map Modbus calls to OpenEMS channels for a Generic Modbus CombinedHeatPowerPlant Module."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Device ID", description = "Unique Id of the CombinedHeatPowerPlant Module.")
    String id() default "chp0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Modbus Register WMZ Energy Amount register", description = "Modbus Register for WMZ Energy Amount .")
    int modbusRegisterWMZEnergyAmount() default -1;

    @AttributeDefinition(name = "Modbus Register WMZ Temp Source register", description = "Modbus Register for WMZ Temp Source .")
    int modbusRegisterWMZTempSource() default -1;

    @AttributeDefinition(name = "Modbus Register WMZ Temp Source sink", description = "Modbus Register for WMZ Temp sink .")
    int modbusRegisterWMZTempSink() default -1;

    @AttributeDefinition(name = "Modbus Register WMZ Power", description = "Modbus Register for WMZ Power .")
    int modbusRegisterWMZTPower() default -1;

    @AttributeDefinition(name = "Modbus Register Gas Meter ", description = "Modbus Register for Gas Meter .")
    int modbusRegisterGasMeter() default -1;

    @AttributeDefinition(name = "Modbus Register Gas kind", description = "Modbus Register for Gas kind .")
    int modbusRegisterGasKind() default -1;

    @AttributeDefinition(name = "Modbus Register Current power", description = "Modbus Register for current power .")
    int modbusRegisterCurrentPower() default -1;

    @AttributeDefinition(name = "Modbus Register target power", description = "Modbus Register for target power.")
    int modbusRegisterTargetPower() default -1;

    @AttributeDefinition(name = "Modbus Register Hours after last service", description = "Modbus Register for working hours after last service.")
    int modbusRegisterHoursAfterLastService() default -1;

    @AttributeDefinition(name = "Modbus Register Security off extern", description = "Modbus Register for security off extern.")
    int modbusRegisterSecurityOffExtern() default -1;

    @AttributeDefinition(name = "Modbus Register Start request evu", description = "Modbus Register for start request evu.")
    int modbusRegisterStartRequestEvu() default -1;

    @AttributeDefinition(name = "Modbus Register start request extern", description = "Modbus Register for start request extern.")
    int modbusRegisterStartRequestExtern() default -1;

    @AttributeDefinition(name = "Modbus Register stop request extern evu", description = "Modbus Register for stop request evu extern.")
    int modbusRegisterStopRequestEVUExtern() default -1;

    @AttributeDefinition(name = "Modbus Register stop request extern evu grid disconnect", description = "Modbus Register for stop request extern evu grid disconnect.")
    int modbusRegisterStopRequestEVUExternGridDisconnect() default -1;

    @AttributeDefinition(name = "Modbus Register electricity produced", description = "Modbus Register for electricity produced.")
    int modbusRegisterElectricityProduced() default -1;

    @AttributeDefinition(name = "Modbus Register electricity power", description = "Modbus Register for electricity power.")
    int modbusRegisterElectricityPower() default -1;

    ControlMode controlMode() default ControlMode.READ_WRITE;

    @AttributeDefinition(name = "Modbus Register Enable Signal", description = "Modbus Register for Enabling the Electrolyzer")
    int modbusRegisterEnableSignal() default -1;
    @AttributeDefinition(name = "Modbus Register Power Write", description = "Modbus Register for Power Write")
    int modbusRegisterWritePower() default -1;

    HydrogenMode hydrogenMode() default HydrogenMode.ACTIVE;
    @AttributeDefinition(name = "Hydrogen EnableSignal Address", description = "Modbus Register Enable Signal Address")
    int modbusRegisterHydrogenUse() default -1;

    @AttributeDefinition(name = "Timer for NeedHeatResponse", description = "Timer Id either TimerByTime or TimerByCycles")
    String timerNeedHeatResponse() default "TimerByCycles";

    @AttributeDefinition(name = "Cycles to wait when Need Heat Enable Signal (Central Controller) isn't present", description = "How many Cycles do you wait for the Central Communication Controller if Communication is lost")
    int timeNeedHeatResponse() default 8;

    boolean enableExceptionalStateHandling() default true;

    @AttributeDefinition(name = "Timer for ExceptionalState", description = "Timer Id for ExceptionalState Handling, either TimerByTimer or TimerByCycles")
    String timerExceptionalState() default "TimerByCycles";

    @AttributeDefinition(name = "Wait Time/Cycles for ExceptionalState", description = "How long do you await a new Exceptional State enable Signal.")
    int timeToWaitExceptionalState() default 60;


    @AttributeDefinition(name = "alias", description = "Human readable name of the natural gas sensor Module.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Generic Modbus Component [{id}]";

}