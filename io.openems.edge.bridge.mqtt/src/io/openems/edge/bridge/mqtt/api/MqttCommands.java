package io.openems.edge.bridge.mqtt.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This Nature allows the CommandComponent to write the commandvalues into corresponding channel.
 * Any Device that should react to commands, can implement this nature and react if any channel has anything written into.
 * Remember: If you want more commands, simply expand the MqttcommandType, Integrate the Channel here and tell the
 * CommandComponent to write the Value into the corresponding channel. (Can be improved tho)
 */
public interface MqttCommands extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        SET_TEMPERATURE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue))),
        SET_SCHEDULE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue))),
        SET_PERFORMANCE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue))),
        SET_POWER(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
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

    default WriteChannel<String> getSetTemperature() {
        return this.channel(ChannelId.SET_TEMPERATURE);
    }

    default WriteChannel<String> getSetSchedule() {
        return this.channel(ChannelId.SET_SCHEDULE);
    }

    default WriteChannel<String> getSetPerformance() {
        return this.channel(ChannelId.SET_PERFORMANCE);
    }

    default WriteChannel<String> getSetPower() {
        return this.channel(ChannelId.SET_POWER);
    }

}
