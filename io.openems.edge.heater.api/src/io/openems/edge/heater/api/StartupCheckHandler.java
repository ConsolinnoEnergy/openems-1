package io.openems.edge.heater.api;

import io.openems.common.exceptions.OpenemsError;
import org.slf4j.Logger;

/**
 * This interface provides a static helper method to do a state check at module startup of a heater being controlled via
 * the Heater interface ENABLE_SIGNAL.
 * If the heater is detected to be running, keep it running by sending 'EnableSignal = true' to the heater once. This
 * avoids the heater always switching off when OpenEMS restarts, because the EnableSignal has the initial value ’false’.
 * By sending the EnableSignal to the heater once, the heater will stay on until the EnableSignal timer runs out. This
 * should give any controllers enough time to send that signal themselves and allow heater operation without unnecessary
 * disruptions.
 * Make sure the method is called only once. It needs to be called after the heater state is determined and just before
 * sending the first commands to the heater.
 */
public interface StartupCheckHandler {

    /**
     * Check the heater state. If the heater is running, send the enable signal to the heater and return true. Otherwise
     * return false
     *
     * @param heater the heater.
     * @param log the logger of the heater.
     * @return true if heater is running, false otherwise.
     */
    static boolean deviceAlreadyHeating(Heater heater, Logger log) {
        boolean turnOnHeater;
        HeaterState heaterState = HeaterState.valueOf(heater.getHeaterState().orElse(-1));
        switch (heaterState) {
            case RUNNING:
            case STARTING_UP_OR_PREHEAT:
                turnOnHeater = true;
                break;
            default:
                turnOnHeater = false;
        }
        if (turnOnHeater) {
            try {
                heater.getEnableSignalChannel().setNextWriteValue(true);
            } catch (OpenemsError.OpenemsNamedException e) {
                log.warn("Couldn't write in Channel " + e.getMessage());
            }
        }
        return turnOnHeater;
    }
}
