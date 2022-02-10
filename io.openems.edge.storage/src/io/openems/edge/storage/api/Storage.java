package io.openems.edge.storage.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Storage extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Storage capacity.
         * <ul>
         *      <li> Type: kwh
         * </ul>
         */
        CAPACITY(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        FILL_LEVEL(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }
    /**
     * Gets the Channel for {@link ChannelId#CAPACITY}.
     *
     * @return the Channel
     */
    default FloatReadChannel getCapacityChannel() {
        return this.channel(ChannelId.CAPACITY);
    }
    /**
     * Gets the Channel for {@link ChannelId#FILL_LEVEL}.
     *
     * @return the Channel
     */
    default FloatReadChannel getFillLevelChannel() {
        return this.channel(ChannelId.FILL_LEVEL);
    }
}

