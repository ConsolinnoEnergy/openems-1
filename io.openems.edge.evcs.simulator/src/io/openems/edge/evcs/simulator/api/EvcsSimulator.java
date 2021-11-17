package io.openems.edge.evcs.simulator.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface EvcsSimulator extends OpenemsComponent {

    /**
     *
     * <ul>
     * <li>Interface:
     * <li>Type:
     * <li>Unit:
     * </ul>
     */
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        PHASES(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        CAR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
        CHARGE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    default Channel<Boolean> getCarChannel() {
        return this.channel(ChannelId.CAR);
    }


    default boolean getCarValue() {
        Channel<Boolean> channel = this.getCarChannel();
        return channel.value().orElse(channel.getNextValue().orElse(false));
    }

    default void setCar(boolean value) {
        Channel<Boolean> channel = this.getCarChannel();
        channel.setNextValue(value);
    }

    default Channel<Integer> getChargeChannel() {
        return this.channel(ChannelId.CHARGE);
    }


    default int getChargeValue() {
        Channel<Integer> channel = this.getChargeChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    default void setCharge(int value) {
        Channel<Integer> channel = this.getChargeChannel();
        channel.setNextValue(value);
    }
    default Channel<Integer> getSimulatorPhasesChannel() {
        return this.channel(ChannelId.PHASES);
    }


    default int getPhasesValue() {
        Channel<Integer> channel = this.getSimulatorPhasesChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    default void setPhases(int value) {
        Channel<Integer> channel = this.getSimulatorPhasesChannel();
        channel.setNextValue(value);
    }
}

