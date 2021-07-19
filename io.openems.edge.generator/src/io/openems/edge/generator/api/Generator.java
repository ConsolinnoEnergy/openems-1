package io.openems.edge.generator.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface Generator extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * Generator Power
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        POWER(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#POWER}.
     *
     * @return the Channel
     */
    default Channel<Float> getPowerChannel() {
        return this.channel(ChannelId.POWER);
    }

    /**
     * Set the Channel for {@link ChannelId#POWER}.
     *
     * @return the Channel
     */
    default Channel<Float> setPowerChannel() {
        return this.channel(ChannelId.POWER);
    }

}