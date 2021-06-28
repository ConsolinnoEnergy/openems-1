package io.openems.edge.heater.api;

/**
 * The Interface of the EnableSignalHandler. It allows for easy and correct use of the EnableSignal.
 * The convention of the EnableSignal channel is to write ’true’ in nextWrite to turn the device on, or nothing to turn
 * it off (after the timer runs out). This way multiple controllers can access the device without needing controller
 * hierarchy. For a more detailed description see {@link io.openems.edge.heater.api.EnableSignalHandlerImpl}.
 * The EnableSignalHandler provides an implementation of this functionality. The handler needs to be initialized with a
 * timer (see {@link io.openems.edge.timer.api.Timer}). Once initialized, the method deviceShouldBeHeating() can be
 * called to query whether the device should be on on not.
 */
public interface EnableSignalHandler {

    /**
     * Checks if the device should be on or not in accordance with the EnableSignal rules.
     *
     * @param heaterComponent the Component that implements the {@link Heater} interface.
     * @return true if EnableSignal is active or was active and the wait time is not up.
     */
    boolean deviceShouldBeHeating(Heater heaterComponent);
}
