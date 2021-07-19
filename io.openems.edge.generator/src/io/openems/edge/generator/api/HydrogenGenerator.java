package io.openems.edge.generator.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;

public interface HydrogenGenerator extends Generator {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        ENABLE_HYDROGEN_USE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    default WriteChannel<Boolean> getEnableHydrogenUse(){
        return this.channel(ChannelId.ENABLE_HYDROGEN_USE);
    }
}
