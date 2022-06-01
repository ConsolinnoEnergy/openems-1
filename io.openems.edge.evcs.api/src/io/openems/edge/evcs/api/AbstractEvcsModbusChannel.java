package io.openems.edge.evcs.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface AbstractEvcsModbusChannel extends OpenemsComponent {

    /**
     * <ul>
     * <li>Interface:
     * <li>Type:
     * <li>Unit:
     * </ul>
     */
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        MAXIMUM_CHARGE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)),
        CURRENT_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        CURRENT_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        CURRENT_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        INTERNAL_CHARGE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        EV_STATUS(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY))
        ;
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }
    /**
     * Gets the Channel for {@link ChannelId#MAXIMUM_CHARGE_POWER}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getMaximumChargePowerChannel() {
        return this.channel(ChannelId.MAXIMUM_CHARGE_POWER);
    }

    /**
     * Gets the Value of {@link ChannelId#MAXIMUM_CHARGE_POWER}.
     *
     * @return the value
     */
    default int getMaximumChargePower() {
        Channel<Integer> channel = this.getMaximumChargePowerChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Sets the Value of {@link ChannelId#MAXIMUM_CHARGE_POWER}.
     *
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setMaximumChargePower(int value) throws OpenemsError.OpenemsNamedException {
        WriteChannel<Integer> channel = this.getMaximumChargePowerChannel();
        channel.setNextWriteValue(value);
    }
    /**
     * Gets the Channel for {@link ChannelId#INTERNAL_CHARGE_POWER}.
     *
     * @return the Channel
     */
    default Channel<Integer> getInternalChargePowerChannel() {
        return this.channel(ChannelId.INTERNAL_CHARGE_POWER);
    }

    /**
     * Gets the Value of {@link ChannelId#INTERNAL_CHARGE_POWER}.
     *
     * @return the value
     */
    default int getInternalChargePower() {
        Channel<Integer> channel = this.getInternalChargePowerChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }
    /**
     * Gets the Channel for {@link ChannelId#CURRENT_L1}.
     *
     * @return the Channel
     */
    default Channel<Integer> getCurrentL1Channel() {
        return this.channel(ChannelId.CURRENT_L1);
    }

    /**
     * Gets the Value of {@link ChannelId#CURRENT_L1}.
     *
     * @return the value
     */
    default int getCurrentL1() {
        Channel<Integer> channel = this.getCurrentL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }
    /**
     * Gets the Channel for {@link ChannelId#CURRENT_L2}.
     *
     * @return the Channel
     */
    default Channel<Integer> getCurrentL2Channel() {
        return this.channel(ChannelId.CURRENT_L2);
    }

    /**
     * Gets the Value of {@link ChannelId#CURRENT_L2}.
     *
     * @return the value
     */
    default int getCurrentL2() {
        Channel<Integer> channel = this.getCurrentL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }
    /**
     * Gets the Channel for {@link ChannelId#CURRENT_L3}.
     *
     * @return the Channel
     */
    default Channel<Integer> getCurrentL3Channel() {
        return this.channel(ChannelId.CURRENT_L3);
    }

    /**
     * Gets the Value of {@link ChannelId#CURRENT_L3}.
     *
     * @return the value
     */
    default int getCurrentL3() {
        Channel<Integer> channel = this.getCurrentL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link ChannelId#EV_STATUS}.
     *
     * @return the Channel
     */
    default Channel<Integer> getEvStatusChannel() {
        return this.channel(ChannelId.EV_STATUS);
    }

    /**
     * Gets the Value of {@link ChannelId#EV_STATUS}.
     *
     * @return the value
     */
    default int getEvStatus() {
        Channel<Integer> channel = this.getEvStatusChannel();
        return channel.value().orElse(channel.getNextValue().orElse(-1));
    }
}

