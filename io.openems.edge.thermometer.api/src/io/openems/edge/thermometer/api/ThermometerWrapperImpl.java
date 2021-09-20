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

public class ThermometerWrapperImpl implements ThermometerWrapper {

    private final ComponentManager cpm;
    //Map the Thermometer to their min/max Value
    private final Map<Thermometer, ThermometerValueWrapper> thermometerAndValue = new HashMap<>();
    //thermometerType == Activate/Deactivate on HeatControl. Mapped thermometerType to Thermometer
    private final Map<ThermometerType, Thermometer> thermometerTypeThermometerMap = new HashMap<>();


    public ThermometerWrapperImpl(Thermometer activateThermometer, Thermometer deactivateThermometer, String activateValue, String deactivateValue, ComponentManager cpm)
            throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.cpm = cpm;
        this.thermometerTypeThermometerMap.put(ThermometerType.ACTIVATE_THERMOMETER, activateThermometer);
        this.thermometerAndValue.put(activateThermometer, new ThermometerValueWrapper(activateValue));
        this.thermometerTypeThermometerMap.put(ThermometerType.DEACTIVATE_THERMOMETER, deactivateThermometer);
        this.thermometerAndValue.put(deactivateThermometer, new ThermometerValueWrapper(deactivateValue));
        this.thermometerAndValue.get(activateThermometer).validateChannelAndGetValue(this.cpm);
        this.thermometerAndValue.get(deactivateThermometer).validateChannelAndGetValue(this.cpm);
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
     * Getter for the deactivation Thermometer.
     *
     * @return the Deactivation {@link Thermometer}
     */
    @Override
    public Thermometer getDeactivationThermometer() {
        return this.thermometerTypeThermometerMap.get(ThermometerType.DEACTIVATE_THERMOMETER);
    }

    /**
     * Getter for the activation Thermometer.
     *
     * @return the Activation {@link Thermometer}
     */

    @Override
    public Thermometer getActivationThermometer() {
        return this.thermometerTypeThermometerMap.get(ThermometerType.ACTIVATE_THERMOMETER);
    }

    /**
     * Internal Method to get the Activation Temperature.
     *
     * @return the ActivationTemperature value.
     */

    private int getActivationTemperature() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.thermometerAndValue.get(this.thermometerTypeThermometerMap.get(ThermometerType.ACTIVATE_THERMOMETER))
                .validateChannelAndGetValue(this.cpm);
    }

    /**
     * Internal Method to get the Deactivation Temperature.
     *
     * @return the DeactivationTemperature value.
     */
    private int getDeactivationTemperature() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.thermometerAndValue.get(this.thermometerTypeThermometerMap.get(ThermometerType.DEACTIVATE_THERMOMETER))
                .validateChannelAndGetValue(this.cpm);
    }

    /**
     * Method, usually called by Heater Controller.
     * to determine if the enabled Value should be set to true within the heater.
     *
     * @return the result of the Comparison of the DeactivationThermometer Temperature Value and the stored deactivation Temperature.
     */

    @Override
    public boolean shouldDeactivate() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.getDeactivationThermometer().getTemperatureValue() >= this.getDeactivationTemperature();
    }

    /**
     * Method, usually called by Heater Controller.
     * to determine if the enabled Value should be set to true within the heater.
     *
     * @return the result of the Comparison of the ActivationThermometer Temperature Value and the stored activation Temperature.
     */

    @Override
    public boolean shouldActivate() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.getActivationThermometer().getTemperatureValue() <= this.getActivationTemperature();
    }

    Map<Thermometer, ThermometerValueWrapper> getThermometerAndValue() {
        return this.thermometerAndValue;
    }

    Map<ThermometerType, Thermometer> getThermometerKindThermometerMap() {
        return this.thermometerTypeThermometerMap;
    }

}
