package io.openems.edge.utility.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The Nature of the VirtualChannel. It allows Components to write any Output into this.
 * Other Utility classes can use the input to write their output to a real or another virtual Channel.
 */
public interface VirtualChannel extends OpenemsComponent {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Virtual Boolean.
         *
         * <ul>
         * <li>Interface: VirtualChannel
         * <li>Type: Boolean
         * </ul>
         */
        VIRTUAL_BOOLEAN(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * Virtual Integer.
         *
         * <ul>
         * <li>Interface: VirtualChannel
         * <li>Type: Integer
         * </ul>
         */
        VIRTUAL_INTEGER(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Virtual String.
         *
         * <ul>
         * <li>Interface: VirtualChannel
         * <li>Type: String
         * </ul>
         */
        VIRTUAL_STRING(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),
        /**
         * Virtual Double.
         *
         * <ul>
         * <li>Interface: VirtualChannel
         * <li>Type: Double
         * </ul>
         */
        VIRTUAL_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Get the {@link ChannelId#VIRTUAL_BOOLEAN} Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<Boolean> getBooleanChannel() {
        return this.channel(ChannelId.VIRTUAL_BOOLEAN);
    }

    /**
     * Get the {@link ChannelId#VIRTUAL_INTEGER} Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<Integer> getIntegerChannel() {
        return this.channel(ChannelId.VIRTUAL_INTEGER);
    }

    /**
     * Get the {@link ChannelId#VIRTUAL_STRING} Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<String> getStringChannel() {
        return this.channel(ChannelId.VIRTUAL_STRING);
    }

    /**
     * Get the {@link ChannelId#VIRTUAL_DOUBLE} Channel.
     *
     * @return the Channel.
     */
    default WriteChannel<Double> getDoubleChannel() {
        return this.channel(ChannelId.VIRTUAL_DOUBLE);
    }


}

