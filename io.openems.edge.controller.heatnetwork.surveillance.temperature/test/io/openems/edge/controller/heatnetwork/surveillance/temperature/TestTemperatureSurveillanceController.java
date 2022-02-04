package io.openems.edge.controller.heatnetwork.surveillance.temperature;


import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.hydrauliccomponent.api.DummyHydraulicController;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.heater.api.test.DummyHeater;
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

public class TestTemperatureSurveillanceController {
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
            }  else if (entry.contains("Valve")) {
                this.cpm.addComponent(new DummyHydraulicController(entry));
            } else if (entry.contains("Timer")) {
                TimerType type = TimerType.TIME;
                if (entry.contains("Cycles")) {
                    type = TimerType.COUNTING;
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
    public void heaterRunsAndValveControllerEnabledAfterTimeIsUp() throws Exception {

            this.cpm.addComponent(new DummyHeater(heaterId));
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new TemperatureSurveillanceControllerHeatingImpl(), components)
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
                            .setSurveillanceType(SurveillanceType.HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER)
                            .setTimeToWaitValveOpen(timeToWaitValveOpen)
                            .setService_pid("EverythingIsFineHeaterAndValve")
                            .setTimerId(timerCycles)
                            .build())
                    //Everythings heating
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get(referenceThermometerId), 800)
                            .input(this.channelAddresses.get(thermometerActivateId), 300)
                            .input(this.channelAddresses.get(thermometerDeactivateId), 650)
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), true)
                    )
                    //Should still Heat heater2
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), true)
                    )

                    //Heater 2 should deactivate
                    .next(new AbstractComponentTest.TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get(thermometerDeactivateId), 800)
                            .output(this.channelAddresses.get(heaterId), null)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), null)
                    )
                    .getSut().run();
    }

    /**
     * The Configuration is correct and the Controller is running/Heating as expected. (Heater Only)
     */

    @Test
    public void heaterRuns() throws Exception {

            this.cpm.addComponent(new DummyHeater(heaterId));
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new TemperatureSurveillanceControllerHeatingImpl(), components)
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
                            .setSurveillanceType(SurveillanceType.HEATER_OR_COOLER_ONLY)
                            .setTimeToWaitValveOpen(timeToWaitValveOpen)
                            .setService_pid("EverythingsFineHeater")
                            .setTimerId(timerCycles)
                            .build())
                    //Everythings heating
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get(referenceThermometerId), 800)
                            .input(this.channelAddresses.get(thermometerActivateId), 400)
                            .input(this.channelAddresses.get(thermometerDeactivateId), 650)
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), true)
                    )
                    .getSut().run();
    }

    /**
     * The Configuration is correct and the Controller is running/Heating as expected. (ValveOnly)
     */

    @Test
    public void valveRuns() throws Exception {
            this.cpm.addComponent(new DummyHeater(heaterId));
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new TemperatureSurveillanceControllerHeatingImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setHeaterId(wrongHeaterId)
                            .setEnabled(enabled)
                            .setOffsetActivate(offsetActivate)
                            .setOffsetDeactivate(offsetDeactivate)
                            .setReferenceThermometerId(referenceThermometerId)
                            .setThermometerActivateId(thermometerActivateId)
                            .setThermometerDeactivateId(thermometerDeactivateId)
                            .setValveControllerId(valveControllerId)
                            .setSurveillanceType(SurveillanceType.HYDRAULIC_CONTROLLER_ONLY)
                            .setTimeToWaitValveOpen(timeToWaitValveOpen)
                            .setService_pid("EverythingsFineController")
                            .setTimerId(timerCycles)
                            .build())
                    //Everythings heating
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get(referenceThermometerId), 400)
                            .input(this.channelAddresses.get(thermometerActivateId), 650)
                            .input(this.channelAddresses.get(thermometerDeactivateId), 650)

                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(valveControllerId), false)
                    )
                    .getSut().run();
    }

    /**
     * The Configuration is correct and the Controller is running/Heating as expected.
     * (First Activates and then disables Components)
     */

    @Test
    public void heaterRunsAndThenDeactivatesAndStaysInactive() throws Exception {
            this.cpm.addComponent(new DummyHeater(heaterId));
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new TemperatureSurveillanceControllerHeatingImpl(), components)
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
                            .setSurveillanceType(SurveillanceType.HEATER_OR_COOLER_ONLY)
                            .setTimeToWaitValveOpen(timeToWaitValveOpen)
                            .setService_pid("EverythingsFineHeater")
                            .setTimerId(timerCycles)
                            .build())
                    //Everythings heating
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get(referenceThermometerId), 400)
                            .input(this.channelAddresses.get(thermometerActivateId), 650)
                            .input(this.channelAddresses.get(thermometerDeactivateId), 650)
                            .output(this.channelAddresses.get(heaterId), null)
                    )
                    .next(new TestCase()
                            .input(this.channelAddresses.get(referenceThermometerId), 800)
                            //simulate getNextWriteValueAndReset
                            .input(this.channelAddresses.get(heaterId), null)
                            .output(this.channelAddresses.get(heaterId), true))
                    .getSut().run();
    }

    /**
     * The Configuration is incorrect at first. But after a needed Component is enabled -> run as expected.
     */

    @Test
    public void heaterRunsAndValveControllerEnabledAfterConfigIsOk() throws Exception {
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size() - 1];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new TemperatureSurveillanceControllerHeatingImpl(), components)
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
                            .setSurveillanceType(SurveillanceType.HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER)
                            .setTimeToWaitValveOpen(timeToWaitValveOpen)
                            .setService_pid("EverythingsFineHeaterAndValve")
                            .setTimerId(timerCycles)
                            .build())
                    .addComponent(this.getAndAddPreviouslyToCpm(heaterId))
                    //Everythings heating
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get(referenceThermometerId), 500)
                            .input(this.channelAddresses.get(thermometerActivateId), 300)
                            .input(this.channelAddresses.get(thermometerDeactivateId), 300)
                            .output(this.channelAddresses.get(heaterId), null)
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), true)
                    )
                    //Should still Heat heater2
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), true)
                    )

                    //Heater 2 should deactivate
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get(thermometerDeactivateId), 500)
                            .output(this.channelAddresses.get(heaterId), null)
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), null)
                    )
                    .next(new TestCase()
                            .output(this.channelAddresses.get(heaterId), null))
                    .getSut().run();

    }

    /**
     * The Configuration is wrong and the Controller won't work.
     */

    @Test
    public void wrongConfigured() {
        try {
            this.cpm.addComponent(new DummyHeater(heaterId));
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size() - 1];
            components = this.cpm.getAllComponents().toArray(components);
            OpenemsComponent cpmThatIsMissing = new DummyHeater(heaterId);
            new ControllerTest(new TemperatureSurveillanceControllerHeatingImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setHeaterId(wrongHeaterId)
                            .setEnabled(enabled)
                            .setOffsetActivate(offsetActivate)
                            .setOffsetDeactivate(offsetDeactivate)
                            .setReferenceThermometerId(referenceThermometerId)
                            .setThermometerActivateId(thermometerActivateId)
                            .setThermometerDeactivateId(thermometerDeactivateId)
                            .setValveControllerId(wrongValveControllerId)
                            .setSurveillanceType(SurveillanceType.HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER)
                            .setTimeToWaitValveOpen(timeToWaitValveOpen)
                            .setService_pid("EverythingsFineHeaterAndValve")
                            .setTimerId(timerCycles)
                            .build())
                    .addComponent(this.getAndAddPreviouslyToCpm(heaterId))
                    //Everythings heating
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get(referenceThermometerId), 400)
                            .input(this.channelAddresses.get(thermometerActivateId), 650)
                            .input(this.channelAddresses.get(thermometerDeactivateId), 650)
                            .output(this.channelAddresses.get(heaterId), null)
                            .output(this.channelAddresses.get(valveControllerId), null)
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), null)
                            .output(this.channelAddresses.get(valveControllerId), null)
                    )
                    //Should still Heat heater2
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(valveControllerId), null)
                            .output(this.channelAddresses.get(valveControllerId), null)
                    )

                    //Heater 2 should deactivate
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), null)
                            .output(this.channelAddresses.get(heaterId), null)
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), null)
                            .output(this.channelAddresses.get(heaterId), null)
                    )
                    .next(new TestCase()
                            .output(this.channelAddresses.get(heaterId), null))

                    .getSut().run();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * A Component of the Configuration is not enabled at first.
     * After the component is enabled, run as expected -> Activate and enable Heater and Valve and deactivate them after
     * reaching deactivationConditions.
     */

    @Test
    public void heaterRunsAndValveControllerEnabledAfterConfigIsOkAndDisablesLater() throws Exception {
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size() - 1];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new TemperatureSurveillanceControllerHeatingImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setHeaterId(heaterId)
                            .setEnabled(enabled)
                            .setOffsetActivate(0)
                            .setOffsetDeactivate(offsetDeactivate)
                            .setReferenceThermometerId(referenceThermometerId)
                            .setThermometerActivateId(thermometerActivateId)
                            .setThermometerDeactivateId(thermometerDeactivateId)
                            .setValveControllerId(valveControllerId)
                            .setSurveillanceType(SurveillanceType.HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER)
                            .setTimeToWaitValveOpen(timeToWaitValveOpen)
                            .setService_pid("ConfigFirstWrongThenOkAndHeatThenDeactivate")
                            .setTimerId(timerCycles)
                            .build())
                    .addComponent(this.getAndAddPreviouslyToCpm(heaterId))
                    //Everythings heating
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get(referenceThermometerId), 401)
                            .input(this.channelAddresses.get(thermometerActivateId), 300)
                            .input(this.channelAddresses.get(thermometerDeactivateId), 300)
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), true)
                    )
                    //Should still Heat heater2
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), true)
                    )

                    //Heater 2 should deactivate
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), true)
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .output(this.channelAddresses.get(heaterId), true)
                    )
                    .next(new TestCase()
                            .output(this.channelAddresses.get(heaterId), true)
                    )
                    .next(new TestCase()
                            .input(this.channelAddresses.get(referenceThermometerId), 800)
                            .input(this.channelAddresses.get(heaterId), null)
                            .output(this.channelAddresses.get(valveControllerId), true)
                            .output(this.channelAddresses.get(heaterId), true)
                    )

                    .getSut().run();

    }

    /**
     * This is a Helper method, to simulate that a Component is activated/configured later.
     *
     * @param componentId the componentId that will be added to the cpm
     * @return the OpenEmsComponent seems to be activated later.
     */
    private OpenemsComponent getAndAddPreviouslyToCpm(String componentId) {
        OpenemsComponent component = new DummyHeater(componentId);
        this.cpm.addComponent(component);
        return component;
    }

}