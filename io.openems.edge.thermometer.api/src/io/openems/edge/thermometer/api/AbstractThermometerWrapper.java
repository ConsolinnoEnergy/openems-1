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

public abstract class AbstractThermometerWrapper implements ThermometerWrapper {

    private final ComponentManager cpm;
    //Map the Thermometer to their min/max Value
    private final Map<Thermometer, ThermometerValueWrapper> thermometerAndValue = new HashMap<>();
    //thermometerType == Activate/Deactivate on HeatControl. Mapped thermometerType to Thermometer
    private final Map<ThermometerType, Thermometer> thermometerTypeThermometerMap = new HashMap<>();

    protected int deactivationOffset = 0;
    protected int activationOffset = 0;


    public AbstractThermometerWrapper(Thermometer activateThermometer, Thermometer deactivateThermometer, String activateValue, String deactivateValue, ComponentManager cpm)
            throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this(activateThermometer, deactivateThermometer, activateValue, deactivateValue, cpm, 0, 0);
    }

    public AbstractThermometerWrapper(Thermometer activateThermometer, Thermometer deactivateThermometer, String activateValue, String deactivateValue, ComponentManager cpm, int activationOffset, int deactivationOffset) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.cpm = cpm;
        this.thermometerTypeThermometerMap.put(ThermometerType.ACTIVATE_THERMOMETER, activateThermometer);
        this.thermometerAndValue.put(activateThermometer, new ThermometerValueWrapper(activateValue));
        this.thermometerTypeThermometerMap.put(ThermometerType.DEACTIVATE_THERMOMETER, deactivateThermometer);
        this.thermometerAndValue.put(deactivateThermometer, new ThermometerValueWrapper(deactivateValue));
        this.thermometerAndValue.get(activateThermometer).validateChannelAndGetValue(this.cpm);
        this.thermometerAndValue.get(deactivateThermometer).validateChannelAndGetValue(this.cpm);
        this.deactivationOffset = deactivationOffset;
        this.activationOffset = activationOffset;
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

    protected int getActivationTemperature() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.thermometerAndValue.get(this.thermometerTypeThermometerMap.get(ThermometerType.ACTIVATE_THERMOMETER))
                .validateChannelAndGetValue(this.cpm);
    }

    /**
     * Internal Method to get the Deactivation Temperature.
     *
     * @return the DeactivationTemperature value.
     */
    protected int getDeactivationTemperature() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.thermometerAndValue.get(this.thermometerTypeThermometerMap.get(ThermometerType.DEACTIVATE_THERMOMETER))
                .validateChannelAndGetValue(this.cpm);
    }

    Map<Thermometer, ThermometerValueWrapper> getThermometerAndValue() {
        return this.thermometerAndValue;
    }

    Map<ThermometerType, Thermometer> getThermometerKindThermometerMap() {
        return this.thermometerTypeThermometerMap;
    }

}
