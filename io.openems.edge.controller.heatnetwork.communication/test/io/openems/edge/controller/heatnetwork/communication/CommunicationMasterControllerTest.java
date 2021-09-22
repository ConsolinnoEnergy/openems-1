package io.openems.edge.controller.heatnetwork.communication;

import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.rest.api.DummyRestDevice;
import io.openems.edge.bridge.rest.api.RestRemoteDevice;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.heatnetwork.communication.api.ConnectionType;
import io.openems.edge.controller.heatnetwork.communication.api.FallbackHandling;
import io.openems.edge.controller.heatnetwork.communication.api.ManageType;
import io.openems.edge.controller.heatnetwork.communication.request.api.MasterResponseType;
import io.openems.edge.controller.heatnetwork.communication.request.api.MethodTypes;
import io.openems.edge.controller.heatnetwork.communication.request.api.RequestType;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api.HydraulicLineHeater;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.test.DummyHydraulicLineHeater;
import io.openems.edge.controller.test.ControllerTest;
import io.openems.edge.heatsystem.components.PumpType;
import io.openems.edge.heatsystem.components.test.DummyPump;
import io.openems.edge.thermometer.api.test.DummyVirtualThermometer;
import io.openems.edge.timer.api.DummyTimer;
import io.openems.edge.timer.api.TimerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CommunicationMasterControllerTest {
    private static final String id = "test";
    private static final String exampleConfigStringValue = "exampleConfigStringValue";
    private final boolean enabled = true;
    private final ConnectionType connectionType = ConnectionType.REST;
    private final int maxRequestsAllowedAtOnce = 2;
    private final TimerType timerForManager = TimerType.CYCLES;
    private final int maxWaitTimeAllowed = 2;
    private final ManageType manageType = ManageType.FIFO;
    private final int keepAlive = 1;
    private final FallbackHandling fallback = FallbackHandling.DEFAULT;
    private String[] restDeviceIds;
    private String[] requestMap;

    private String[] requestTypes;
    private String[] methodTypes;
    private String[] requestTypeToResponse;
    private String[] masterResponseTypes;
    private final boolean useHydraulicLineHeater = true;
    private final boolean usePump = true;
    private final boolean useExceptionalStateHandling = true;
    private final String timerIdExceptionalState = TIMER_ID;
    private final int exceptioalStateTime = 2;
    private final boolean forceHeating = false;
    //Do 12 Times and add X
    private final static String HEAT_PUMP_ID = "Pump0";
    private final static String HYDRAULIC_LINE_HEATER_ID = "LineHeater0";
    private final static String THERMOMETER_ID = "VirtualThermometer0";
    private final static String TIMER_ID = "TimerDummy";

    private final List<RestRemoteDevice> deviceList = new ArrayList<>();
    private DummyVirtualThermometer dummyThermometer;
    private DummyComponentManager cpm;
    private final TimeLeapClock clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);
    private DummyTimer timer;
    private DummyPump pump;
    private HydraulicLineHeater dummyLineHeater;
    private final Map<String, ChannelAddress> channelAddressMap = new HashMap<>();
    /*
     * 1. Setup 8 RestRemoteDevices -> 4 Read 4 Write
     * 2. Setup dummyPump -> ResponseMethod
     * 3. Setup dummyThermometer -> Response
     * 4. Setup dummyHydraulicLineHeater -> ResponseMethod
     * 5. Setup dummyTimer
     * 6. Add ChannelAddresses -> 4 Read 4 Write RestRemotes Heat
     *                          -> 2 Read 2 Write MoreHeat
     *                         -> HydraulicEnableSignal
     *                          ->VirtualThermometer
     *                          ->Pump -> setPowerLevel
     *
     * ---------------------TEST CASES ---------------------------
     * 1. Normal: Heat 3 up and activate Pump and HydraulicLineHeater
     * 2. Swap 1 after Time is up and activate Pump and HydraulicLineHeater
     * 3. Normal with -> no heat response at some time -> deactivate response and set to 0 the RestRemoteDevices
     * 4. ForceHeating -> Every Response to 1
     * 5. SETUP Failures -> RestRemote is wrong
     *                   -> ChannelAddress Wrong
     *                   -> Order is wrong
     * 6. FallbackHandling -> Everything to true and Pump and HydraulicLineHeater to true/100%
     * 7. ExceptionalState 0 and 100
     *
     * */


    @Before
    public void setup() {
        this.timer = new DummyTimer(TIMER_ID, TimerType.CYCLES);
        this.dummyThermometer = new DummyVirtualThermometer(THERMOMETER_ID);
        this.pump = new DummyPump(HEAT_PUMP_ID, PumpType.RELAY);
        this.cpm = new DummyComponentManager(clock);
        this.dummyLineHeater = new DummyHydraulicLineHeater(HYDRAULIC_LINE_HEATER_ID);
        restDeviceIds = new String[]{"Rest0", "Rest1", "Rest2", "Rest3", "Rest4", "Rest5", "Rest6", "Rest7", "Rest8", "Rest9", "Rest10", "Rest11"};
        requestMap = new String[]{"Rest0:Rest1:1:HEAT", "Rest2:Rest3:1:MORE_HEAT",
                "Rest4:Rest5:2:HEAT", "Rest6:Rest7:2:MORE_HEAT",
                "Rest8:Rest9:3:HEAT", "Rest10:Rest11:3:MORE_HEAT"};
        requestTypeToResponse = new String[]{"HEAT:CHANNEL_ADDRESS:Pump0/SetPointPowerLevel:100:0",
                "HEAT:CHANNEL_ADDRESS:VirtualThermometer0/VirtualTemperature:70:0",
                "MORE_HEAT:METHOD:ACTIVATE_LINE_HEATER:true:null"};
        AtomicInteger counter = new AtomicInteger(0);
        Arrays.stream(restDeviceIds).forEach(entry -> {
            DummyRestDevice device;
            String channel = "ValueRead";
            if (counter.get() % 2 == 1) {
                device = new DummyRestDevice(entry, true);
                channel = "ValueWrite";
            } else {
                device = new DummyRestDevice(entry);
            }
            deviceList.add(device);
            this.cpm.addComponent(device);
            this.channelAddressMap.put(entry, new ChannelAddress(entry, channel));
            counter.getAndIncrement();
        });
        this.cpm.addComponent(this.pump);
        this.cpm.addComponent(this.dummyThermometer);
        this.cpm.addComponent(this.timer);
        this.cpm.addComponent(this.dummyLineHeater);
        this.requestTypes = new String[RequestType.values().length];
        this.methodTypes = new String[MethodTypes.values().length];
        this.masterResponseTypes = new String[MasterResponseType.values().length];
        AtomicInteger counterForTypes = new AtomicInteger(0);
        Arrays.stream(RequestType.values()).forEach(entry -> {
            requestTypes[counterForTypes.getAndIncrement()] = entry.name();
        });
        counterForTypes.set(0);
        Arrays.stream(MethodTypes.values()).forEach(entry -> {
            methodTypes[counterForTypes.getAndIncrement()] = entry.name();
        });
        counterForTypes.set(0);
        Arrays.stream(MasterResponseType.values()).forEach(entry -> {
            masterResponseTypes[counterForTypes.getAndIncrement()] = entry.name();
        });
    }


    /**
     * The Configuration is correct and the Controller is running/Heating as expected.
     */
    @Test
    public void callbackTrueOnRequest() {
        try {
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new CommunicationMasterControllerImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setEnabled(enabled)
                            .setService_pid("EverythingIsFine")
                            .setTimerId(TIMER_ID)
                            .setConnectionType(this.connectionType)
                            .setExceptioalStateTime(this.exceptioalStateTime)
                            .setFallback(this.fallback)
                            .setForceHeating(this.forceHeating)
                            .setHydraulicLineHeaterId(HYDRAULIC_LINE_HEATER_ID)
                            .setKeepAlive(this.keepAlive)
                            .setManageType(this.manageType)
                            .setMaxRequestsAllowedAtOnce(this.maxRequestsAllowedAtOnce)
                            .setPumpId(HEAT_PUMP_ID)
                            .setUsePumpId(this.usePump)
                            .setTimerForManager(this.timerForManager)
                            .setUsePump(this.usePump)
                            .setUseHydraulicLineHeater(this.useHydraulicLineHeater)
                            .setTimerIdExceptionalState(this.timerIdExceptionalState)
                            .setUseExceptionalStateHandling(this.useExceptionalStateHandling)
                            .setRequestMap(this.requestMap)
                            .setRequestTypeToResponse(this.requestTypeToResponse)
                            .setMaxWaitTimeAllowed(this.maxWaitTimeAllowed)
                            .setRequestTypes(this.requestTypes)
                            .setMasterResponseTypes(this.masterResponseTypes)
                            .setMethodTypes(this.methodTypes)
                            .setConfigurationDone(true)
                            .build())
                    //1 and 2 having heat and more heat requests
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            //Request
                            .input(this.channelAddressMap.get("Rest0"), 1)
                            .input(this.channelAddressMap.get("Rest2"), 1)
                            .input(this.channelAddressMap.get("Rest4"), 1)
                            .input(this.channelAddressMap.get("Rest6"), 1)
                            //Callback
                            .output(this.channelAddressMap.get("Rest1"), "1")
                            .output(this.channelAddressMap.get("Rest3"), "1")
                            .output(this.channelAddressMap.get("Rest5"), "1")
                            .output(this.channelAddressMap.get("Rest7"), "1")
                    )
                    //no need more Heat -> internal method calls changed -> Passive Values
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            //Request
                            .input(this.channelAddressMap.get("Rest0"), 1)
                            .input(this.channelAddressMap.get("Rest2"), 0)
                            .input(this.channelAddressMap.get("Rest4"), 1)
                            .input(this.channelAddressMap.get("Rest6"), 0)
                            //Callback
                            .output(this.channelAddressMap.get("Rest1"), "1")
                            .output(this.channelAddressMap.get("Rest5"), "1")
                    )
                    //No Heat Request -> Response with 0 and use Passive Values
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            //Request
                            .input(this.channelAddressMap.get("Rest0"), 0)
                            .input(this.channelAddressMap.get("Rest2"), 0)
                            .input(this.channelAddressMap.get("Rest4"), 0)
                            .input(this.channelAddressMap.get("Rest6"), 0)
                            //Callback
                            .output(this.channelAddressMap.get("Rest1"), "0")
                            .output(this.channelAddressMap.get("Rest5"), "0")
                    )
                    //3 has a heating request but no response yet
                    .next(new TestCase()
                            //Request
                            .input(this.channelAddressMap.get("Rest0"), 1)
                            .input(this.channelAddressMap.get("Rest2"), 1)
                            .input(this.channelAddressMap.get("Rest4"), 1)
                            .input(this.channelAddressMap.get("Rest6"), 1)
                            .input(this.channelAddressMap.get("Rest8"), 1)
                            //Callback
                            .output(this.channelAddressMap.get("Rest1"), "1")
                            .output(this.channelAddressMap.get("Rest3"), "1")
                            .output(this.channelAddressMap.get("Rest5"), "1")
                            .output(this.channelAddressMap.get("Rest7"), "1")
                            .output(this.channelAddressMap.get("Rest9"), "0")
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            //Request
                            .input(this.channelAddressMap.get("Rest0"), 1)
                            .input(this.channelAddressMap.get("Rest2"), 1)
                            .input(this.channelAddressMap.get("Rest4"), 1)
                            .input(this.channelAddressMap.get("Rest6"), 1)
                            .input(this.channelAddressMap.get("Rest8"), 1)
                            //Callback
                            .output(this.channelAddressMap.get("Rest1"), "1")
                            .output(this.channelAddressMap.get("Rest3"), "1")
                            .output(this.channelAddressMap.get("Rest5"), "1")
                            .output(this.channelAddressMap.get("Rest7"), "1")
                            .output(this.channelAddressMap.get("Rest9"), "0")
                    )
                    //bc of FIFO 1 is "disabled" and 3 is allowed to heat
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            //Request
                            .input(this.channelAddressMap.get("Rest0"), 1)
                            .input(this.channelAddressMap.get("Rest2"), 1)
                            .input(this.channelAddressMap.get("Rest4"), 1)
                            .input(this.channelAddressMap.get("Rest6"), 1)
                            .input(this.channelAddressMap.get("Rest8"), 1)
                            //Callback
                            .output(this.channelAddressMap.get("Rest9"), "1")

                            .output(this.channelAddressMap.get("Rest1"), "0")
                            .output(this.channelAddressMap.get("Rest3"), "0")
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            //Request
                            .input(this.channelAddressMap.get("Rest0"), 1)
                            .input(this.channelAddressMap.get("Rest2"), 1)
                            .input(this.channelAddressMap.get("Rest4"), 1)
                            .input(this.channelAddressMap.get("Rest6"), 1)
                            .input(this.channelAddressMap.get("Rest8"), 1)
                            //Callback
                            .output(this.channelAddressMap.get("Rest9"), "1")

                            .output(this.channelAddressMap.get("Rest1"), "0")
                            .output(this.channelAddressMap.get("Rest3"), "0")
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            .input(ChannelAddress.fromString(id + "/" + "ExceptionalStateEnableSignal"), 1)
                            .input(ChannelAddress.fromString(id + "/" + "ExceptionalStateValue"), 50)
                            .input(this.channelAddressMap.get("Rest0"), 1)
                            .input(this.channelAddressMap.get("Rest2"), 1)
                            .input(this.channelAddressMap.get("Rest4"), 1)
                            .input(this.channelAddressMap.get("Rest6"), 1)
                            .input(this.channelAddressMap.get("Rest8"), 1)
                            .input(this.channelAddressMap.get("Rest10"), 1)
                            //Callback
                            .output(this.channelAddressMap.get("Rest1"), "1")
                            .output(this.channelAddressMap.get("Rest3"), "1")
                            .output(this.channelAddressMap.get("Rest5"), "1")
                            .output(this.channelAddressMap.get("Rest7"), "1")
                            .output(this.channelAddressMap.get("Rest9"), "1")
                            .output(this.channelAddressMap.get("Rest11"), "1")
                    )
                    .getSut().run();
        } catch (Exception e) {
            Assert.fail();
        }
    }

    /**
     * The Configuration is correct and the Controller has to use FallbackLogic.
     */
    @Test
    public void fallBack() {
        try {
            this.cpm.addComponent(new DummyRestDevice("Rest12", false, true))
                    .addComponent(new DummyRestDevice("Rest13", true, true));
            requestMap = new String[]{"Rest0:Rest1:1:HEAT", "Rest2:Rest3:1:MORE_HEAT",
                    "Rest4:Rest5:2:HEAT", "Rest6:Rest7:2:MORE_HEAT",
                    "Rest8:Rest9:3:HEAT", "Rest10:Rest11:3:MORE_HEAT", "Rest12:Rest13:4:HEAT"};
            channelAddressMap.put("Rest12", ChannelAddress.fromString("Rest12/ValueRead"));
            channelAddressMap.put("Rest13", ChannelAddress.fromString("Rest13/ValueWrite"));
            OpenemsComponent[] components = new OpenemsComponent[this.cpm.getAllComponents().size()];
            components = this.cpm.getAllComponents().toArray(components);
            new ControllerTest(new CommunicationMasterControllerImpl(), components)
                    .addReference("cpm", this.cpm)
                    .activate(MyConfig.create()
                            .setId(id)
                            .setEnabled(enabled)
                            .setService_pid("FallBack")
                            .setTimerId(TIMER_ID)
                            .setConnectionType(this.connectionType)
                            .setExceptioalStateTime(this.exceptioalStateTime)
                            .setFallback(this.fallback)
                            .setForceHeating(this.forceHeating)
                            .setHydraulicLineHeaterId(HYDRAULIC_LINE_HEATER_ID)
                            .setKeepAlive(this.keepAlive)
                            .setManageType(this.manageType)
                            .setMaxRequestsAllowedAtOnce(this.maxRequestsAllowedAtOnce)
                            .setPumpId(HEAT_PUMP_ID)
                            .setUsePumpId(this.usePump)
                            .setTimerForManager(this.timerForManager)
                            .setUsePump(this.usePump)
                            .setUseHydraulicLineHeater(this.useHydraulicLineHeater)
                            .setTimerIdExceptionalState(this.timerIdExceptionalState)
                            .setUseExceptionalStateHandling(this.useExceptionalStateHandling)
                            .setRequestMap(this.requestMap)
                            .setRequestTypeToResponse(this.requestTypeToResponse)
                            .setMaxWaitTimeAllowed(this.maxWaitTimeAllowed)
                            .setRequestTypes(this.requestTypes)
                            .setMasterResponseTypes(this.masterResponseTypes)
                            .setMethodTypes(this.methodTypes)
                            .setConfigurationDone(true)
                            .build())
                    //1 and 2 having heat and more heat requests
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                    )
                    .next(new TestCase()
                            .timeleap(this.clock, 1, ChronoUnit.SECONDS)
                            //Request
                            .input(this.channelAddressMap.get("Rest0"), 1)
                            .input(this.channelAddressMap.get("Rest2"), 1)
                            .input(this.channelAddressMap.get("Rest4"), 1)
                            .input(this.channelAddressMap.get("Rest6"), 1)
                            //Callback
                            .output(this.channelAddressMap.get("Rest1"), "1")
                            .output(this.channelAddressMap.get("Rest3"), "1")
                            .output(this.channelAddressMap.get("Rest5"), "1")
                            .output(this.channelAddressMap.get("Rest7"), "1")
                            .output(this.channelAddressMap.get("Rest9"), "1")
                            .output(this.channelAddressMap.get("Rest11"), "1")
                            .output(this.channelAddressMap.get("Rest13"), "1")
                    )
                    .getSut().run();
        } catch (Exception e) {
            Assert.fail();
        }
    }
}