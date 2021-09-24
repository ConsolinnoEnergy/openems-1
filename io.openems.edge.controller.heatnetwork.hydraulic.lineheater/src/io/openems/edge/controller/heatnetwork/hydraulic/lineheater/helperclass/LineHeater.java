package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

import io.openems.common.exceptions.OpenemsError;
import org.joda.time.DateTime;

/**
 * A LineHeater. This is the Interface to help out the HydraulicLineHeaterController. Providing basic methods.
 * Such as starting and stopping the HeatingProgress.
 */
public interface LineHeater {
    /**
     * Starts the heating progress.
     *
     * @return true on success
     * @throws OpenemsError.OpenemsNamedException thrown if component or channelAddress cannot be found.
     */
    boolean startHeating() throws OpenemsError.OpenemsNamedException;

    /**
     * Stops the heating progress.
     *
     * @param lifecycle the lifecycle. Tells the controller, when it was the last time the lineHeater was active.
     * @return true on success
     * @throws OpenemsError.OpenemsNamedException thrown if component or channelAddress cannot be found.
     */
    boolean stopHeating(DateTime lifecycle) throws OpenemsError.OpenemsNamedException;

    /**
     * Gets the last LifeCycle of the LineHeater.
     *
     * @return the LifeCycle.
     */
    DateTime getLifeCycle();

    /**
     * Sets the LifeCycle of the LineHeater.
     *
     * @param lifeCycle the current LifeCycle.
     */
    void setLifeCycle(DateTime lifeCycle);

    /**
     * Sets the Maximum and Minimum Value for the LinHeater.
     *
     * @param max the maximum value.
     * @param min the minimum value.
     */
    void setMaxAndMin(Double max, Double min);

    /**
     * Sets only the stored min and max value of the LineHeater.
     */
    void onlySetMaxMin();
}
