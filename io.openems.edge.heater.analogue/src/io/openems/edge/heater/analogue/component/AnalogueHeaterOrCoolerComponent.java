package io.openems.edge.heater.analogue.component;

import io.openems.common.exceptions.OpenemsError;

/**
 * The AnalogueHeaterComponent Interface. This is needed by the {@link io.openems.edge.heater.analogue.AnalogueHeater}
 * to start and stop the Heating Process as well as getting the Currently Applied Power (-> just beacuse you are setting
 * a PowerValue, fluctuations, errors etc can occur)
 */
public interface AnalogueHeaterOrCoolerComponent {
    /**
     * Starts the Heating Process with a given PowerValue which either can be percent or a KW value depending on the
     * {@link ControlType}
     * @param powerToApply the powerValue that will be applied
     * @throws OpenemsError.OpenemsNamedException if the ChannelAddress couldn't be found
     */
    void startProcess(int powerToApply) throws OpenemsError.OpenemsNamedException;

    /**
     * Stops the Heating Process with the MinPowerValue.
     * @throws OpenemsError.OpenemsNamedException if the ChannelAddress cannot be found.
     */
    void stopProcess() throws OpenemsError.OpenemsNamedException;

    /**
     * Gets the currently Applied Power to the analogueDevice.
     * The Value will always be a percent Value.
     * @return the percentPowerValue Applied
     * @throws OpenemsError.OpenemsNamedException if ChannelAddress not found.
     */
    int getCurrentPowerApplied() throws OpenemsError.OpenemsNamedException;
}
