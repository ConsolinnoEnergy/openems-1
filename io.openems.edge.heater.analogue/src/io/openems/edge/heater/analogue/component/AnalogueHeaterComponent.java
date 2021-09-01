package io.openems.edge.heater.analogue.component;

import io.openems.common.exceptions.OpenemsError;

public interface AnalogueHeaterComponent {
    void startHeating(int powerToApply) throws OpenemsError.OpenemsNamedException;

    void stopHeating() throws OpenemsError.OpenemsNamedException;

    /**
     * Always PERCENT!
     * @return the percentPowerValue Applied
     * @throws OpenemsError.OpenemsNamedException if ChannelAddress not found.
     */
    int getCurrentPowerApplied() throws OpenemsError.OpenemsNamedException;
}
