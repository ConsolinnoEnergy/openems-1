package io.openems.edge.controller.temperature.overseer;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Consolinno Overseer",
        description = "This Controller oversees two Thermometers with a given logic."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Passing Controller.")
    String id() default "ControllerOverseer0";

    @AttributeDefinition(name = "alias", description = "Human Readable Name of Component.")
    String alias() default "";

    @AttributeDefinition(name = "SourceComponent", description = "Unique Id of the Component that specifies if the test should start (see below).")
    String componentId();

    @AttributeDefinition(name = "ChannelIds", description = "This List will automatically filled with ChannelIds")
    String[] channelIdList() default {};

    @AttributeDefinition(name = "EnableChannel", description = "ChannelAddress(Component/Channel) that should activate the test if a specified value is reached.")
    String enableChannel();

    @AttributeDefinition(name = "ExpectedValue", description = "Value that has to be reached to activate the test.")
    String expectedValue();

    @AttributeDefinition(name = "Allocated Thermometer 1", description = "Unique ID of the \"source\" Thermometer.")
    String sourceThermometer() default "TemperatureSensor0";

    @AttributeDefinition(name = "Allocated Thermometer 2", description = "Unique ID of the \"target\" Thermometer.")
    String targetThermometer() default "TemperatureSensor1";

    @AttributeDefinition(name = "TargetComponentId", description = "Unique Id of the Component who has to enter the ExceptionalState in case of an Error.")
    String targetComponentId();

    @AttributeDefinition(name = "ErrorDelta", description = "Temperature difference between the Thermometers that shouldn't be exceeded.")
    int errorDelta();

    @AttributeDefinition(name = "Invert", description = "Invert the logic of the Overseer (the Temperature difference has to be greater then the delta).")
    boolean invert() default true;

    @AttributeDefinition(name = "TestPeriod", description = "Time (in s) for how long the test has to run.")
    int testPeriod();

    @AttributeDefinition(name = "ErrorPeriod", description = "Time (in s) for how long the Target should stay in the Exceptional State.")
    int errorPeriod();

    @AttributeDefinition(name = "TimerId", description = "Unique ID of the TimeHandler.")
    String timerId();

    boolean configurationDone() default false;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Consolinno Overseer [{id}]";
}