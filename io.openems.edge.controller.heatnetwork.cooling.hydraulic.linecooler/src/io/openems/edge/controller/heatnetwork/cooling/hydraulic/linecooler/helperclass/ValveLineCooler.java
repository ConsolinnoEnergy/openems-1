package io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.helperclass;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import org.joda.time.DateTime;

/**
 * the ValveLineCooler. An extension of the AbstractLineCooler. It controls a Valve, by setting min and max values,
 * as well was the powerLevel.
 */
public class ValveLineCooler extends AbstractLineCooler {

    private final HydraulicComponent valve;
    private Double max;
    private Double min;


    public ValveLineCooler(boolean booleanControlled, HydraulicComponent valve, boolean useMinMax) {
        super(booleanControlled, useMinMax);
        this.valve = valve;
    }

    /**
     * Starts the Cooling process.
     *
     * @return true if successful.
     * @throws OpenemsError.OpenemsNamedException if e.g. a ChannelAddress or Component does not exist.
     */

    @Override
    public boolean startCooling() throws OpenemsError.OpenemsNamedException {

        if (this.isRunning == false || this.valve.powerLevelReached() == false) {
            if (super.useMinMax) {
                this.valve.maxValueChannel().setNextWriteValue(this.max);
                this.valve.minValueChannel().setNextWriteValue(this.min);
            }
            this.isRunning = true;
            //Either fullpower set .--> ValveManager handles OR you could change Valve directly
            //this.valveBypass.setPowerLevelPercent().setNextValue((FULL_POWER));
            if (super.isBooleanControlled()) {
                this.valve.setPointPowerLevelChannel().setNextWriteValueFromObject(true);
            } else {
                this.valve.setPointPowerLevelChannel().setNextWriteValueFromObject(FULL_POWER);
            }

            return true;
        }

        return false;
    }

    /**
     * Stops the Cooling process.
     *
     * @param lifecycle the currentTime when the Stop Command was set -> prevent hysteresis, by checking the lifecycle
     *                  with the waitTime.
     * @return true on success.
     * @throws OpenemsError.OpenemsNamedException if the Component or Channel could not be found.
     */

    @Override
    public boolean stopCooling(DateTime lifecycle) throws OpenemsError.OpenemsNamedException {

        if (this.isRunning || this.valve.powerLevelReached() == false) {
            this.isRunning = false;
            this.valve.setPointPowerLevelChannel().setNextWriteValueFromObject(0);
            this.setLifeCycle(lifecycle);
            return true;
        }
        return false;
    }

    /**
     * Sets the Max and Min Value of either a {@link io.openems.edge.heatsystem.components.HydraulicComponent} or Channel.
     * It does NOT Start a HeatingProcess.
     * By setting the values.
     *
     * @param max the max Value.
     * @param min the min Value.
     */

    @Override
    public void setMaxAndMinValues(Double max, Double min) {
        this.max = max;
        this.min = min;
    }

    /**
     * Writes the stored min and Max Value to the Component of the Line.
     */
    @Override
    public void onlyWriteMaxMinToLine() {
        try {
            this.valve.maxValueChannel().setNextWriteValue(this.max);
            this.valve.minValueChannel().setNextWriteValue(this.min);
        } catch (OpenemsError.OpenemsNamedException e) {
            super.log.warn("Couldn't write the min, max Value. Reason: " + e.getMessage());
        }
    }
}
