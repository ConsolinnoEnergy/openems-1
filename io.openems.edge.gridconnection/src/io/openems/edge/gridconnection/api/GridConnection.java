package io.openems.edge.gridconnection.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface GridConnection extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * GridConnection Voltage
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        GRID_METER_NATURAL_GAS(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        GRID_METER_COLD_WATER(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#GRID_METER_NATURAL_GAS}.
     *
     * @return the Channel
     */
    default Channel<Float> getGridMeterNaturalGasChannel() {
        return this.channel(ChannelId.GRID_METER_NATURAL_GAS);
    }

    /**
     * Gets the Channel for {@link ChannelId#GRID_METER_COLD_WATER}.
     *
     * @return the Channel
     */
    default Channel<Float> getGridMeterColdWaterChannel() {
        return this.channel(ChannelId.GRID_METER_COLD_WATER);
    }

}