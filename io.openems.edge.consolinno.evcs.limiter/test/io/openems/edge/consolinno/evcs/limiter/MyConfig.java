package io.openems.edge.consolinno.evcs.limiter;

import io.openems.edge.common.test.AbstractComponentConfig;
import io.openems.edge.evcs.api.GridVoltage;


@SuppressWarnings("all")

public class MyConfig extends AbstractComponentConfig implements Config {


    protected static class Builder {

        private String[] evcss;
        private boolean useMeter;
        private String meter;
        private boolean symmetry;
        private int offTime;
        private int phaseLimit;
        private int powerLimit;
        private int priorityCurrent;
        private GridVoltage grid;
        private String id;
        private String alias;
        private boolean enabled = true;

        private Builder() {
        }

        public Builder setId(String id) {
            this.id = id;
            return this;
        }


        public Builder setEvcss(String[] evcss) {
            this.evcss = evcss;
            return this;
        }

        public Builder setUseMeter(boolean useMeter) {
            this.useMeter = useMeter;
            return this;
        }

        public Builder setMeter(String meter) {
            this.meter = meter;
            return this;
        }

        public Builder setSymmetry(boolean symmetry) {
            this.symmetry = symmetry;
            return this;
        }

        public Builder setOffTime(int offTime) {
            this.offTime = offTime;
            return this;
        }

        public Builder setPhaseLimit(int phaseLimit) {
            this.phaseLimit = phaseLimit;
            return this;
        }

        public Builder setPowerLimit(int powerLimit) {
            this.powerLimit = powerLimit;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setPriorityCurrent(int priorityCurrent) {
            this.priorityCurrent = priorityCurrent;
            return this;
        }

        public Builder setGrid(GridVoltage grid) {
            this.grid = grid;
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
    public boolean enabled() {
        return this.builder.enabled;
    }

    @Override
    public GridVoltage grid() {
        return this.builder.grid;
    }

    @Override
    public String[] evcss() {
        return this.builder.evcss;
    }

    @Override
    public boolean useMeter() {
        return this.builder.useMeter;
    }

    @Override
    public String meter() {
        return this.builder.meter;
    }

    @Override
    public boolean symmetry() {
        return this.builder.symmetry;
    }

    @Override
    public int offTime() {
        return this.builder.offTime;
    }

    @Override
    public int phaseLimit() {
        return this.builder.phaseLimit;
    }

    @Override
    public int powerLimit() {
        return this.builder.powerLimit;
    }

    @Override
    public int priorityCurrent() {
        return this.builder.priorityCurrent;
    }
}