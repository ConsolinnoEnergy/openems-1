package io.openems.edge.thermometer.api;

import io.openems.common.exceptions.OpenemsError;
import org.osgi.service.cm.ConfigurationException;


public interface ThermometerWrapper {
    /**
     * Refreshes the Thermometer References in case they were changed/disabled/renewed.
     *
     * @param thermometerType the ThermometerType, to identify the Map Value.
     * @param newThermometer  the new Thermometer put into the stored Maps,.
     */

    void renewThermometer(ThermometerType thermometerType, Thermometer newThermometer);

    /**
     * Getter for the deactivation Thermometer.
     *
     * @return the Deactivation {@link Thermometer}
     */

    Thermometer getDeactivationThermometer();

    /**
     * Getter for the activation Thermometer.
     *
     * @return the Activation {@link Thermometer}
     */

    Thermometer getActivationThermometer();

    /**
     * Method, usually called by Heater Controller.
     * to determine if the enabled Value should be set to true within the heater.
     *
     * @return the result of the Comparison of the DeactivationThermometer Temperature Value and the stored deactivation Temperature.
     */

    boolean shouldDeactivate() throws OpenemsError.OpenemsNamedException, ConfigurationException;

    /**
     * Method, usually called by Heater Controller.
     * to determine if the enabled Value should be set to true within the heater.
     *
     * @return the result of the Comparison of the ActivationThermometer Temperature Value and the stored activation Temperature.
     */

    boolean shouldActivate() throws OpenemsError.OpenemsNamedException, ConfigurationException;

}
