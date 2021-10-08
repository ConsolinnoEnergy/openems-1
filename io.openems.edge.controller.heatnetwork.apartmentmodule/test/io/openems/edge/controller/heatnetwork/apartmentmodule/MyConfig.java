package io.openems.edge.controller.heatnetwork.apartmentmodule;

import io.openems.edge.common.test.AbstractComponentConfig;

public class MyConfig extends AbstractComponentConfig implements Config {


    protected static class Builder{
        public String servicePid;
        public String id;
       public String alias;
       public String[] apartmentCords;
       public String[] apartmentToThermometer;
       public String[] apartmentResponse;
       public int setPointTemperature;
       public String[] thresholdId;
       public String heatPumpId;
       public double powerLevelPump;
       public boolean useHeatBooster;
       public int heatBoostTemperature;
       public String heatBoosterId;
       public String timerId;
       public boolean enabled;
       
       private Builder(){}

        public Builder setServicePid(String servicePid) {
            this.servicePid = servicePid;
            return this;
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setAlias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder setApartmentCords(String[] apartmentCords) {
            this.apartmentCords = apartmentCords;
            return this;
        }

        public Builder setApartmentToThermometer(String[] apartmentToThermometer) {
            this.apartmentToThermometer = apartmentToThermometer;
            return this;
        }

        public Builder setApartmentResponse(String[] apartmentResponse) {
            this.apartmentResponse = apartmentResponse;
            return this;
        }

        public Builder setSetPointTemperature(int setPointTemperature) {
            this.setPointTemperature = setPointTemperature;
            return this;
        }

        public Builder setThresholdId(String[] thresholdId) {
            this.thresholdId = thresholdId;
            return this;
        }

        public Builder setHeatPumpId(String heatPumpId) {
            this.heatPumpId = heatPumpId;
            return this;
        }

        public Builder setPowerLevelPump(double powerLevelPump) {
            this.powerLevelPump = powerLevelPump;
            return this;
        }

        public Builder setUseHeatBooster(boolean useHeatBooster) {
            this.useHeatBooster = useHeatBooster;
            return this;
        }

        public Builder setHeatBoostTemperature(int heatBoostTemperature) {
            this.heatBoostTemperature = heatBoostTemperature;
            return this;
        }

        public Builder setHeatBoosterId(String heatBoosterId) {
            this.heatBoosterId = heatBoosterId;
            return this;
        }

        public Builder setTimerId(String timerId) {
            this.timerId = timerId;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
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

    private MyConfig(Builder builder){
        super(MyConfig.class, builder.id);
        this.builder = builder;
    }



    @Override
    public String service_pid() {
        return this.builder.servicePid;
    }

    @Override
    public String[] apartmentCords() {
        return this.builder.apartmentCords;
    }

    @Override
    public String[] apartmentToThermometer() {
        return this.builder.apartmentToThermometer;
    }

    @Override
    public String[] apartmentResponse() {
        return this.builder.apartmentResponse;
    }

    @Override
    public int setPointTemperature() {
        return this.builder.setPointTemperature;
    }

    @Override
    public String[] thresholdId() {
        return this.builder.thresholdId;
    }

    @Override
    public String heatPumpId() {
        return this.builder.heatPumpId;
    }

    @Override
    public double powerLevelPump() {
        return this.builder.powerLevelPump;
    }

    @Override
    public boolean useHeatBooster() {
        return this.builder.useHeatBooster;
    }

    @Override
    public int heatBoostTemperature() {
        return this.builder.heatBoostTemperature;
    }

    @Override
    public String heatBoosterId() {
        return this.builder.heatBoosterId;
    }

    @Override
    public String timerId() {
        return this.builder.timerId;
    }

    @Override
    public boolean enabled() {
        return this.builder.enabled;
    }

}
