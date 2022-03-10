package io.openems.edge.heater.gasboiler.buderus;

import io.openems.edge.heater.gasboiler.buderus.api.OperatingMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Heater Buderus",
        description = "A module to map Modbus calls to OpenEMS channels for a Buderus heater."
)
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "Heater0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Modbus-Bridge Id", description = "The Unique Id of the Modbus-Bridge you want to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
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

    @AttributeDefinition(name = "EnableSignal timer Id", description = "Name of the timer used for the EnableSignal.")
    String enableSignalTimerId() default "TimerByTime";

    @AttributeDefinition(name = "Use ExceptionalState", description = "React to commands from the Exceptional State "
            + "interface. When the Exceptional State is active, this will override any other commands.")
    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "Wait time ExceptionalState", description = "How long to wait after the Exceptional "
            + "State Active Signal is no longer received before the heater leaves the Exceptional State. Unit is "
            + "seconds, unless cycles option is selected.")
    int waitTimeExceptionalState() default 30;

    @AttributeDefinition(name = "ExceptionalState timer Id", description = "Name of the timer used for the ExceptionalState.")
    String exceptionalStateTimerId() default "TimerByTime";

    @AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
    boolean readOnly() default false;

    @AttributeDefinition(name = "Print info to log", description = "Print status info to the log.")
    boolean printInfoToLog() default false;

    String webconsole_configurationFactory_nameHint() default "Heater Gas Boiler Buderus [{id}]";

}