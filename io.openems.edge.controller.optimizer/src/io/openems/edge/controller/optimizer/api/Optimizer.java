package io.openems.edge.controller.optimizer.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

import java.util.List;

public interface Optimizer extends OpenemsComponent {

    /**
     * Tell the JsonPatchWorker to add the schedule given by the Translator.
     *
     * @param schedule return of the Translator
     */
    void handleNewSchedule(List<List<String>> schedule);

    /**
     * Delete Channel if a Translator deactivates.
     *
     * @param channelId The Channel that has to be deleted from the Json
     */
    void deleteChannel(String channelId);

    /**
     * Tell the JsonPatchWorker to add the fallback given by the Translator into the Fallback schedule.
     *
     * @param fallbackSchedule The Fallback Schedule part
     */
    void addFallbackSchedule(List<List<String>> fallbackSchedule);


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Schedule Json for the Optimizer.
         *
         * <ul>
         * <li>Interface: Optimizer
         * <li>Type: String
         * </ul>
         */
        SCHEDULE_JSON(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),

        /**
         * Fallback Schedule Json for the Optimizer.
         *
         * <ul>
         * <li>Interface: Optimizer
         * <li>Type: String
         * </ul>
         */
        FALLBACK_JSON(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE)),

        /**
         * Current Status of the Optimizer.
         * "Online" if the Optimizer is connected to the Mqtt Broker.
         * "Error" otherwise.
         *
         * <ul>
         * <li>Interface: Optimizer
         * <li>Type: String
         * </ul>
         */
        STATUS(Doc.of(OpenemsType.STRING));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Return channel for the Schedule.
     *
     * @return String Schedule Channel
     */
    default WriteChannel<String> getJsonChannel() {
        return this.channel(ChannelId.SCHEDULE_JSON);
    }

    /**
     * Return channel for the Fallback Schedule.
     *
     * @return String Fallback Schedule Channel
     */
    default WriteChannel<String> getFallbackChannel() {
        return this.channel(ChannelId.FALLBACK_JSON);
    }

    /**
     * Return channel for the Status.
     *
     * @return String Status Channel
     */
    default Channel<String> getStatusChannel() {
        return this.channel(ChannelId.STATUS);
    }

    /**
     * Return the Schedule Json as String.
     *
     * @return String Schedule
     */
    default String getJsonString() {
        if (this.getJsonChannel().value().isDefined()) {
            return this.getJsonChannel().value().get();
        } else if (this.getJsonChannel().getNextWriteValue().isPresent()) {
            return this.getJsonChannel().getNextWriteValueAndReset().orElse("null");
        } else {
            return "null";
        }
    }

    /**
     * Return the Fallback Schedule Json as String.
     *
     * @return String Fallback Schedule Channel
     */
    default String getFallbackString() {
        if (this.getFallbackChannel().value().isDefined()) {
            return this.getFallbackChannel().value().get();
        } else if (this.getFallbackChannel().getNextWriteValue().isPresent()) {
            return this.getFallbackChannel().getNextWriteValue().orElse("null");
        } else {
            return "null";
        }
    }

    /**
     * Return the Status as String.
     *
     * @return String Fallback Schedule Channel
     */
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
