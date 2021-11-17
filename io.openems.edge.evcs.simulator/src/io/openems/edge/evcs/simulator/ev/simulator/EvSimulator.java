package io.openems.edge.evcs.simulator.ev.simulator;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface EvSimulator extends OpenemsComponent {

    /**
     * <ul>
     * <li>Interface:
     * <li>Type:
     * <li>Unit:
     * </ul>
     */
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        PHASES(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        CHARGE_POWER(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    default Channel<Integer> getChargePowerChannel() {
        return this.channel(ChannelId.CHARGE_POWER);
    }

    default WriteChannel<Integer> getPhasesChannel() {
        return this.channel(ChannelId.PHASES);
    }

    default int getPhases() {
        Channel<Integer> channel = this.getPhasesChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    default int getChargePower() {
        Channel<Integer> channel = this.getChargePowerChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    default void setChargePower(int value) {
        Channel<Integer> channel = this.getChargePowerChannel();
        channel.setNextValue(value);
    }

    default void setPhases(int value) {
        Channel<Integer> channel = this.getPhasesChannel();
        channel.setNextValue(value);
    }
}

