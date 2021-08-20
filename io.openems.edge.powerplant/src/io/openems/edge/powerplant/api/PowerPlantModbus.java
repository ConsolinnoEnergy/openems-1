package io.openems.edge.powerplant.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.generator.api.Generator;
import io.openems.edge.generator.api.GeneratorModbus;

public interface PowerPlantModbus extends GeneratorModbus {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * PowerLevel.
         *
         * <ul>
         * <li>Interface: PassingChannel
         * <li>Type: Double
         * <li> Unit: Percentage
         * </ul>
         */

        POWER_LEVEL_PERCENT_LONG(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT)),

        /**
         * PowerLevel.
         *
         * <ul>
         * <li>Interface: PassingChannel
         * <li>Type: Double
         * <li> Unit: Percentage
         * </ul>
         */

        POWER_LEVEL_PERCENT_DOUBLE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT)),


        /**
         * PowerLevelKW.
         *
         * <ul>
         * <li>
         * <li>Type: Double
         * <li>Unit: Kw
         * </ul>
         */

        SET_POWER_LEVEL_KW_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        SET_POWER_LEVE_KW_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        MAXIMUM_KW_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        MAXIMUM_KW_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Gets the Channel for {@link ChannelId#POWER_LEVEL_PERCENT_LONG}.
     *
     * @return the Channel
     */
    default WriteChannel<Long> _setPowerLevelPercentLongChannel() {
        return this.channel(ChannelId.POWER_LEVEL_PERCENT_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#POWER_LEVEL_PERCENT_DOUBLE}.
     *
     * @return the Channel
     */
    default WriteChannel<Double> _setPowerLevelPercentDoubleChannel() {
        return this.channel(ChannelId.POWER_LEVEL_PERCENT_DOUBLE);
    }

    /**
     * Checks if the PowerPlant has a Reading Power Percent set. After that the stored value will be written to the actual {@link PowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasPowerLevelPercent() {
        return GeneratorModbus.getValueDefinedChannel(this._getPowerDoubleChannel(), this._getPowerLongChannel());
    }

    /**
     * Gets the Channel for {@link ChannelId#SET_POWER_LEVEL_KW_LONG}.
     *
     * @return the Channel
     */
    default WriteChannel<Long> _setPowerLevelKwLongChannel() {
        return this.channel(ChannelId.SET_POWER_LEVEL_KW_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#SET_POWER_LEVE_KW_DOUBLE}.
     *
     * @return the Channel
     */
    default WriteChannel<Double> _setPowerLevelKwDoubleChannel() {
        return this.channel(ChannelId.SET_POWER_LEVE_KW_DOUBLE);
    }

    /**
     * Checks if the PowerPlant has a set PowerLevel KW set. After that the stored value will be written to the actual {@link PowerPlant}
     * This will be usually not the case. Usually Controller should write in the {@link PowerPlant} and the Impl.
     * Class handles it to write in the ModbusChannel.
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasSetPowerLevelKw() {
        return GeneratorModbus.getValueDefinedChannel(this._setPowerLevelKwLongChannel(), this._setPowerLevelKwDoubleChannel());
    }

    /**
     * Gets the Channel for {@link ChannelId#MAXIMUM_KW_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getMaximumKwLongChannel() {
        return this.channel(ChannelId.MAXIMUM_KW_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#MAXIMUM_KW_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getMaximumKwDoubleChannel() {
        return this.channel(ChannelId.MAXIMUM_KW_DOUBLE);
    }

    /**
     * Checks if the PowerPlant has a set PowerLevel KW set. After that the stored value will be written to the actual {@link PowerPlant}
     * This will be usually not the case. Usually Controller should write in the {@link PowerPlant} and the Impl.
     * Class handles it to write in the ModbusChannel.
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasMaximumKw() {
        return GeneratorModbus.getValueDefinedChannel(this._getMaximumKwLongChannel(), this._getMaximumKwDoubleChannel());
    }
}
