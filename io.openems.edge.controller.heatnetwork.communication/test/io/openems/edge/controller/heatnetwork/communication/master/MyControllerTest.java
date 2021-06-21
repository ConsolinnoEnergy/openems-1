package io.openems.edge.controller.heatnetwork.communication.master;

import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.TimeLeapClock;
import io.openems.edge.controller.heatnetwork.communication.api.ConnectionType;
import io.openems.edge.controller.heatnetwork.communication.api.FallbackHandling;
import io.openems.edge.controller.heatnetwork.communication.api.ManageType;
import io.openems.edge.controller.heatnetwork.communication.request.api.RequestType;
import io.openems.edge.heatsystem.components.test.DummyPump;
import io.openems.edge.remote.rest.device.simulator.RestRemoteTestDevice;
import io.openems.edge.thermometer.api.test.DummyThermometer;
import io.openems.edge.timer.api.DummyTimer;
import io.openems.edge.timer.api.TimerType;
import org.junit.Before;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;

public class MyControllerTest {
    private static final String id = "test";
    private static final String exampleConfigStringValue = "exampleConfigStringValue";
    private boolean enabled = true;
    private ConnectionType connectionType;
    private int maxRequestsAllowedAtOnce;
    private TimerType timerForManager;
    private int maxWaitTimeAllowed;
    private ManageType manageType;
    private String timerId;
    private int keepAlive;
    private FallbackHandling fallback;
    private String[] requestMap;
    private String[] requestTypes;
    private String[] methodTypes;
    private String[] requestTypeToResponse;
    private String[] masterResponseTypes;
    private boolean useHydraulicLineHeater;
    private String hydraulicLineHeaterId;
    private boolean usePump;
    private String pumpId;
    private String service_pid;
    private boolean useExceptionalStateHandling;
    private String timerIdExceptionalState;
    private int exceptioalStateTime;
    private boolean forceHeating;
    private boolean configurationDone;
    //Do 12 Times and add X
    private final static String availableRestDeviceId = "RestRemoteDevice";
    private final static String heatPumpId = "Pump0";
    private final static String thermometerId = "Thermometer0";

    private List<RestRemoteTestDevice> list;
    private DummyThermometer dummyThermometer;
    private String virtualThermometer;
    private DummyComponentManager cpm;
    private final TimeLeapClock clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);
    private DummyTimer timer;
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
    public void setup()
}