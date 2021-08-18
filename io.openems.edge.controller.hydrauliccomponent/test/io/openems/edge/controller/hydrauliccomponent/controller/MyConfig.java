package io.openems.edge.controller.hydrauliccomponent.controller;

import io.openems.edge.common.test.AbstractComponentConfig;


@SuppressWarnings("all")

public class MyConfig extends AbstractComponentConfig implements Config {


    protected static class Builder {

        public String service_pid;
        public String componentToControl;
        public String[] temperaturePositionMap;
        public String controlType;
        public String thermometerId;
        public boolean autorun;
        public boolean allowForcing;
        public boolean shouldCloseWhenNoSignal;
        public int defaultPosition;
        public String timerForRunning;
        public int waitForSignalAfterActivation;
        public boolean useFallback;
        public String timerForFallback;
        public int fallbackRunTime;
        private String id;
        private String alias;
        private boolean enabled = true;

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

        public Builder setService_pid(String service_pid) {
            this.service_pid = service_pid;
            return this;
        }

        public Builder setComponentToControl(String componentToControl) {
            this.componentToControl = componentToControl;
            return this;
        }

        public Builder setTemperaturePositionMap(String[] temperaturePositionMap) {
            this.temperaturePositionMap = temperaturePositionMap;
            return this;
        }

        public Builder setControlType(String controlType) {
            this.controlType = controlType;
            return this;
        }

        public Builder setThermometerId(String thermometerId) {
            this.thermometerId = thermometerId;
            return this;
        }

        public Builder setAutorun(boolean autorun) {
            this.autorun = autorun;
            return this;
        }

        public Builder setAllowForcing(boolean allowForcing) {
            this.allowForcing = allowForcing;
            return this;
        }

        public Builder setShouldCloseWhenNoSignal(boolean shouldCloseWhenNoSignal) {
            this.shouldCloseWhenNoSignal = shouldCloseWhenNoSignal;
            return this;
        }

        public Builder setDefaultPosition(int defaultPosition) {
            this.defaultPosition = defaultPosition;
            return this;
        }

        public Builder setTimerForRunning(String timerForRunning) {
            this.timerForRunning = timerForRunning;
            return this;
        }

        public Builder setWaitForSignalAfterActivation(int waitForSignalAfterActivation) {
            this.waitForSignalAfterActivation = waitForSignalAfterActivation;
            return this;
        }

        public Builder setUseFallback(boolean useFallback) {
            this.useFallback = useFallback;
            return this;
        }

        public Builder setTimerForFallback(String timerForFallback) {
            this.timerForFallback = timerForFallback;
            return this;
        }

        public Builder setFallbackRunTime(int fallbackRunTime) {
            this.fallbackRunTime = fallbackRunTime;
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
    public String componentToControl() {
        return this.builder.componentToControl;
    }

    @Override
    public String[] temperaturePositionMap() {
        return this.builder.temperaturePositionMap;
    }

    @Override
    public String controlType() {
        return this.builder.controlType;
    }

    @Override
    public String thermometerId() {
        return this.builder.thermometerId;
    }

    @Override
    public boolean autorun() {
        return this.builder.autorun;
    }

    @Override
    public boolean allowForcing() {
        return this.builder.allowForcing;
    }

    @Override
    public boolean shouldCloseWhenNoSignal() {
        return this.builder.shouldCloseWhenNoSignal;
    }

    @Override
    public int defaultPosition() {
        return this.builder.defaultPosition;
    }

    @Override
    public String timerForRunning() {
        return this.builder.timerForRunning;
    }

    @Override
    public int waitForSignalAfterActivation() {
        return this.builder.waitForSignalAfterActivation;
    }

    @Override
    public boolean useFallback() {
        return this.builder.useFallback;
    }

    @Override
    public String timerForFallback() {
        return this.builder.timerForFallback;
    }

    @Override
    public int fallbackRunTime() {
        return this.builder.fallbackRunTime;
    }

    @Override
    public boolean enabled() {
        return this.builder.enabled;
    }
}
