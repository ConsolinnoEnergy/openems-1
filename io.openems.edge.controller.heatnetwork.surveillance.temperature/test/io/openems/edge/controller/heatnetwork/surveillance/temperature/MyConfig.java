package io.openems.edge.controller.heatnetwork.surveillance.temperature;

import io.openems.edge.common.test.AbstractComponentConfig;


@SuppressWarnings("all")

public class MyConfig extends AbstractComponentConfig implements Config {


    protected static class Builder {

        private String id;
        private boolean enabled = true;
        private String referenceThermometerId;
        private String thermometerActivateId;
        private int offsetActivate;
        private String thermometerDeactivateId;
        private int offsetDeactivate;
        private String valveControllerId;
        private boolean useValveController;
        private boolean useHeater;
        private String heaterId;
        private String timerId;
        private int timeToWaitValveOpen;
        private String service_pid;
        private TemperatureSurveillanceType surveillanceType;

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


        public Builder setReferenceThermometerId(String referenceThermometerId) {
            this.referenceThermometerId = referenceThermometerId;
            return this;
        }

        public Builder setThermometerActivateId(String thermometerActivateId) {
            this.thermometerActivateId = thermometerActivateId;
            return this;
        }

        public Builder setOffsetActivate(int offsetActivate) {
            this.offsetActivate = offsetActivate;
            return this;
        }

        public Builder setThermometerDeactivateId(String thermometerDeactivateId) {
            this.thermometerDeactivateId = thermometerDeactivateId;
            return this;
        }

        public Builder setOffsetDeactivate(int offsetDeactivate) {
            this.offsetDeactivate = offsetDeactivate;
            return this;
        }

        public Builder setSurveillanceType(TemperatureSurveillanceType type){
            this.surveillanceType = type;
            return this;
        }

        public Builder setValveControllerId(String valveControllerId) {
            this.valveControllerId = valveControllerId;
            return this;
        }

        public Builder setHeaterId(String heaterId) {
            this.heaterId = heaterId;
            return this;
        }

        public Builder setTimerId(String timerId) {
            this.timerId = timerId;
            return this;
        }

        public Builder setTimeToWaitValveOpen(int timeToWaitValveOpen) {
            this.timeToWaitValveOpen = timeToWaitValveOpen;
            return this;
        }

        public Builder setService_pid(String service_pid) {
            this.service_pid = service_pid;
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

    private MyConfig(Builder builder) {
        super(Config.class, builder.id);
        this.builder = builder;
    }


    @Override
    public String service_pid() {
        return this.builder.service_pid;
    }

    @Override
    public String referenceThermometerId() {
        return this.builder.referenceThermometerId;
    }

    @Override
    public String thermometerActivateId() {
        return this.builder.thermometerActivateId;
    }

    @Override
    public int offsetActivate() {
        return this.builder.offsetActivate;
    }

    @Override
    public String thermometerDeactivateId() {
        return this.builder.thermometerDeactivateId;
    }

    @Override
    public int offsetDeactivate() {
        return this.builder.offsetDeactivate;
    }


    @Override
    public String valveControllerId() {
        return this.builder.valveControllerId;
    }

    @Override
    public TemperatureSurveillanceType surveillanceType() {
        return this.builder.surveillanceType;
    }

    @Override
    public String heaterId() {
        return this.builder.heaterId;
    }

    @Override
    public String timerId() {
        return this.builder.timerId;
    }

    @Override
    public int timeToWaitValveOpen() {
        return this.builder.timeToWaitValveOpen;
    }

    @Override
    public boolean enabled() {
        return this.builder.enabled;
    }


}
