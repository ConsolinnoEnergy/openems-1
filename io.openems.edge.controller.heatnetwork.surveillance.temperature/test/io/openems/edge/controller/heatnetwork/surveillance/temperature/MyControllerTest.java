package io.openems.edge.controller.heatnetwork.surveillance.temperature;


import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.heatnetwork.valve.api.DummyValveController;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.heater.test.DummyHeater;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.test.DummyThermometer;
import io.openems.edge.timer.api.DummyTimer;
import io.openems.edge.timer.api.TimerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MyControllerTest {
    private static final String id = "test";
    private static final boolean enabled = true;
    private static final String referenceThermometerId = "Thermometer0";
    private static final String thermometerActivateId = "Thermometer1";
    private static final int offsetActivate = 100;
    private static final String thermometerDeactivateId = "Thermometer2";
    private static final int offsetDeactivate = -100;
    private static final String valveControllerId = "Valve0";
    private static final String wrongValveControllerId = "Heater0";
    private static final boolean useValveController = true;
    private static final boolean notUseValveController = false;
    private static final boolean useHeater = true;
    private static final boolean notUseHeater = false;
    private static final String heaterId = "Heater0";
    private static final String wrongHeaterId = "Valve0";
    private static final String timerTime = "TimerByTime";
    private static final String timerCycles = "TimerByCycles";
    private static final String[] componentStringsToActivate = {referenceThermometerId, thermometerActivateId,
            thermometerDeactivateId, valveControllerId, heaterId, timerTime, timerCycles};
    private static final int timeToWaitValveOpen = 3;
    final Map<String, Thermometer> thermometerMap = new HashMap<>();
    private final Map<String, ChannelAddress> channelAddresses = new HashMap<>();
    private DummyComponentManager cpm;
    private final TimeLeapClock clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);

    @Before
    public void setup() {

        this.cpm = new DummyComponentManager(clock);
        Arrays.stream(componentStringsToActivate).forEach(entry -> {
            String channelId = "EnableSignal";
            if (entry.contains("Thermometer")) {
                Thermometer th = new DummyThermometer(entry);
                this.cpm.addComponent(th);
                this.thermometerMap.put(entry, th);
                channelId = "Temperature";
            } else if (entry.contains("Heat")) {
                this.cpm.addComponent(new DummyHeater(entry));
            } else if (entry.contains("Valve")) {
                this.cpm.addComponent(new DummyValveController(entry));
            } else if (entry.contains("Timer")) {
                TimerType type = TimerType.TIME;
                if (entry.contains("Cycles")) {
                    type = TimerType.CYCLES;
                }
                this.cpm.addComponent(new DummyTimer(entry, type));
            }
            this.channelAddresses.put(entry, new ChannelAddress(entry, channelId));
        });
    }

    /**
     * The Configuration is correct and the Controller is running/Heating as expected.
     */
    @Test
    public void heaterRunsAndValveControllerEnabledAfterTimeIsUp() {
        try {
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new TemperatureSurveillanceControllerImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setHeaterId(heaterId)
                            .setEnabled(enabled)
                            .setOffsetActivate(offsetActivate)
                            .setOffsetDeactivate(offsetDeactivate)
                            .setReferenceThermometerId(referenceThermometerId)
                            .setThermometerActivateId(thermometerActivateId)
                            .setThermometerDeactivateId(thermometerDeactivateId)
                            .setValveControllerId(valveControllerId)
                            .setSurveillanceType(TemperatureSurveillanceType.HEATER_AND_VALVE_CONTROLLER)
                            .setTimeToWaitValveOpen(timeToWaitValveOpen)
                            .setService_pid("EverythingsFineHeaterAndValve")
                            .setTimerId(timerCycles)
                            .build())
                    //Everythings heating
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get("Thermometer0"), 400)
                            .input(this.channelAddresses.get("Thermometer1"), 650)
                            .input(this.channelAddresses.get("Thermometer2"), 650)
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), true)
                    )
                    //Should still Heat heater2
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get("Heater0"), true)
                    )

                    //Heater 2 should deactivate
                    .next(new AbstractComponentTest.TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get("Heater0"), true)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get("Valve0"), true)
                    )
                    .getSut().run();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * The Configuration is correct and the Controller is running/Heating as expected.
     */
    @Test
    public void heaterRuns() {
        try {
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new TemperatureSurveillanceControllerImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setHeaterId(heaterId)
                            .setEnabled(enabled)
                            .setOffsetActivate(offsetActivate)
                            .setOffsetDeactivate(offsetDeactivate)
                            .setReferenceThermometerId(referenceThermometerId)
                            .setThermometerActivateId(thermometerActivateId)
                            .setThermometerDeactivateId(thermometerDeactivateId)
                            .setValveControllerId(valveControllerId)
                            .setSurveillanceType(TemperatureSurveillanceType.HEATER_ONLY)
                            .setTimeToWaitValveOpen(timeToWaitValveOpen)
                            .setService_pid("EverythingsFineHeater")
                            .setTimerId(timerCycles)
                            .build())
                    //Everythings heating
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get("Thermometer0"), 400)
                            .input(this.channelAddresses.get("Thermometer1"), 650)
                            .input(this.channelAddresses.get("Thermometer2"), 650)
                            .output(this.channelAddresses.get(heaterId), true)
                    )
                    .getSut().run();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    

}