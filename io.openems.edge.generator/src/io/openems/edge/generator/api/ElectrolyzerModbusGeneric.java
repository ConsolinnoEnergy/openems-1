package io.openems.edge.generator.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;

public interface ElectrolyzerModbusGeneric extends GeneratorModbus {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         *
         * <ul>
         *      <li>
         * </ul>
         */

        WMZ_ENERGY_PRODUCED_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        WMZ_ENERGY_PRODUCED_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SOURCE_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SOURCE_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SINK_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SINK_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),
        WMZ_POWER_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        WMZ_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY));
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
    default Channel<Long> _getWMZPowerLongChannel() {
        return this.channel(ChannelId.WMZ_POWER_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_POWER_LONG}.
     *
     * @return the Channel
     */
    default Channel<Double> _getWMZPowerDoubleChannel() {
        return this.channel(ChannelId.WMZ_POWER_DOUBLE);
    }

    /**
     * Checks if the Electrolyzer has a WMZ Power set. After that the stored value will be written to the actual {@link Electrolyzer}
     * Only call this within the implementing Class.
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasWMZPower() {
        return GeneratorModbus.getValueDefinedChannel(this._getWMZPowerDoubleChannel(), this._getWMZPowerLongChannel());
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SINK_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getWMZTempSinkLongChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SINK_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SINK_LONG}.
     *
     * @return the Channel
     */
    default Channel<Double> _getWMZTempSinkDoubleChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SINK_DOUBLE);
    }



    /**
     * Checks if the Electrolyzer has a WMZ Temp Sink set. After that the stored value will be written to the actual {@link Electrolyzer}
     * Only call this within the implementing Class.
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasWMZTempSink() {
        return GeneratorModbus.getValueDefinedChannel(this._getWMZTempSinkLongChannel(), this._getWMZTempSinkDoubleChannel());
    }


    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SOURCE_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getWMZTempSourceLongChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SOURCE_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SOURCE_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getWMZTempSourceDoubleChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SOURCE_DOUBLE);
    }


    /**
     * Checks if the Electrolyzer has a WMZ Temp Source set. After that the stored value will be written to the actual {@link Electrolyzer}
     * Only call this within the implementing Class.
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasWMZTempSource() {
        return GeneratorModbus.getValueDefinedChannel(this._getWMZTempSourceLongChannel(), this._getWMZTempSourceDoubleChannel());
    }


    /**
     * Gets the Channel for {@link ChannelId#WMZ_ENERGY_PRODUCED_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getWMZEnergyProducedLongChannel() {
        return this.channel(ChannelId.WMZ_ENERGY_PRODUCED_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_ENERGY_PRODUCED_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getWMZEnergyProducedDoubleChannel() {
        return this.channel(ChannelId.WMZ_ENERGY_PRODUCED_DOUBLE);
    }

    /**
     * Checks if the Electrolyzer has a WMZ Energy Produced set. After that the stored value will be written to the actual {@link Electrolyzer}
     * Only call this within the implementing Class.
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasWMZEnergyProduced() {
        return GeneratorModbus.getValueDefinedChannel(this._getWMZEnergyProducedLongChannel(), this._getWMZEnergyProducedDoubleChannel());
    }

    /**
     * Checks if the Electrolyzer has a WMZ Energy Produced set. After that the stored value will be written to the actual {@link Electrolyzer}
     * Only call this within the implementing Class.
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasPowerSetPointPercent() {
        return GeneratorModbus.getValueDefinedChannel(this._getSetPointPowerPercentLongChannel(), this._getSetPointPowerPercentDoubleChannel());
    }
    /**
     * Checks if the Electrolyzer has a WMZ Energy Produced set. After that the stored value will be written to the actual {@link Electrolyzer}
     * Only call this within the implementing Class.
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasPowerSetPoint() {
        return GeneratorModbus.getValueDefinedChannel(this._getSetPointPowerLongChannel(), this._getSetPointPowerDoubleChannel());
    }

}
