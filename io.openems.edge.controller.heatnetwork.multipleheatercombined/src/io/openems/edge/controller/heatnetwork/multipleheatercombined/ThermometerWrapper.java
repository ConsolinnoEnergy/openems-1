package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.edge.thermometer.api.Thermometer;

import java.util.HashMap;
import java.util.Map;

/**
 * This class helps to get the Correct Thermometer of the corresponding Heater as well as "setpoints" for the Thermometer.
 * Provides the methods to check, if min or max temperature is reached --> therefore heater can activate/deactivate correctly.
 */

class ThermometerWrapper {

    //Map the Thermometer to their min/max Value
    private final Map<Thermometer, Integer> thermometerAndValue = new HashMap<>();
    //ThermometerKind == Activate/Deactivate on Heatcontrol. Mapped thermometerkind to Thermometer
    private final Map<ThermometerKind, Thermometer> thermometerKindThermometerMap = new HashMap<>();


    ThermometerWrapper(Thermometer minThermometer, Thermometer maxThermometer, int minValue, int maxValue) {

        this.thermometerKindThermometerMap.put(ThermometerKind.ACTIVATE_THERMOMETER, minThermometer);
        this.thermometerAndValue.put(minThermometer, minValue);
        this.thermometerKindThermometerMap.put(ThermometerKind.DEACTIVATE_THERMOMETER, maxThermometer);
        this.thermometerAndValue.put(maxThermometer, maxValue);
    }

    /**
     * Getter for the deactivation Thermometer.
     *
     * @return the Deactivation {@link Thermometer}
     */
    Thermometer getDeactivationThermometer() {
        return this.thermometerKindThermometerMap.get(ThermometerKind.DEACTIVATE_THERMOMETER);
    }

    /**
     * Getter for the activation Thermometer.
     *
     * @return the Activation {@link Thermometer}
     */
    Thermometer getActivationThermometer() {
        return this.thermometerKindThermometerMap.get(ThermometerKind.ACTIVATE_THERMOMETER);
    }

    /**
     * Internal Method to get the Activation Temperature.
     *
     * @return the ActivationTemperature value.
     */
    private int getActivationTemperature() {
        return this.thermometerAndValue.get(this.thermometerKindThermometerMap.get(ThermometerKind.ACTIVATE_THERMOMETER));
    }

    /**
     * Internal Method to get the Deactivation Temperature.
     *
     * @return the DeactivationTemperature value.
     */
    private int getDeactivationTemperature() {
        return this.thermometerAndValue.get(this.thermometerKindThermometerMap.get(ThermometerKind.DEACTIVATE_THERMOMETER));
    }

    /**
     * Method, usually called by the {@link MultipleHeaterCombinedControllerImpl}
     * to determine if the {@link HeaterActiveWrapper#setActive(boolean)} with value false should be called.
     *
     * @return the result of the Comparison of the DeactivationThermometer Temperature Value and the stored deactivation Temperature.
     */

    boolean shouldDeactivate() {
        return this.getDeactivationThermometer().getTemperatureValue() >= this.getDeactivationTemperature();
    }

    /**
     * Method, usually called by the {@link MultipleHeaterCombinedControllerImpl}
     * to determine if the {@link HeaterActiveWrapper#setActive(boolean)} with value true should be called.
     *
     * @return the result of the Comparison of the ActivationThermometer Temperature Value and the stored activation Temperature.
     */

    boolean shouldActivate() {
        return this.getActivationThermometer().getTemperatureValue() <= this.getActivationTemperature();
    }

}
