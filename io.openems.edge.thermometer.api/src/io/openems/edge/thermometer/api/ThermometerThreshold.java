package io.openems.edge.thermometer.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;


/**
 * The Nature of the ThermometerThreshold, an expansion of the Thermometer, helps other controller and Heatsystem-Components
 * to decide what to do on changing temperatures, preventing fluctuations.
 * The Threshold can be overwritten.
 * The ThermometerState represents rising or falling in temperature.
 * The TemperatureSetPoint can be set by a component and deposit it's id. The temperature will be locked.
 * However it is possible to overwrite the Temperature with any device, if the requested Temperature is above the current SetPoint.
 * If you need to implement a cooling system where a lower Temperature should always be accepted, you need to implement that yourself.
 * The Temperature is stored in SetPointTemperature.
 */
public interface ThermometerThreshold extends Thermometer {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Threshold.
         * Set the threshold for TemperatureSettings.
         * <ul>
         * <li>Interface: ThermometerThreshold
         * <li>Type: Integer
         * <li>Unit: decimal degree Celsius
         * </ul>
         */
        THRESHOLD(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE).onInit(
                        channel -> ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue)
                )),
        /**
         * State of the Thermometer -> rise or fall; depending on regression values --> Threshold.
         * <ul>
         *     <li>Interface ThermometerThreshold
         *     <li>Type: String
         * </ul>
         */
        THERMOMETER_STATE(Doc.of(ThermometerState.values())),;

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }


    // ------------------ THRESHOLD ---------------- //

    /**
     * get ThresholdTemperatureChannel. Only for the Owning component! Never call channel directley
     *
     * @return the channel.
     */

    default WriteChannel<Integer> getThresholdChannel() {
        return this.channel(ChannelId.THRESHOLD);
    }

    /**
     * Get The ThresholdValue of the Channel or 1 on null.
     *
     * @return the Threshold.
     */
    default int getThreshold() {
        Integer thresholdValue = (Integer) this.getValueOfChannel(this.getThresholdChannel());
        if (thresholdValue == null) {
            thresholdValue = (Integer) this.getNextValueOfChannel(this.getThresholdChannel());
        }
        if (thresholdValue == null) {
            thresholdValue = 1;
        }
        return thresholdValue;
    }

    /**
     * Set the Threshold to new value in dC.
     *
     * @param threshold the new Value for threshold.
     */
    default void setThresholdInDeciDegree(int threshold) {
        this.getThresholdChannel().setNextValue(threshold);
    }

    // --------------------------------------------------------------//

    //-----------------ThermometerState---------------------//

    /**
     * Get the ThermometerState Channel.
     *
     * @return the channel.
     */

    default Channel<ThermometerState> getThermometerStateChannel() {
        return this.channel(ChannelId.THERMOMETER_STATE);
    }

    /**
     * get the ThermometerState or undefined on null.
     *
     * @return the ThermometerState.
     */

    default ThermometerState getThermometerState() {
        ThermometerState thermometerState = ThermometerState.getThermometerStateFromInteger((Integer) this.getValueOfChannel(this.getThermometerStateChannel()));
        if (thermometerState.equals(ThermometerState.UNDEFINED)) {
            thermometerState = ThermometerState.getThermometerStateFromInteger((Integer) this.getNextValueOfChannel(this.getThermometerStateChannel()));
        }
        return thermometerState;
    }

    /**
     * Set the Thermometerstate, depending on Falling or Rising Temperatures. will be decided by the ThresholdThermometer.
     *
     * @param state the ThermometerState.
     */
    default void setThermometerState(ThermometerState state) {
        this.getThermometerStateChannel().setNextValue(state);
    }
    //--------------------------------------------------------//

    /**
     * Important: If Temperature is falling --> "less than" given temperature is still ok if the current Temperature
     * is less than or equal to the given Temperature.
     * <p>
     * Example: Temperature is falling: 500 dc 450 dc 400dc Set point is 400 dC.
     * --> Since the temperature is falling --> 400 of thermometer will be equal or greater than given so return true.
     * However if the temperature is rising ---> 500 --> 550 dC 600 dC and the given is 600 -->
     * The Temperature will only be checked if it's less not less than or equals (< instead of <=)
     * Since the expected temperature will be rising further.
     * </p>
     * <p>
     * This will be Equivalent to "givenTemperatureAboveThermometer"
     * </p>
     *
     * @param temperature the given temperature either by calling component or this.
     * @return a boolean: result -> lessOrEquals than given Temperature /less than given Temperature
     */
    default boolean thermometerBelowGivenTemperature(int temperature) {
        boolean result;
        if (this.getThermometerState().equals(ThermometerState.FALLING)) {
            result = this.getTemperatureValue() <= temperature;
        } else {
            result = this.getTemperatureValue() < temperature;
        }
        return result;
    }

    /**
     * Important: If Temperature is rising --> "Greater than" given temperature is still ok if the current Temperature
     * is greater than or equal to the given Temperature.
     * <p>
     * Example: Temperature is rising: 400 dc 450 dc 500dc Set point is 500 dC.
     * --> Since the temperature is rising --> 500dC of thermometer will be equal or greater than given Temperature so return true.
     * However if the temperature is falling ---> 600dC --> 550 dC 500 dC and the given Temperature is 500 -->
     * The Temperature will only be checked if it's greater not greater than or equals (> instead of >=)
     * Since the expected temperature will be falling further.
     * </p>
     * <p>
     * This will be Equivalent to "givenTemperatureBelowThermometer"
     * </p>
     *
     * @param temperature the given temperature either by calling component or this.
     * @return a boolean: result -> greaterOrEquals than given Temperature /greater than given Temperature
     */
    default boolean thermometerAboveGivenTemperature(int temperature) {
        boolean result;
        if (this.getThermometerState().equals(ThermometerState.RISING)) {
            result = this.getTemperatureValue() >= temperature;
        } else {
            result = this.getTemperatureValue() > temperature;
        }
        return result;
    }

    /**
     * Returns the current value of a Channel.
     *
     * @param requestedChannel the Channel, usually from this nature.
     * @return the Value or null if not defined.
     */
    default Object getValueOfChannel(Channel<?> requestedChannel) {
        if (requestedChannel.value().isDefined()) {
            return requestedChannel.value().get();
        } else {
            return null;
        }
    }

    /**
     * get the next value of a Channel. Happens if current value is not defined.
     *
     * @param requestedChannel the Channel, usually from this nature.
     * @return the Value or null if not defined.
     */

    default Object getNextValueOfChannel(Channel<?> requestedChannel) {
        if (requestedChannel.getNextValue().isDefined()) {
            return requestedChannel.getNextValue().get();
        }
        return null;
    }

}
