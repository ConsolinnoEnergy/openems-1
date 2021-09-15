package io.openems.edge.controller.optimizer.simulator;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Simulator extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        WRITE_CHANNEL(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        WRITE_CHANNEL_FLOAT(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),
        ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    default WriteChannel<Integer> getWriteChannel() {
        return this.channel(ChannelId.WRITE_CHANNEL);
    }

    default String getWriteString() {
        if (this.getWriteChannel().value().isDefined()) {
            return this.getWriteChannel().value().get() + "";
        } else if (this.getWriteChannel().getNextWriteValue().isPresent()) {
            return this.getWriteChannel().getNextWriteValueAndReset().orElse(null) + "";
        } else {
            return "null";
        }
    }

    default WriteChannel<Integer> getWriteChannelFloat() {
        return this.channel(ChannelId.WRITE_CHANNEL_FLOAT);
    }

    default String getWriteFloatString() {
        if (this.getWriteChannelFloat().value().isDefined()) {
            return this.getWriteChannelFloat().value().get() + "";
        } else if (this.getWriteChannelFloat().getNextWriteValue().isPresent()) {
            return this.getWriteChannelFloat().getNextWriteValueAndReset().orElse(null) + "";
        } else {
            return "null";
        }
    }

    default WriteChannel<Boolean> getEnableChannel() {
        return this.channel(ChannelId.ENABLE_SIGNAL);
    }

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
