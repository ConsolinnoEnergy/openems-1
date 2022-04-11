package io.openems.edge.meter.gasmeter.generic;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Meter Gas MBus Generic",
        description = "A GasMeter Communicating via MBus."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "GasMeter-Device ID", description = "Unique Id of the GasBoiler.")
    String id() default "GasMeter0";

    @AttributeDefinition(name = "alias", description = "Human readable name of the gasmeter.")
    String alias() default "";

    @AttributeDefinition(name = "MBus-Bridge Id", description = "The Unique Id of the mBus-Bridge you what to allocate to this device.")
    String mbusBridgeId() default "mbus0";

    @AttributeDefinition(name = "Meter reading", description = "The address for meter reading of this meter.")
    int meterReading() default -404;

    @AttributeDefinition(name = "Meter reading energy", description = "The address for meter reading energy of this meter.")
    int totalConsumedEnergyAddress() default -404;

    @AttributeDefinition(name = "Flow temp", description = "The address for flow temp of this meter.")
    int flowTempAddress() default -404;

    @AttributeDefinition(name = "Return temp", description = "The address for return temp of this meter.")
    int returnTempAddress() default -404;

    @AttributeDefinition(name = "Flow rate", description = "The address for flow rate of this meter.")
    int flowRateAddress() default -404;


    @AttributeDefinition(name = "PrimaryAddress", description = "primary Address of the Mbus Component.")
    int primaryAddress();

    @AttributeDefinition(name = "Don't poll every second", description = "Turn this on if the meter should not be polled every second.")
    boolean usePollingInterval() default false;

    @AttributeDefinition(name = "Polling interval in seconds", description = "If the \"Don't poll every second\" option is turned on, this is the wait time between polling. Unit is seconds.")
    int pollingIntervalSeconds() default 600;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Gas-meter Device Id [{id}]";

}