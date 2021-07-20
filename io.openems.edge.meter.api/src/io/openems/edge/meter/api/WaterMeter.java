package io.openems.edge.meter.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface WaterMeter extends Meter {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Counted volume.
         *
         * <ul>
         * <li>Interface: WaterMeter
         * <li>Type: Double
         * <li>Unit: cubic meters (m³)
         * </ul>
         */
        TOTAL_CONSUMED_WATER(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUBIC_METER).accessMode(AccessMode.READ_ONLY)),


        /**
         * Error message. Will contain "No error" when there is no error. Otherwise a description of the error will be
         * given.
         *
         * <ul>
         * <li>Interface: WaterMeter
         * <li>Type: String
         * </ul>
         */
        ERROR_MESSAGE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#TOTAL_CONSUMED_WATER}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getTotalConsumedWaterChannel() {
        return this.channel(ChannelId.TOTAL_CONSUMED_WATER);
    }

    /**
     * Gets the total consumed water in [m³]. See {@link ChannelId#TOTAL_CONSUMED_WATER}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Double> getTotalConsumedWater() {
        return this.getTotalConsumedWaterChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#TOTAL_CONSUMED_WATER}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setTotalConsumedWater(Double value) {
        this.getTotalConsumedWaterChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#TOTAL_CONSUMED_WATER}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setTotalConsumedWater(double value) {
        this.getTotalConsumedWaterChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#ERROR_MESSAGE}.
     *
     * @return the Channel
     */
    default StringReadChannel getErrorChannel() {
        return this.channel(ChannelId.ERROR_MESSAGE);
    }

    /**
     * Gets the error message. Will contain "No error" when there is no error. See {@link ChannelId#ERROR_MESSAGE}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<String> isError() {
        return this.getErrorChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#ERROR_MESSAGE}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setError(String value) {
        this.getErrorChannel().setNextValue(value);
    }
}
