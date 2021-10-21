package io.openems.edge.utility.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.GenericModbusComponent;

public interface VirtualChannelModbus extends GenericModbusComponent {


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Reading a Long Param.
         * <ul>
         * <li> Interface: VirtualChannelModbus
         * <li> Type: Long
         * </ul>
         */
        READ_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),


        /**
         * Reading a Double Param.
         * <ul>
         * <li> Interface: VirtualChannelModbus
         * <li> Type: Double
         * </ul>
         */
        READ_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),


        /**
         * Reading a String Param.
         * <ul>
         * <li> Interface: VirtualChannelModbus
         * <li> Type: String
         * </ul>
         */
        READ_STRING(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),

        /**
         * Reading a Boolean Param.
         * <ul>
         * <li> Interface: VirtualChannelModbus
         * <li> Type: Boolean
         * </ul>
         */
        READ_BOOLEAN(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),


        /**
         * Writing a Long Param.
         * <ul>
         * <li> Interface: VirtualChannelModbus
         * <li> Type: Long
         * </ul>
         */
        WRITE_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        /**
         * Reading a Double Param.
         * <ul>
         * <li> Interface: VirtualChannelModbus
         * <li> Type: Double
         * </ul>
         */
        WRITE_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Reading a String Param.
         * <ul>
         * <li> Interface: VirtualChannelModbus
         * <li> Type: String
         * </ul>
         */
        WRITE_STRING(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),
        /**
         * Reading a Boolean Param.
         * <ul>
         * <li> Interface: VirtualChannelModbus
         * <li> Type: Boolean
         * </ul>
         */
        WRITE_BOOLEAN(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Long Read Channel.
     *
     * @return the Channel.
     */
    default Channel<Long> _getReadLongChannel() {
        return this.channel(ChannelId.READ_LONG);
    }

    /**
     * Gets the Double Read Channel.
     *
     * @return the Channel.
     */
    default Channel<Double> _getReadDoubleChannel() {
        return this.channel(ChannelId.READ_DOUBLE);
    }

    /**
     * Gets the String Read Channel.
     *
     * @return the Channel.
     */
    default Channel<String> _getReadStringChannel() {
        return this.channel(ChannelId.READ_STRING);
    }

    /**
     * Gets the Boolean Read Channel.
     *
     * @return the Channel.
     */
    default Channel<Boolean> _getReadBooleanChannel() {
        return this.channel(ChannelId.READ_BOOLEAN);
    }

    /**
     * Gets the Long Write Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<Long> _getWriteLongChannel() {
        return this.channel(ChannelId.WRITE_LONG);
    }

    /**
     * Gets the Double Write Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<Double> _getWriteDoubleChannel() {
        return this.channel(ChannelId.WRITE_DOUBLE);
    }

    /**
     * Gets the String Write Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<String> _getWriteStringChannel() {
        return this.channel(ChannelId.WRITE_STRING);
    }

    /**
     * Gets the Long Write Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<Boolean> _getWriteBooleanChannel() {
        return this.channel(ChannelId.WRITE_LONG);
    }

    /**
     * Checks if the Read Value is available.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasReadLong() {
        return GenericModbusComponent.getValueDefinedChannel(this._getReadLongChannel(), this._getReadLongChannel());
    }

    /**
     * Checks if the Read Value is available.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasReadDouble() {
        return GenericModbusComponent.getValueDefinedChannel(this._getReadDoubleChannel(), this._getReadDoubleChannel());
    }

    /**
     * Checks if the Read Value is available.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasReadString() {
        return GenericModbusComponent.getValueDefinedChannel(this._getReadStringChannel(), this._getReadStringChannel());
    }

    /**
     * Checks if the Read Value is available.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasReadBoolean() {
        return GenericModbusComponent.getValueDefinedChannel(this._getReadBooleanChannel(), this._getReadBooleanChannel());
    }
}