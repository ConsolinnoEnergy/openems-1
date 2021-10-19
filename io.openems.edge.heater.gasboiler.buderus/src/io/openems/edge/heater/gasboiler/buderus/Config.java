package io.openems.edge.heater.gasboiler.buderus;

import io.openems.edge.heater.gasboiler.buderus.api.OperatingMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Heater Buderus",
        description = "A module to map Modbus calls to OpenEMS channels for a Buderus heater."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Heater-Device ID", description = "Unique Id of the heater.")
    String id() default "Heater0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "alias", description = "Human readable name of heater.")
    String alias() default "";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Integer Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Default control mode", description = "Control mode set at startup. Heater will stay in "
            + "that mode until it is changed by a command.")
    OperatingMode operatingMode() default OperatingMode.SET_POINT_POWER_PERCENT;

    @AttributeDefinition(name = "Default set point for control mode \"power percent\"", description = "If the heater is "
            + "in control mode \"power percent\" and receives the command to turn on, this value will be used if no set "
            + "point is received. Valid values are 0 to 100, unit is percent. So 50 means 50% of maximum power.")
    int defaultSetPointPowerPercent() default 100;

    @AttributeDefinition(name = "Default set point for control mode \"temperature\"", description = "If the heater is "
            + "in control mode \"temperature\" and receives the command to turn on, this value will be used if no set "
            + "point is received. Valid values are 0 to 120, unit is °C.")
    int defaultSetPointTemperature() default 100;

    @AttributeDefinition(name = "Wait time EnableSignal", description = "How long to wait after the EnableSignal is "
            + "no longer received before the heater is switched off. Unit is seconds, unless cycles option is selected.")
    int waitTimeEnableSignal() default 30;

    @AttributeDefinition(name = "EnableSignal timer unit is cycles not seconds", description = "Use OpenEMS cycles "
            + "instead of seconds as the unit for the timer.")
    boolean enableSignalTimerIsCyclesNotSeconds() default false;

    @AttributeDefinition(name = "Use ExceptionalState", description = "React to commands from the Exceptional State "
            + "interface. When the Exceptional State is active, this will override any other commands.")
    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "Wait time ExceptionalState", description = "How long to wait after the Exceptional "
            + "State Active Signal is no longer received before the heater leaves the Exceptional State. Unit is "
            + "seconds, unless cycles option is selected.")
    int waitTimeExceptionalState() default 30;

    @AttributeDefinition(name = "ExceptionalState timer unit is cycles not seconds", description = "Use OpenEMS cycles "
            + "instead of seconds as the unit for the timer.")
    boolean exceptionalStateTimerIsCyclesNotSeconds() default false;

    @AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
    boolean readOnly() default false;

    @AttributeDefinition(name = "Print info to log", description = "Print status info to the log.")
    boolean printInfoToLog() default false;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Heater Buderus Device [{id}]";

}