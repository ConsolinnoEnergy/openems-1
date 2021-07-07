package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.heater.test.DummyHeater;
import io.openems.edge.thermometer.api.test.DummyThermometer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests most of the Code in the {@link MultipleHeaterCombinedControllerImpl}.
 * The TestCases includes:
 * <ul>
 * <li> Everything is correct, and the Controller is working as intended.</li>
 * <li>A Component is not configured, but is active in another cycle -> work as intended</li>
 * <li>A Component is not configured for too long|configuration is wrong/Temperature is not defined so the hasErrorChannel is true</li>
 * <li>The Configuration is wrong -> Channel is not existing -> throws an error </li>
 * </ul>
 * -
 */
public class MyControllerTest {
    private static final String id = "test";
    private static final String[] correctHeaterIds = {"Heater0", "Heater1", "Heater2"};
    private static final String[] wrongHeaterIds = {"Heater0", "Thermometer0", "Heater1"};
    private static final String[] activationTemperatures = {"400.154", "400", "Thermometer0/Temperature"};
    private static final String[] wrongActivationTemperatures = {"Heater0/Temperature", "400", "400"};
    private static final String[] deactivationTemperatures = {"650", "650", "650"};
    private static final String[] activationThermometer = {"Thermometer1", "Thermometer2", "Thermometer3"};
    private static final String[] wrongActivationThermometer = {"Heater1"};
    private static final String[] deactivationThermometer = {"Thermometer4", "Thermometer5", "Thermometer6"};
    private DummyComponentManager cpm;
    private final Map<String, DummyThermometer> dummyActivationThermometerMap = new HashMap<>();
    private final Map<String, DummyThermometer> dummyDeactivationThermometerMap = new HashMap<>();
    private final Map<String, DummyHeater> dummyHeaterMap = new HashMap<>();
    private final Map<String, ChannelAddress> channelAddresses = new HashMap<>();
    private final TimeLeapClock clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);

    @Before
    public void setup() {

        this.cpm = new DummyComponentManager(clock);
        Arrays.stream(correctHeaterIds).forEach(entry -> {
            this.dummyHeaterMap.put(entry, new DummyHeater(entry));
        });
        Arrays.stream(activationThermometer).forEach(entry -> {
            this.dummyActivationThermometerMap.put(entry, new DummyThermometer(entry));
        });
        Arrays.stream(deactivationThermometer).forEach(entry -> {
            this.dummyDeactivationThermometerMap.put(entry, new DummyThermometer(entry));
        });
        this.cpm.addComponent(new DummyThermometer("Thermometer0"));
        this.channelAddresses.put("Thermometer0", new ChannelAddress("Thermometer0", "Temperature"));
        this.dummyActivationThermometerMap.forEach((key, value) -> {
            this.cpm.addComponent(value);
            this.channelAddresses.put(key, new ChannelAddress(key, "Temperature"));
        });
        this.dummyDeactivationThermometerMap.forEach((key, value) -> {
            this.cpm.addComponent(value);
            this.channelAddresses.put(key, new ChannelAddress(key, "Temperature"));
        });
        this.dummyHeaterMap.forEach((key, value) -> {
            if (!key.equals("Heater2")) {
                this.cpm.addComponent(value);
            }
            this.channelAddresses.put(key, new ChannelAddress(key, "EnableSignal"));

        });
    }

    /**
     * The Configuration is correct and the Controller is running/Heating as expected.
     */
    @Test
    public void everythingsFineAndHeating() {
        try {
            this.cpm.addComponent(this.dummyHeaterMap.get("Heater2"));
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new MultipleHeaterCombinedControllerImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setHeaterIds(correctHeaterIds)
                            .setActivationTemperatures(activationTemperatures)
                            .setEnabled(true)
                            .setDeactivationTemperatures(deactivationTemperatures)
                            .setActivationThermometer(activationThermometer)
                            .setDeactivationThermometer(deactivationThermometer)
                            .setServicePid("EverythingsFine")
                            .build())
                    //Everythings heating
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get("Thermometer0"), 500)
                            .input(this.channelAddresses.get("Thermometer1"), 400)
                            .input(this.channelAddresses.get("Thermometer2"), 400)
                            .input(this.channelAddresses.get("Thermometer3"), 400)
                            .input(this.channelAddresses.get("Thermometer4"), 300)
                            .input(this.channelAddresses.get("Thermometer5"), 300)
                            .input(this.channelAddresses.get("Thermometer6"), 300)
                    )
                    .next(new TestCase()
                            .output(this.channelAddresses.get("Heater0"), true)
                            .output(this.channelAddresses.get("Heater1"), true)
                            .output(this.channelAddresses.get("Heater2"), true))
                    //Should still Heat heater2
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get("Thermometer0"), 200)
                            .input(this.channelAddresses.get("Heater2"), null)
                            .output(this.channelAddresses.get("Heater2"), true))
                    //Heater 2 should deactivate
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get("Thermometer6"), 1000)
                            .input(this.channelAddresses.get("Heater2"), null)
                            .output(this.channelAddresses.get("Heater2"), null))
                    .getSut().run();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void configurationAtFirstWrong() {
        try {
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new MultipleHeaterCombinedControllerImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setHeaterIds(correctHeaterIds)
                            .setActivationTemperatures(activationTemperatures)
                            .setEnabled(true)
                            .setDeactivationTemperatures(deactivationTemperatures)
                            .setActivationThermometer(activationThermometer)
                            .setDeactivationThermometer(deactivationThermometer)
                            .setServicePid("FineAfterFirstRun")
                            .build())
                    //Everythings heating
                    .addComponent(this.getAndAddPreviouslyToCpm("Heater2"))
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get("Thermometer0"), 500)
                            .input(this.channelAddresses.get("Thermometer1"), 400)
                            .input(this.channelAddresses.get("Thermometer2"), 400)
                            .input(this.channelAddresses.get("Thermometer3"), 400)
                            .input(this.channelAddresses.get("Thermometer4"), 300)
                            .input(this.channelAddresses.get("Thermometer5"), 300)
                            .input(this.channelAddresses.get("Thermometer6"), 300)
                    )
                    .next(new TestCase()
                            .output(this.channelAddresses.get("Heater0"), true)
                            .output(this.channelAddresses.get("Heater1"), true)
                            .output(this.channelAddresses.get("Heater2"), true))
                    .getSut().run();
        } catch (Exception e) {
            Assert.fail();
        }

    }

    /**
     * Check if the Controller responds correctly to wrong Configuration.
     */
    @Test
    public void hasErrorAfterWrongConfigurationForTooLong() {
        try {
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new MultipleHeaterCombinedControllerImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setHeaterIds(correctHeaterIds)
                            .setActivationTemperatures(activationTemperatures)
                            .setEnabled(true)
                            .setDeactivationTemperatures(deactivationTemperatures)
                            .setActivationThermometer(activationThermometer)
                            .setDeactivationThermometer(deactivationThermometer)
                            .setServicePid("ConfigurationWrongForTooLong")
                            .build())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase()
                            .output(ChannelAddress.fromString(id + "/Error"), true)
                    ).getSut().run();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * Check if the Controller reacts properly to the wrong Config.
     */
    @Test
    public void testWrongHeater() {
        try {
            this.cpm.addComponent(this.dummyHeaterMap.get("Heater2"));
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components =  this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new MultipleHeaterCombinedControllerImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setHeaterIds(wrongHeaterIds)
                            .setActivationTemperatures(activationTemperatures)
                            .setEnabled(true)
                            .setDeactivationTemperatures(deactivationTemperatures)
                            .setActivationThermometer(activationThermometer)
                            .setDeactivationThermometer(deactivationThermometer)
                            .setServicePid("wrongHeaterForTooLong")
                            .build())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase()
                            .output(ChannelAddress.fromString(id + "/Error"), true)
                    ).getSut().run();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * The Configuration contains a wrong Channel / ChannelAddress -> throws IllegalArgumentException
     */
    @Test(expected = IllegalArgumentException.class)
    public void wrongTemperaturesAndNotExistingChannel() {
        try {
            this.cpm.addComponent(this.dummyHeaterMap.get("Heater2"));
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new MultipleHeaterCombinedControllerImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setHeaterIds(correctHeaterIds)
                            .setActivationTemperatures(wrongActivationTemperatures)
                            .setEnabled(true)
                            .setDeactivationTemperatures(deactivationTemperatures)
                            .setActivationThermometer(activationThermometer)
                            .setDeactivationThermometer(deactivationThermometer)
                            .setServicePid("IllegalChannel")
                            .build())
                    .getSut().run();
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof IllegalArgumentException) {
                throw (IllegalArgumentException) e.getTargetException();
            } else {
                Assert.fail();
            }
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * Check if the Controller reacts correctly, if an activation Thermometer is wrong.
     */
    @Test
    public void wrongThermometer() {
        try {
            this.cpm.addComponent(this.dummyHeaterMap.get("Heater2"));
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new MultipleHeaterCombinedControllerImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setHeaterIds(wrongHeaterIds)
                            .setActivationTemperatures(activationTemperatures)
                            .setEnabled(true)
                            .setDeactivationTemperatures(deactivationTemperatures)
                            .setActivationThermometer(wrongActivationThermometer)
                            .setDeactivationThermometer(deactivationThermometer)
                            .setServicePid("wrongThermometer")
                            .build())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase()
                            .output(ChannelAddress.fromString(id + "/Error"), true)
                    ).getSut().run();
        } catch (Exception e) {
            Assert.fail();
        }

    }

    /**
     * Checks if the Controller acts correctly if a Thermometer has no value.
     */
    @Test
    public void ThermometerWithoutValue() {
        try {
            this.cpm.addComponent(this.dummyHeaterMap.get("Heater2"));
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new MultipleHeaterCombinedControllerImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setHeaterIds(correctHeaterIds)
                            .setActivationTemperatures(activationTemperatures)
                            .setEnabled(true)
                            .setDeactivationTemperatures(deactivationTemperatures)
                            .setActivationThermometer(activationThermometer)
                            .setDeactivationThermometer(deactivationThermometer)
                            .setServicePid("ThermometerNoValue")
                            .build())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase())
                    .next(new TestCase()
                            .output(ChannelAddress.fromString(id + "/Error"), true)
                    ).getSut().run();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * This is a Helper method, to simulate that a Component is activated/configured later.
     *
     * @param componentId the componentId that will be added to the cpm
     * @return the OpenEmsComponent seems to be activated later.
     */
    private OpenemsComponent getAndAddPreviouslyToCpm(String componentId) {
        OpenemsComponent component = this.dummyHeaterMap.get(componentId);
        this.cpm.addComponent(component);
        return component;
    }

}