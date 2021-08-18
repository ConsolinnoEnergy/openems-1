package io.openems.edge.generator.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface GeneratorModbusGeneric extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * Generator Power
         * <ul>
         *      <li> Type: Double
         * </ul>
         */

        POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        POWER_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#POWER_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> getPowerLongChannel() {
        return this.channel(ChannelId.POWER_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#POWER_LONG}.
     *
     * @return the Channel
     */
    default Channel<Double> getPowerDoubleChannel() {
        return this.channel(ChannelId.POWER_DOUBLE);
    }


}
