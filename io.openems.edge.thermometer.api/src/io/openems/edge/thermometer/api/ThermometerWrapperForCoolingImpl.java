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

public class ThermometerWrapperForCoolingImpl extends AbstractThermometerWrapper implements ThermometerWrapper {




    public ThermometerWrapperForCoolingImpl(Thermometer activateThermometer, Thermometer deactivateThermometer, String activateValue, String deactivateValue, ComponentManager cpm)
            throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super(activateThermometer, deactivateThermometer, activateValue, deactivateValue, cpm);
    }

    /**
     * Refreshes the Thermometer References in case they were changed/disabled/renewed.
     *
     * @param thermometerType the ThermometerType, to identify the Map Value.
     * @param newThermometer  the new Thermometer put into the stored Maps,.
     */

    @Override
    public void renewThermometer(ThermometerType thermometerType, Thermometer newThermometer) {
        Thermometer oldThermometer = this.getThermometerKindThermometerMap().get(thermometerType);
        this.getThermometerKindThermometerMap().remove(thermometerType);
        this.getThermometerKindThermometerMap().put(thermometerType, newThermometer);
        ThermometerValueWrapper value = this.getThermometerAndValue().get(oldThermometer);
        this.getThermometerAndValue().remove(oldThermometer);
        this.getThermometerAndValue().put(newThermometer, value);
    }


    /**
     * Method, usually called by Heater Controller.
     * to determine if the enabled Value should be set to true within the heater.
     *
     * @return the result of the Comparison of the DeactivationThermometer Temperature Value and the stored deactivation Temperature.
     */

    @Override
    public boolean shouldDeactivate() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.getDeactivationThermometer().getTemperatureValue() <= this.getDeactivationTemperature();
    }

    /**
     * Method, usually called by Heater Controller.
     * to determine if the enabled Value should be set to true within the heater.
     *
     * @return the result of the Comparison of the ActivationThermometer Temperature Value and the stored activation Temperature.
     */

    @Override
    public boolean shouldActivate() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.getActivationThermometer().getTemperatureValue() >= this.getActivationTemperature();
    }

}
