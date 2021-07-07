package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.thermometer.api.Thermometer;
import org.osgi.service.cm.ConfigurationException;

import java.util.HashMap;
import java.util.Map;

/**
 * This class helps to get the Correct Thermometer of the corresponding Heater as well as "setPoints" for the Thermometer.
 * Provides the methods to check if min or max temperature is reached --> therefore heater can activate/deactivate correctly.
 */

class ThermometerWrapper {

    private final ComponentManager cpm;
    //Map the Thermometer to their min/max Value
    private final Map<Thermometer, ThermometerValue> thermometerAndValue = new HashMap<>();
    //ThermometerKind == Activate/Deactivate on Heatcontrol. Mapped thermometerkind to Thermometer
    private final Map<ThermometerKind, Thermometer> thermometerKindThermometerMap = new HashMap<>();


    ThermometerWrapper(Thermometer minThermometer, Thermometer maxThermometer, String minValue, String maxValue, ComponentManager cpm)
            throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.cpm = cpm;
        this.thermometerKindThermometerMap.put(ThermometerKind.ACTIVATE_THERMOMETER, minThermometer);
        this.thermometerAndValue.put(minThermometer, new ThermometerValue(minValue));
        this.thermometerKindThermometerMap.put(ThermometerKind.DEACTIVATE_THERMOMETER, maxThermometer);
        this.thermometerAndValue.put(maxThermometer, new ThermometerValue(maxValue));
        this.thermometerAndValue.get(minThermometer).validateChannelAndGetValue(this.cpm);
        this.thermometerAndValue.get(maxThermometer).validateChannelAndGetValue(this.cpm);

    }

    /**
     * The inner static Class Thermometer Value, containing  a temperature Value or a ChannelAddress depending what is configured.
     * This is determined, if the channelAddressOrValue String (usually from MultiHeaterConfig) contains only numbers,
     * the TemperatureValue will be set, otherwise the class tries to get a ChannelAddress from the given String.
     * This class helps the Thermometerwrapper to determine, if the Temperature is above/below activation/deactivation SetPoint.
     * The benefit of having a ChannelAddress as an activation/deactivation SetPoint is, to dynamically change the SetPoints.
     * (E.g. Using a virtualThermometer).
     */
    private static class ThermometerValue {
        private int temperatureValue;
        private ChannelAddress temperatureValueAddress;
        private boolean usesChannel;

        private ThermometerValue(String channelAddressOrValue) throws OpenemsError.OpenemsNamedException {
            if (this.containsOnlyValidNumbers(channelAddressOrValue)) {
                this.temperatureValue = (int) Double.parseDouble(channelAddressOrValue);
            } else {
                this.temperatureValueAddress = ChannelAddress.fromString(channelAddressOrValue);
                this.usesChannel = true;

            }
        }

        /**
         * Small helper method to determine if a String contains only numbers with decimals.
         *
         * @param value the String that will be checked, if it's only containing numbers.
         * @return the result oft the match with the regex that describes only numbers.
         */
        private boolean containsOnlyValidNumbers(String value) {
            return value.matches("[-+]?([0-9]*[.][0-9]+|[0-9]+)");
        }

        /**
         * If the static class ThermometerValue uses a ChannelAddress:
         * The method helps to validate the configured ChannelAddress and get the value from it.
         * If the Thermometer does not use a channelAddress, it returns the stored temperatureValue.
         *
         * @param cpm the ComponentManager
         * @return the value of the channel or the stored value {@link #temperatureValue};
         * @throws OpenemsError.OpenemsNamedException if the channelAddress is false/cannot be found
         * @throws ConfigurationException             if the ChannelValue is not defined/not containing only numbers
         */
        public int validateChannelAndGetValue(ComponentManager cpm) throws OpenemsError.OpenemsNamedException, ConfigurationException {
            if (this.usesChannel) {
                Channel<?> channel = cpm.getChannel(this.temperatureValueAddress);
                if (channel.value().isDefined() && this.containsOnlyValidNumbers(channel.value().get().toString())) {
                    return (Integer) channel.value().get();
                } else {
                    throw new ConfigurationException("ValidateChannelAndGetValue", "Either Channel does not contain a value or is not valid!");
                }

            } else {
                return this.temperatureValue;
            }
        }
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
    private int getActivationTemperature() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.thermometerAndValue.get(this.thermometerKindThermometerMap.get(ThermometerKind.ACTIVATE_THERMOMETER))
                .validateChannelAndGetValue(this.cpm);
    }

    /**
     * Internal Method to get the Deactivation Temperature.
     *
     * @return the DeactivationTemperature value.
     */
    private int getDeactivationTemperature() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.thermometerAndValue.get(this.thermometerKindThermometerMap.get(ThermometerKind.DEACTIVATE_THERMOMETER))
                .validateChannelAndGetValue(this.cpm);
    }

    /**
     * Method, usually called by the {@link MultipleHeaterCombinedControllerImpl}
     * to determine if the {@link HeaterActiveWrapper#setActive(boolean)} with value false should be called.
     *
     * @return the result of the Comparison of the DeactivationThermometer Temperature Value and the stored deactivation Temperature.
     */

    boolean shouldDeactivate() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.getDeactivationThermometer().getTemperatureValue() >= this.getDeactivationTemperature();
    }

    /**
     * Method, usually called by the {@link MultipleHeaterCombinedControllerImpl}
     * to determine if the {@link HeaterActiveWrapper#setActive(boolean)} with value true should be called.
     *
     * @return the result of the Comparison of the ActivationThermometer Temperature Value and the stored activation Temperature.
     */

    boolean shouldActivate() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        return this.getActivationThermometer().getTemperatureValue() <= this.getActivationTemperature();
    }

}
