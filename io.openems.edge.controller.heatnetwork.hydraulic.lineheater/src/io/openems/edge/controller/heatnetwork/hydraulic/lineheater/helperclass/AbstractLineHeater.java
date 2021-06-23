package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

import org.joda.time.DateTime;

public abstract class AbstractLineHeater implements LineHeater {

    protected boolean useMinMax;
    private boolean booleanControlled;
    private DateTime lifeCycle;
    protected double previouslyCheckedPowerLevel = 0;
    protected static final int FULL_POWER = 100;
    protected static final int DEFAULT_LAST_POWER_VALUE = 0;
    protected boolean isRunning;

    protected AbstractLineHeater(boolean booleanControlled, boolean useMinMax) {
        this.booleanControlled = booleanControlled;
        this.useMinMax = useMinMax;
    }

    protected boolean isBooleanControlled() {
        return this.booleanControlled;
    }

    @Override
    public DateTime getLifeCycle() {
        return lifeCycle;
    }

    @Override
    public void setLifeCycle(DateTime lifeCycle) {
        this.lifeCycle = lifeCycle;
    }
}
