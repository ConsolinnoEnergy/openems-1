package io.openems.edge.thermometer.api;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.ComponentManager;
import org.osgi.service.cm.ConfigurationException;

import java.util.HashMap;
import java.util.Map;

/**
 * This class helps to get the Correct Thermometer of the corresponding Heater as well as "setPoints" for the Thermometer.
 * Provides the methods to check if min or max temperature is reached --> therefore heater can activate/deactivate correctly.
 */

public class ThermometerWrapperForHeatingImpl extends AbstractThermometerWrapper implements ThermometerWrapper {

    public ThermometerWrapperForHeatingImpl(Thermometer activateThermometer, Thermometer deactivateThermometer, String activateValue, String deactivateValue, ComponentManager cpm)
            throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super(activateThermometer, deactivateThermometer, activateValue, deactivateValue, cpm);
    }

    public ThermometerWrapperForHeatingImpl(Thermometer activateThermometer, Thermometer deactivateThermometer, String activateValue, String deactivateValue, ComponentManager cpm, int activationOffset, int deactivationOffset)  throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super(activateThermometer, deactivateThermometer, activateValue, deactivateValue, cpm, activationOffset, deactivationOffset);
    }

    /**
     * Method, usually called by Heater Controller.
     * to determine if the enabled Value should be set to true within the heater.
     *
     * @return the result of the Comparison of the DeactivationThermometer Temperature Value and the stored deactivation Temperature.
     */

    @Override
    public boolean shouldDeactivate() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.getDeactivationThermometer().getTemperatureValue() >= this.getDeactivationTemperature() + super.deactivationOffset;
    }

    /**
     * Method, usually called by Heater Controller.
     * to determine if the enabled Value should be set to true within the heater.
     *
     * @return the result of the Comparison of the ActivationThermometer Temperature Value and the stored activation Temperature.
     */

    @Override
    public boolean shouldActivate() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.getActivationThermometer().getTemperatureValue() <= this.getActivationTemperature() + super.activationOffset;
    }

}
