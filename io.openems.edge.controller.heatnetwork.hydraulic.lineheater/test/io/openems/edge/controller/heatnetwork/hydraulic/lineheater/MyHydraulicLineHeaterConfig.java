package io.openems.edge.controller.heatnetwork.hydraulic.lineheater;

import io.openems.edge.common.test.AbstractComponentConfig;


@SuppressWarnings("all")

public class MyHydraulicLineHeaterConfig extends AbstractComponentConfig implements ConfigLineHeater {


    protected static class Builder {

        private String id;
        private String alias;
        private boolean enabled;
        private String service_pid;
        private String tempSensorReference;
        private String temperatureDefault;
        private LineHeaterType lineHeaterType;
        private boolean valueToWriteIsBoolean;
        private String channelAddress;
        private String[] channels;
        private String valveBypass;
        private String timerId;
        private int timeoutMaxRemote;
        private int timeoutRestartCycle;
        private boolean shouldFallback;
        private int minuteFallbackStart;
        private int minuteFallbackStop;
        private boolean useMinMax;
        private double maxValveValue;
        private double minValveValue;
        private boolean maxMinOnly;
        private boolean useDecentralizedHeater;
        private String decentralizedHeaterReference;
        private DecentralizedHeaterReactionType reactionType;


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

        public Builder setAlias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder setService_pid(String service_pid) {
            this.service_pid = service_pid;
            return this;
        }

        public Builder setTempSensorReference(String tempSensorReference) {
            this.tempSensorReference = tempSensorReference;
            return this;
        }

        public Builder setTemperatureDefault(String temperatureDefault) {
            this.temperatureDefault = temperatureDefault;
            return this;
        }

        public Builder setLineHeaterType(LineHeaterType lineHeaterType) {
            this.lineHeaterType = lineHeaterType;
            return this;
        }

        public Builder setValueToWriteIsBoolean(boolean valueToWriteIsBoolean) {
            this.valueToWriteIsBoolean = valueToWriteIsBoolean;
            return this;
        }

        public Builder setChannelAddress(String channelAddress) {
            this.channelAddress = channelAddress;
            return this;
        }

        public Builder setChannels(String[] channels) {
            this.channels = channels;
            return this;
        }

        public Builder setValveBypass(String valveBypass) {
            this.valveBypass = valveBypass;
            return this;
        }

        public Builder setTimerId(String timerId) {
            this.timerId = timerId;
            return this;
        }

        public Builder setTimeoutMaxRemote(int timeoutMaxRemote) {
            this.timeoutMaxRemote = timeoutMaxRemote;
            return this;
        }

        public Builder setTimeoutRestartCycle(int timeoutRestartCycle) {
            this.timeoutRestartCycle = timeoutRestartCycle;
            return this;
        }

        public Builder setShouldFallback(boolean shouldFallback) {
            this.shouldFallback = shouldFallback;
            return this;
        }

        public Builder setMinuteFallbackStart(int minuteFallbackStart) {
            this.minuteFallbackStart = minuteFallbackStart;
            return this;
        }

        public Builder setMinuteFallbackStop(int minuteFallbackStop) {
            this.minuteFallbackStop = minuteFallbackStop;
            return this;
        }

        public Builder setUseMinMax(boolean useMinMax) {
            this.useMinMax = useMinMax;
            return this;
        }

        public Builder setMaxValveValue(double maxValveValue) {
            this.maxValveValue = maxValveValue;
            return this;
        }

        public Builder setMinValveValue(double minValveValue) {
            this.minValveValue = minValveValue;
            return this;
        }

        public Builder setMaxMinOnly(boolean maxMinOnly) {
            this.maxMinOnly = maxMinOnly;
            return this;
        }

        public Builder setUseDecentralizedHeater(boolean useDecentralizedHeater) {
            this.useDecentralizedHeater = useDecentralizedHeater;
            return this;
        }

        public Builder setDecentralizedHeaterReference(String decentralizedHeaterReference) {
            this.decentralizedHeaterReference = decentralizedHeaterReference;
            return this;
        }

        public Builder setReactionType(DecentralizedHeaterReactionType reactionType) {
            this.reactionType = reactionType;
            return this;
        }

        public MyHydraulicLineHeaterConfig build() {
            return new MyHydraulicLineHeaterConfig(this);
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

    private MyHydraulicLineHeaterConfig(Builder builder) {
        super(MyHydraulicLineHeaterConfig.class, builder.id);
        this.builder = builder;
    }


    @Override
    public String service_pid() {
        return this.builder.service_pid;
    }

    @Override
    public String tempSensorReference() {
        return this.builder.tempSensorReference;
    }

    @Override
    public String temperatureDefault() {
        return this.builder.temperatureDefault;
    }

    @Override
    public LineHeaterType lineHeaterType() {
        return this.builder.lineHeaterType;
    }

    @Override
    public boolean valueToWriteIsBoolean() {
        return this.builder.valueToWriteIsBoolean;
    }

    @Override
    public String channelAddress() {
        return this.builder.channelAddress;
    }

    @Override
    public String[] channels() {
        return this.builder.channels;
    }

    @Override
    public String valveBypass() {
        return this.builder.valveBypass;
    }

    @Override
    public String timerId() {
        return this.builder.timerId;
    }

    @Override
    public int timeoutMaxRemote() {
        return this.builder.timeoutMaxRemote;
    }

    @Override
    public int timeoutRestartCycle() {
        return this.builder.timeoutRestartCycle;
    }

    @Override
    public boolean shouldFallback() {
        return this.builder.shouldFallback;
    }

    @Override
    public int minuteFallbackStart() {
        return this.builder.minuteFallbackStart;
    }

    @Override
    public int minuteFallbackStop() {
        return this.builder.minuteFallbackStop;
    }

    @Override
    public boolean useMinMax() {
        return this.builder.useMinMax;
    }

    @Override
    public double maxValveValue() {
        return this.builder.maxValveValue;
    }

    @Override
    public double minValveValue() {
        return this.builder.minValveValue;
    }

    @Override
    public boolean maxMinOnly() {
        return this.builder.maxMinOnly;
    }

    @Override
    public boolean useDecentralizedHeater() {
        return this.builder.useDecentralizedHeater;
    }

    @Override
    public String decentralizedHeaterReference() {
        return this.builder.decentralizedHeaterReference;
    }

    @Override
    public DecentralizedHeaterReactionType reactionType() {
        return this.builder.reactionType;
    }

    @Override
    public boolean enabled() {
        return this.builder.enabled;
    }
}
