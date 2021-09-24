package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

import org.joda.time.DateTime;

/**
 * A LineHeater. This is the Parent of impl. of the LineHeaterHelperClasses. Helps out the HydraulicLineHeaterController.
 * Providing basic methods.
 * Such as starting and stopping the HeatingProgress.
 */
public abstract class AbstractLineHeater implements LineHeater {

    protected boolean useMinMax;
    private final boolean booleanControlled;
    private DateTime lifeCycle;
    protected double previouslyCheckedPowerLevel = 0;
    protected static final int FULL_POWER = 100;
    protected static final int DEFAULT_LAST_POWER_VALUE = 0;
    protected boolean isRunning;

    protected AbstractLineHeater(boolean booleanControlled, boolean useMinMax) {
        this.booleanControlled = booleanControlled;
        this.useMinMax = useMinMax;
    }

    /**
     * Is the WriteAddress expected to be from OpenEmsType Boolean.
     *
     * @return true
     */
    protected boolean isBooleanControlled() {
        return this.booleanControlled;
    }

    /**
     * Gets the last LifeCycle of the LineHeater.
     *
     * @return the LifeCycle.
     */
    @Override
    public DateTime getLifeCycle() {
        return this.lifeCycle;
    }


    /**
     * Sets the LifeCycle of the LineHeater.
     *
     * @param lifeCycle the current LifeCycle.
     */
    @Override
    public void setLifeCycle(DateTime lifeCycle) {
        this.lifeCycle = lifeCycle;
    }
}
