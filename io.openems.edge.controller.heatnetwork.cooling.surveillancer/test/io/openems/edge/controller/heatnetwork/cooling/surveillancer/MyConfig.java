package io.openems.edge.controller.heatnetwork.cooling.surveillancer;

import io.openems.edge.common.test.AbstractComponentConfig;


@SuppressWarnings("all")

public class MyConfig extends AbstractComponentConfig implements Config {


    protected static class Builder {

        public String activeValue;
        public String passiveValue;
        private String[] inputRequest;
        private String[] inputWatchdogs;
        private String[] output;
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

        public Builder setInputRequest(String[] inputRequest) {
            this.inputRequest = inputRequest;
            return this;
        }

        public Builder setInputWatchdogs(String[] inputWatchdogs) {
            this.inputWatchdogs = inputWatchdogs;
            return this;
        }

        public Builder setOutput(String[] output) {
            this.output = output;
            return this;
        }

        public Builder setActiveValue(String activeValue) {
            this.activeValue = activeValue;
            return this;
        }

        public Builder setPassiveValue(String passiveValue) {
            this.passiveValue = passiveValue;
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
    public String[] inputRequest() {
        return this.builder.inputRequest;
    }

    @Override
    public String[] inputWatchdogs() {
        return this.builder.inputWatchdogs;
    }

    @Override
    public String[] output() {
       return this.builder.output;
    }

    @Override
    public String activeValue() {
        return this.builder.activeValue;
    }

    @Override
    public String passiveValue() {
        return this.builder.passiveValue;
    }

    @Override
    public boolean enabled() {
        return this.builder.enabled;
    }
}
