package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.heater.test.DummyHeater;
import io.openems.edge.thermometer.api.test.DummyThermometer;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MyControllerTest {
    private static final String id = "test";
    private static final String[] correctHeaterIds = {"Heater0", "Heater1", "Heater2"};
    private static final String[] wrongHeaterIds = {"Heater0", "Thermometer0", "Heater1"};
    private static final String[] activationTemperatures = {"400", "400", "Thermometer0/Temperature"};
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


    @Test
    public void everythingsFineAndHeating() throws Exception {
        this.cpm.addComponent(this.dummyHeaterMap.get("Heater2"));
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        this.cpm.getAllComponents().toArray(components);
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

    }

    @Test
    public void configurationAtFirstWrong() throws Exception {
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        this.cpm.getAllComponents().toArray(components);
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
    }

    @Test
    public void hasErrorAfterWrongConfigurationForTooLong() throws Exception {
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        this.cpm.getAllComponents().toArray(components);
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
    }

    @Test
    public void testWrongHeater() throws Exception {
        this.cpm.addComponent(this.dummyHeaterMap.get("Heater2"));
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        this.cpm.getAllComponents().toArray(components);
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
                        .setServicePid("EverythingsFine")
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

    }

    @Test(expected = IllegalArgumentException.class)
    public void wrongTemperaturesAndNotExistingChannel() throws Throwable {
        try {
            this.cpm.addComponent(this.dummyHeaterMap.get("Heater2"));
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            this.cpm.getAllComponents().toArray(components);
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
                            .setServicePid("EverythingsFine")
                            .build())
                    .getSut().run();
        } catch (InvocationTargetException e) {
            if (e.getTargetException() instanceof IllegalArgumentException) {
                throw e.getTargetException();
            } else {
                throw e;
            }
        }
    }

    @Test
    public void wrongThermometer() throws Exception{
        this.cpm.addComponent(this.dummyHeaterMap.get("Heater2"));
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        this.cpm.getAllComponents().toArray(components);
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
                        .setServicePid("EverythingsFine")
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

    }

    @Test
    public void ThermometerWithoutValue() throws Exception {
        this.cpm.addComponent(this.dummyHeaterMap.get("Heater2"));
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        this.cpm.getAllComponents().toArray(components);
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

    }


    private OpenemsComponent getAndAddPreviouslyToCpm(String heaterId) {
        OpenemsComponent component = this.dummyHeaterMap.get(heaterId);
        this.cpm.addComponent(component);
        return component;
    }

}