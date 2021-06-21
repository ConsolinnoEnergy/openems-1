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
import io.openems.edge.timer.api.TimerType;

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
    private List<RestRemoteTestDevice> list;
    private DummyPump pump;
    private DummyThermometer dummyThermometer;
    private String virtualThermometer;
    private DummyComponentManager cpm;
    private final TimeLeapClock clock = new TimeLeapClock(Instant.ofEpochSecond(1577836800), ZoneOffset.UTC);
}