package io.openems.edge.heater.chp.wolf;

import io.openems.edge.heater.chp.wolf.api.OperatingMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Chp Wolf",
        description = "A module to map Modbus calls to OpenEMS channels for a Wolf Chp.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "Chp0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";	

	@AttributeDefinition(name = "Modbus-Bridge ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Default operating mode", description = "Default setting for the operating mode.")
	OperatingMode defaultOperatingMode() default OperatingMode.ELECTRIC_POWER;

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

	@AttributeDefinition(name = "Chp electric power [kW]", description = "Maximum electric "
			+ "output of this chp. Unit is kilowatt.")
	int chpMaxElectricPower() default 4;

	@AttributeDefinition(name = "Default electric power setpoint [kW]", description = "Value for "
			+ "electric power setpoint until a value is set via channel EffectiveElectricPowerSetpoint. "
			+ "Valid values are half of maximum electric power up to maximum electric "
			+ "power, unit is kilowatt.")
	int defaultSetPointElectricPower() default 4;

	@AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
	boolean readOnly() default false;

	@AttributeDefinition(name = "Print info to log", description = "Print status info to the log.")
	boolean printInfoToLog() default false;
	
	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Heater Chp Wolf [{id}]";

}