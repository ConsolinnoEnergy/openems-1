package io.openems.edge.controller.heatnetwork.communication.master;

import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.controller.heatnetwork.communication.api.FallbackHandling;
import io.openems.edge.controller.heatnetwork.communication.api.ManageType;
import io.openems.edge.timer.api.TimerType;


@SuppressWarnings("all")

public class MyConfig extends AbstractComponentConfig implements Config {


    protected static class Builder {

        private String id;
        private boolean enabled = true;
        private String connectionType;
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
        private boolean useHydraulicLineHeater;

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
        return null;
    }

    @Override
    public String connectionType() {
        return null;
    }

    @Override
    public int maxRequestAllowedAtOnce() {
        return 0;
    }

    @Override
    public TimerType timerForManager() {
        return null;
    }

    @Override
    public int maxWaitTimeAllowed() {
        return 0;
    }

    @Override
    public String manageType() {
        return null;
    }

    @Override
    public String timerId() {
        return null;
    }

    @Override
    public int keepAlive() {
        return 0;
    }

    @Override
    public FallbackHandling fallback() {
        return null;
    }

    @Override
    public String[] requestMap() {
        return new String[0];
    }

    @Override
    public String[] requestTypes() {
        return new String[0];
    }

    @Override
    public String[] methodTypes() {
        return new String[0];
    }

    @Override
    public String[] requestTypeToResponse() {
        return new String[0];
    }

    @Override
    public boolean useHydraulicLineHeater() {
        return false;
    }

    @Override
    public String hydraulicLineHeaterId() {
        return null;
    }

    @Override
    public boolean usePump() {
        return false;
    }

    @Override
    public String pumpId() {
        return null;
    }

    @Override
    public boolean useExceptionalStateHandling() {
        return false;
    }

    @Override
    public String timerIdExceptionalState() {
        return null;
    }

    @Override
    public int exceptionalStateTime() {
        return 0;
    }

    @Override
    public boolean enabled() {
        return this.builder.enabled;
    }

    @Override
    public boolean forceHeating() {
        return false;
    }

    @Override
    public boolean configurationDone() {
        return false;
    }
}
