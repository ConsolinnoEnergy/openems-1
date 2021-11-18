package io.openems.edge.controller.hydrauliccomponent.controller;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.hydrauliccomponent.api.ControlType;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.heatsystem.components.test.DummyValve;

import io.openems.edge.io.test.DummyRelay;
import io.openems.edge.thermometer.api.test.DummyThermometer;
import io.openems.edge.timer.api.DummyTimer;
import io.openems.edge.timer.api.TimerType;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class MyControllerTest {
    private static final String id = "test";
    private static final String tempId = "thermometer";
    private static final String relayOpenId = "openRelay";
    private static final String relayCloseId = "closeRelay";
    private static final String valveId = "valve";
    private static final String timerId = "TimerByCycles";
    private static final ChannelAddress input = new ChannelAddress(tempId, "Temperature");
    private static final ChannelAddress output = new ChannelAddress(valveId, "SetPointPowerLevel");

    @Test
    public void initialHeatingTest() throws Exception {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);
        DummyRelay close = new DummyRelay(relayCloseId);
        DummyRelay open = new DummyRelay(relayOpenId);
        new ControllerTest(new HydraulicPositionControllerImpl())
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(new DummyThermometer(tempId))
                .addComponent(close)
                .addComponent(open)
                .addComponent(new DummyValve(open, close, valveId, 10))
                .addComponent(new DummyTimer(timerId, TimerType.CYCLES))
                .activate(MyConfig.create()
                        .setId(id)
                        .setService_pid("")
                        .setComponentToControl(valveId)
                        .setTemperaturePositionMap(new String[]{"700:20","800:30"})
                        .setControlType(ControlType.TEMPERATURE)
                        .setThermometerId(tempId)
                        .setAutorun(true)
                        .setAllowForcing(false)
                        .setShouldCloseWhenNoSignal(true)
                        .setDefaultPosition(0)
                        .setTimerForRunning(timerId)
                        .setWaitForSignalAfterActivation(10)
                        .setUseFallback(false)
                        .setTimerForFallback(timerId)
                        .setFallbackRunTime(100)
                        .setEnabled(true)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(input, 750)
                        .output(output,30)
                )
                .getSut().run();
    }
    @Test
    public void initialCoolingTest() throws Exception {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);
        DummyRelay close = new DummyRelay(relayCloseId);
        DummyRelay open = new DummyRelay(relayOpenId);
        new ControllerTest(new HydraulicPositionControllerImpl())
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(new DummyThermometer(tempId))
                .addComponent(close)
                .addComponent(open)
                .addComponent(new DummyValve(open, close, valveId, 10))
                .addComponent(new DummyTimer(timerId, TimerType.CYCLES))
                .activate(MyConfig.create()
                        .setId(id)
                        .setShouldCool(true)
                        .setService_pid("")
                        .setComponentToControl(valveId)
                        .setTemperaturePositionMap(new String[]{"700:20","800:30"})
                        .setControlType(ControlType.TEMPERATURE)
                        .setThermometerId(tempId)
                        .setAutorun(true)
                        .setAllowForcing(false)
                        .setShouldCloseWhenNoSignal(true)
                        .setDefaultPosition(0)
                        .setTimerForRunning(timerId)
                        .setWaitForSignalAfterActivation(10)
                        .setUseFallback(false)
                        .setTimerForFallback(timerId)
                        .setFallbackRunTime(100)
                        .setEnabled(true)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(input, 750)
                        .output(output,20)
                )
                .getSut().run();
    }

    @Test
    public void multipleHeatingTest() throws Exception {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);
        DummyRelay close = new DummyRelay(relayCloseId);
        DummyRelay open = new DummyRelay(relayOpenId);
        new ControllerTest(new HydraulicPositionControllerImpl())
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(new DummyThermometer(tempId))
                .addComponent(close)
                .addComponent(open)
                .addComponent(new DummyValve(open, close, valveId, 10))
                .addComponent(new DummyTimer(timerId, TimerType.CYCLES))
                .activate(MyConfig.create()
                        .setId(id)
                        .setService_pid("")
                        .setComponentToControl(valveId)
                        .setTemperaturePositionMap(new String[]{"700:20","800:30"})
                        .setControlType(ControlType.TEMPERATURE)
                        .setThermometerId(tempId)
                        .setAutorun(true)
                        .setAllowForcing(false)
                        .setShouldCloseWhenNoSignal(true)
                        .setDefaultPosition(100)
                        .setTimerForRunning(timerId)
                        .setWaitForSignalAfterActivation(10)
                        .setUseFallback(false)
                        .setTimerForFallback(timerId)
                        .setFallbackRunTime(100)
                        .setEnabled(true)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(input, 750)
                        .output(output,30)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(input, 900)
                        .output(output,100)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(input, 600)
                        .output(output,20)
                )
                .getSut().run();
    }
    
}