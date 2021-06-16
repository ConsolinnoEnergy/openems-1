package io.openems.edge.evcs.compleo.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Compleo extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Maximum allowed Power on the Station.
         * <ul>
         * <li>Interface: Compleo
         * <li>Type:Short
         * <li>Unit:Ampere
         * </ul>
         */
        MAX_POWER(Doc.of(OpenemsType.SHORT).unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Status.
         * <ul>
         * <li>Interface: Compleo
         * <li>Type:Short
         * <li>Unit:
         * </ul>
         */
        STATUS(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current Power bein Drawn.
         * <ul>
         * <li>Interface: Compleo
         * <li>Type:Short
         * <li>Unit:W
         * </ul>
         */
        POWER(Doc.of(OpenemsType.SHORT).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on L1.
         * <ul>
         * <li>Interface: Compleo
         * <li>Type:Short
         * <li>Unit:Ampere
         * </ul>
         */
        CURRENT_L1(Doc.of(OpenemsType.SHORT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on L2.
         * <ul>
         * <li>Interface: Compleo
         * <li>Type:Short
         * <li>Unit:Ampere
         * </ul>
         */
        CURRENT_L2(Doc.of(OpenemsType.SHORT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on L3.
         * <ul>
         * <li>Interface: Compleo
         * <li>Type:Short
         * <li>Unit:Ampere
         * </ul>
         */
        CURRENT_L3(Doc.of(OpenemsType.SHORT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Energy Consumption sum of the current Session.
         * <ul>
         * <li>Interface: Compleo
         * <li>Type:Short
         * <li>Unit:Wh
         * </ul>
         */
        ENERGY_SESSION(Doc.of(OpenemsType.SHORT).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link Compleo.ChannelId#MAX_POWER}.
     *
     * @return the Channel
     */
    default WriteChannel<Short> getMaxPowerChannel() {
        return this.channel(ChannelId.MAX_POWER);
    }

    /**
     * Gets the Value of {@link Compleo.ChannelId#MAX_POWER}.
     *
     * @return the value
     */
    default short getMaxAllowedPower() {
        Channel<Short> channel = this.getMaxPowerChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Sets the Value of {@link Compleo.ChannelId#MAX_POWER}.
     *
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setMaxPower(short value) throws OpenemsError.OpenemsNamedException {
        WriteChannel<Short> channel = this.getMaxPowerChannel();
        channel.setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link Compleo.ChannelId#STATUS}.
     *
     * @return the Channel
     */
    default Channel<Short> getEvStatusChannel() {
        return this.channel(ChannelId.STATUS);
    }

    /**
     * Gets the Value of {@link Compleo.ChannelId#STATUS}.
     *
     * @return the value
     */
    default short getEvStatus() {
        Channel<Short> channel = this.getEvStatusChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Compleo.ChannelId#POWER}.
     *
     * @return the Channel
     */
    default Channel<Short> getPowerChannel() {
        return this.channel(ChannelId.POWER);
    }

    /**
     * Gets the Value of {@link Compleo.ChannelId#POWER}.
     *
     * @return the value
     */
    default short getPower() {
        Channel<Short> channel = this.getPowerChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Compleo.ChannelId#CURRENT_L1}.
     *
     * @return the Channel
     */
    default Channel<Short> getCurrentL1Channel() {
        return this.channel(ChannelId.CURRENT_L1);
    }

    /**
     * Gets the Value of {@link Compleo.ChannelId#CURRENT_L1}.
     *
     * @return the value
     */
    default short getCurrentL1() {
        Channel<Short> channel = this.getCurrentL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Compleo.ChannelId#CURRENT_L2}.
     *
     * @return the Channel
     */
    default Channel<Short> getCurrentL2Channel() {
        return this.channel(ChannelId.CURRENT_L2);
    }

    /**
     * Gets the Value of {@link Compleo.ChannelId#CURRENT_L2}.
     *
     * @return the value
     */
    default short getCurrentL2() {
        Channel<Short> channel = this.getCurrentL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Compleo.ChannelId#CURRENT_L3}.
     *
     * @return the Channel
     */
    default Channel<Short> getCurrentL3Channel() {
        return this.channel(ChannelId.CURRENT_L3);
    }

    /**
     * Gets the Value of {@link Compleo.ChannelId#CURRENT_L3}.
     *
     * @return the value
     */
    default short getCurrentL3() {
        Channel<Short> channel = this.getCurrentL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Compleo.ChannelId#ENERGY_SESSION}.
     *
     * @return the Channel
     */
    default Channel<Short> getEnergyChannel() {
        return this.channel(ChannelId.ENERGY_SESSION);
    }

    /**
     * Gets the Value of {@link Compleo.ChannelId#ENERGY_SESSION}.
     *
     * @return the value
     */
    default short getEnergy() {
        Channel<Short> channel = this.getEnergyChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }
}

