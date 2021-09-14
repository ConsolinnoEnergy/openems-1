package io.openems.edge.cooler.analogue.component;

import io.openems.common.exceptions.OpenemsError;

public interface AnalogueCoolerComponent {
    void startCooling(int powerToApply) throws OpenemsError.OpenemsNamedException;

    void stopCooling() throws OpenemsError.OpenemsNamedException;

    /**
     * Always PERCENT!
     * @return the percentPowerValue Applied
     * @throws OpenemsError.OpenemsNamedException if ChannelAddress not found.
     */
    int getCurrentPowerApplied() throws OpenemsError.OpenemsNamedException;
}


