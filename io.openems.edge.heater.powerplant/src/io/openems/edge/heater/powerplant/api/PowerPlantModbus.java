package io.openems.edge.heater.powerplant.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.GenericModbusComponent;
import io.openems.edge.heater.api.HeaterModbus;


public interface PowerPlantModbus extends HeaterModbus {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {


        MAXIMUM_KW_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        MAXIMUM_KW_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        ERROR_OCCURRED_BOOLEAN(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
        ERROR_OCCURRED_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY));

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
        return GenericModbusComponent.getValueDefinedChannel(this._getMaximumKwLongChannel(), this._getMaximumKwDoubleChannel());
    }

    default Channel<Boolean> _getErrorOccurredBooleanChannel() {
        return this.channel(ChannelId.ERROR_OCCURRED_BOOLEAN);
    }

    default Channel<Long> _getErrorOccurredLongChannel() {
        return this.channel(ChannelId.ERROR_OCCURRED_LONG);
    }

    default Channel<?> _hasErrorOccurred() {
        return GenericModbusComponent.getValueDefinedChannel(this._getErrorOccurredBooleanChannel(), this._getErrorOccurredLongChannel());
    }
}
