package io.openems.edge.heater.chp.kwenergy;

import io.openems.edge.heater.chp.kwenergy.api.ControlMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Chp KW Energy Smartblock",
        description = "A module to map Modbus calls to OpenEMS channels for a KW Energy Smartblock CHP."
)
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "Chp0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Modbus-Bridge Id", description = "The Unique Id of the Modbus-Bridge you want to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Wait time EnableSignal", description = "How long to wait after the EnableSignal is "
            + "no longer received before the Chp is switched off. Unit is seconds, unless cycles option is selected.")
    int waitTimeEnableSignal() default 30;

    @AttributeDefinition(name = "EnableSignal timer Id", description = "Name of the timer used for the EnableSignal.")
    String enableSignalTimerId() default "TimerByTime";

    @AttributeDefinition(name = "Use ExceptionalState", description = "React to commands from the Exceptional State "
            + "interface. When the Exceptional State is active, this will override any other commands.")
    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "Wait time ExceptionalState", description = "How long to wait after the Exceptional "
            + "State Active Signal is no longer received before the chp leaves the Exceptional State. Unit is "
            + "seconds, unless cycles option is selected.")
    int waitTimeExceptionalState() default 30;

    @AttributeDefinition(name = "ExceptionalState timer Id", description = "Name of the timer used for the ExceptionalState.")
    String exceptionalStateTimerId() default "TimerByTime";

    @AttributeDefinition(name = "Default control mode", description = "Control mode of the Chp until a mode is set via "
            + "channel ControlMode. Control mode \"consumption\" requires the CHP to get the current power "
            + "consumption value either via cable or Modbus.")
    ControlMode controlMode() default ControlMode.POWER_PERCENT;

    @AttributeDefinition(name = "Maximum electrical power [kW]", description = "Maximum electric power of this KW Energy "
            + "Smartblock model. Unit is [kW]. Required for control mode \"set point electric power\" to work.")
    double maxChpPower() default 22;

    @AttributeDefinition(name = "Default set point for control mode \"power percent\" [%]", description = "Value for "
            + "control mode \"set point power percent\" until a value is set via channel SetPointHeatingPowerPercent. "
            + "Valid values are 0 to 100, unit is percent. So 50 means 50% of maximum power.", max = "100", min = "0")
    int defaultSetPointPowerPercent() default 70;

    @AttributeDefinition(name = "Default set point for control mode \"power\" [kW]", description = "Value for "
            + "control mode \"set point electric power\" until a value is set via channel EffectiveElectricPowerSetpoint. "
            + "Valid values are 0 up to maximum electric power, unit is kilowatt.")
    double defaultSetPointElectricPower() default 18;

    @AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
    boolean readOnly() default false;

    @AttributeDefinition(name = "Print info to log", description = "Print status info to the log.")
    boolean printInfoToLog() default false;

    String webconsole_configurationFactory_nameHint() default "Heater Chp KW Energy Smartblock [{id}]";

}