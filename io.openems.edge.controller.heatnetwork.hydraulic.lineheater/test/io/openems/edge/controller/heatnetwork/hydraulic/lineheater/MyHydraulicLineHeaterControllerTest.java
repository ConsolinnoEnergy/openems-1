package io.openems.edge.controller.heatnetwork.hydraulic.lineheater;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.apartmentmodule.api.test.DummyApartmentModule;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.test.DummyHydraulicLineController;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.heater.decentralized.test.DummyDecentralizedHeater;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.heatsystem.components.test.DummyValve;
import io.openems.edge.thermometer.api.test.DummyThermometer;
import io.openems.edge.timer.api.DummyTimer;
import io.openems.edge.timer.api.TimerType;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public class MyHydraulicLineHeaterControllerTest {
    private static final String id = "test";
    private static final ChannelAddress controllerEnableSignal = new ChannelAddress(id, "EnableSignal");
    private static final ChannelAddress controllerFallbackSignal = new ChannelAddress(id, "IsFallback");
    // Thermometer
    private static final String correctThermometerId = "Thermometer0";
    private static final DummyThermometer dummyThermometer = new DummyThermometer(correctThermometerId);
    private static final ChannelAddress thermometerAddress = new ChannelAddress(correctThermometerId, "Temperature");
    //ActivationTemp
    private static final String correctReferenceTemperature = "600";
    // LineHeaterType changes depending on test
    //ApartmentModule for OneChannel
    private static final String correctApartmentModuleId = "AM_2";
    private static final DummyApartmentModule correctDummyApartmentModule = new DummyApartmentModule("AM_2", true);
    private static final ChannelAddress apartmentModuleAddress = new ChannelAddress(correctApartmentModuleId, "CordRequest");
    //HydraulicComponent -> Valve
    private static final String correctValveId = "Valve0";
    private static final DummyValve correctDummyValve = new DummyValve(correctValveId);
    private static final ChannelAddress valveSetPointPowerAddress = new ChannelAddress(correctValveId, "SetPointPowerLevel");
    private static final ChannelAddress valveMaxAddress = new ChannelAddress(correctValveId, "MaxAllowedValue");
    private static final ChannelAddress valveMinAddress = new ChannelAddress(correctValveId, "MinAllowedValue");
    //TimeOuts
    private static final String timerId = "TimerByCycles";
    private static final int defaultTimeOutTimeRemote = 10;
    private static final int defaultTimeOutRestartCycle = 5;
    //Should Fallback
    private static final int defaultFallbackStart = HydraulicComponent.DEFAULT_MIN_POWER_VALUE;
    private static final int defaultFallbackStop = 15;
    private static final double defaultMaxValue = 95.0d;
    private static final double defaultMinValue = 10.0d;
    //decentralizedHeater
    private static final String decentralizedHeaterId = "Heater0";
    private static final DummyDecentralizedHeater dummyDecentralizedHeater
            = new DummyDecentralizedHeater(decentralizedHeaterId);
    private static final DecentralizedHeaterReactionType reactionType = DecentralizedHeaterReactionType.NEED_HEAT;
    private static final ChannelAddress decentralizedHeaterAddress = new ChannelAddress(decentralizedHeaterId, "NeedHeat");
    //Basic
    private DummyComponentManager cpm;
    private final TimeLeapClock clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);


    @Before
    public void setup() {
        this.cpm = new DummyComponentManager(clock);
        this.cpm.addComponent(dummyThermometer);
        this.cpm.addComponent(dummyDecentralizedHeater);
        this.cpm.addComponent(correctDummyApartmentModule);
        this.cpm.addComponent(correctDummyValve);
        this.cpm.addComponent(new DummyTimer(timerId, TimerType.CYCLES));
    }


    /*
     *   TESTCASES
     * 1. 1 ChannelLineHeater
     * 2. ValveLineHeater/HydraulicComponent
     * 3. MultiChannelLineHeater (Like Valve just with the Channel)
     * 4. DecentralizedHeater
     * 5. Fallback
     * Note: Each LineHeater -> EnableSignal with
     * a) Thermometer too low -> activate Heating
     * b) Heat for process
     * c) Increment Thermometer
     * d) heating stops
     * e) Thermometer drops
     * f) LineHeater waits till cycleRestart
     *
     *
     * */


    @Test
    public void oneChannelLineHeaterNoFallbackNoDecentralized() throws Exception {
        HydraulicLineHeaterController controller = new HydraulicLineHeaterController();
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        components = this.cpm.getAllComponents().toArray(components);
        new ControllerTest(controller, components)
                .addReference("cpm", new DummyComponentManager(clock)) // "cpm" in this case is the name of the componentmanager of the controller. Note: has to be the same name as the reference in the controller
                .activate(MyHydraulicLineHeaterConfig.create()
                        .setId(id)
                        .setService_pid("ThisIsFine")
                        .setTempSensorReference(correctThermometerId)
                        .setTemperatureDefault(correctReferenceTemperature)
                        .setLineHeaterType(LineHeaterType.ONE_CHANNEL)
                        .setValueToWriteIsBoolean(true)
                        .setChannelAddress(apartmentModuleAddress.toString())
                        .setChannels(new String[]{"foo", "bar"})
                        .setValveBypass(correctValveId)
                        .setTimerId(timerId)
                        .setTimeoutMaxRemote(defaultTimeOutTimeRemote)
                        .setTimeoutRestartCycle(defaultTimeOutRestartCycle)
                        .setShouldFallback(false)
                        .setMinuteFallbackStart(defaultFallbackStart)
                        .setMinuteFallbackStop(defaultFallbackStop)
                        .setUseMinMax(false)
                        .setMaxValveValue(defaultMaxValue)
                        .setMinValveValue(defaultMinValue)
                        .setUseDecentralizedHeater(false)
                        .setDecentralizedHeaterReference(decentralizedHeaterId)
                        .setReactionType(DecentralizedHeaterReactionType.NEED_HEAT)
                        .setEnabled(true)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .input(controllerEnableSignal, true)
                        .output(apartmentModuleAddress, true)
                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 450)
                        .output(apartmentModuleAddress, true)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 500)
                        .output(apartmentModuleAddress, true)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 550)
                        .output(apartmentModuleAddress, true)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 600)
                        .output(apartmentModuleAddress, false)

                )
                //5 Restart Cycles until
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 450)
                        .output(apartmentModuleAddress, false)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(apartmentModuleAddress, false)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(apartmentModuleAddress, false)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(apartmentModuleAddress, false)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(apartmentModuleAddress, false)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(apartmentModuleAddress, true)

                )
                .getSut().run(); // execute Run method of controller

    }

    @Test
    public void valveLineHeater() throws Exception {
        HydraulicLineHeaterController controller = new HydraulicLineHeaterController();
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        components = this.cpm.getAllComponents().toArray(components);
        new ControllerTest(controller, components)
                .addReference("cpm", new DummyComponentManager(clock)) // "cpm" in this case is the name of the componentmanager of the controller. Note: has to be the same name as the reference in the controller
                .activate(MyHydraulicLineHeaterConfig.create()
                        .setId(id)
                        .setService_pid("ThisIsFine")
                        .setTempSensorReference(correctThermometerId)
                        .setTemperatureDefault(correctReferenceTemperature)
                        .setLineHeaterType(LineHeaterType.VALVE)
                        .setValueToWriteIsBoolean(false)
                        .setChannelAddress(apartmentModuleAddress.toString())
                        .setChannels(new String[]{"foo", "bar"})
                        .setValveBypass(correctValveId)
                        .setTimerId(timerId)
                        .setTimeoutMaxRemote(defaultTimeOutTimeRemote)
                        .setTimeoutRestartCycle(defaultTimeOutRestartCycle)
                        .setShouldFallback(false)
                        .setMinuteFallbackStart(defaultFallbackStart)
                        .setMinuteFallbackStop(defaultFallbackStop)
                        .setUseMinMax(false)
                        .setMaxValveValue(defaultMaxValue)
                        .setMinValveValue(defaultMinValue)
                        .setUseDecentralizedHeater(false)
                        .setDecentralizedHeaterReference(decentralizedHeaterId)
                        .setReactionType(DecentralizedHeaterReactionType.NEED_HEAT)
                        .setEnabled(true)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .input(controllerEnableSignal, true)
                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 450)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 500)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 550)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 600)

                )
                //5 Restart Cycles until
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 450)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)

                )
                .getSut().run(); // execute Run method of controller
    }

    @Test
    public void multiChannelLineHeaterNoMinMax() throws Exception {
        HydraulicLineHeaterController controller = new HydraulicLineHeaterController();
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        components = this.cpm.getAllComponents().toArray(components);
        new ControllerTest(controller, components)
                .addReference("cpm", new DummyComponentManager(clock)) // "cpm" in this case is the name of the componentmanager of the controller. Note: has to be the same name as the reference in the controller
                .activate(MyHydraulicLineHeaterConfig.create()
                        .setId(id)
                        .setService_pid("ThisIsFine")
                        .setTempSensorReference(correctThermometerId)
                        .setTemperatureDefault(correctReferenceTemperature)
                        .setLineHeaterType(LineHeaterType.MULTIPLE_CHANNEL)
                        .setValueToWriteIsBoolean(false)
                        .setChannelAddress(apartmentModuleAddress.toString())
                        .setChannels(new String[]{correctValveId + "/CurrentPowerLevel", correctValveId + "/SetPointPowerLevel"})
                        .setValveBypass(correctValveId)
                        .setTimerId(timerId)
                        .setTimeoutMaxRemote(defaultTimeOutTimeRemote)
                        .setTimeoutRestartCycle(defaultTimeOutRestartCycle)
                        .setShouldFallback(false)
                        .setMinuteFallbackStart(defaultFallbackStart)
                        .setMinuteFallbackStop(defaultFallbackStop)
                        .setUseMinMax(false)
                        .setMaxValveValue(defaultMaxValue)
                        .setMinValveValue(defaultMinValue)
                        .setUseDecentralizedHeater(false)
                        .setDecentralizedHeaterReference(decentralizedHeaterId)
                        .setReactionType(DecentralizedHeaterReactionType.NEED_HEAT)
                        .setEnabled(true)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .input(controllerEnableSignal, true)
                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 450)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 500)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 550)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 600)

                )
                //5 Restart Cycles until
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 450)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)

                )
                .getSut().run(); // execute Run method of controller
    }

    @Test
    public void multiChannelLineHeaterWithMinMax() throws Exception {
        HydraulicLineHeaterController controller = new HydraulicLineHeaterController();
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        components = this.cpm.getAllComponents().toArray(components);
        new ControllerTest(controller, components)
                .addReference("cpm", new DummyComponentManager(clock)) // "cpm" in this case is the name of the componentmanager of the controller. Note: has to be the same name as the reference in the controller
                .activate(MyHydraulicLineHeaterConfig.create()
                        .setId(id)
                        .setService_pid("ThisIsFine")
                        .setTempSensorReference(correctThermometerId)
                        .setTemperatureDefault(correctReferenceTemperature)
                        .setLineHeaterType(LineHeaterType.MULTIPLE_CHANNEL)
                        .setValueToWriteIsBoolean(false)
                        .setChannelAddress(apartmentModuleAddress.toString())
                        .setChannels(new String[]{correctValveId + "/CurrentPowerLevel", correctValveId + "/SetPointPowerLevel",
                                correctValveId + "/MaxAllowedValue", correctValveId + "/MinAllowedValue"})
                        .setValveBypass(correctValveId)
                        .setTimerId(timerId)
                        .setTimeoutMaxRemote(defaultTimeOutTimeRemote)
                        .setTimeoutRestartCycle(defaultTimeOutRestartCycle)
                        .setShouldFallback(false)
                        .setMinuteFallbackStart(defaultFallbackStart)
                        .setMinuteFallbackStop(defaultFallbackStop)
                        .setUseMinMax(true)
                        .setMaxValveValue(defaultMaxValue)
                        .setMinValveValue(defaultMinValue)
                        .setUseDecentralizedHeater(false)
                        .setDecentralizedHeaterReference(decentralizedHeaterId)
                        .setReactionType(DecentralizedHeaterReactionType.NEED_HEAT)
                        .setEnabled(true)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .input(controllerEnableSignal, true)
                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 450)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)
                        .output(valveMaxAddress, defaultMaxValue)
                        .output(valveMinAddress, defaultMinValue)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 500)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)
                        .output(valveMaxAddress, defaultMaxValue)
                        .output(valveMinAddress, defaultMinValue)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 550)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)
                        .output(valveMaxAddress, defaultMaxValue)
                        .output(valveMinAddress, defaultMinValue)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 600)

                )
                //5 Restart Cycles until
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 450)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)
                        .output(valveMaxAddress, defaultMaxValue)
                        .output(valveMinAddress, defaultMinValue)

                )
                .getSut().run(); // execute Run method of controller
    }

    @Test
    public void decentralizedOneChannel() throws Exception {
        HydraulicLineHeaterController controller = new HydraulicLineHeaterController();
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        components = this.cpm.getAllComponents().toArray(components);
        new ControllerTest(controller, components)
                .addReference("cpm", new DummyComponentManager(clock))
                .activate(MyHydraulicLineHeaterConfig.create()
                        .setId(id)
                        .setService_pid("ThisIsFine")
                        .setTempSensorReference(correctThermometerId)
                        .setTemperatureDefault(correctReferenceTemperature)
                        .setLineHeaterType(LineHeaterType.ONE_CHANNEL)
                        .setValueToWriteIsBoolean(true)
                        .setChannelAddress(apartmentModuleAddress.toString())
                        .setChannels(new String[]{"foo", "bar"})
                        .setValveBypass(correctValveId)
                        .setTimerId(timerId)
                        .setTimeoutMaxRemote(defaultTimeOutTimeRemote)
                        .setTimeoutRestartCycle(defaultTimeOutRestartCycle)
                        .setShouldFallback(false)
                        .setMinuteFallbackStart(defaultFallbackStart)
                        .setMinuteFallbackStop(defaultFallbackStop)
                        .setUseMinMax(false)
                        .setMaxValveValue(defaultMaxValue)
                        .setMinValveValue(defaultMinValue)
                        .setUseDecentralizedHeater(true)
                        .setDecentralizedHeaterReference(decentralizedHeaterId)
                        .setReactionType(reactionType)
                        .setEnabled(true)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .input(decentralizedHeaterAddress, true)
                        .output(apartmentModuleAddress, true)
                )
                .next(new TestCase()
                        .input(decentralizedHeaterAddress, true)
                        .input(thermometerAddress, 450)
                        .output(apartmentModuleAddress, true)

                )
                .next(new TestCase()
                        .input(decentralizedHeaterAddress, true)
                        .input(thermometerAddress, 500)
                        .output(apartmentModuleAddress, true)

                )
                .next(new TestCase()
                        .input(decentralizedHeaterAddress, true)
                        .input(thermometerAddress, 550)
                        .output(apartmentModuleAddress, true)

                )
                .next(new TestCase()
                        .input(decentralizedHeaterAddress, true)
                        .input(thermometerAddress, 600)
                        .output(apartmentModuleAddress, false)

                )
                //5 Restart Cycles until
                .next(new TestCase()
                        .input(decentralizedHeaterAddress, true)
                        .input(thermometerAddress, 450)
                        .output(apartmentModuleAddress, false)

                )
                .next(new TestCase()
                        .input(decentralizedHeaterAddress, true)
                        .output(apartmentModuleAddress, false)

                )
                .next(new TestCase()
                        .input(decentralizedHeaterAddress, true)
                        .output(apartmentModuleAddress, false)

                )
                .next(new TestCase()
                        .input(decentralizedHeaterAddress, true)
                        .output(apartmentModuleAddress, false)

                )
                .next(new TestCase()
                        .input(decentralizedHeaterAddress, true)
                        .output(apartmentModuleAddress, false)

                )
                .next(new TestCase()
                        .input(decentralizedHeaterAddress, true)
                        .output(apartmentModuleAddress, true)

                )
                .getSut().run(); // execute Run method of controller
    }

    @Test
    public void hydraulicUseMinMax() throws Exception {
        HydraulicLineHeaterController controller = new HydraulicLineHeaterController();
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        components = this.cpm.getAllComponents().toArray(components);
        new ControllerTest(controller, components)
                .addReference("cpm", new DummyComponentManager(clock)) // "cpm" in this case is the name of the componentmanager of the controller. Note: has to be the same name as the reference in the controller
                .activate(MyHydraulicLineHeaterConfig.create()
                        .setId(id)
                        .setService_pid("ThisIsFine")
                        .setTempSensorReference(correctThermometerId)
                        .setTemperatureDefault(correctReferenceTemperature)
                        .setLineHeaterType(LineHeaterType.VALVE)
                        .setValueToWriteIsBoolean(false)
                        .setChannelAddress(apartmentModuleAddress.toString())
                        .setChannels(new String[]{"foo", "bar"})
                        .setValveBypass(correctValveId)
                        .setTimerId(timerId)
                        .setTimeoutMaxRemote(defaultTimeOutTimeRemote)
                        .setTimeoutRestartCycle(defaultTimeOutRestartCycle)
                        .setShouldFallback(false)
                        .setMinuteFallbackStart(defaultFallbackStart)
                        .setMinuteFallbackStop(defaultFallbackStop)
                        .setUseMinMax(true)
                        .setMaxValveValue(defaultMaxValue)
                        .setMinValveValue(defaultMinValue)
                        .setUseDecentralizedHeater(false)
                        .setDecentralizedHeaterReference(decentralizedHeaterId)
                        .setReactionType(DecentralizedHeaterReactionType.NEED_HEAT)
                        .setEnabled(true)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .input(controllerEnableSignal, true)
                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 450)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)
                        .output(valveMaxAddress, defaultMaxValue)
                        .output(valveMinAddress, defaultMinValue)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 500)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)
                        .output(valveMaxAddress, defaultMaxValue)
                        .output(valveMinAddress, defaultMinValue)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 550)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)
                        .output(valveMaxAddress, defaultMaxValue)
                        .output(valveMinAddress, defaultMinValue)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 600)

                )
                //5 Restart Cycles until
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 450)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MIN_POWER_VALUE)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, HydraulicComponent.DEFAULT_MAX_POWER_VALUE)
                        .output(valveMaxAddress, defaultMaxValue)
                        .output(valveMinAddress, defaultMinValue)

                )
                .getSut().run(); // execute Run method of controller
    }

    @Test
    public void hydraulicMinMaxOnly() throws Exception {
        HydraulicLineHeaterController controller = new HydraulicLineHeaterController();
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        components = this.cpm.getAllComponents().toArray(components);
        new ControllerTest(controller, components)
                .addReference("cpm", new DummyComponentManager(clock)) // "cpm" in this case is the name of the componentmanager of the controller. Note: has to be the same name as the reference in the controller
                .activate(MyHydraulicLineHeaterConfig.create()
                        .setId(id)
                        .setService_pid("ThisIsFine")
                        .setTempSensorReference(correctThermometerId)
                        .setTemperatureDefault(correctReferenceTemperature)
                        .setLineHeaterType(LineHeaterType.VALVE)
                        .setValueToWriteIsBoolean(false)
                        .setChannelAddress(apartmentModuleAddress.toString())
                        .setChannels(new String[]{"foo", "bar"})
                        .setValveBypass(correctValveId)
                        .setTimerId(timerId)
                        .setTimeoutMaxRemote(defaultTimeOutTimeRemote)
                        .setTimeoutRestartCycle(defaultTimeOutRestartCycle)
                        .setShouldFallback(false)
                        .setMinuteFallbackStart(defaultFallbackStart)
                        .setMinuteFallbackStop(defaultFallbackStop)
                        .setUseMinMax(true)
                        .setMaxMinOnly(true)
                        .setMaxValveValue(defaultMaxValue)
                        .setMinValveValue(defaultMinValue)
                        .setUseDecentralizedHeater(false)
                        .setDecentralizedHeaterReference(decentralizedHeaterId)
                        .setReactionType(DecentralizedHeaterReactionType.NEED_HEAT)
                        .setEnabled(true)
                        .build())
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .input(controllerEnableSignal, true)
                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 450)
                        .output(valveSetPointPowerAddress, null)
                        .output(valveMaxAddress, defaultMaxValue)
                        .output(valveMinAddress, defaultMinValue)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 500)
                        .output(valveSetPointPowerAddress, null)
                        .output(valveMaxAddress, defaultMaxValue)
                        .output(valveMinAddress, defaultMinValue)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 550)
                        .output(valveSetPointPowerAddress, null)
                        .output(valveMaxAddress, defaultMaxValue)
                        .output(valveMinAddress, defaultMinValue)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 600)

                )
                //5 Restart Cycles until
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .input(thermometerAddress, 450)
                        .output(valveSetPointPowerAddress, null)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, null)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, null)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, null)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, null)

                )
                .next(new TestCase()
                        .input(controllerEnableSignal, true)
                        .output(valveSetPointPowerAddress, null)
                        .output(valveMaxAddress, defaultMaxValue)
                        .output(valveMinAddress, defaultMinValue)

                )
                .getSut().run(); // execute Run method of controller
    }

    @Test
    public void fallbackOneChannel() throws Exception {
        HydraulicLineHeaterController controller = new HydraulicLineHeaterController();
        OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
        components = this.cpm.getAllComponents().toArray(components);
        new ControllerTest(controller, components)
                .addReference("cpm", new DummyComponentManager(clock)) // "cpm" in this case is the name of the componentmanager of the controller. Note: has to be the same name as the reference in the controller
                .activate(MyHydraulicLineHeaterConfig.create()
                        .setId(id)
                        .setService_pid("ThisIsFine")
                        .setTempSensorReference(correctThermometerId)
                        .setTemperatureDefault(correctReferenceTemperature)
                        .setLineHeaterType(LineHeaterType.VALVE)
                        .setValueToWriteIsBoolean(false)
                        .setChannelAddress(apartmentModuleAddress.toString())
                        .setChannels(new String[]{"foo", "bar"})
                        .setValveBypass(correctValveId)
                        .setTimerId(timerId)
                        .setTimeoutMaxRemote(10)
                        .setTimeoutRestartCycle(defaultTimeOutRestartCycle)
                        .setShouldFallback(true)
                        .setMinuteFallbackStart(defaultFallbackStart)
                        .setMinuteFallbackStop(defaultFallbackStop)
                        .setUseMinMax(true)
                        .setMaxMinOnly(false)
                        .setMaxValveValue(defaultMaxValue)
                        .setMinValveValue(defaultMinValue)
                        .setUseDecentralizedHeater(false)
                        .setDecentralizedHeaterReference(decentralizedHeaterId)
                        .setReactionType(DecentralizedHeaterReactionType.NEED_HEAT)
                        .setEnabled(true)
                        .build())
                //NOTE: Fallback needs to be set to 10
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .input(controllerEnableSignal, true)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .output(controllerFallbackSignal, false)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .output(controllerFallbackSignal, false)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .output(controllerFallbackSignal, false)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .output(controllerFallbackSignal, false)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .output(controllerFallbackSignal, false)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .output(controllerFallbackSignal, false)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .output(controllerFallbackSignal, false)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .output(controllerFallbackSignal, false)

                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .output(controllerFallbackSignal, false)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .output(controllerFallbackSignal, false)
                )
                .next(new TestCase()
                        .timeleap(clock, 1, ChronoUnit.SECONDS)//not strictly necessary but some things just won't work sometimes otherwise
                        .input(thermometerAddress, 400)
                        .output(controllerFallbackSignal, true)
                )
                .getSut().run();
    }

    @Test
    public void createDummy() {
        DummyHydraulicLineController hydraulicLineController = new DummyHydraulicLineController(id);
    }

}