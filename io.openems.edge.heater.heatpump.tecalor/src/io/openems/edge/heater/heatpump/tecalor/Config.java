package io.openems.edge.heater.heatpump.tecalor;

import io.openems.edge.heater.api.HeatpumpControlMode;
import io.openems.edge.heater.heatpump.tecalor.api.OperatingMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Heat Pump Tecalor", //
		description = "Implements a Tecalor heat pump.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "HeatPump0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-Bridge Id", description = "The Unique Id of the Modbus-Bridge you want to allocate to this device.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "OpenEMS control mode", description = "Use EnableSignal or SmartGridState to control the "
			+ "heat pump. They are mutually exclusive.")
	HeatpumpControlMode openEmsControlMode() default HeatpumpControlMode.ENABLE_SIGNAL;

	@AttributeDefinition(name = "Wait time EnableSignal", description = "How long to wait after the EnableSignal is "
			+ "no longer received before the heat pump is switched off. Unit is seconds, unless cycles option is selected.")
	int waitTimeEnableSignal() default 30;

	@AttributeDefinition(name = "EnableSignal timer Id", description = "Name of the timer used for the EnableSignal.")
	String enableSignalTimerId() default "TimerByTime";

	@AttributeDefinition(name = "Use ExceptionalState", description = "React to commands from the Exceptional State "
			+ "interface. When the Exceptional State is active, this will override any other commands.")
	boolean useExceptionalState() default false;

	@AttributeDefinition(name = "Wait time ExceptionalState", description = "How long to wait after the Exceptional "
			+ "State Active Signal is no longer received before the heat pump leaves the Exceptional State. Unit is "
			+ "seconds, unless cycles option is selected.")
	int waitTimeExceptionalState() default 30;

	@AttributeDefinition(name = "ExceptionalState timer Id", description = "Name of the timer used for the ExceptionalState.")
	String exceptionalStateTimerId() default "TimerByTime";

	@AttributeDefinition(name = "Default on state", description = "When EnableSignal or ExceptionalState turns "
			+ "on the heat pump, switch to this mode. (The \"off\" state is \"antifreeze\".)")
	OperatingMode defaultModeOfOperation() default OperatingMode.PROGRAM_MODE;

	@AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
	boolean readOnly() default false;

	@AttributeDefinition(name = "Print info to log", description = "Print status info to the log.")
    boolean printInfoToLog() default false;

	String webconsole_configurationFactory_nameHint() default "Heat Pump Tecalor [{id}]";

}