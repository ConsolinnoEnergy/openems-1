package io.openems.edge.bridge.mqtt.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This interface provides the Nature for a Schedule.
 * This Nature is for a more complex schedule of Components (Not just a simple method: SetSchedule value: foo expiration: bar Schedule)
 * The Schedule needs to be extracted manually.
 */
public interface Schedule extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {


        SCHEDULE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue)));


        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Get The Schedule Channel.
     *
     * @return the channel.
     */
    default WriteChannel<String> getSchedule() {
        return this.channel(ChannelId.SCHEDULE);
    }
}
