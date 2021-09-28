package io.openems.edge.heater.electrolyzer.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.heater.Heater;


public interface Electrolyzer extends Heater {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * Electrolyzer get temperature
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        WMZ_ENERGY_PRODUCED(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SOURCE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SINK(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        WMZ_POWER(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_POWER}.
     *
     * @return the Channel
     */
    default Channel<Float> getWMZPowerChannel() {
        return this.channel(ChannelId.WMZ_POWER);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SINK}.
     *
     * @return the Channel
     */
    default Channel<Float> getWMZTempSinkChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SINK);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SOURCE}.
     *
     * @return the Channel
     */
    default Channel<Float> getWMZTempSourceChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SOURCE);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_ENERGY_PRODUCED}.
     *
     * @return the Channel
     */
    default Channel<Float> getWMZEnergyProducedChannel() {
        return this.channel(ChannelId.WMZ_ENERGY_PRODUCED);
    }
}