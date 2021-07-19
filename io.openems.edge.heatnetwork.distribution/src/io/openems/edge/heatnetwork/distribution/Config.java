package io.openems.edge.heatnetwork.distribution;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Generic Modbus HeatDistributionNetwork ",
        description = "A module to map Modbus calls to OpenEMS channels for a Generic Modbus HeatDistributionNetwork Module."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Device ID", description = "Unique Id of HeatDistributionNetwork .")
    String id() default "heatdistributionnetwork0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Modbus Register WMZ Energy Amount", description = "Modbus Register for wmz energy amount.")
    int modbusRegisterWMZEnergyAmount() default -1;

    @AttributeDefinition(name = "Modbus Register WMZ Temp Source", description = "Modbus Register for wmz temp source.")
    int modbusRegisterWMZTempSource() default -1;

    @AttributeDefinition(name = "Modbus Register WMZ Temp Sink", description = "Modbus Register for wmz temp sink.")
    int modbusRegisterWMZTempSink() default -1;

    @AttributeDefinition(name = "Modbus Register WMZ Power", description = "Modbus Register for wmz power.")
    int modbusRegisterWMZPower() default -1;

    @AttributeDefinition(name = "alias", description = "Human readable name of HeatDistributionNetwork.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Generic Modbus Component [{id}]";

}