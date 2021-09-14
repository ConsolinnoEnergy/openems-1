package io.openems.edge.heater.chp.kwenergy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Chp KW Energy Smartblock",
        description = "A module to map Modbus calls to OpenEMS channels for a KW Energy Smartblock CHP."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "CHP-Device ID", description = "Unique Id of the CHP.")
    String id() default "Chp0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "alias", description = "Human readable name of CHP.")
    String alias() default "";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Integer Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Turn on Chp", description = "The Chp will run as long as this module is running. "
            + "Checking the \"Use EnableSignal\" box will override this option.")
    boolean turnOnChp() default false;

    @AttributeDefinition(name = "Use EnableSignal", description = "React to commands from the Heater interface "
            + "EnableSignal channel. Will turn off the Chp when there is no signal.")
    boolean useEnableSignalChannel() default false;

    @AttributeDefinition(name = "Wait time EnableSignal", description = "How long to wait after the EnableSignal is "
            + "no longer received before the Chp is switched off. Unit is seconds, unless cycles option is selected.")
    int waitTimeEnableSignal() default 30;

    @AttributeDefinition(name = "EnableSignal timer unit is cycles not seconds", description = "Use OpenEMS cycles "
            + "instead of seconds as the unit for the timer.")
    boolean enableSignalTimerIsCyclesNotSeconds() default false;

    @AttributeDefinition(name = "Use ExceptionalState", description = "React to commands from the Exceptional State "
            + "interface. When the Exceptional State is active, this will override any other commands.")
    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "Wait time ExceptionalState", description = "How long to wait after the Exceptional "
            + "State Active Signal is no longer received before the chp leaves the Exceptional State. Unit is "
            + "seconds, unless cycles option is selected.")
    int waitTimeExceptionalState() default 30;

    @AttributeDefinition(name = "ExceptionalState timer unit is cycles not seconds", description = "Use OpenEMS cycles "
            + "instead of seconds as the unit for the timer.")
    boolean exceptionalStateTimerIsCyclesNotSeconds() default false;

    @AttributeDefinition(name = "Default control mode", description = "Control mode of the Chp until a mode is set via "
            + "channel ControlMode. Control mode \"power consumption\" requires the CHP to get the current power "
            + "consumption value either via cable or Modbus.",
            options = {
                    @Option(label = "Setpoint power percent", value = "powerPercent"),
                    @Option(label = "Setpoint electric power", value = "power"),
                    @Option(label = "Power consumption", value = "consumption"),
            })
    String controlMode() default "powerPercent";

    @AttributeDefinition(name = "Maximum electrical power [kW]", description = "Maximum electric power of this KW Energy "
            + "Smartblock model. Unit is [kW]. Required for control mode \"setpoint electric power\" to work.")
    int maxChpPower() default 22;

    @AttributeDefinition(name = "Default setpoint for control mode \"power percent\" [%]", description = "Value for "
            + "control mode \"setpoint power percent\" until a value is set via channel SetPointHeatingPowerPercent. "
            + "Valid values are 50 to 100, unit is percent. So 50 means 50% of maximum power.")
    int defaultSetPointPowerPercent() default 70;

    @AttributeDefinition(name = "Default setpoint for control mode \"electric power\" [kW]", description = "Value for "
            + "control mode \"setpoint electric power\" until a value is set via channel EffectiveElectricPowerSetpoint. "
            + "Valid values are half of maximum electric power up to maximum electric "
            + "power, unit is kilowatt.")
    int defaultSetPointElectricPower() default 18;

    @AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
    boolean readOnly() default false;

    @AttributeDefinition(name = "Debug", description = "Enable debug mode, prints status info to the log.")
    boolean debug() default false;

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Modbus target filter", description = "This is auto-generated by 'Modbus-ID'.")
    String Modbus_target() default "";

    String webconsole_configurationFactory_nameHint() default "CHP KW Energy Smartblock [{id}]";

}