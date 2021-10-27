package io.openems.edge.heater.heatpump.heliotherm;

import io.openems.edge.heater.heatpump.heliotherm.api.ControlMode;
import io.openems.edge.heater.heatpump.heliotherm.api.PowerControlSetting;
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

    @AttributeDefinition(name = "Default control mode", description = "Use EnableSignal or temperature set point to "
            + "control heat pump activation.")
    ControlMode defaultControlMode() default ControlMode.ENABLE_SIGNAL;

    @AttributeDefinition(name = "Temperature set point for control mode \"temperature set point\" [°C]", description = "If "
            + "the heat pump is in control mode \"temperature set point\", it will turn itself on and off to keep the "
            + "specified temperature. Configuration of the heat pump determines which temperature "
            + "input is used as reference. Valid values are 0 to 120, unit is °C.")
    int defaultSetPointTemperature() default 60;

    @AttributeDefinition(name = "Power control setting", description = "Enter the power control mode that results from "
            + "the heat pump configuration. Consumption (photovoltaic mode on) or compressor speed (photovoltaic mode off).")
    PowerControlSetting powerControlSetting() default PowerControlSetting.CONSUMPTION;

    @AttributeDefinition(name = "Maximum electric power consumption [W]", description = "The maximum electric power "
            + "consumption of this heat pump. Only required if power control setting is \"consumption\". Unit is watt.")
    int maxElectricPower() default 2000;

    @AttributeDefinition(name = "Use SetPointHeatingPowerPercent in consumption mode", description = "Calculate a power "
            + "consumption value from the Heater interface channel SetPointHeatingPowerPercent. When this is active, you "
            + "cannot set the electric power consumption directly.")
    boolean mapPowerPercentToConsumption() default true;

    @AttributeDefinition(name = "Compressor speed limit [%]", description = "Enter the the compressor speed limit that is "
            + "set by the heat pump configuration. Only required if power control setting is \"compressor speed\". Unit is %.")
    int maxCompressorSpeed() default 60;

    @AttributeDefinition(name = "Default power set point [%]", description = "The default power setting the heat pump uses "
            + "when it is switched on, either because of EnableSignal or temperature set point. Used only when nothing else "
            + "is specified via the power set point channels. Put 0 to use heat pump automatic mode. Depending on the "
            + "power control setting, 100% means either maximum consumption or maximum compressor speed.")
    int defaultSetPointPowerPercent() default 100;

    @AttributeDefinition(name = "Wait time EnableSignal", description = "For control mode EnableSignal: How long to wait "
            + "after the EnableSignal is no longer received before the heat pump is switched off. Unit is seconds, unless "
            + "cycles option is selected.")
    int waitTimeEnableSignal() default 30;

    @AttributeDefinition(name = "EnableSignal timer Id", description = "Name of the timer used for the EnableSignal.")
    String enableSignalTimerId() default "TimerByTime";

    @AttributeDefinition(name = "Use ExceptionalState", description = "For control mode EnableSignal: React to commands "
            + "from the Exceptional State interface. When the Exceptional State is active, this will override any other "
            + "commands.")
    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "Wait time ExceptionalState", description = "How long to wait after the Exceptional "
            + "State Active Signal is no longer received before the heat pump leaves the Exceptional State. Unit is "
            + "seconds, unless cycles option is selected.")
    int waitTimeExceptionalState() default 30;

    @AttributeDefinition(name = "ExceptionalState timer Id", description = "Name of the timer used for the ExceptionalState.")
    String exceptionalStateTimerId() default "TimerByTime";

    @AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
    boolean readOnly() default false;

    @AttributeDefinition(name = "Print info to log", description = "Print status info to the log.")
    boolean printInfoToLog() default false;


    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Heat Pump Heliotherm Device [{id}]";

}