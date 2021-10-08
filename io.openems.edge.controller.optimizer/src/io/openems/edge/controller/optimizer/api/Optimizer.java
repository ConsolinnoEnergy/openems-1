package io.openems.edge.controller.optimizer.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

import java.util.List;

public interface Optimizer extends OpenemsComponent {

    void handleNewSchedule(List<List<String>> schedule);

    void deleteChannel(String channelId);

    void addFallbackSchedule(List<List<String>> fallbackSchedule);



    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Schedule Json for the Optimizer.
         *
         * <ul>
         * <li>Interface: Optimizer
         * <li>Type: String
         * <li>Unit: SCHEDULE_JSON
         * </ul>
         */

        SCHEDULE_JSON(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),
        FALLBACK_JSON(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),
        STATUS(Doc.of(OpenemsType.STRING));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    default WriteChannel<String> getJsonChannel() {
        return this.channel(ChannelId.SCHEDULE_JSON);
    }
    default WriteChannel<String> getFallbackChannel() {
        return this.channel(ChannelId.FALLBACK_JSON);
    }



    default Channel<String> getStatusChannel() {
        return this.channel(ChannelId.STATUS);
    }

    default String getJsonString() {
        if (this.getJsonChannel().value().isDefined()) {
            return this.getJsonChannel().value().get();
        } else if (this.getJsonChannel().getNextWriteValue().isPresent()) {
            return this.getJsonChannel().getNextWriteValueAndReset().orElse("null");
        } else {
            return "null";
        }
    }

    default String getFallbackString() {
        if (this.getFallbackChannel().value().isDefined()) {
            return this.getFallbackChannel().value().get();
        } else if (this.getFallbackChannel().getNextWriteValue().isPresent()) {
            return this.getFallbackChannel().getNextWriteValue().orElse("null");
        } else {
            return "null";
        }
    }

    default String getStatus() {
        if (this.getStatusChannel().value().isDefined()) {
            return this.getStatusChannel().value().get();
        } else if (this.getStatusChannel().getNextValue().isDefined()) {
            return this.getStatusChannel().getNextValue().get();
        } else {
            return "null";
        }
    }


}
