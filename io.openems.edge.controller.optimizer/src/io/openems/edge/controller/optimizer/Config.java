package io.openems.edge.controller.optimizer;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;



@ObjectClassDefinition(name = "Consolinno Optimizer Broker", description = "Optimizer that writes Channels based on a json.")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Optimizer.")
    String id() default "Optimizer0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Sensor.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "MqttBridgeId", description = "Unique Id for the Mqtt Bridge.")
    String bridgeId() default "MqttBridge";

    @AttributeDefinition(name = "StopOnError", description = "Stop the Optimizer, when connection with the Mqtt Bridge has been lost.")
    boolean stop() default false;

    @AttributeDefinition(name = "LastMemberTime", description = "Time (in seconds) the last member of the Schedule has to run.")
    int lastMemberTime() default 900;

    String webconsole_configurationFactory_nameHint() default "Optimizer [{id}]";
}