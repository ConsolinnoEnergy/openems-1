package io.openems.edge.storage.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;

public interface HydrogenStorage extends Storage {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * Pressure in mbar
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        PRESSURE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#PRESSURE}.
     *
     * @return the Channel
     */
    default Channel<Float> getPressureChannel() {
        return this.channel(ChannelId.PRESSURE);
    }

}
