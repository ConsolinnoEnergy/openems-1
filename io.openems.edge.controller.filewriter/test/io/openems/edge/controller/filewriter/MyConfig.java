package io.openems.edge.controller.filewriter;

import io.openems.edge.common.test.AbstractComponentConfig;


@SuppressWarnings("all")

public class MyConfig extends AbstractComponentConfig implements Config {


    protected static class Builder {

        public String fileLocation;
        public String service_pid;
        public String otherComponentId;
        public String[] channels;
        public String[] keyValuePairs;
        private String id;
        private String alias;
        private boolean enabled = true;
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

        public Builder setFileLocation(String fileLocation) {
            this.fileLocation = fileLocation;
            return this;
        }

        public Builder setService_pid(String service_pid) {
            this.service_pid = service_pid;
            return this;
        }

        public Builder setOtherComponentId(String otherComponentId) {
            this.otherComponentId = otherComponentId;
            return this;
        }

        public Builder setChannels(String[] channels) {
            this.channels = channels;
            return this;
        }

        public Builder setKeyValuePairs(String[] keyValuePairs) {
            this.keyValuePairs = keyValuePairs;
            return this;
        }

        public Builder setAlias(String alias) {
            this.alias = alias;
            return this;
        }

        public Builder setConfigurationDone(boolean configurationDone) {
            this.configurationDone = configurationDone;
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
    public String otherComponentId() {
        return this.builder.otherComponentId;
    }

    @Override
    public String[] channels() {
        return this.builder.channels;
    }

    @Override
    public String[] keyValuePairs() {
        return this.builder.keyValuePairs;
    }

    @Override
    public String fileLocation() {
        return this.builder.fileLocation;
    }

    @Override
    public boolean configurationDone() {
        return this.builder.configurationDone;
    }

    @Override
    public boolean enabled() {
        return this.builder.enabled;
    }
}
