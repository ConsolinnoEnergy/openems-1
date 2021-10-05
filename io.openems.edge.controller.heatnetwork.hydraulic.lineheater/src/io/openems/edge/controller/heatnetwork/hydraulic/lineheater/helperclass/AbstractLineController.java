package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

/**
 * A LineHeater. This is the Parent of impl. of the LineHeaterHelperClasses. Helps out the HydraulicLineHeaterController.
 * Providing basic methods.
 * Such as starting and stopping the HeatingProgress.
 */
public abstract class AbstractLineController implements LineController {

    protected boolean useMinMax;
    private final boolean booleanControlled;
    protected double previouslyCheckedPowerLevel = 0;
    protected static final int FULL_POWER = 100;
    protected static final int DEFAULT_LAST_POWER_VALUE = 0;
    protected boolean isRunning;

    protected AbstractLineController(boolean booleanControlled, boolean useMinMax) {
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

}
