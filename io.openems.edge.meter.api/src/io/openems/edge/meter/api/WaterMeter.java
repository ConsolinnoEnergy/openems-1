package io.openems.edge.meter.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;

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
        READ_WATER(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUBIC_METER).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#READ_WATER}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getTotalConsumedWaterChannel() {
        return this.channel(ChannelId.READ_WATER);
    }

    /**
     * Gets the total consumed water in [m³]. See {@link ChannelId#READ_WATER}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Double> getTotalConsumedWater() {
        return this.getTotalConsumedWaterChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#READ_WATER}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setTotalConsumedWater(Double value) {
        this.getTotalConsumedWaterChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#READ_WATER}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setTotalConsumedWater(double value) {
        this.getTotalConsumedWaterChannel().setNextValue(value);
    }
}
