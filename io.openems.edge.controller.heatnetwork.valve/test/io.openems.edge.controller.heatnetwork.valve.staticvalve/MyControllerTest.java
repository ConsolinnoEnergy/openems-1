package io.openems.edge.controller.heatnetwork.valve.staticvalve;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.heatsystem.components.test.DummyValve;
import io.openems.edge.relay.api.test.DummyRelay;
import io.openems.edge.thermometer.api.test.DummyThermometer;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationException;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class  MyControllerTest {
    private static final String id = "test";

    private static final String valveToControl = "testValve";
    private static final String[] temperaturePositionMap = {"700:20"};
    private static final String controlTypePosition = "Position";
    private static final String controlTypeTemperature = "Temperature";
    private static final String thermometerId = "TemperatureThreshold0";
    private static final boolean autorun = true;
    private static final boolean allowForcing = true;
    private static final boolean shouldCloseWhenNoSignal = true;
    private static final int defaultPosition = 100;
    private static final boolean useFallback = true;
    private static final ChannelAddress temperature = new ChannelAddress(thermometerId, "Temperature");
    private static final ChannelAddress requestPosition = new ChannelAddress(id, "RequestPosition");
    private static final ChannelAddress setPointPosition = new ChannelAddress(id, "SetPointPosition");
    private static final ChannelAddress setPowerLevel = new ChannelAddress(valveToControl, "SetPowerLevel");
    private static final ChannelAddress forceOpen = new ChannelAddress(id, "IsForcedOpen");
    private static final ChannelAddress forceClose = new ChannelAddress(id, "IsForcedClose");
    private static final ChannelAddress allowForce = new ChannelAddress(id, "ForceControlAllowed");

    @Test
    public void positionTest() throws Exception {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);

        new ControllerTest(new ValveControllerStaticPositionImpl())
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(new DummyThermometer(thermometerId))
                .addComponent(new DummyValve(new DummyRelay("relay1"), new DummyRelay("relay2"), valveToControl, 240)
                )

                .activate(MyConfig.create()
                        .setId(id)
                        .settemperaturePositionMap(temperaturePositionMap)
                        .setControlType(controlTypePosition)
                        .setThermometerId(thermometerId)
                        .setAutorun(autorun)
                        .setAllowForcing(!allowForcing)
                        .setShouldCloseWhenNoSignal(shouldCloseWhenNoSignal)
                        .setDefaultPosition(defaultPosition)
                        .setUseFallback(useFallback)
                        .setValveToControl(valveToControl)
                        .setServicePid("Position")
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(requestPosition, 50)
                        .output(requestPosition, 50)
                        .output(setPointPosition, 50)
                )
                .getSut().run();

    }

    @Test
    public void temperatureTest() throws Exception {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);


        new ControllerTest(new ValveControllerStaticPositionImpl())
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(new DummyThermometer(thermometerId))
                .addComponent(new DummyValve(new DummyRelay("relay1"), new DummyRelay("relay2"), valveToControl, 240))

                .activate(MyConfig.create()
                        .setId(id)
                        .settemperaturePositionMap(temperaturePositionMap)
                        .setControlType(controlTypeTemperature)
                        .setThermometerId(thermometerId)
                        .setAutorun(autorun)
                        .setAllowForcing(!allowForcing)
                        .setShouldCloseWhenNoSignal(shouldCloseWhenNoSignal)
                        .setDefaultPosition(defaultPosition)
                        .setUseFallback(useFallback)
                        .setValveToControl(valveToControl)
                        .setServicePid("Temperature")
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 10, ChronoUnit.SECONDS)
                        .input(temperature, 800)
                     //   .output(setPowerLevel,20.0)


                )
                .next(new TestCase()
                        .timeleap(clock, 10, ChronoUnit.SECONDS)
                        .input(temperature, -100)

                )

                .getSut().run();



    }

    @Test
    public void forceTest() throws Exception {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);


        new ControllerTest(new ValveControllerStaticPositionImpl())
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(new DummyThermometer(thermometerId))
                .addComponent(new DummyValve(new DummyRelay("relay1"), new DummyRelay("relay2"), valveToControl, 240))

                .activate(MyConfig.create()
                        .setId(id)
                        .settemperaturePositionMap(temperaturePositionMap)
                        .setControlType(controlTypePosition)
                        .setThermometerId(thermometerId)
                        .setAutorun(autorun)
                        .setAllowForcing(allowForcing)
                        .setShouldCloseWhenNoSignal(shouldCloseWhenNoSignal)
                        .setDefaultPosition(defaultPosition)
                        .setUseFallback(useFallback)
                        .setValveToControl(valveToControl)
                        .setServicePid("Temperature")
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 0, ChronoUnit.SECONDS)
                        .input(allowForce, true)
                        .input(forceOpen, true)

                )
                .next(new TestCase()
                        .input(allowForce, true)
                        .input(forceClose, true)

                )
                .getSut().run();

    }

    @Test
    public void disableTest() throws Exception {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);


        new ControllerTest(new ValveControllerStaticPositionImpl())
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(new DummyThermometer(thermometerId))
                .addComponent(new DummyValve(new DummyRelay("relay1"), new DummyRelay("relay2"), valveToControl, 240))

                .activate(MyConfig.create()
                        .setId(id)
                        .setEnabled(false)
                        .settemperaturePositionMap(temperaturePositionMap)
                        .setControlType(controlTypePosition)
                        .setThermometerId(thermometerId)
                        .setAutorun(!autorun)
                        .setAllowForcing(allowForcing)
                        .setShouldCloseWhenNoSignal(shouldCloseWhenNoSignal)
                        .setDefaultPosition(defaultPosition)
                        .setUseFallback(useFallback)
                        .setValveToControl(valveToControl)
                        .setServicePid("Temperature")
                        .build())

                .getSut().run();

    }

    @Test(expected = OpenemsError.OpenemsNamedException.class)
    public void exceptionTestNoComponentId() throws Throwable {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);

        try {
            new ControllerTest(new ValveControllerStaticPositionImpl())
                    .addReference("cpm", new DummyComponentManager(clock))
                    .addComponent(new DummyThermometer(thermometerId))
                    .addComponent(new DummyValve(new DummyRelay("relay1"), new DummyRelay("relay2"), valveToControl, 240))
                    .activate(MyConfig.create()
                            .setId(id)
                            .setEnabled(false)
                            .settemperaturePositionMap(temperaturePositionMap)
                            .setControlType(controlTypePosition)
                            .setThermometerId("")
                            .setAutorun(!autorun)
                            .setAllowForcing(allowForcing)
                            .setShouldCloseWhenNoSignal(shouldCloseWhenNoSignal)
                            .setDefaultPosition(defaultPosition)
                            .setUseFallback(useFallback)
                            .setValveToControl("")
                            .setServicePid("Temperature")
                            .build())

                    .getSut().run();
        } catch (Exception e) {
            if (((InvocationTargetException) e).getTargetException() instanceof OpenemsError.OpenemsNamedException) {
                throw ((InvocationTargetException) e).getTargetException();
            } else {
                throw e;
            }
        }
    }


    @Test(expected = ConfigurationException.class)
    public void exceptionTestWrongValveId() throws Throwable {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);

        try {
            new ControllerTest(new ValveControllerStaticPositionImpl())
                    .addReference("cpm", new DummyComponentManager(clock))
                    .addComponent(new DummyThermometer(thermometerId))
                    .addComponent(new DummyValve(new DummyRelay("relay1"), new DummyRelay("relay2"), valveToControl, 240))
                    .activate(MyConfig.create()
                            .setId(id)
                            .setEnabled(false)
                            .settemperaturePositionMap(temperaturePositionMap)
                            .setControlType(controlTypePosition)
                            .setThermometerId(thermometerId)
                            .setAutorun(!autorun)
                            .setAllowForcing(allowForcing)
                            .setShouldCloseWhenNoSignal(shouldCloseWhenNoSignal)
                            .setDefaultPosition(defaultPosition)
                            .setUseFallback(useFallback)
                            .setValveToControl(thermometerId)
                            .setServicePid("Temperature")
                            .build())

                    .getSut().run();
        } catch (Exception e) {
            if (((InvocationTargetException) e).getTargetException() instanceof ConfigurationException) {
                throw ((InvocationTargetException) e).getTargetException();
            } else {
                throw e;
            }
        }
    }

    @Test(expected = ConfigurationException.class)
    public void exceptionTestWrongThermometerId() throws Throwable {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);

        try {
            new ControllerTest(new ValveControllerStaticPositionImpl())
                    .addReference("cpm", new DummyComponentManager(clock))
                    .addComponent(new DummyThermometer(thermometerId))
                    .addComponent(new DummyValve(new DummyRelay("relay1"), new DummyRelay("relay2"), valveToControl, 240))
                    .activate(MyConfig.create()
                            .setId(id)
                            .setEnabled(false)
                            .settemperaturePositionMap(temperaturePositionMap)
                            .setControlType(controlTypePosition)
                            .setThermometerId(valveToControl)
                            .setAutorun(!autorun)
                            .setAllowForcing(allowForcing)
                            .setShouldCloseWhenNoSignal(shouldCloseWhenNoSignal)
                            .setDefaultPosition(defaultPosition)
                            .setUseFallback(useFallback)
                            .setValveToControl(valveToControl)
                            .setServicePid("Temperature")
                            .build())

                    .getSut().run();
        } catch (Exception e) {
            if (((InvocationTargetException) e).getTargetException() instanceof ConfigurationException) {
                throw ((InvocationTargetException) e).getTargetException();
            } else {
                throw e;
            }
        }
    }

    @Test(expected = ConfigurationException.class)
    public void exceptionTestWrongTemperatureMap() throws Throwable {

        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */, ZoneOffset.UTC);

        try {
            new ControllerTest(new ValveControllerStaticPositionImpl())
                    .addReference("cpm", new DummyComponentManager(clock))
                    .addComponent(new DummyThermometer(thermometerId))
                    .addComponent(new DummyValve(new DummyRelay("relay1"), new DummyRelay("relay2"), valveToControl, 240))
                    .activate(MyConfig.create()
                            .setId(id)
                            .setEnabled(false)
                            .settemperaturePositionMap(new String[]{":"})
                            .setControlType(controlTypePosition)
                            .setThermometerId(thermometerId)
                            .setAutorun(autorun)
                            .setAllowForcing(allowForcing)
                            .setShouldCloseWhenNoSignal(shouldCloseWhenNoSignal)
                            .setDefaultPosition(defaultPosition)
                            .setUseFallback(useFallback)
                            .setValveToControl(valveToControl)
                            .setServicePid("Temperature")
                            .build())

                    .getSut().run();
        } catch (Exception e) {
            if (((InvocationTargetException) e).getTargetException() instanceof ConfigurationException) {
                throw ((InvocationTargetException) e).getTargetException();
            } else {
                throw e;
            }
        }
    }

}



