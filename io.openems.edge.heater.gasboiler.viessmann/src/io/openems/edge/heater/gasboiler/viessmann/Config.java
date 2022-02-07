package io.openems.edge.heater.gasboiler.viessmann;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Heater Gasboiler Viessmann",
        description = "A Gasboiler provided by Viessmann, communicating via ModbusTCP."
)
@interface Config {

    @AttributeDefinition(name = "GasBoiler-Device ID", description = "Unique Id of the GasBoiler.")
    String id() default "GasBoiler0";

    @AttributeDefinition(name = "alias", description = "Human readable name of GasBoiler.")
    String alias() default "";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Integer Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Default power percent setpoint [%]", description = "Value for "
            + "The default power percent setting of the heater until a value is set via channel SetPointHeatingPowerPercent. "
            + "Valid values are 50 to 100, unit is percent. So 50 means 50% of maximum power.")
    double defaultSetPointPowerPercent() default 70;

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

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Heater Gas Boiler Viessmann [{id}]";

}