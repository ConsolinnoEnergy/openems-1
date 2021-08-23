package io.openems.edge.heater.chp.viessmann;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Chp Vitobloc",
        description = "A combined heat and power system."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Chp-Device ID", description = "Unique Id of the Chp Device.")
    String id() default "Chp0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Chp.")
    String alias() default "";

    @AttributeDefinition(name = "Chp Type", description = "What Chp Type do you want to use(Not important for pure"
            + "controlling if no further information is needed).",
            options = {
                    @Option(label = "Vitobloc 200 EM - 6/15", value = "EM_6_15"),
                    @Option(label = "Vitobloc 200 EM - 9/20", value = "EM_9_20"),
                    @Option(label = "Vitobloc 200 EM - 20/39", value = "EM_20_39"),
                    @Option(label = "Vitobloc 200 EM - 20/39_70", value = "EM_20_39_70"),
                    @Option(label = "Vitobloc 200 EM - 50/81", value = "EM_50_81"),
                    @Option(label = "Vitobloc 200 EM - 70/115", value = "EM_70_115"),
                    @Option(label = "Vitobloc 200 EM - 100/167", value = "EM_100_167"),
                    @Option(label = "Vitobloc 200 EM - 140/207 SCR", value = "EM_140_207"),
                    @Option(label = "Vitobloc 200 EM - 199/263", value = "EM_199_263"),
                    @Option(label = "Vitobloc 200 EM - 199/293", value = "EM_199_293"),
                    @Option(label = "Vitobloc 200 EM - 238/363", value = "EM_238_363"),
                    @Option(label = "Vitobloc 200 EM - 363/498", value = "EM_363_498"),
                    @Option(label = "Vitobloc 200 EM - 401/549 SCR", value = "EM_401_549"),
                    @Option(label = "Vitobloc 200 EM - 530/660", value = "EM_530_660"),
                    @Option(label = "Vitobloc 200 BM - 36/66", value = "BM_36_66"),
                    @Option(label = "Vitobloc 200 BM - 55/88", value = "BM_55_88"),
                    @Option(label = "Vitobloc 200 BM - 190/238", value = "BM_190_238"),
                    @Option(label = "Vitobloc 200 BM - 366/437", value = "BM_366_437")
            })
    String chpType() default "EM_140_207";

    @AttributeDefinition(name = "Chp activated by relay", description = "If the chp needs a relay to switch it on.")
    boolean useRelay() default true;

    @AttributeDefinition(name = "RelayID", description = "OpenEMS component ID of the relay to switch the Chp on.")
    String relayId() default "Relay0";

    @AttributeDefinition(name = "AIO module ID", description = "Chp power is regulated using the Consolinno AIO module. "
            + "Not needed in read only mode.")
    String aioModuleId() default "Aio0";

    @AttributeDefinition(name = "Current min [mA]", description = "For chp power regulation with AIO module: lower limit in mA.")
    short minLimit() default 0;

    @AttributeDefinition(name = "Current max [mA]", description = "For chp power regulation with AIO module: upper limit in mA.")
    short maxLimit() default 20;

    @AttributeDefinition(name = "Percentage range", description = "Range of power percent setpoint (depending on chp type):"
            + " 0-100% (type 0) or 50-100% (type 50).")
    int percentageRange() default 0;

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

    @AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
    boolean readOnly() default false;

    @AttributeDefinition(name = "Print info to log", description = "Print status info to the log.")
    boolean printInfoToLog() default false;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Chp Device [{id}]";

}
