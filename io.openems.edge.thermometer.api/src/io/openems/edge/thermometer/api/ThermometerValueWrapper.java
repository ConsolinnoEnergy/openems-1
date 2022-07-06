package io.openems.edge.thermometer.api;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.type.TypeUtils;
import org.osgi.service.cm.ConfigurationException;

/**
 * The Thermometer Value. It contains  a temperature Value or a ChannelAddress depending what is configured.
 * This is determined, if the channelAddressOrValue String (usually from MultiHeaterConfig) contains only numbers,
 * the TemperatureValue will be set, otherwise the class tries to get a ChannelAddress from the given String.
 * This class helps the {@link ThermometerWrapper} to determine, if the Temperature is above/below activation/deactivation SetPoint.
 * The benefit of having a ChannelAddress as an activation/deactivation SetPoint is, to dynamically change the SetPoints.
 * (E.g. Using a virtualThermometer).
 */
public class ThermometerValueWrapper {

    private static final String REG_EX_VALID_NUMBER_CHECK = "[-+]?([0-9]*[.][0-9]+|[0-9]+)";
    private int temperatureValue;
    private ChannelAddress temperatureValueAddress;
    private boolean usesChannel;

    public ThermometerValueWrapper(String channelAddressOrValue) throws OpenemsError.OpenemsNamedException {
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
        return value.matches(REG_EX_VALID_NUMBER_CHECK);
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
                Integer returnValue = TypeUtils.getAsType(OpenemsType.INTEGER, channel.value());
                if (returnValue == null) {
                    returnValue = Thermometer.MISSING_TEMPERATURE;
                }
                return returnValue;
            } else {
                throw new ConfigurationException("ValidateChannelAndGetValue", "Either Channel does not contain a value or is not valid!");
            }

        } else {
            return this.temperatureValue;
        }
    }
}