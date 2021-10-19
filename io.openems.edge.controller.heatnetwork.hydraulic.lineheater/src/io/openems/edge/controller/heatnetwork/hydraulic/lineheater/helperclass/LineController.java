package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

import io.openems.common.exceptions.OpenemsError;
import org.joda.time.DateTime;

/**
 * A LineHeater. This is the Interface to help out the HydraulicLineHeaterController. Providing basic methods.
 * Such as starting and stopping the HeatingProgress.
 */
public interface LineController {
    /**
     * Starts the heating progress.
     *
     * @return true on success
     * @throws OpenemsError.OpenemsNamedException thrown if component or channelAddress cannot be found.
     */
    boolean startProcess() throws OpenemsError.OpenemsNamedException;

    /**
     * Stops the heating progress.
     *
     * @return true on success
     * @throws OpenemsError.OpenemsNamedException thrown if component or channelAddress cannot be found.
     */
    boolean stopProcess() throws OpenemsError.OpenemsNamedException;

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
