package io.openems.edge.controller.optimizer.simulator;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Simulator extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Integer Test Value Channel.
         *
         * <ul>
         * <li>Interface: Simulator
         * <li>Type: Integer
         * </ul>
         */
        WRITE_CHANNEL(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Float Test Value Channel.
         *
         * <ul>
         * <li>Interface: Simulator
         * <li>Type: Float
         * </ul>
         */
        WRITE_CHANNEL_FLOAT(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),

        /**
         * Test enableSignal Channel.
         *
         * <ul>
         * <li>Interface: Simulator
         * <li>Type: Boolean
         * </ul>
         */
        ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Return Integer Write channel for the Test Value.
     *
     * @return Integer WriteChannel Channel
     */
    default WriteChannel<Integer> getWriteChannel() {
        return this.channel(ChannelId.WRITE_CHANNEL);
    }

    /**
     * Return the Integer Test Value as a String.
     *
     * @return Test value as String
     */
    default String getWriteString() {
        if (this.getWriteChannel().value().isDefined()) {
            return this.getWriteChannel().value().get() + "";
        } else if (this.getWriteChannel().getNextWriteValue().isPresent()) {
            return this.getWriteChannel().getNextWriteValueAndReset().orElse(null) + "";
        } else {
            return "null";
        }
    }

    /**
     * Return Float Write channel for the Test Value.
     *
     * @return Float WriteChannel Channel
     */
    default WriteChannel<Integer> getWriteChannelFloat() {
        return this.channel(ChannelId.WRITE_CHANNEL_FLOAT);
    }

    /**
     * Return the Float Test Value as a String.
     *
     * @return Test value as String
     */
    default String getWriteFloatString() {
        if (this.getWriteChannelFloat().value().isDefined()) {
            return this.getWriteChannelFloat().value().get() + "";
        } else if (this.getWriteChannelFloat().getNextWriteValue().isPresent()) {
            return this.getWriteChannelFloat().getNextWriteValueAndReset().orElse(null) + "";
        } else {
            return "null";
        }
    }

    /**
     * Return enableSignal channel.
     *
     * @return enableSignal Channel
     */
    default WriteChannel<Boolean> getEnableChannel() {
        return this.channel(ChannelId.ENABLE_SIGNAL);
    }

    /**
     * Return enableSignal Value.
     *
     * @return enableSignal Value
     */
    default String getEnableString() {
        if (this.getEnableChannel().value().isDefined()) {
            return this.getEnableChannel().value().get() + "";
        } else if (this.getEnableChannel().getNextWriteValue().isPresent()) {
            return this.getEnableChannel().getNextWriteValueAndReset().orElse(null) + "";
        } else {
            return "null";
        }
    }


}
