package io.openems.edge.meter.mbus.electricity;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(//
        name = "MeterM-BusElectricity", //
        description = "Implements M-Bus water meters.")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "ME-0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Mbus-ID", description = "ID of M-Bus brige.")
    String mbus_id() default "mbus0";

    @AttributeDefinition(name = "Mbus PrimaryAddress", description = "PrimaryAddress of the M-Bus device.")
    int primaryAddress();

    @AttributeDefinition(name = "Model", description = "Identification via Type",
            options = {
                    @Option(label = "Eltako DSZ15DM", value = "ELTAKO_DSZ15DM"),
                    @Option(label = "ABB B23 113-100", value = "ABB_B23_113"),
                    @Option(label = "None of the above", value = "none"),
            })
    String model() default "none";

    @AttributeDefinition(name="Total Consumed Energy", description = "Record position of metered energy in M-Bus Telegram; only relevant if \"none\" meter is selected")
    int totalConsumedEnergyAddress();

    @AttributeDefinition(name="Address Power", description = "Record position of metered power in M-Bus Telegram; only relevant if \"none\" meter is selected")
    int powerAddress();

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Electricity meter M-Bus [{id}]";

}
