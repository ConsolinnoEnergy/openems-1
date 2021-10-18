package io.openems.edge.consolinno.evcs.limiter;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.evcs.test.DummyEvcsPower;
import io.openems.edge.evcs.test.DummyManagedEvcs;
import org.junit.Test;

import org.osgi.service.cm.ConfigurationException;
import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class MyControllerTest {
    private static final String id = "test";
    private static final String evcsId = "evcs0";
    private static final ChannelAddress CHARGE_POWER = new ChannelAddress(evcsId, "ChargePower");
    private static final ChannelAddress PHASES = new ChannelAddress(evcsId, "Phases");
    //private static final ChannelAddress output = new ChannelAddress(outputComponentId, outputChannelId);
/*
    @Test
    public void initialTest() throws Exception {
        EvcsLimiterImpl test = new EvcsLimiterImpl();
        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */ /*, ZoneOffset.UTC);
/*
        new ComponentTest(test)
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(new DummyManagedEvcs(evcsId, new DummyEvcsPower()))
                .activate(MyConfig.create()
                        .setId(id)
                        .setEnabled(true)
                        .setEvcss(new String[]{evcsId})
                        .setUseMeter(false)
                        .setMeter("")
                        .setSymmetry(true)
                        .setOffTime(20)
                        .setPhaseLimit(16 * 230)
                        .setPowerLimit(32 * 230)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                )
                .modified(MyConfig.create()
                        .setId(id)
                        .setEnabled(true)
                        .setEvcss(new String[]{evcsId})
                        .setUseMeter(false)
                        .setMeter("")
                        .setSymmetry(true)
                        .setOffTime(20)
                        .setPhaseLimit(16 * 230)
                        .setPowerLimit(32 * 230)
                        .build())
                .next(new TestCase()
                )
                .next(new TestCase())
        ;

    }

    @Test
    public void balanceTest() throws Exception {
        EvcsLimiterImpl test = new EvcsLimiterImpl();
        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */ /*, ZoneOffset.UTC);

        new ComponentTest(test)
                .addReference("cpm", new DummyComponentManager(clock))
                .addComponent(new DummyManagedEvcs(evcsId, new DummyEvcsPower()))
                .activate(MyConfig.create()
                        .setId(id)
                        .setEnabled(true)
                        .setEvcss(new String[]{evcsId})
                        .setUseMeter(false)
                        .setMeter("")
                        .setSymmetry(true)
                        .setOffTime(20)
                        .setPhaseLimit(10 * 230)
                        .setPowerLimit(7 * 230)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)
                        .input(PHASES, 1)
                        .input(CHARGE_POWER, 30 * 230)
                )

                .next(new TestCase()
                        .input(PHASES, 2)
                        .input(CHARGE_POWER, 60 * 230)
                )
                .next(new TestCase()
                        .input(PHASES, 3)
                        .input(CHARGE_POWER, 90 * 230))
        ;

    }

    @Test(expected = OpenemsError.OpenemsNamedException.class)
    public void namedExceptionTest() throws Throwable {
        EvcsLimiterImpl test = new EvcsLimiterImpl();
        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800) /* starts at 1. January 2020 00:00:00 */ /*, ZoneOffset.UTC);

/*
        try {
            new ComponentTest(test)
                    .addReference("cpm", new DummyComponentManager(clock))
                    .addComponent(new DummyManagedEvcs(evcsId, new DummyEvcsPower()))
                    .activate(MyConfig.create()
                            .setId(id)
                            .setEnabled(true)
                            .setEvcss(new String[]{"wrong"})
                            .setUseMeter(false)
                            .setMeter("")
                            .setSymmetry(true)
                            .setOffTime(20)
                            .setPhaseLimit(16 * 230)
                            .setPowerLimit(32 * 230)
                            .build())
                    .next(new TestCase()
                            .timeleap(clock, 1, ChronoUnit.SECONDS)
                            .input(PHASES, 1)
                            .input(CHARGE_POWER, 30 * 230)
                    )

            ;

        } catch (Exception e) {
            if (((InvocationTargetException) e).getTargetException() instanceof OpenemsError.OpenemsNamedException) {
                throw ((InvocationTargetException) e).getTargetException();
            } else {
                throw e;
            }
        }
    }
    /*
    @Test(expected = ConfigurationException.class)
    public void configurationExceptionTest() throws Throwable {
        EvcsLimiterImpl test = new EvcsLimiterImpl();
        final TimeLeapClock clock = new TimeLeapClock(
                Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);


        try {
            new ComponentTest(test)
                    .addReference("cpm", new DummyComponentManager(clock))
                    .addComponent(new DummyManagedAsymmetricMeter("wrong"))
                    .activate(MyConfig.create()
                            .setId(id)
                            .setEnabled(true)
                            .setEvcss(new String[]{"wrong"})
                            .setUseMeter(false)
                            .setMeter("")
                            .setSymmetry(true)
                            .setOffTime(20)
                            .setPhaseLimit(16 * 230)
                            .setPowerLimit(32 * 230)
                            .build())
                    .next(new TestCase()
                            .timeleap(clock, 1, ChronoUnit.SECONDS)
                            .input(PHASES, 1)
                            .input(CHARGE_POWER, 30 * 230)
                    )
            ;

        } catch (Exception e) {
            if (((InvocationTargetException) e).getTargetException() instanceof ConfigurationException) {
                throw ((InvocationTargetException) e).getTargetException();
            } else {
                throw e;
            }
        }
    }
    */

}