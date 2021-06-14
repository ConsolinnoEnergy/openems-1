package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class MyControllerTest {
    private static final String id = "test";
    private static final String exampleConfigStringValue = "exampleConfigStringValue";
    private static final ChannelAddress input = new ChannelAddress(inputComponentId, inputChannelId);
    private static final ChannelAddress output = new ChannelAddress(outputComponentId, outputChannelId);

    @Test
    public void positionTest() throws Exception {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);

        new ControllerTest(new ControllerToTestImpl())
                .addReference("cpm", new DummyComponentManager(clock)) // "cpm" in this case is the name of the componentmanager of the controller. Note: has to be the same name as the reference in the controller
                .addComponent(new DummyComponent(componentId) // only important if the controller actually needs other components
                )

                .activate(MyConfig.create()
                        .setId(id)
                        .setExampleConfigStringValue(exampleConfigStringValue)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(input, inputValue)//this will write the inputValue in the Channeladdress input. Note: not strictly necessary for a test
                        .output(output, expectedOutputValue)//this is the actual channel test, the test will pass if the value you expect here is actually in that channel after the input. Note: .output is not strictly necessary for a test

                )
                .next(new TestCase()
                        .input(input, inputValue)

                )
                .getSut().run(); // execute Run method of controller

    }
}