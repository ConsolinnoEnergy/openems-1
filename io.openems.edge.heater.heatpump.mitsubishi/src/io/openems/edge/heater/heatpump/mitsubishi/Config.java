package io.openems.edge.heater.heatpump.mitsubishi;

import io.openems.edge.heater.heatpump.mitsubishi.api.SystemOnOff;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Heat Pump Mitsubishi", //
		description = "Implements a Mitsubishi heat pump.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "HeatPump0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-Bridge ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 1;
	
	@AttributeDefinition(name = "Turn on heat pump", description = "Test Modbus write by turning the pump on and off.")
    boolean turnOnPump() default false;

	@AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
	boolean readOnly() default false;

	@AttributeDefinition(name = "Print info to log", description = "Print status info to the log.")
    boolean printInfoToLog() default false;

	String webconsole_configurationFactory_nameHint() default "Heat Pump Mitsubishi [{id}]";

}