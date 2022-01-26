package io.openems.edge.heater.heatpump.alphainnotec;

import io.openems.edge.heater.api.HeatpumpControlMode;
import io.openems.edge.heater.heatpump.alphainnotec.api.CoolingMode;
import io.openems.edge.heater.heatpump.alphainnotec.api.HeatingMode;
import io.openems.edge.heater.heatpump.alphainnotec.api.PoolMode;
import io.openems.edge.heater.heatpump.alphainnotec.api.VentilationMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(//
		name = "Heat Pump Alpha Innotec", //
		description = "Implements an Alpha Innotec heat pump")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "HeatPump0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Modbus-Bridge ID", description = "ID of Modbus bridge.")
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

	@AttributeDefinition(name = "Default Heating mode", description = "Default setting for Heating mode. This mode is "
			+ "active when the heat pump is on either because of EnableSignal/ExceptionalState or SmartGridState.")
	HeatingMode defaultHeatingMode() default HeatingMode.AUTOMATIC;

	@AttributeDefinition(name = "Default MixingCircuit2 mode", description = "Default setting for MixingCircuit2 mode. "
			+ "This mode is active when the heat pump is on either because of EnableSignal/ExceptionalState or SmartGridState.")
	HeatingMode defaultMixingCircuit2Mode() default HeatingMode.OFF;

	@AttributeDefinition(name = "Default MixingCircuit3 mode", description = "Default setting for MixingCircuit3 mode. "
			+ "This mode is active when the heat pump is on either because of EnableSignal/ExceptionalState or SmartGridState.")
	HeatingMode defaultMixingCircuit3Mode() default HeatingMode.OFF;

	@AttributeDefinition(name = "Default DomesticHotWater mode", description = "Default setting for DomesticHotWater mode. "
			+ "This mode is active when the heat pump is on either because of EnableSignal/ExceptionalState or SmartGridState.")
	HeatingMode defaultDomesticHotWaterMode() default HeatingMode.OFF;

	@AttributeDefinition(name = "Default Cooling mode", description = "Default setting for Cooling mode. "
			+ "This mode is active when the heat pump is on either because of EnableSignal/ExceptionalState or SmartGridState.")
	CoolingMode defaultCoolingMode() default CoolingMode.OFF;

	@AttributeDefinition(name = "Default Ventilation mode", description = "Default setting for Ventilation mode. "
			+ "This mode is active when the heat pump is on either because of EnableSignal/ExceptionalState or SmartGridState.")
	VentilationMode defaultVentilationMode() default VentilationMode.OFF;

	@AttributeDefinition(name = "Default SwimmingPool mode", description = "Default setting for SwimmingPool mode. "
			+ "This mode is active when the heat pump is on either because of EnableSignal/ExceptionalState or SmartGridState.")
	PoolMode defaultPoolMode() default PoolMode.OFF;

	@AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
	boolean readOnly() default false;

	@AttributeDefinition(name = "Print info to log", description = "Print status info to the log.")
	boolean printInfoToLog() default false;

	@AttributeDefinition(name = "Full remote mode", description = "Full remote mode. Only relevant for sending commands, "
			+ "irrelevant in read only mode.")
	boolean fullRemoteMode() default false;

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus target filter", description = "This is auto-generated by 'Modbus-ID'.")
	String Modbus_target() default "";

	String webconsole_configurationFactory_nameHint() default "Heat Pump Alpha Innotec [{id}]";
}