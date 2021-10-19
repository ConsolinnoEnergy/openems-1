package io.openems.edge.sensor.gas.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface NaturalGasSensor extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Hydrogen concentration in percent.
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        HYDROGEN_CONCENTRATION(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT));

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
    default Channel<Float> getHydrogenConcentrationChannel() {
        return this.channel(ChannelId.HYDROGEN_CONCENTRATION);
    }

}