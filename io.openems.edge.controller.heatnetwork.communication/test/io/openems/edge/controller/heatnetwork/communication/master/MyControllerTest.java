package io.openems.edge.controller.heatnetwork.communication.master;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.heatnetwork.communication.api.ConnectionType;
import io.openems.edge.controller.heatnetwork.communication.api.FallbackHandling;
import io.openems.edge.controller.heatnetwork.communication.api.ManageType;
import io.openems.edge.controller.heatnetwork.communication.request.api.RequestType;
import io.openems.edge.heatsystem.components.test.DummyPump;
import io.openems.edge.remote.rest.device.api.DummyRestDevice;
import io.openems.edge.remote.rest.device.api.RestRemoteDevice;
import io.openems.edge.remote.rest.device.simulator.RestRemoteTestDevice;
import io.openems.edge.thermometer.api.test.DummyThermometer;
import io.openems.edge.thermometer.api.test.DummyVirtualThermometer;
import io.openems.edge.timer.api.DummyTimer;
import io.openems.edge.timer.api.TimerType;
import org.junit.Before;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MyControllerTest {
    private static final String id = "test";
    private static final String exampleConfigStringValue = "exampleConfigStringValue";
    private boolean enabled = true;
    private ConnectionType connectionType = ConnectionType.REST;
    private int maxRequestsAllowedAtOnce = 3;
    private TimerType timerForManager = TimerType.CYCLES;
    private int maxWaitTimeAllowed = 5;
    private ManageType manageType = ManageType.FIFO;
    private String timerId = TIMER_ID;
    private int keepAlive = 2;
    private FallbackHandling fallback = FallbackHandling.DEFAULT;
    private String[] restDeviceIds;
    private String[] requestMap;

    private String[] requestTypes;
    private String[] methodTypes;
    private String[] requestTypeToResponse;
    private String[] masterResponseTypes;
    private boolean useHydraulicLineHeater = true;
    private String hydraulicLineHeaterId = HYDRAULIC_LINE_HEATER_ID;
    private boolean usePump = true;
    private String pumpId = HEAT_PUMP_ID;
    private String service_pid;
    private boolean useExceptionalStateHandling = true;
    private String timerIdExceptionalState = TIMER_ID;
    private int exceptioalStateTime = 10;
    private boolean forceHeating = false;
    private boolean configurationDone = true;
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
        this.pump = new DummyPump(HEAT_PUMP_ID, "Relays");
        restDeviceIds = new String[]{"Rest0", "Rest1", "Rest2", "Rest3", "Rest4", "Rest5", "Rest6", "Rest7", "Rest8", "Rest9", "Rest10", "Rest11"};
        requestMap = new String[]{"Rest0:Rest1:1:HEAT", "Rest2:Rest3:1:MORE_HEAT",
                "Rest4:Rest5:2:HEAT", "Rest6:Rest7:2:MORE_HEAT",
                "Rest8:Rest9:3:HEAT", "Rest10:Rest11:3:MORE_HEAT"};
        requestTypeToResponse = new String[]{"HEAT:CHANNEL_ADDRESS:Pump0/SetPowerLevel:100:0",
                "HEAT:CHANNEL_ADDRESS:VirtualThermometer0/VirtualTemperature:70:0",
                "MORE_HEAT:METHOD:ACTIVATE_LINEHEATER:true:null"};
        AtomicInteger counter = new AtomicInteger(0);
        Arrays.stream(restDeviceIds).forEach(entry -> {
            if(counter.get() % 2 == 1) {
                deviceList.add(new DummyRestDevice(entry));
            } else {
                deviceList.add(new DummyRestDevice(entry, true));
            }
            counter.getAndIncrement();
        });
        this.cpm = new DummyComponentManager(clock);
    }
}