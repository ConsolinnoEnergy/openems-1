package io.openems.edge.controller.heatnetwork.valve.staticvalve;

import io.openems.edge.common.test.AbstractComponentConfig;


@SuppressWarnings("all")
public class MyConfig extends AbstractComponentConfig implements Config {


    protected static class Builder {
        private String id;
        private String servicePid;
        private String alias;
        private String valveToControl;
        private String[] temperaturePositionMap;
        private String controlType;
        private String thermometerId;
        private boolean autorun;
        private boolean allowForcing;
        private boolean shouldCloseWhenNoSignal;
        private int defaultPosition;
        private boolean useFallback;
        private boolean enabled = true;

        private Builder() {
        }


        public Builder setId(String id) {
            this.id = id;
            return this;
        }

        public Builder setServicePid(String Pid) {
            this.servicePid = Pid;
            return this;
        }

        public MyConfig build() {
            return new MyConfig(this);
        }

        public Builder setValveToControl(String valveToControl) {
            this.valveToControl = valveToControl;
            return this;
        }

        public Builder settemperaturePositionMap(String[] atemperaturePositionMap) {
            this.temperaturePositionMap = atemperaturePositionMap;
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

        public Builder setUseFallback(boolean useFallback) {
            this.useFallback = useFallback;
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

    private MyConfig(Builder builder) {
        super(Config.class, builder.id);
        this.builder = builder;
    }


    @Override
    public String service_pid() {
        return this.builder.servicePid;
    }

    @Override
    public String valveToControl() {
        return this.builder.valveToControl;
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
    public boolean useFallback() {
        return this.builder.useFallback;
    }
    @Override
    public boolean enabled() {
        return this.builder.enabled;
    }
}
