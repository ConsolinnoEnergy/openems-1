package io.openems.edge.controller.heatnetwork.heatingcurveregulator.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The HeatingCurve Regulator implements the functionality of a Heating Curve.
 *
 * @see <a href="https://de.wikipedia.org/wiki/Heizkurve">Wikipedia</a>
 * @see <a href="https://second.wiki/wiki/curva_de_calefaccic3b3n">English Wiki</a>
 *
 * <p>
 * This Nature holds the RoomTemperature, actviationtemperature, an offset and a slope.
 * All of those params are needed to calculate a "HeatingTemperature".
 * When the HeatingCurve calculates a HeatingTemperature, ActivateHeater will be set to true, otherwise false.
 * This can be used in other Controller that controls a small portion of a heatsystem.
 * If either RoomTemperature,ActivationTemperature,Offset or Slope receive a nextWriteValue, the config will be updated.
 * </p>
 */
public interface HeatingCurveRegulator extends OpenemsComponent {

    double SLOPE_MULTIPLIER = 1.8317984;
    int CELSIUS_TO_DECI_DEGREE_CONVERTER = 10;
    double EXPONENT = 0.8281902;

    /**
     * Function to calculate the heating curve set point Temperature in dC.
     *
     * @param slope                            the slope of the function
     * @param offset                           the offset on the y - axis
     * @param setPointTemperatureDegreeCelsius the SetPoint/RoomTemperature in °C
     * @param currentTemperatureDc             the CurrentTemperature in dC
     * @return heatingCurveSetPointTemperature in dC
     */
    static int calculateHeatingCurveTemperatureInDeciDegree(double slope, int offset, int setPointTemperatureDegreeCelsius, int currentTemperatureDc) {
        double base = setPointTemperatureDegreeCelsius - ((double) currentTemperatureDc / (double) CELSIUS_TO_DECI_DEGREE_CONVERTER);
        return (int) (slope * SLOPE_MULTIPLIER * Math.pow(base, EXPONENT) + setPointTemperatureDegreeCelsius + offset) * CELSIUS_TO_DECI_DEGREE_CONVERTER;
    }

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * The RoomTemperature. What Temperature is the SetPoint of your HeatingCurve.
         * <ul>
         * <li> RoomTemperature
         * <li>Type: Integer
         * <li>Unit: DegreeCelsius
         * </ul>
         */
        ROOM_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),
        /**
         * The ActivationTemperature. When should the HeatingCurve start to calculate a SetPoint/Heating Temperature.
         * This should be less or equal to the RoomTemperature.
         * <ul>
         * <li> ActivationTemperature
         * <li>Type: Integer
         * <li>Unit: DegreeCelsius
         * </ul>
         */
        ACTIVATION_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),
        /**
         * The Offset. A parameter of the HeatingCurve itself. (Offset on the Y-axis)
         * <ul>
         * <li> Offset
         * <li>Type: Integer
         * </ul>
         */
        OFFSET(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * The Slope. A parameter of the HeatingCurve itself.
         * <ul>
         * <li> Slope
         * <li>Type: Double
         * </ul>
         */
        SLOPE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Heating Temperature. The SetPoint Temperature, calculated by the HeatingCurve.
         * <ul>
         * <li>Controller output. Value for the heating temperature.
         * <li>Type: Integer
         * <li>Unit: Decimal degrees Celsius
         * </ul>
         */

        HEATING_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Activate the heater. This should be read from another Controller. Since the Boolean is only stored internally.
         * <ul>
         * <li>Controller output. If the heater should be turned on.
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        ACTIVATE_HEATER(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

        /**
         * When an error occurred in this controller, set to "false" otherwise "true".
         * <ul>
         * <li> No Error.
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        NO_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel of {@link ChannelId#ROOM_TEMPERATURE}.
     *
     * @return the channel
     */
    default WriteChannel<Integer> getRoomTemperature() {
        return this.channel(ChannelId.ROOM_TEMPERATURE);
    }

    /**
     * Gets the Channel of {@link ChannelId#ACTIVATION_TEMPERATURE}.
     *
     * @return the channel
     */
    default WriteChannel<Integer> getActivationTemperature() {
        return this.channel(ChannelId.ACTIVATION_TEMPERATURE);
    }

    /**
     * Gets the Channel of {@link ChannelId#OFFSET}.
     *
     * @return the channel
     */
    default WriteChannel<Integer> getOffset() {
        return this.channel(ChannelId.OFFSET);
    }

    /**
     * Gets the Channel of {@link ChannelId#SLOPE}.
     *
     * @return the channel
     */
    default WriteChannel<Double> getSlope() {
        return this.channel(ChannelId.SLOPE);
    }

    /**
     * Gets the Channel of {@link ChannelId#HEATING_TEMPERATURE}.
     *
     * @return the channel
     */
    default Channel<Integer> getHeatingTemperature() {
        return this.channel(ChannelId.HEATING_TEMPERATURE);
    }

    /**
     * Gets the Channel of {@link ChannelId#ACTIVATE_HEATER}.
     *
     * @return the channel
     */

    default Channel<Boolean> signalTurnOnHeater() {
        return this.channel(ChannelId.ACTIVATE_HEATER);
    }

    /**
     * Gets the Channel of {@link ChannelId#NO_ERROR}.
     *
     * @return the channel
     */
    default Channel<Boolean> noErrorChannel() {
        return this.channel(ChannelId.NO_ERROR);
    }

    /**
     * Return if the HeatingCurveRegulator has no Error.
     *
     * @return a Boolean.
     */
    default boolean noError() {
        return this.noErrorChannel().value().orElse(false);
    }

    /**
     * Return if the HeatingCurveRegulator has an error.
     *
     * @return a boolean
     */
    default boolean hasError() {
        return !this.noError();
    }

}
