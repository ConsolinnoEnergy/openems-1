package io.openems.edge.controller.heatnetwork.communication;

import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.controller.heatnetwork.communication.api.ConnectionType;
import io.openems.edge.controller.heatnetwork.communication.api.FallbackHandling;
import io.openems.edge.controller.heatnetwork.communication.api.ManageType;
import io.openems.edge.timer.api.TimerType;


@SuppressWarnings("all")

public class MyConfig extends AbstractComponentConfig implements Config {


    protected static class Builder {

        private String id;
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


        private Builder() {
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }


        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setConnectionType(ConnectionType connectionType) {
            this.connectionType = connectionType;

            return this;
        }

        public Builder setMaxRequestsAllowedAtOnce(int maxRequestsAllowedAtOnce) {
            this.maxRequestsAllowedAtOnce = maxRequestsAllowedAtOnce;
            return this;
        }

        public Builder setTimerForManager(TimerType timerForManager) {
            this.timerForManager = timerForManager;
            return this;
        }

        public Builder setMaxWaitTimeAllowed(int maxWaitTimeAllowed) {
            this.maxWaitTimeAllowed = maxWaitTimeAllowed;
            return this;
        }

        public Builder setManageType(ManageType manageType) {
            this.manageType = manageType;
            return this;
        }

        public Builder setTimerId(String timerId) {
            this.timerId = timerId;
            return this;
        }

        public Builder setKeepAlive(int keepAlive) {
            this.keepAlive = keepAlive;
            return this;
        }

        public Builder setFallback(FallbackHandling fallback) {
            this.fallback = fallback;
            return this;
        }

        public Builder setRequestMap(String[] requestMap) {
            this.requestMap = requestMap;
            return this;
        }

        public Builder setRequestTypes(String[] requestTypes) {
            this.requestTypes = requestTypes;
            return this;
        }

        public Builder setMethodTypes(String[] methodTypes) {
            this.methodTypes = methodTypes;
            return this;
        }

        public Builder setRequestTypeToResponse(String[] requestTypeToResponse) {
            this.requestTypeToResponse = requestTypeToResponse;
            return this;
        }

        public Builder setMasterResponseTypes(String[] masterResponseTypes) {
            this.masterResponseTypes = masterResponseTypes;
            return this;
        }

        public Builder setUseHydraulicLineHeater(boolean useHydraulicLineHeater) {
            this.useHydraulicLineHeater = useHydraulicLineHeater;
            return this;
        }

        public Builder setUsePumpId(boolean usePump) {
            this.usePump = usePump;
            return this;
        }

        public Builder setPumpId(String pumpId) {
            this.pumpId = pumpId;
            return this;
        }

        public Builder setHydraulicLineHeaterId(String hydraulicLineHeaterId) {
            this.hydraulicLineHeaterId = hydraulicLineHeaterId;
            return this;
        }

        public Builder setService_pid(String service_pid) {
            this.service_pid = service_pid;
            return this;
        }

        public Builder setUsePump(boolean usePump) {
            this.usePump = usePump;
            return this;
        }

        public Builder setUseExceptionalStateHandling(boolean useExceptionalStateHandling) {
            this.useExceptionalStateHandling = useExceptionalStateHandling;
            return this;
        }

        public Builder setTimerIdExceptionalState(String timerIdExceptionalState) {
            this.timerIdExceptionalState = timerIdExceptionalState;
            return this;
        }

        public Builder setExceptioalStateTime(int exceptioalStateTime) {
            this.exceptioalStateTime = exceptioalStateTime;
            return this;
        }

        public Builder setForceHeating(boolean forceHeating) {
            this.forceHeating = forceHeating;
            return this;
        }

        public Builder setConfigurationDone(boolean done) {
            this.configurationDone = done;
            return this;
        }

        public MyConfig build() {
            return new MyConfig(this);
        }
    }

    /**
     * Create a Config builder.
     *
     * @return a {@link Builder}
     */
    public static Builder create() {
        return new Builder();
    }

    private final Builder builder;

    private MyConfig(Builder builder) {
        super(Config.class, builder.id);
        this.builder = builder;
    }


    @Override
    public String service_pid() {
        return this.builder.service_pid;
    }

    @Override
    public ConnectionType connectionType() {
        return this.builder.connectionType;
    }

    @Override
    public int maxDecentralizedSystemsAllowedAtOnce() {
        return this.builder.maxRequestsAllowedAtOnce;
    }

    @Override
    public TimerType timerForManager() {
        return this.builder.timerForManager;
    }

    @Override
    public int maxWaitTimeAllowed() {
        return this.builder.maxWaitTimeAllowed;
    }

    @Override
    public ManageType manageType() {
        return this.builder.manageType;
    }

    @Override
    public String timerId() {
        return this.builder.timerId;
    }

    @Override
    public int keepAlive() {
        return this.builder.keepAlive;
    }

    @Override
    public FallbackHandling fallback() {
        return this.builder.fallback;
    }

    @Override
    public String[] requestMap() {
        return this.builder.requestMap;
    }

    @Override
    public String[] requestTypes() {
        return this.builder.requestTypes;
    }

    @Override
    public String[] methodTypes() {
        return this.builder.methodTypes;
    }

    @Override
    public String[] masterResponseTypes() {
        return this.builder.masterResponseTypes;
    }

    @Override
    public String[] requestTypeToResponse() {
        return this.builder.requestTypeToResponse;
    }

    @Override
    public boolean useHydraulicLineHeater() {
        return this.builder.useHydraulicLineHeater;
    }

    @Override
    public String hydraulicLineHeaterId() {
        return this.builder.hydraulicLineHeaterId;
    }

    @Override
    public boolean usePump() {
        return this.builder.usePump;
    }

    @Override
    public String pumpId() {
        return this.builder.pumpId;
    }

    @Override
    public boolean useExceptionalStateHandling() {
        return this.builder.useExceptionalStateHandling;
    }

    @Override
    public String timerIdExceptionalState() {
        return this.builder.timerIdExceptionalState;
    }

    @Override
    public int exceptionalStateTime() {
        return this.builder.exceptioalStateTime;
    }

    @Override
    public boolean enabled() {
        return this.builder.enabled;
    }

    @Override
    public boolean forceHeating() {
        return this.builder.forceHeating;
    }

    @Override
    public boolean configurationDone() {
        return this.builder.configurationDone;
    }
}
