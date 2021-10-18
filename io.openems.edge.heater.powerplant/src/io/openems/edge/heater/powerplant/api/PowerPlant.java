package io.openems.edge.heater.powerplant.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.heater.api.Heater;


public interface PowerPlant extends Heater {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        MAXIMUM_KW(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY).unit(Unit.KILOWATT)),

        ERROR_OCCURRED(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    default Channel<Long> getMaximumKwChannel() {
        return this.channel(ChannelId.MAXIMUM_KW);
    }

    default Channel<Boolean> getErrorOccurredChannel() {
        return this.channel(ChannelId.MAXIMUM_KW);
    }
}