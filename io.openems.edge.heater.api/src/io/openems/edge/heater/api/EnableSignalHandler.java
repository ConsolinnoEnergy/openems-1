package io.openems.edge.heater.api;

/**
 * The Interface of the EnableSignalHandler. It allows for easy and uniform handling of the EnableSignal in the heater
 * component.
 * The convention of the EnableSignal channel is to write ’true’ in nextWrite to turn the device on, or nothing to turn
 * it off (after the timer runs out). This way multiple controllers can access the device without needing controller
 * hierarchy. For a more detailed description see {@link io.openems.edge.heater.api.EnableSignalHandlerImpl}.
 * The EnableSignalHandler provides an implementation of this functionality. The handler needs to be initialized with a
 * timer (see {@link io.openems.edge.timer.api.Timer}). Once initialized, the method deviceShouldBeHeating() can be
 * called to query what the EnableSignal wants the heater to do.
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
