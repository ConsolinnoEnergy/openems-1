package io.openems.edge.controller.heatnetwork.cooling.surveillancer;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.consolinno.leaflet.sensor.signal.api.DummySignalSensor;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.io.test.DummyInputOutput;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class MyControllerTest {
    private static final String id = "test";
    private static final String signalSensorRequest = "dummy";
    private static final String signalSensorWatchdog = "watchdog";
    private static final String output = "output";
    private static final String requestChannel = "dummy/SignalActive";
    private static final String watchdogChannel = "watchdog/SignalActive";
    private static final String outputChannel = "output/InputOutput0";
    private static final ChannelAddress inputRequestChannel = new ChannelAddress(signalSensorRequest, "SignalActive");
    private static final ChannelAddress inputWatchdogChannel = new ChannelAddress(signalSensorWatchdog, "SignalActive");
    private static final ChannelAddress outputChannelAddress = new ChannelAddress(output, "InputOutput0");

    @Test
    public void initialTest() throws Exception {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);

        new ControllerTest(new CoolingSurveillancerImpl())
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(new DummySignalSensor(signalSensorRequest,true))
                .addComponent(new DummySignalSensor(signalSensorWatchdog,true))
                .addComponent(new DummyInputOutput(output))
                .activate(MyConfig.create()
                        .setId(id)
                        .setEnabled(true)
                        .setInputRequest(new String[]{requestChannel})
                        .setInputWatchdogs(new String[]{watchdogChannel})
                        .setOutput(new String[]{outputChannel})
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(inputRequestChannel,true)
                )
                .next(new TestCase()
                        .timeleap(clock,1,ChronoUnit.SECONDS)
                        .output(outputChannelAddress,true)
                )
                .getSut().run();
    }
    @Test
    public void blockTest() throws Exception {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);

        new ControllerTest(new CoolingSurveillancerImpl())
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(new DummySignalSensor(signalSensorRequest,true))
                .addComponent(new DummySignalSensor(signalSensorWatchdog,true))
                .addComponent(new DummyInputOutput(output))
                .activate(MyConfig.create()
                        .setId(id)
                        .setEnabled(true)
                        .setInputRequest(new String[]{requestChannel})
                        .setInputWatchdogs(new String[]{watchdogChannel})
                        .setOutput(new String[]{outputChannel})
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(inputRequestChannel,true)
                        .input(inputWatchdogChannel,true)
                )
                .next(new TestCase()
                        .timeleap(clock,1,ChronoUnit.SECONDS)
                        .output(outputChannelAddress,null)
                )
                .getSut().run();
    }
}