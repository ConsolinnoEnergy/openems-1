package io.openems.edge.generator.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface GeneratorModbus extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * Generator Power
         * <ul>
         *      <li> Type: Double
         * </ul>
         */

        POWER_DOUBLE_READ(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        POWER_LONG_READ(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),

        POWER_PERCENT_DOUBLE_READ(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        POWER_PERCENT_LONG_READ(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),

        SET_POINT_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        SET_POINT_POWER_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),

        SET_POINT_POWER_PERCENT_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),

        SET_POINT_POWER_PERCENT_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#POWER_LONG_READ}.
     *
     * @return the Channel
     */
    default Channel<Long> _getPowerLongChannel() {
        return this.channel(ChannelId.POWER_LONG_READ);
    }


    /**
     * Gets the Channel for {@link ChannelId#POWER_LONG_READ}.
     *
     * @return the Channel
     */
    default Channel<Double> _getPowerDoubleChannel() {
        return this.channel(ChannelId.POWER_DOUBLE_READ);
    }

    /**
     * Gets the Channel for {@link ChannelId#POWER_LONG_READ}.
     *
     * @return the Channel
     */
    default Channel<Long> _getPowerPercentLongChannel() {
        return this.channel(ChannelId.POWER_LONG_READ);
    }


    /**
     * Gets the Channel for {@link ChannelId#POWER_LONG_READ}.
     *
     * @return the Channel
     */
    default Channel<Double> _getPowerPercentDoubleChannel() {
        return this.channel(ChannelId.POWER_DOUBLE_READ);
    }

    /**
     * Gets the Channel for {@link ChannelId#SET_POINT_POWER_LONG}.
     *
     * @return the Channel
     */
    default WriteChannel<Long> _getSetPointPowerLongChannel() {
        return this.channel(ChannelId.SET_POINT_POWER_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#SET_POINT_POWER_DOUBLE}.
     *
     * @return the Channel
     */
    default WriteChannel<Double> _getSetPointPowerDoubleChannel() {
        return this.channel(ChannelId.SET_POINT_POWER_DOUBLE);
    }

    /**
     * Gets the Channel for {@link ChannelId#SET_POINT_POWER_PERCENT_LONG}.
     *
     * @return the Channel
     */
    default WriteChannel<Long> _getSetPointPowerPercentLongChannel() {
        return this.channel(ChannelId.SET_POINT_POWER_PERCENT_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#SET_POINT_POWER_PERCENT_DOUBLE}.
     *
     * @return the Channel
     */
    default WriteChannel<Double> _getSetPointPowerPercentDoubleChannel() {
        return this.channel(ChannelId.SET_POINT_POWER_PERCENT_DOUBLE);
    }




    /**
     * Checks if the Generator has a Reading Power set. After that the stored value will be written to the actual {@link Generator}
     * Only call this within the implementing Class.
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasPower() {
        return getValueDefinedChannel(this._getPowerDoubleChannel(), this._getPowerLongChannel());
    }


    /**
     * Checks if the Generator has a Reading Power set. After that the stored value will be written to the actual {@link Generator}
     * Only call this within the implementing Class.
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasPowerPercent() {
        return getValueDefinedChannel(this._getPowerPercentDoubleChannel(), this._getPowerPercentLongChannel());
    }

    static Channel<?> getValueDefinedChannel(Channel<?> firstChannel, Channel<?> secondChannel) {
        if (firstChannel.getNextValue().isDefined()) {
            return firstChannel;
        } else if (secondChannel.getNextValue().isDefined()) {
            return secondChannel;
        } else {
            return null;
        }
    }


}
