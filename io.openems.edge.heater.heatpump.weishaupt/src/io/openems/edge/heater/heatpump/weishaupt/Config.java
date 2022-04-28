package io.openems.edge.heater.heatpump.weishaupt;

import io.openems.edge.heater.heatpump.weishaupt.api.FlowTempRegulationMode;
import io.openems.edge.heater.heatpump.weishaupt.api.OperatingMode;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Heat Pump Weishaupt", //
		description = "A module to map Modbus calls to OpenEMS channels for a Weishaupt heat pump.")

@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "HeatPump0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
	boolean readOnly() default false;

	@AttributeDefinition(name = "Modbus-Bridge Id", description = "The Unique Id of the Modbus-Bridge you want to allocate to this device.")
	String modbusBridgeId() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 1;
	
	@AttributeDefinition(name = "Default mode of operation", description = "The default mode of operation that is applied at startup.")
	OperatingMode defaultModeOfOperation() default OperatingMode.AUTOMATIC;

	@AttributeDefinition(name = "Flow temperature regulation mode", description = "Set the flow temperature regulation mode. "
			+ "Available options are: 1. Use the outside temperature sensor and the heating curve 2. Manual mode, let OpenEMS "
			+ "set the flow temperature 3. Use the room temperature sensor and room temperature set point.")
	FlowTempRegulationMode defaultFlowTempRegulationMode() default FlowTempRegulationMode.OUTSIDE_TEMP;

	@AttributeDefinition(name = "Manual flow temperature set point [°C]", description = "If \"Flow temperature regulation mode\" "
			+ "is set to \"manual\", this is the default flow temperature set point that is applied at startup. Unit is °C, range is 18 to 60 °C.")
	int defaultFlowTempSetpoint() default 60;

	@AttributeDefinition(name = "Use EnableSignal", description = "Use the EnableSignal mechanism to turn the heat pump "
			+ "on and off by changing the mode of operation. In the \"on\" state, the heat pump is set to \"automatic\". "
			+ "In the \"off\" state, the pump is set to \"throttling\".")
	boolean useEnableSignal() default false;

	@AttributeDefinition(name = "Wait time EnableSignal", description = "For control mode EnableSignal: How long to wait "
			+ "after the EnableSignal is no longer received before the heat pump is switched off. Unit is seconds, unless "
			+ "cycles option is selected.")
	int waitTimeEnableSignal() default 30;

	@AttributeDefinition(name = "EnableSignal timer Id", description = "Name of the timer used for the EnableSignal.")
	String enableSignalTimerId() default "TimerByTime";

	@AttributeDefinition(name = "Use ExceptionalState", description = "For control mode EnableSignal: React to commands "
			+ "from the Exceptional State interface. When the Exceptional State is active, this will override any other "
			+ "commands.")
	boolean useExceptionalState() default false;

	@AttributeDefinition(name = "Wait time ExceptionalState", description = "How long to wait after the Exceptional "
			+ "State Active Signal is no longer received before the heat pump leaves the Exceptional State. Unit is "
			+ "seconds, unless cycles option is selected.")
	int waitTimeExceptionalState() default 30;

	@AttributeDefinition(name = "ExceptionalState timer Id", description = "Name of the timer used for the ExceptionalState.")
	String exceptionalStateTimerId() default "TimerByTime";

	@AttributeDefinition(name = "Print info to log", description = "Print status info to the log.")
    boolean printInfoToLog() default false;

	String webconsole_configurationFactory_nameHint() default "Heat Pump Weishaupt [{id}]";

}