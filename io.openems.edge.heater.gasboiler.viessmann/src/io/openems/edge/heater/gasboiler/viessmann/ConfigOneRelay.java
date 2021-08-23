package io.openems.edge.heater.gasboiler.viessmann;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "GasBoiler Viessmann One Relay",
        description = "A Gasboiler provided by Viessmann, communicating via One Relay."
)
@interface ConfigOneRelay {

    String service_pid();

    @AttributeDefinition(name = "GasBoiler-Device ID", description = "Unique Id of the GasBoiler.")
    String id() default "GasBoiler0";

    @AttributeDefinition(name = "alias", description = "Human readable name of GasBoiler.")
    String alias() default "";

    @AttributeDefinition(name = "Relay ID", description = "The Unique Id of the relay you what to allocate to this device.")
    String relayId() default "relay0";

    @AttributeDefinition(name = "Wait time EnableSignal", description = "How long to wait after the EnableSignal is "
            + "no longer received before the heater is switched off. Unit is seconds, unless cycles option is selected.")
    int waitTimeEnableSignal() default 30;

    @AttributeDefinition(name = "EnableSignal timer unit is cycles not seconds", description = "Use OpenEMS cycles "
            + "instead of seconds as the unit for the timer.")
    boolean enableSignalTimerIsCyclesNotSeconds() default false;

    @AttributeDefinition(name = "Use ExceptionalState", description = "React to commands from the Exceptional State "
            + "interface. When the Exceptional State is active, this will override any other commands.")
    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "Wait time ExceptionalState", description = "How long to wait after the Exceptional "
            + "State Active Signal is no longer received before the heater leaves the Exceptional State. Unit is "
            + "seconds, unless cycles option is selected.")
    int waitTimeExceptionalState() default 30;

    @AttributeDefinition(name = "ExceptionalState timer unit is cycles not seconds", description = "Use OpenEMS cycles "
            + "instead of seconds as the unit for the timer.")
    boolean exceptionalStateTimerIsCyclesNotSeconds() default false;

    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "GasBoiler Device One Relay [{id}]";


}
