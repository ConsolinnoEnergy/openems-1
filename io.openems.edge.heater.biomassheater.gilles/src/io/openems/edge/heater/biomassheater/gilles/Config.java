package io.openems.edge.heater.biomassheater.gilles;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Heater Woodchips Gilles",
        description = "A Massheater by Gilles, using Woodchips. Communicating via Modbus."
)
@interface Config {

    @AttributeDefinition(name = "MassHeater-Device ID", description = "Unique Id of the MassHeater.")
    String id() default "WoodChipHeater0";

    @AttributeDefinition(name = "alias", description = "Human readable name of MassHeater.")
    String alias() default "";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modBusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Integer Unit Id of the Component.")
    int modBusUnitId() default 0;

    @AttributeDefinition(name = "Default power percent set point", description = "If the heater receives the command to "
            + "turn on, this value will be used if no set point is received. Valid values are 0 to 100, unit is percent. "
            + "So 50 means 50% of maximum power.")
    int defaultSetPointPowerPercent() default 100;

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

    @AttributeDefinition(name = "Only Activate the Heater", description = "Only start the heater, no SetPoint.")
    boolean onlyEnableBioMassHeater() default true;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Heater Woodchips Gilles [{id}]";

}