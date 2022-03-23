package io.openems.edge.utility.timeframehandler;

import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.TimeFrameType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Utility TimeFrameHandler ", description = "This component handles a time frame. "
        + "When the time measured is within a time frame. Then write boolean true, otherwise false."
        + "The TimeFrameType describes, what numbers to look at. E.g. look at minutes/months/year/hour etc")
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for the Component.")
    String id() default "TimeFrameHandler";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Type", description = "Time Frame Type: Check this Time Type.")
    TimeFrameType timeFrameType() default TimeFrameType.MONTH;

    @AttributeDefinition(name = "Start time", description = "This is the starting point, where the time frame is true.")
    int start() default 1;

    @AttributeDefinition(name = "Stop time", description = "This is the endpoint, where the time frame is true.")
    int stop() default 12;

    @AttributeDefinition(name = "Use output", description = "Set the result of the time frame to another channel.")
    boolean useOutput() default false;

    @AttributeDefinition(name = "Output channel address", description = "The output channel address where the result of time frame is additionally stored")
    String channelOutput() default "VirtualChannel0/VirtualBoolean";

    @AttributeDefinition(name = "Output Type", description = "Write the Value, set to next value or to value.")
    InputOutputType inputOutputType() default InputOutputType.NEXT_WRITE_VALUE;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Utility TimeFrameHandler {id}";
}
