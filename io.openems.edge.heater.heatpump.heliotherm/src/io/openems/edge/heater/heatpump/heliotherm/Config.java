package io.openems.edge.heater.heatpump.heliotherm;

import io.openems.edge.heater.heatpump.heliotherm.api.OperatingMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Heat Pump Heliotherm",
        description = "A module to map Modbus calls to OpenEMS channels for a Heliotherm heat pump."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "HeatPump-Device ID", description = "Unique Id of the heat pump.")
    String id() default "HeatPump0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "alias", description = "Human readable name of heat pump.")
    String alias() default "";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Integer Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Maximum electric power [W]", description = "Maximum electric power consumption of this heat pump. Unit is Watt. This is needed for control mode power percent.")
    int maxElectricPower() default 2000;

    @AttributeDefinition(name = "Default set point for control mode \"power percent\" [%]", description = "If the heater is in control mode \"power percent\" and receives the command to turn on, this value will be used if no set point is received. Valid values are 0 to 100, unit is percent. So 50 means 50% of maximum power.")
    int defaultSetPointPowerPercent() default 100;

    @AttributeDefinition(name = "Default set point for control mode \"temperature\" [°C]", description = "If the heater is in control mode \"temperature\" and receives the command to turn on, this value will be used if no set point is received. Valid values are 0 to 120, unit is °C.")
    int defaultSetPointTemperature() default 100;

    OperatingMode defaultOperatingMode() default OperatingMode.SET_POINT_TEMPERATURE;

    @AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
    boolean readOnly() default false;

    @AttributeDefinition(name = "Timer Id EnableSignal", description = "TimerByCycles or TimerByTime")
    String timerIdEnableSignal() default "TimerByCycles";

    @AttributeDefinition(name = "Wait time EnableSignal", description = "How long to wait after the EnableSignal is "
            + "no longer received before the heat pump is switched off.")
    int waitTimeEnableSignal() default 30;

    @AttributeDefinition(name = "Debug", description = "Enable debug mode, prints status info to the log.")
    boolean debug() default false;


    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Heat Pump Heliotherm Device [{id}]";

}