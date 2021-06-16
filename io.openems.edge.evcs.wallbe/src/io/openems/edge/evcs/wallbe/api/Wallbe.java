package io.openems.edge.evcs.wallbe.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Wallbe extends OpenemsComponent {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Sets the Maximum allowed Current the Station can charge with.
         * The value is in 100mA so 130 in the Channel means 13A in the Station.
         * <ul>
         * <li>Interface: Wallbe
         * <li>Type: Short
         * <li>Unit: A
         * </ul>
         */
        MAXIMUM_CHARGE_CURRENT(Doc.of(OpenemsType.SHORT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Bit to Set if Charging is allowed or not.
         * <ul>
         * <li>Interface: Wallbe
         * <li>Type: Boolean
         * <li>Unit: Na
         * </ul>
         */
        CHARGE_ENABLE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * The Current Status of the EV.
         * See WallbeStatus for more info.
         * <ul>
         * <li>Interface: Wallbe
         * <li>Type: String
         * <li>Unit: Na
         * </ul>
         */
        STATUS(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        /**
         * The Time for how long the current EV is connected to the station.
         * <ul>
         * <li>Interface: Wallbe
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        LOAD_TIME(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * The Current Dip-Switch Configuration of the EVCS.
         * <ul>
         * <li>Interface: Wallbe
         * <li>Type: Short
         * <li>Unit: Na
         * </ul>
         */
        DIP_SWITCHES(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Firmware-Version of the Station.
         * <ul>
         * <li>Interface: Wallbe
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        FIRMWARE_VERSION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current Error Code, if an Error occurred.
         * <ul>
         * <li>Interface: Wallbe
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        ERROR(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on Phase L1.
         * <ul>
         * <li>Interface: Wallbe
         * <li>Type: Integer
         * <li>Unit: A
         * </ul>
         */
        CURRENT_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on Phase L2.
         * <ul>
         * <li>Interface: Wallbe
         * <li>Type: Integer
         * <li>Unit: A
         * </ul>
         */
        CURRENT_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on Phase L3.
         * <ul>
         * <li>Interface: Wallbe
         * <li>Type: Integer
         * <li>Unit: A
         * </ul>
         */
        CURRENT_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current Power being drawn.
         * <ul>
         * <li>Interface: Wallbe
         * <li>Type: Integer
         * <li>Unit: W
         * </ul>
         */
        APPARENT_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current Energy Session.
         * <ul>
         * <li>Interface: Wallbe
         * <li>Type: Integer
         * <li>Unit: kWh
         * </ul>
         */
        ENERGY(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)),
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
     * Gets the Channel for {@link Wallbe.ChannelId#MAXIMUM_CHARGE_CURRENT}.
     *
     * @return the Channel
     */
    default WriteChannel<Short> getMaximumChargeCurrentChannel() {
        return this.channel(ChannelId.MAXIMUM_CHARGE_CURRENT);
    }

    /**
     * Gets the Value of {@link Wallbe.ChannelId#MAXIMUM_CHARGE_CURRENT}.
     *
     * @return the value
     */
    default short getMaximumChargeCurrent() {
        WriteChannel<Short> channel = this.getMaximumChargeCurrentChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Sets the Value of {@link Wallbe.ChannelId#MAXIMUM_CHARGE_CURRENT}.
     *
     * @param value the new value
     */
    default void setMaximumChargeCurrent(short value) throws OpenemsError.OpenemsNamedException {
        WriteChannel<Short> channel = this.getMaximumChargeCurrentChannel();
        channel.setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link Wallbe.ChannelId#STATUS}.
     *
     * @return the Channel
     */
    default Channel<String> getWallbeStatusChannel() {
        return this.channel(ChannelId.STATUS);
    }

    /**
     * Gets the Value of {@link Wallbe.ChannelId#STATUS}.
     *
     * @return the value
     */
    default String getWallbeStatus() {
        Channel<String> channel = this.getWallbeStatusChannel();
        return channel.value().orElse(channel.getNextValue().orElse("0"));
    }

    /**
     * Gets the Channel for {@link Wallbe.ChannelId#LOAD_TIME}.
     *
     * @return the Channel
     */
    default Channel<Integer> getLoadTimeChannel() {
        return this.channel(ChannelId.LOAD_TIME);
    }

    /**
     * Gets the Value of {@link Wallbe.ChannelId#LOAD_TIME}.
     *
     * @return the value
     */
    default int getLoadTime() {
        Channel<Integer> channel = this.getLoadTimeChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Wallbe.ChannelId#DIP_SWITCHES}.
     *
     * @return the Channel
     */
    default Channel<Short> getDipSwitchesChannel() {
        return this.channel(ChannelId.DIP_SWITCHES);
    }

    /**
     * Gets the Value of {@link Wallbe.ChannelId#DIP_SWITCHES}.
     *
     * @return the value
     */
    default int getDipSwitches() {
        Channel<Short> channel = this.getDipSwitchesChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Wallbe.ChannelId#FIRMWARE_VERSION}.
     *
     * @return the Channel
     */
    default Channel<Integer> getFirmwareVersionChannel() {
        return this.channel(ChannelId.FIRMWARE_VERSION);
    }

    /**
     * Gets the Value of {@link Wallbe.ChannelId#FIRMWARE_VERSION}.
     *
     * @return the value
     */
    default int getFirmwareVersion() {
        Channel<Integer> channel = this.getFirmwareVersionChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Wallbe.ChannelId#ERROR}.
     *
     * @return the Channel
     */
    default Channel<Short> getErrorChannel() {
        return this.channel(ChannelId.ERROR);
    }

    /**
     * Gets the Value of {@link Wallbe.ChannelId#ERROR}.
     *
     * @return the value
     */
    default int getError() {
        Channel<Short> channel = this.getErrorChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Wallbe.ChannelId#CURRENT_L1}.
     *
     * @return the Channel
     */
    default Channel<Integer> getCurrentL1Channel() {
        return this.channel(ChannelId.CURRENT_L1);
    }

    /**
     * Gets the Value of {@link Wallbe.ChannelId#CURRENT_L1}.
     *
     * @return the value
     */
    default int getCurrentL1() {
        Channel<Integer> channel = this.getCurrentL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Wallbe.ChannelId#CURRENT_L2}.
     *
     * @return the Channel
     */
    default Channel<Integer> getCurrentL2Channel() {
        return this.channel(ChannelId.CURRENT_L2);
    }

    /**
     * Gets the Value of {@link Wallbe.ChannelId#CURRENT_L2}.
     *
     * @return the value
     */
    default int getCurrentL2() {
        Channel<Integer> channel = this.getCurrentL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Wallbe.ChannelId#CURRENT_L3}.
     *
     * @return the Channel
     */
    default Channel<Integer> getCurrentL3Channel() {
        return this.channel(ChannelId.CURRENT_L3);
    }

    /**
     * Gets the Value of {@link Wallbe.ChannelId#CURRENT_L3}.
     *
     * @return the value
     */
    default int getCurrentL3() {
        Channel<Integer> channel = this.getCurrentL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Wallbe.ChannelId#APPARENT_POWER}.
     *
     * @return the Channel
     */
    default Channel<Integer> getApparentPowerChannel() {
        return this.channel(ChannelId.APPARENT_POWER);
    }

    /**
     * Gets the Value of {@link Wallbe.ChannelId#APPARENT_POWER}.
     *
     * @return the value
     */
    default int getApparentPower() {
        Channel<Integer> channel = this.getApparentPowerChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Wallbe.ChannelId#ENERGY}.
     *
     * @return the Channel
     */
    default Channel<Integer> getEnergyChannel() {
        return this.channel(ChannelId.ENERGY);
    }

    /**
     * Gets the Value of {@link Wallbe.ChannelId#ENERGY}.
     *
     * @return the value
     */
    default int getEnergy() {
        Channel<Integer> channel = this.getEnergyChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Wallbe.ChannelId#CHARGE_ENABLE}.
     *
     * @return the Channel
     */
    default WriteChannel<Boolean> getEnableChargeChannel() {
        return this.channel(ChannelId.CHARGE_ENABLE);
    }

    /**
     * Gets the Value of {@link Wallbe.ChannelId#CHARGE_ENABLE}.
     *
     * @return the value
     */
    default boolean getEnableCharge() {
        WriteChannel<Boolean> channel = this.getEnableChargeChannel();
        return channel.value().orElse(channel.getNextValue().orElse(false));
    }

    /**
     * Sets the Value of {@link Wallbe.ChannelId#MAXIMUM_CHARGE_CURRENT}.
     *
     * @param value the new value
     */
    default void setEnableCharge(boolean value) throws OpenemsError.OpenemsNamedException {
        WriteChannel<Boolean> channel = this.getEnableChargeChannel();
        channel.setNextWriteValue(value);
    }

}

