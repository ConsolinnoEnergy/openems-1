package io.openems.edge.storage.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;


public interface HeatStorageTriple extends HeatStorage {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * TEMP Sensor 1
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        TEMP_1(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        /**
         * TEMP Sensor 2
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        TEMP_2(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        /**
         * TEMP Sensor 1
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        TEMP_3(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#TEMP_1}.
     *
     * @return the Channel
     */
    default Channel<Float> getTempSensorFirstChannel() {
        return this.channel(ChannelId.TEMP_1);
    }
    /**
     * Gets the Channel for {@link ChannelId#TEMP_2}.
     *
     * @return the Channel
     */
    default Channel<Float> getTempSensorSecondChannel() {
        return this.channel(ChannelId.TEMP_2);
    }
    /**
     * Gets the Channel for {@link ChannelId#TEMP_3}.
     *
     * @return the Channel
     */
    default Channel<Float> getTempSensorThirdChannel() {
        return this.channel(ChannelId.TEMP_3);
    }

}

