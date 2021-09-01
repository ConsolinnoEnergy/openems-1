package io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.helperclass;

import io.openems.common.exceptions.OpenemsError;
import org.joda.time.DateTime;

/**
 * The Interface of the LineCooler. Usually used by the {@link io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.HydraulicLineCoolerController}
 * It provides the ability to start or stop the Cooling Process.
 * Getting the LifeCycle to prevent hysteresis and allows to set only a Maximum and Minimum.
 */
public interface LineCooler {
    /**
     * Starts the Cooling process.
     *
     * @return true if successful.
     * @throws OpenemsError.OpenemsNamedException if e.g. a ChannelAddress or Component does not exist.
     */
    boolean startCooling() throws OpenemsError.OpenemsNamedException;

    /**
     * Stops the Cooling process.
     *
     * @param lifecycle the currentTime when the Stop Command was set -> prevent hysteresis, by checking the lifecycle
     *                  with the waitTime.
     * @return true on success.
     * @throws OpenemsError.OpenemsNamedException if the Component or Channel could not be found.
     */
    boolean stopCooling(DateTime lifecycle) throws OpenemsError.OpenemsNamedException;

    /**
     * Gets the Current LifeCycle.
     *
     * @return the LifeCycle
     */
    DateTime getLifeCycle();

    /**
     * Usually used as an internal Method. It Sets the LifeCycle of a LineCooler.
     *
     * @param lifeCycle the lifeCycle.
     */
    void setLifeCycle(DateTime lifeCycle);

    /**
     * Sets the Max and Min Value of either a {@link io.openems.edge.heatsystem.components.Valve} or Channel.
     * It does NOT Start a HeatingProcess.
     * By setting the values.
     *
     * @param max the max Value.
     * @param min the min Value.
     */
    void setMaxAndMinValues(Double max, Double min);


    /**
     * Writes the stored min and Max Value to the Component of the Line.
     */
    void onlyWriteMaxMinToLine();
}
