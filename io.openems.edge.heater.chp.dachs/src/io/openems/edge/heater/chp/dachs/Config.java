package io.openems.edge.heater.chp.dachs;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Chp Dachs GLT-Interface",
        description = "Implements the Senertec Dachs Chp.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "Chp0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

    @AttributeDefinition(name = "IP address", description = "IP address of the GLT web interface")
    String address() default "localhost";

    @AttributeDefinition(name = "Username", description = "Username for the GLT web interface")
    String username() default "glt";

    @AttributeDefinition(name = "Password", description = "Password for the GLT web interface", type = AttributeType.PASSWORD)
    String password() default "";

    @AttributeDefinition(name = "Polling interval [s]", description = "Unit: seconds. Time between calls to the GLT interface to update the values. Maximum 540.")
    int interval() default 10;

    @AttributeDefinition(name = "Wait time EnableSignal", description = "How long to wait after the EnableSignal is "
            + "no longer received before the Chp is switched off. Unit is seconds, unless cycles option is selected.")
    int waitTimeEnableSignal() default 30;

    @AttributeDefinition(name = "EnableSignal timer Id", description = "Name of the timer used for the EnableSignal.")
    String enableSignalTimerId() default "TimerByTime";

    @AttributeDefinition(name = "Use ExceptionalState", description = "React to commands from the Exceptional State "
            + "interface. When the Exceptional State is active, this will override any other commands.")
    boolean useExceptionalState() default false;

    @AttributeDefinition(name = "Wait time ExceptionalState", description = "How long to wait after the Exceptional "
            + "State Active Signal is no longer received before the chp leaves the Exceptional State. Unit is "
            + "seconds, unless cycles option is selected.")
    int waitTimeExceptionalState() default 30;

    @AttributeDefinition(name = "ExceptionalState timer Id", description = "Name of the timer used for the ExceptionalState.")
    String exceptionalStateTimerId() default "TimerByTime";

    @AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
    boolean readOnly() default false;
    
    @AttributeDefinition(name = "Write info to log", description = "Write basic data to log.")
    boolean basicInfo() default false;
    
    @AttributeDefinition(name = "Debug", description = "Write debug messages to log.")
    boolean debug() default false;

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Heater Chp Dachs GLT-Interface [{id}]";
}