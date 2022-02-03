package io.openems.edge.storage.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatReadChannel;

public interface HeatStorageQuad extends HeatStorage {
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
         * TEMP Sensor 3
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        TEMP_3(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        /**
         * TEMP Sensor 4
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        TEMP_4(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY));

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
    default FloatReadChannel getTempSensorFirstChannel() {
        return this.channel(ChannelId.TEMP_1);
    }
    /**
     * Gets the Channel for {@link ChannelId#TEMP_2}.
     *
     * @return the Channel
     */
    default FloatReadChannel getTempSensorSecondChannel() {
        return this.channel(ChannelId.TEMP_2);
    }
    /**
     * Gets the Channel for {@link ChannelId#TEMP_3}.
     *
     * @return the Channel
     */
    default FloatReadChannel getTempSensorThirdChannel() {
        return this.channel(ChannelId.TEMP_3);
    }
    /**
     * Gets the Channel for {@link ChannelId#TEMP_4}.
     *
     * @return the Channel
     */
    default FloatReadChannel getTempSensorFourthChannel() {
        return this.channel(ChannelId.TEMP_4);
    }

}
