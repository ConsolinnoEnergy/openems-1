package io.openems.edge.heater.chp.viessmann;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Chp Vitobloc",
        description = "A combined heat and power system."
)
@interface Config {

    @AttributeDefinition(name = "Chp-Device ID", description = "Unique Id of the Chp Device.")
    String id() default "Chp0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Chp.")
    String alias() default "";

    @AttributeDefinition(name = "Chp Type", description = "What Chp Type do you want to use(Not important for pure"
            + "controlling if no further information is needed).")
    ViessmannChpType chpType() default ViessmannChpType.Vito_EM_140_207;

    @AttributeDefinition(name = "Chp activated by relay", description = "If the chp needs a relay to switch it on.")
    boolean useRelay() default true;

    @AttributeDefinition(name = "RelayID", description = "OpenEMS component ID of the relay to switch the Chp on.")
    String relayId() default "Relay0";

    @AttributeDefinition(name = "AIO module ID", description = "Chp power is regulated using the Consolinno AIO module. "
            + "Not needed in read only mode.")
    String aioModuleId() default "Aio0";

    @AttributeDefinition(name = "Percentage range", description = "Range of power percent set point (depending on chp type):"
            + " 0-100% or 50-100%.")
    PercentageRange percentageRange() default PercentageRange.RANGE_0_100;

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Integer Unit Id of the Component.")
    int modbusUnitId() default 0;

    @AttributeDefinition(name = "Default power percent setpoint [%]", description = "Value for "
            + "The default power percent setting of the chp until a value is set via channel SetPointHeatingPowerPercent. "
            + "Valid values are 50 to 100, unit is percent. So 50 means 50% of maximum power.")
    double defaultSetPointPowerPercent() default 70;

    @AttributeDefinition(name = "Wait time EnableSignal", description = "How long to wait after the EnableSignal is "
            + "no longer received before the chp is switched off. Unit is seconds, unless cycles option is selected.")
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

    @AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
    boolean readOnly() default false;

    @AttributeDefinition(name = "Print info to log", description = "Print status info to the log.")
    boolean printInfoToLog() default false;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Heater Chp Viessmann [{id}]";

}
