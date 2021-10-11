package io.openems.edge.controller.heatnetwork.apartmentmodule;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.apartmentmodule.api.test.DummyApartmentModule;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.heatnetwork.apartmentmodule.api.ApartmentModuleControllerState;
import io.openems.edge.controller.heatnetwork.apartmentmodule.api.ControllerHeatingApartmentModule;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.test.DummyHydraulicLineController;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.heatsystem.components.PumpType;
import io.openems.edge.heatsystem.components.test.DummyPump;
import io.openems.edge.hydraulic.test.DummyHydraulicBooster;
import io.openems.edge.thermometer.api.test.DummyThermometer;
import io.openems.edge.timer.api.DummyTimer;
import io.openems.edge.timer.api.Timer;
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
    private static final String exampleConfigStringValue = "exampleConfigStringValue";
    // -------------- SETUP COMPONENT IDs ------------------ //
    private static final String[] apartmentModules = {"AM_1", "AM_2", "AM_3", "AM_4", "AM_5"};
    private static final String[] thermometerThresholds = {"Thermometer1",
            "Thermometer2", "Thermometer3", "Thermometer4", "Thermometer5", "ThermometerLineHeater1", "ThermometerLineHeater2"};
    private static final String[] hydraulicLineController = {"HydraulicLine1", "HydraulicLine2"};
    private static final String correctPumpId = "Pump0";
    private static final String correctHeatBoosterId = "Booster0";
    private static final String timerId = "TimerByCycles";
    private final Map<String, DummyApartmentModule> dummyApartmentModuleMap = new HashMap<>();
    private final Map<String, DummyThermometer> dummyThermometerMap = new HashMap<>();
    private final Map<String, DummyHydraulicLineController> dummyHydraulicLineControllerMap = new HashMap<>();
    private final DummyPump dummyPump = new DummyPump(correctPumpId, PumpType.RELAY);
    private final Map<String, ChannelAddress> channelAddresses = new HashMap<>();
    // ------------------------------------------------- //
    private DummyComponentManager cpm;
    private final TimeLeapClock clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);
    // ------------------- CONFIG PARAMS ------------------- //
    private static final String[] correctThermometerApartmentModuleConfig = {"AM_1:Thermometer1", "AM_2:Thermometer2",
            "AM_3:Thermometer3", "AM_4:Thermometer4", "AM_5:Thermometer5"};
    String[] correctThresholdConfig = {"ThermometerLineHeater1", "ThermometerLineHeater2"};
    private static final String[] correctApartmentCords = {"AM_1:AM_2", "AM_3:AM_4:AM_5"};
    private static final String[] wrongApartmentCords = {"AM_1:AM_2:AM_3", "AM_4:AM_5"};
    private static final String[] correctApartmentResponse = {"HydraulicLine1/EnableSignal", "HydraulicLine2/EnableSignal"};
    private static final String[] wrongApartmentResponse = {"HydraulicLine1/EnableSignal"};
    String[] wrongThresholdConfig = {"ThermometerLineHeater2"};
    private static final String wrongPump = "Thermometer0";
    private static final double powerLevelPump = 100.d;


    @Before
    public void setup() {

        this.cpm = new DummyComponentManager(clock);
        Arrays.stream(apartmentModules).forEach(entry -> {
            boolean isTopAm = entry.contains("2") || entry.contains("3");
            this.dummyApartmentModuleMap.put(entry, new DummyApartmentModule(entry, isTopAm));
        });
        Arrays.stream(thermometerThresholds).forEach(entry -> {
            this.dummyThermometerMap.put(entry, new DummyThermometer(entry));
        });
        Arrays.stream(hydraulicLineController).forEach(entry -> {
            this.dummyHydraulicLineControllerMap.put(entry, new DummyHydraulicLineController(entry));
        });

        this.cpm.addComponent(new DummyTimer(timerId, TimerType.CYCLES));
        this.cpm.addComponent(dummyPump);
        this.cpm.addComponent(new DummyHydraulicBooster(correctHeatBoosterId));
        this.dummyThermometerMap.forEach((key, value) -> {
            this.cpm.addComponent(value);
            this.channelAddresses.put(key, new ChannelAddress(key, "Temperature"));
        });
        this.dummyApartmentModuleMap.forEach((key, value) -> {
            this.cpm.addComponent(value);
            this.channelAddresses.put(key, new ChannelAddress(key, "LastKnownRequestStatus"));
        });
        this.dummyHydraulicLineControllerMap.forEach((key, value) -> {
            this.channelAddresses.put(key, new ChannelAddress(key, "EnableSignal"));
            this.cpm.addComponent(value);
        });
        this.channelAddresses.put(correctHeatBoosterId, new ChannelAddress(correctHeatBoosterId, "HeatBoosterEnableSignal"));

    }

    @Test
    public void everythingOk() {
        try {
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            ChannelAddress am2RefTemp = ChannelAddress.fromString("AM_2/ReferenceTemperature");
            ChannelAddress am3RefTemp = ChannelAddress.fromString("AM_3/ReferenceTemperature");
            ControllerHeatingApartmentModuleImpl controller = new ControllerHeatingApartmentModuleImpl();
            new ControllerTest(controller, components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setApartmentCords(correctApartmentCords)
                            .setApartmentToThermometer(correctThermometerApartmentModuleConfig)
                            .setApartmentResponse(correctApartmentResponse)
                            .setThresholdId(correctThresholdConfig)
                            .setHeatPumpId(correctPumpId)
                            .setPowerLevelPump(powerLevelPump)
                            .setUseHeatBooster(true)
                            .setHeatBoostTemperature(500)
                            .setHeatBoosterId(correctHeatBoosterId)
                            .setSetPointTemperature(600)
                            .setTimerId(timerId)
                            .setEnabled(true)
                            .setServicePid("ThisIsFine")
                            .build())
                    .next(new AbstractComponentTest.TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get("Thermometer1"), 500)
                            .input(this.channelAddresses.get("Thermometer2"), 500)
                            .input(this.channelAddresses.get("Thermometer3"), 500)
                            .input(this.channelAddresses.get("Thermometer4"), 500)
                            .input(this.channelAddresses.get("Thermometer5"), 500)
                            .input(this.channelAddresses.get("ThermometerLineHeater1"), 550)
                            .input(this.channelAddresses.get("ThermometerLineHeater2"), 550)
                            .input(this.channelAddresses.get("AM_1"), true)
                            .input(this.channelAddresses.get("AM_4"), true)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .output(this.channelAddresses.get("HydraulicLine1"), true)
                            .output(this.channelAddresses.get("HydraulicLine2"), true)
                            .output(this.channelAddresses.get(correctHeatBoosterId), null)
                            .output(controller.getControllerStateChannel().address(), ApartmentModuleControllerState.EXTRA_HEAT)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .input(this.channelAddresses.get("AM_1"), true)
                            .input(this.channelAddresses.get("AM_4"), false)
                            .input(this.channelAddresses.get("ThermometerLineHeater1"), 650)
                            .input(this.channelAddresses.get("ThermometerLineHeater2"), 650)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .output(this.channelAddresses.get("HydraulicLine1"), null)
                            .output(this.channelAddresses.get("HydraulicLine2"), null)
                            .output(this.channelAddresses.get(correctHeatBoosterId), null)
                            .output(controller.getControllerStateChannel().address(), ApartmentModuleControllerState.HEAT_PUMP_ACTIVE)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .input(this.channelAddresses.get("AM_1"), false)
                            .input(this.channelAddresses.get("AM_4"), false)
                            .input(this.channelAddresses.get("ThermometerLineHeater1"), 300)
                            .input(this.channelAddresses.get("ThermometerLineHeater2"), 300)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .output(this.channelAddresses.get("HydraulicLine1"), null)
                            .output(this.channelAddresses.get("HydraulicLine2"), null)
                            .output(this.channelAddresses.get(correctHeatBoosterId), null)
                            .output(controller.getControllerStateChannel().address(), ApartmentModuleControllerState.IDLE)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .input(this.channelAddresses.get("AM_1"), true)
                            .input(this.channelAddresses.get("AM_4"), true)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .output(this.channelAddresses.get("HydraulicLine1"), true)
                            .output(this.channelAddresses.get("HydraulicLine2"), true)
                            .output(this.channelAddresses.get(correctHeatBoosterId), true)
                            .output(controller.getControllerStateChannel().address(), ApartmentModuleControllerState.EXTRA_HEAT)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .input(this.channelAddresses.get("Thermometer1"), 200)
                            .input(this.channelAddresses.get("Thermometer2"), 500)
                            .input(this.channelAddresses.get("Thermometer3"), 180)
                            .input(this.channelAddresses.get("Thermometer4"), 600)
                            .input(this.channelAddresses.get("Thermometer5"), 450)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .output(am2RefTemp, 200)
                            .output(am3RefTemp, 600)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .input(this.channelAddresses.get("AM_1"), true)
                            .input(this.channelAddresses.get("AM_2"), true)
                            .input(this.channelAddresses.get("AM_3"), true)
                            .input(this.channelAddresses.get("AM_4"), true)
                            .input(this.channelAddresses.get("AM_5"), true)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .output(am2RefTemp, 200)
                            .output(am3RefTemp, 180)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .input(this.channelAddresses.get("AM_1"), false)
                            .input(this.channelAddresses.get("AM_2"), true)
                            .input(this.channelAddresses.get("AM_3"), false)
                            .input(this.channelAddresses.get("AM_4"), false)
                            .input(this.channelAddresses.get("AM_5"), false)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .output(am2RefTemp, 500)
                    )
                    //Check missing Components
                    .getSut().run();
            System.out.println(controller.debugLog());
            controller.deactivate();
            Timer timer = this.cpm.getComponent(timerId);
            for (int x = 0; x <= ControllerHeatingApartmentModule.CHECK_MISSING_COMPONENTS_TIME; x++) {
                timer.checkIsTimeUp(controller.id(), ControllerHeatingApartmentModule.CHECK_MISSING_COMPONENT_IDENTIFIER);
            }
            controller.run();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void configIsWrong() {
        try {
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            ControllerHeatingApartmentModuleImpl controller = new ControllerHeatingApartmentModuleImpl();
            new ControllerTest(controller, components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setApartmentCords(wrongApartmentCords)
                            .setApartmentToThermometer(correctThermometerApartmentModuleConfig)
                            .setApartmentResponse(wrongApartmentResponse)
                            .setThresholdId(wrongThresholdConfig)
                            .setHeatPumpId(wrongPump)
                            .setPowerLevelPump(powerLevelPump)
                            .setUseHeatBooster(true)
                            .setHeatBoostTemperature(500)
                            .setHeatBoosterId(correctHeatBoosterId)
                            .setSetPointTemperature(600)
                            .setTimerId(timerId)
                            .setEnabled(true)
                            .setServicePid("ThisIsNotFine")
                            .build())
                    .next(new AbstractComponentTest.TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get("Thermometer1"), 500)
                            .input(this.channelAddresses.get("Thermometer2"), 500)
                            .input(this.channelAddresses.get("Thermometer3"), 500)
                            .input(this.channelAddresses.get("Thermometer4"), 500)
                            .input(this.channelAddresses.get("Thermometer5"), 500)
                            .input(this.channelAddresses.get("ThermometerLineHeater1"), 550)
                            .input(this.channelAddresses.get("ThermometerLineHeater2"), 550)
                            .input(this.channelAddresses.get("AM_1"), true)
                            .input(this.channelAddresses.get("AM_4"), true)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .output(this.channelAddresses.get("HydraulicLine1"), null)
                            .output(this.channelAddresses.get("HydraulicLine2"), null)
                            .output(this.channelAddresses.get(correctHeatBoosterId), null)
                            .output(controller.getControllerStateChannel().address(), ApartmentModuleControllerState.UNDEFINED)
                    )
                    .getSut().run();
            controller.debugLog();
            controller.deactivate();
        } catch (Exception e) {
            Assert.fail();
        }
    }


    @Test
    public void emergency() {
        try {
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            ControllerHeatingApartmentModuleImpl controller = new ControllerHeatingApartmentModuleImpl();
            new ControllerTest(controller, components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setApartmentCords(correctApartmentCords)
                            .setApartmentToThermometer(correctThermometerApartmentModuleConfig)
                            .setApartmentResponse(correctApartmentResponse)
                            .setThresholdId(correctThresholdConfig)
                            .setHeatPumpId(correctPumpId)
                            .setPowerLevelPump(powerLevelPump)
                            .setUseHeatBooster(true)
                            .setHeatBoostTemperature(500)
                            .setHeatBoosterId(correctHeatBoosterId)
                            .setSetPointTemperature(600)
                            .setTimerId(timerId)
                            .setEnabled(true)
                            .setServicePid("ThisIsFine")
                            .build())
                    .next(new AbstractComponentTest.TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                    .input(this.channelAddresses.get("AM_1"), true)
                    .input(this.channelAddresses.get("AM_4"), false)
                    .input(this.channelAddresses.get("ThermometerLineHeater1"), 650)
                    .input(this.channelAddresses.get("ThermometerLineHeater2"), 650)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(controller.getEmergencyPumpStartChannel().address(), true)
                            .output(this.channelAddresses.get("HydraulicLine1"), null)
                            .output(this.channelAddresses.get("HydraulicLine2"), null)
                            .output(this.channelAddresses.get(correctHeatBoosterId), null)
                            .output(controller.getControllerStateChannel().address(), ApartmentModuleControllerState.EMERGENCY_ON)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(this.channelAddresses.get("AM_1"), true)
                            .input(this.channelAddresses.get("AM_4"), true)
                            .input(this.channelAddresses.get("ThermometerLineHeater1"), 400)
                            .input(this.channelAddresses.get("ThermometerLineHeater2"), 400)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(controller.getEmergencyPumpStartChannel().address(), true)
                            .output(this.channelAddresses.get("HydraulicLine1"), true)
                            .output(this.channelAddresses.get("HydraulicLine2"), true)
                            .output(this.channelAddresses.get(correctHeatBoosterId), true)
                            .output(controller.getControllerStateChannel().address(), ApartmentModuleControllerState.EMERGENCY_ON)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(controller.getEmergencyStopChannel().address(), true)
                            .output(this.channelAddresses.get("HydraulicLine1"), null)
                            .output(this.channelAddresses.get("HydraulicLine2"), null)
                            .output(this.channelAddresses.get(correctHeatBoosterId), null)
                            .output(controller.getControllerStateChannel().address(), ApartmentModuleControllerState.EMERGENCY_STOP)
                    )
                    .next(new AbstractComponentTest.TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(controller.getEmergencyEnableEveryResponseChannel().address(), true)
                            .input(controller.getEmergencyPumpStartChannel().address(), false)
                            .input(controller.getEmergencyStopChannel().address(), false)
                            .output(this.channelAddresses.get("HydraulicLine1"), true)
                            .output(this.channelAddresses.get("HydraulicLine2"), true)
                            .output(this.channelAddresses.get(correctHeatBoosterId), true)
                            .output(controller.getControllerStateChannel().address(), ApartmentModuleControllerState.EMERGENCY_ON)
                    )
                    .getSut().run();
            controller.debugLog();
            controller.deactivate();
            Timer timer = this.cpm.getComponent(timerId);
            for (int x = 0; x <= ControllerHeatingApartmentModule.CHECK_MISSING_COMPONENTS_TIME; x++) {
                timer.checkIsTimeUp(controller.id(), ControllerHeatingApartmentModule.CHECK_MISSING_COMPONENT_IDENTIFIER);
            }
            controller.run();

        } catch (Exception e) {
            Assert.fail();
        }
    }
}