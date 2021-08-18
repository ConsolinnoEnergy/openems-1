package io.openems.edge.generator.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;

public interface ElectrolyzerModbusGeneric extends GeneratorModbusGeneric{
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * Electrolyzer get temperature
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        WMZ_ENERGY_PRODUCED_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        WMZ_ENERGY_PRODUCED_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SOURCE_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SOURCE_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SINK_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SINK_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),
        WMZ_POWER_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        WMZ_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),
        POWER_PERCENT_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        POWER_PERCENT_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_POWER_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> getWMZPowerLongChannel() {
        return this.channel(ChannelId.WMZ_POWER_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_POWER_LONG}.
     *
     * @return the Channel
     */
    default Channel<Double> getWMZPowerDoubleChannel() {
        return this.channel(ChannelId.WMZ_POWER_DOUBLE);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SINK_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> getWMZTempSinkLongChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SINK_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SINK_LONG}.
     *
     * @return the Channel
     */
    default Channel<Double> getWMZTempSinkDoubleChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SINK_DOUBLE);
    }


    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SOURCE_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> getWMZTempSourceLongChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SOURCE_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SOURCE_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> getWMZTempSourceDoubleChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SOURCE_DOUBLE);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_ENERGY_PRODUCED_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> getWMZEnergyProducedLongChannel() {
        return this.channel(ChannelId.WMZ_ENERGY_PRODUCED_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_ENERGY_PRODUCED_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> getWMZEnergyProducedDoubleChannel() {
        return this.channel(ChannelId.WMZ_ENERGY_PRODUCED_DOUBLE);
    }

    /**
     * Gets the Channel for {@link ChannelId#POWER_PERCENT_LONG}.
     *
     * @return the Channel
     */
    default WriteChannel<Long> getPowerPercentLongChannel() {
        return this.channel(ChannelId.POWER_PERCENT_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#POWER_PERCENT_DOUBLE}.
     *
     * @return the Channel
     */
    default WriteChannel<Long> getPowerPercentDoubleChannel() {
        return this.channel(ChannelId.POWER_PERCENT_DOUBLE);
    }

}
