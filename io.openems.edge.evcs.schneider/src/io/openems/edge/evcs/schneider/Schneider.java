package io.openems.edge.evcs.schneider;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Schneider extends OpenemsComponent {

    /**
     * <ul>
     * <li>Interface:
     * <li>Type:
     * <li>Unit:
     * </ul>
     */
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        WRITE_CHANNEL(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),
        READ_CHANNEL(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    default Channel<Float> getReadChannel() {
        return this.channel(ChannelId.READ_CHANNEL);
    }

    default WriteChannel<Float> getWriteChannel() {
        return this.channel(ChannelId.WRITE_CHANNEL);
    }


    default float getReadChannelValue() {
        if (this.getReadChannel().value().isDefined()) {
            return this.getReadChannel().value().get();
        } else if (this.getReadChannel().getNextValue().isDefined()) {
            return this.getReadChannel().getNextValue().get();
        } else {
            return -9001;
        }
    }
}

