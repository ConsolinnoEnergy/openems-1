package io.openems.edge.evcs.mennekes.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Mennekes extends OpenemsComponent {
    /**
     * This Provides the Channels for the Mennekes BRx/BBx EVCS.
     */
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Firmware Version of the EVCS.
         * example: 0.91 = {0x30,0x2E, 0x39, 0x31} 4.40 ={0x34, 0x2E, 0x34, 0x34}.
         * Needs to be converted to ASCII
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: String
         * <li>Unit: Na
         * </ul>
         */
        FIRMWARE_VERSION(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        /**
         * First part of the Error Code. Needs to AND with the other parts to get the full code.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Na
         * </ul>
         */
        ERROR_CODE_1(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Second part of the Error Code. Needs to AND with the other parts to get the full code.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Na
         * </ul>
         */
        ERROR_CODE_2(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Third part of the Error Code. Needs to AND with the other parts to get the full code.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Na
         * </ul>
         */
        ERROR_CODE_3(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Forth part of the Error Code. Needs to AND with the other parts to get the full code.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Na
         * </ul>
         */
        ERROR_CODE_4(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Modbus Protocol Version of the EVCS.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: String
         * <li>Unit: Na
         * </ul>
         */
        PROTOCOL_VERSION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * State of the connected vehicle. This is probably a CHAR and needs to be interpreted.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Na
         * </ul>
         */
        VEHICLE_STATE(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Read or set the Charge Point
         * availability.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Na
         * </ul>
         */
        CP_AVAILABILITY(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Offset of the Modbus Addresses. Defaults to 0.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Na
         * </ul>
         */
        MODBUS_ADDRESS_OFFSET(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Safe Current the EVCS will fall back to in case the Modbus Communication fails.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Na
         * </ul>
         */
        SAFE_CURRENT(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Timer for how long the EVCS will operate without a new message from the Modbus Master before falling back to the Safe current.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Na
         * </ul>
         */
        COMM_TIMEOUT(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Energy that is being drawn on Phase L1.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Float
         * <li>Unit: Wh
         * </ul>
         */
        METER_ENERGY_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Energy that is being drawn on Phase L2.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Float
         * <li>Unit: Wh
         * </ul>
         */
        METER_ENERGY_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Energy that is being drawn on Phase L3.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Float
         * <li>Unit: Wh
         * </ul>
         */
        METER_ENERGY_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Power that is being drawn on Phase L1.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Float
         * <li>Unit: W
         * </ul>
         */
        METER_POWER_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Power that is being drawn on Phase L2.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Float
         * <li>Unit: W
         * </ul>
         */
        METER_POWER_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Power that is being drawn on Phase L3.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Float
         * <li>Unit: W
         * </ul>
         */
        METER_POWER_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current that is being drawn on Phase L1.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Float
         * <li>Unit: A
         * </ul>
         */
        METER_CURRENT_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current that is being drawn on Phase L2.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Float
         * <li>Unit: A
         * </ul>
         */
        METER_CURRENT_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current that is being drawn on Phase L3.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Float
         * <li>Unit: A
         * </ul>
         */
        METER_CURRENT_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Required Energy. Only for 15118
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Wh
         * </ul>
         */
        REQUIRED_ENERGY(Doc.of(OpenemsType.SHORT).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Scheduled Time of Departure. Only for 15118
         * hh-mm-ss
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        SCHEDULED_DEPARTURE_TIME(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Scheduled Date of Departure. Only for 15118
         * dd-mm-yy
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        SCHEDULED_DEPARTURE_DATE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Sum of Charged Energy for the Current Session.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Wh
         * </ul>
         */
        CHARGED_ENERGY(Doc.of(OpenemsType.SHORT).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * The Maximum Current that is being signaled to the EV for charging.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: A
         * </ul>
         */
        SIGNALED_CURRENT(Doc.of(OpenemsType.SHORT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Time when the EV started Charging. Same format as Scheduled Departure Time.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Na
         * </ul>
         */
        START_TIME(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Duration of the charging process.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: s
         * </ul>
         */
        CHARGE_DURATION(Doc.of(OpenemsType.SHORT).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
        /**
         * TBD.
         * Same format as Scheduled departure Time.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: Na
         * </ul>
         */
        END_TIME(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current Limit for the EVCS. Used for controlling the EVCS.
         * <ul>
         * <li>Interface: Mennekes
         * <li>Type: Short
         * <li>Unit: A
         * </ul>
         */
        CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#FIRMWARE_VERSION}.
     *
     * @return the Channel
     */
    default Channel<String> getFirmwareVersionChannel() {
        return this.channel(ChannelId.FIRMWARE_VERSION);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#FIRMWARE_VERSION}.
     *
     * @return the value
     */
    default String getFirmwareVersion() {
        Channel<String> channel = this.getFirmwareVersionChannel();
        return channel.value().orElse(channel.getNextValue().orElse("0"));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#ERROR_CODE_1}.
     *
     * @return the Channel
     */
    default Channel<Short> getErrorCode1Channel() {
        return this.channel(ChannelId.ERROR_CODE_1);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#ERROR_CODE_2}.
     *
     * @return the Channel
     */
    default Channel<Short> getErrorCode2Channel() {
        return this.channel(ChannelId.ERROR_CODE_2);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#ERROR_CODE_3}.
     *
     * @return the Channel
     */
    default Channel<Short> getErrorCode3Channel() {
        return this.channel(ChannelId.ERROR_CODE_3);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#ERROR_CODE_4}.
     *
     * @return the Channel
     */
    default Channel<Short> getErrorCode4Channel() {
        return this.channel(ChannelId.ERROR_CODE_4);
    }

    /**
     * Gets the Value of Error Code Channels.
     *
     * @return the value
     */
    default short getErrorCode() {
        Channel<Short> error1 = this.getErrorCode1Channel();
        short error1Value = error1.value().orElse(error1.getNextValue().orElse((short) 0));
        Channel<Short> error2 = this.getErrorCode2Channel();
        short error2Value = error2.value().orElse(error2.getNextValue().orElse((short) 0));
        Channel<Short> error3 = this.getErrorCode3Channel();
        short error3Value = error3.value().orElse(error3.getNextValue().orElse((short) 0));
        Channel<Short> error4 = this.getErrorCode4Channel();
        short error4Value = error4.value().orElse(error4.getNextValue().orElse((short) 0));
        return (short) (error1Value | error2Value | error3Value | error4Value);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#PROTOCOL_VERSION}.
     *
     * @return the Channel
     */
    default Channel<Integer> getProtocolVersionChannel() {
        return this.channel(ChannelId.PROTOCOL_VERSION);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#PROTOCOL_VERSION}.
     *
     * @return the value
     */
    default Integer getProtocolVersion() {
        Channel<Integer> channel = this.getProtocolVersionChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#VEHICLE_STATE}.
     *
     * @return the Channel
     */
    default Channel<Short> getVehicleStateChannel() {
        return this.channel(ChannelId.VEHICLE_STATE);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#VEHICLE_STATE}.
     *
     * @return the value
     */
    default Short getVehicleState() {
        Channel<Short> channel = this.getVehicleStateChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#CP_AVAILABILITY}.
     *
     * @return the Channel
     */
    default Channel<Short> getChargePointAvailabilityChannel() {
        return this.channel(ChannelId.CP_AVAILABILITY);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#CP_AVAILABILITY}.
     *
     * @return the value
     */
    default Short getChargePointAvailability() {
        Channel<Short> channel = this.getChargePointAvailabilityChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#MODBUS_ADDRESS_OFFSET}.
     *
     * @return the Channel
     */
    default Channel<Short> getModbusAddressOffsetChannel() {
        return this.channel(ChannelId.MODBUS_ADDRESS_OFFSET);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#MODBUS_ADDRESS_OFFSET}.
     *
     * @return the value
     */
    default Short getModbusAddressOffset() {
        Channel<Short> channel = this.getModbusAddressOffsetChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#SAFE_CURRENT}.
     *
     * @return the Channel
     */
    default WriteChannel<Short> getSafeCurrentChannel() {
        return this.channel(ChannelId.SAFE_CURRENT);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#SAFE_CURRENT}.
     *
     * @return the value
     */
    default Short getSafeCurrent() {
        WriteChannel<Short> channel = this.getSafeCurrentChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Sets a value into the SafeCurrent register. See
     * {@link Mennekes.ChannelId#SAFE_CURRENT}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setSafeCurrent(short value) throws OpenemsError.OpenemsNamedException {
        WriteChannel<Short> channel = this.getSafeCurrentChannel();
        channel.setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#COMM_TIMEOUT}.
     *
     * @return the Channel
     */
    default WriteChannel<Short> getCommunicationTimeoutChannel() {
        return this.channel(ChannelId.COMM_TIMEOUT);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#COMM_TIMEOUT}.
     *
     * @return the value
     */
    default Short getCommunicationTimeout() {
        WriteChannel<Short> channel = this.getCommunicationTimeoutChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Sets a value into the CommTimeout register. See
     * {@link Mennekes.ChannelId#COMM_TIMEOUT}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setCommunicationTimeout(short value) throws OpenemsError.OpenemsNamedException {
        WriteChannel<Short> channel = this.getCommunicationTimeoutChannel();
        channel.setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#METER_ENERGY_L1}.
     *
     * @return the Channel
     */
    default Channel<Float> getEnergyL1Channel() {
        return this.channel(ChannelId.METER_ENERGY_L1);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#METER_ENERGY_L2}.
     *
     * @return the Channel
     */
    default Channel<Float> getEnergyL2Channel() {
        return this.channel(ChannelId.METER_ENERGY_L2);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#METER_ENERGY_L3}.
     *
     * @return the Channel
     */
    default Channel<Float> getEnergyL3Channel() {
        return this.channel(ChannelId.METER_ENERGY_L3);
    }

    /**
     * Gets the Value of the Meter Energy Channels.
     *
     * @return the value
     */
    default float getCurrentEnergy() {
        Channel<Float> l1 = this.getEnergyL1Channel();
        float l1Value = l1.value().orElse(l1.getNextValue().orElse(0.f));
        Channel<Float> l2 = this.getEnergyL2Channel();
        float l2Value = l2.value().orElse(l2.getNextValue().orElse(0.f));
        Channel<Float> l3 = this.getEnergyL3Channel();
        float l3Value = l3.value().orElse(l3.getNextValue().orElse(0.f));
        return (l1Value + l2Value + l3Value);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#METER_POWER_L1}.
     *
     * @return the Channel
     */
    default Channel<Float> getPowerL1Channel() {
        return this.channel(ChannelId.METER_POWER_L1);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#METER_POWER_L2}.
     *
     * @return the Channel
     */
    default Channel<Float> getPowerL2Channel() {
        return this.channel(ChannelId.METER_POWER_L2);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#METER_POWER_L3}.
     *
     * @return the Channel
     */
    default Channel<Float> getPowerL3Channel() {
        return this.channel(ChannelId.METER_POWER_L3);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#METER_POWER_L1}.
     *
     * @return the value
     */
    default float getPowerL1() {
        Channel<Float> l1 = this.getPowerL1Channel();
        return l1.value().orElse(l1.getNextValue().orElse(0.f));
    }
    /**
     * Gets the Value of {@link Mennekes.ChannelId#METER_POWER_L2}.
     *
     * @return the value
     */
    default float getPowerL2() {
        Channel<Float> l2 = this.getPowerL2Channel();
        return l2.value().orElse(l2.getNextValue().orElse(0.f));
    }
    /**
     * Gets the Value of {@link Mennekes.ChannelId#METER_POWER_L3}.
     *
     * @return the value
     */
    default float getPowerL3() {
        Channel<Float> l3 = this.getPowerL3Channel();
        return l3.value().orElse(l3.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Value of the Meter Power Channels.
     *
     * @return the value
     */
    default float getCurrentPower() {
        Channel<Float> l1 = this.getPowerL1Channel();
        float l1Value = l1.value().orElse(l1.getNextValue().orElse(0.f));
        Channel<Float> l2 = this.getPowerL2Channel();
        float l2Value = l2.value().orElse(l2.getNextValue().orElse(0.f));
        Channel<Float> l3 = this.getPowerL3Channel();
        float l3Value = l3.value().orElse(l3.getNextValue().orElse(0.f));
        return (l1Value + l2Value + l3Value);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#METER_CURRENT_L1}.
     *
     * @return the Channel
     */
    default Channel<Float> getCurrentL1Channel() {
        return this.channel(ChannelId.METER_CURRENT_L1);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#METER_CURRENT_L2}.
     *
     * @return the Channel
     */
    default Channel<Float> getCurrentL2Channel() {
        return this.channel(ChannelId.METER_CURRENT_L2);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#METER_CURRENT_L3}.
     *
     * @return the Channel
     */
    default Channel<Float> getCurrentL3Channel() {
        return this.channel(ChannelId.METER_CURRENT_L3);
    }

    /**
     * Gets the Value of the Meter Current Channels.
     *
     * @return the value
     */
    default float getCurrentCurrent() {
        Channel<Float> l1 = this.getCurrentL1Channel();
        float l1Value = l1.value().orElse(l1.getNextValue().orElse(0.f));
        Channel<Float> l2 = this.getCurrentL2Channel();
        float l2Value = l2.value().orElse(l2.getNextValue().orElse(0.f));
        Channel<Float> l3 = this.getCurrentL3Channel();
        float l3Value = l3.value().orElse(l3.getNextValue().orElse(0.f));
        return (l1Value + l2Value + l3Value);
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#REQUIRED_ENERGY}.
     *
     * @return the Channel
     */
    default Channel<Short> getRequiredEnergyChannel() {
        return this.channel(ChannelId.REQUIRED_ENERGY);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#REQUIRED_ENERGY}.
     *
     * @return the value
     */
    default Short getRequiredEnergy() {
        Channel<Short> channel = this.getRequiredEnergyChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#SCHEDULED_DEPARTURE_TIME}.
     *
     * @return the Channel
     */
    default Channel<Integer> getScheduledDepartureTimeChannel() {
        return this.channel(ChannelId.SCHEDULED_DEPARTURE_TIME);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#SCHEDULED_DEPARTURE_TIME}.
     *
     * @return the value
     */
    default int getScheduledDepartureTime() {
        Channel<Integer> channel = this.getScheduledDepartureTimeChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#SCHEDULED_DEPARTURE_DATE}.
     *
     * @return the Channel
     */
    default Channel<Integer> getScheduledDepartureDateChannel() {
        return this.channel(ChannelId.SCHEDULED_DEPARTURE_DATE);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#SCHEDULED_DEPARTURE_DATE}.
     *
     * @return the value
     */
    default int getScheduledDepartureDate() {
        Channel<Integer> channel = this.getScheduledDepartureDateChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#CHARGED_ENERGY}.
     *
     * @return the Channel
     */
    default Channel<Short> getChargedEnergyChannel() {
        return this.channel(ChannelId.CHARGED_ENERGY);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#CHARGED_ENERGY}.
     *
     * @return the value
     */
    default Short getChargedEnergy() {
        Channel<Short> channel = this.getChargedEnergyChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#SIGNALED_CURRENT}.
     *
     * @return the Channel
     */
    default Channel<Short> getSignaledCurrentChannel() {
        return this.channel(ChannelId.SIGNALED_CURRENT);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#SIGNALED_CURRENT}.
     *
     * @return the value
     */
    default Short getSignaledCurrent() {
        Channel<Short> channel = this.getSignaledCurrentChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#START_TIME}.
     *
     * @return the Channel
     */
    default Channel<Integer> getStartTimeChannel() {
        return this.channel(ChannelId.START_TIME);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#START_TIME}.
     *
     * @return the value
     */
    default int getStartTime() {
        Channel<Integer> channel = this.getStartTimeChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#CHARGE_DURATION}.
     *
     * @return the Channel
     */
    default Channel<Short> getChargeDurationChannel() {
        return this.channel(ChannelId.CHARGE_DURATION);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#CHARGE_DURATION}.
     *
     * @return the value
     */
    default Short getChargeDuration() {
        Channel<Short> channel = this.getChargeDurationChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#END_TIME}.
     *
     * @return the Channel
     */
    default Channel<Integer> getEndTimeChannel() {
        return this.channel(ChannelId.END_TIME);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#END_TIME}.
     *
     * @return the value
     */
    default int getEndTime() {
        Channel<Integer> channel = this.getStartTimeChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Mennekes.ChannelId#CURRENT_LIMIT}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getCurrentLimitChannel() {
        return this.channel(ChannelId.CURRENT_LIMIT);
    }

    /**
     * Gets the Value of {@link Mennekes.ChannelId#CURRENT_LIMIT}.
     *
     * @return the value
     */
    default int getCurrentLimit() {
        WriteChannel<Integer> channel = this.getCurrentLimitChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Sets a value into the CurrentLimit register. See
     * {@link Mennekes.ChannelId#CURRENT_LIMIT}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setCurrentLimit(int value) throws OpenemsError.OpenemsNamedException {
        WriteChannel<Integer> channel = this.getCurrentLimitChannel();
        channel.setNextWriteValue(value);
    }
}

