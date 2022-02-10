package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.edge.common.test.AbstractComponentConfig;


@SuppressWarnings("all")

public class MyMultiHeaterConfig extends AbstractComponentConfig implements ConfigMultipleHeater {


    protected static class Builder {

        public boolean useTimer;
        public int timeDelta;
        public String timerId;
        private String id;
        private String alias;
        private boolean enabled = true;
        private String[] heaterIds;
        private String[] activationTemperatures;
        private String[] deactivationTemperatures;
        private String[] activationThermometers;
        private String[] deactivationThermometers;
        private String servicePid;
        private boolean useOverride;
        private int overrideValue;


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

        public Builder setHeaterIds(String[] heaterIds) {
            this.heaterIds = heaterIds;
            return this;
        }
        public Builder setActivationTemperatures(String[] activationTemperatures){
            this.activationTemperatures = activationTemperatures;
            return this;
        }
        public Builder setDeactivationTemperatures(String[] deactivationTemperatures){
            this.deactivationTemperatures = deactivationTemperatures;
            return this;
        }
        public Builder setActivationThermometer(String[] activationThermometers){
            this.activationThermometers = activationThermometers;
            return this;
        }
        public Builder setDeactivationThermometer(String[] deactivationThermometers){
            this.deactivationThermometers = deactivationThermometers;
            return this;
        }
        public Builder setUseTimer(boolean useTimer) {
            this.useTimer = useTimer;
            return this;
        }

        public Builder setTimeDelta(int timeDelta) {
            this.timeDelta = timeDelta;
            return this;
        }

        public Builder setTimerId(String timerId) {
            this.timerId = timerId;
            return this;
        }

        public Builder setUseOverride(boolean useOverride) {
            this.useOverride = useOverride;
            return this;
        }

        public Builder setOverrideValue(int overrideValue) {
            this.overrideValue = overrideValue;
            return this;
        }

        public MyMultiHeaterConfig build() {
            return new MyMultiHeaterConfig(this);
        }

        public Builder setServicePid(String pid) {
            this.servicePid = pid;
            return this;
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

    private MyMultiHeaterConfig(Builder builder) {
        super(ConfigMultipleHeater.class, builder.id);
        this.builder = builder;
    }


    @Override
    public String service_pid() {
        return this.builder.servicePid;
    }

    @Override
    public String[] heaterIds() {
        return this.builder.heaterIds;
    }

    @Override
    public String[] activationTemperatures() {
        return this.builder.activationTemperatures;
    }

    @Override
    public String[] deactivationTemperatures() {
        return this.builder.deactivationTemperatures;
    }

    @Override
    public String[] activationThermometers() {
        return this.builder.activationThermometers;
    }

    @Override
    public String[] deactivationThermometers() {
        return this.builder.deactivationThermometers;
    }

    @Override
    public boolean useTimer() {
        return this.builder.useTimer;
    }

    @Override
    public int timeDelta() {
        return this.builder.timeDelta;
    }

    @Override
    public String timerId() {
        return this.builder.timerId;
    }

    @Override
    public boolean useOverrideValue() {
        return this.builder.useOverride;
    }

    @Override
    public int overrideValue() {
        return this.builder.overrideValue;
    }

    @Override
    public boolean enabled() {
        return this.builder.enabled;
    }
}
