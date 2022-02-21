package io.openems.edge.controller.heatnetwork.pump.grundfos;

import io.openems.edge.controller.heatnetwork.pump.grundfos.api.ControlModeSetting;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
        name = "Controller Pump Grundfos", //
        description = "Controller to operate a Gundfos pump over GENIbus.  IMPORTANT: This module requires "
                + "\"Bridge GeniBus\" and \"Pump Grundfos\" to be active. It won't start if that is not the case!")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "ControllerPumpGrundfos0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "PumpId", description = "Unique Id of the Pump.")
    String pumpId() default "Pump0";

    @AttributeDefinition(name = "Control mode", description = "Set the pump control mode.")
    ControlModeSetting controlMode() default ControlModeSetting.CONST_PRESSURE;

    @AttributeDefinition(name = "Pressure set point [bar]",
            description = "Pressure setting for constant pressure mode. Only applied when pump is in constant pressure mode.")
    double pressureSetpoint() default 0.2;

    @AttributeDefinition(name = "Motor speed set point [%]",
            description = "Motor speed setting for constant frequency mode. Unit is percent, so 100 means 100%. Can't go "
                    + "below minimum speed (52%). Only used when in constant frequency mode.")
    double motorSpeedSetpoint() default 60;

    @AttributeDefinition(name = "Stop the pump", description = "Stops the pump")
    boolean stopPump() default false;

    @AttributeDefinition(name = "Write pump status to log", description = "Write pump status parameters in the log.")
    boolean printPumpStatus() default false;

    @AttributeDefinition(name = "Read only", description = "Only reads values from the pump, do not send commands.")
    boolean onlyRead() default false;

    String webconsole_configurationFactory_nameHint() default "Controller Pump Grundfos [{id}]";
}