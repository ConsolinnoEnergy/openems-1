package io.openems.edge.heater.heatpump.alphainnotec;

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

	@AttributeDefinition(name = "Use EnableSignal", description = "React to commands from the Heater interface "
			+ "EnableSignal channel. Will turn off the heat pump when there is no signal, overriding any other commands.")
	boolean useEnableSignalChannel() default false;

	@AttributeDefinition(name = "Wait time EnableSignal", description = "How long to wait after the EnableSignal is "
			+ "no longer received before the heat pump is switched off. Unit is seconds, unless cycles option is selected.")
	int waitTimeEnableSignal() default 30;

	@AttributeDefinition(name = "EnableSignal timer unit is cycles not seconds", description = "Use OpenEMS cycles "
			+ "instead of seconds as the unit for the timer.")
	boolean enableSignalTimerIsCyclesNotSeconds() default false;

	@AttributeDefinition(name = "Use ExceptionalState", description = "React to commands from the Exceptional State "
			+ "interface. When the Exceptional State is active, this will override any other commands.")
	boolean useExceptionalState() default false;

	@AttributeDefinition(name = "Wait time ExceptionalState", description = "How long to wait after the Exceptional "
			+ "State Active Signal is no longer received before the heat pump leaves the Exceptional State. Unit is "
			+ "seconds, unless cycles option is selected.")
	int waitTimeExceptionalState() default 30;

	@AttributeDefinition(name = "ExceptionalState timer unit is cycles not seconds", description = "Use OpenEMS cycles "
			+ "instead of seconds as the unit for the timer.")
	boolean exceptionalStateTimerIsCyclesNotSeconds() default false;

	@AttributeDefinition(name = "Default mode of operation", description = "When EnableSignal or ExceptionalState turns "
			+ "on the heat pump, switch these modes to \"automatic\".",
	options = {
		@Option(label = "Heating", value = "Heating"),
		@Option(label = "DomesticHotWater", value = "DomesticHotWater"),
		@Option(label = "MixingCircuit2", value = "MixingCircuit2"),
		@Option(label = "MixingCircuit3", value = "MixingCircuit3"),
		@Option(label = "Cooling", value = "Cooling"),
		@Option(label = "Ventilation", value = "Ventilation"),
		@Option(label = "SwimmingPool", value = "SwimmingPool")
	})
	String[] defaultModesOfOperation();

	@AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
	boolean readOnly() default false;

    @AttributeDefinition(name = "Debug", description = "Enable debug mode. Print status parameters to the log.")
    boolean debug() default false;
    
	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus target filter", description = "This is auto-generated by 'Modbus-ID'.")
	String Modbus_target() default "";

	String webconsole_configurationFactory_nameHint() default "Heat Pump Alpha Innotec [{id}]";
}