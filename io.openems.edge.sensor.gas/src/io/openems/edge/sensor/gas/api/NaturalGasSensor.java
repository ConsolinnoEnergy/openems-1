package io.openems.edge.sensor.gas.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface NaturalGasSensor extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * Hydrogen concentration in percent
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        HYDROGEN_CONCENTRATION(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#HYDROGEN_CONCENTRATION}.
     *
     * @return the Channel
     */
    default FloatReadChannel getHydrogenConcentrationChannel() {
        return this.channel(ChannelId.HYDROGEN_CONCENTRATION);
    }

}