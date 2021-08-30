package io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.helperclass;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The AbstractLine Cooler is an abstract Class to help the HydraulicLineCooler, by providing methods, depending, if the
 * Cooler has a Valve, 4 Channel or 1 Channel.
 */
public abstract class AbstractLineCooler implements LineCooler {

    protected boolean useMinMax;
    private final boolean booleanControlled;
    private DateTime lifeCycle;
    protected double previouslyCheckedPowerLevel = 0;
    protected static final int FULL_POWER = 100;
    protected static final int DEFAULT_LAST_POWER_VALUE = 0;
    protected boolean isRunning;
    protected final Logger log = LoggerFactory.getLogger(AbstractLineCooler.class);

    protected AbstractLineCooler(boolean booleanControlled, boolean useMinMax) {
        this.booleanControlled = booleanControlled;
        this.useMinMax = useMinMax;
    }

    protected boolean isBooleanControlled() {
        return this.booleanControlled;
    }

    @Override
    public DateTime getLifeCycle() {
        return this.lifeCycle;
    }

    @Override
    public void setLifeCycle(DateTime lifeCycle) {
        this.lifeCycle = lifeCycle;
    }
}
