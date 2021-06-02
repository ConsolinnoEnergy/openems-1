package io.openems.edge.evcs.schneider;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.ManagedEvcs;

/**
 * This Provides the Channels for the Schneider EVCS.
 */

public interface Schneider extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * The state of the Charging Station.
         * See CPWState enum for more info.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        CPW_STATE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Last state of the Charging Station.
         * See LastChargeStatus enum for more info.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        LAST_CHARGE_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Return Value of the Remote Command.
         * Return the Command Code if valid or 0x8000 if an error occurred.
         * See RemoteCommand enum for more info.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        REMOTE_COMMAND_STATUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * First register for Error Status of EVCS.
         * See EVCSEError enum for more info.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        ERROR_STATUS_MSB(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Second register for Error Status of EVCS.
         * See EVCSEError enum for more info.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        ERROR_STATUS_LSB(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * The charge time is the time during the session while the contactor is closed.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        CHARGE_TIME(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        CHARGE_TIME_2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Write Channel for the Remote Command.
         * See RemoteCommand enum for more info.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        REMOTE_COMMAND(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * The Maximum Current that can be distributed to the EVCS.
         * Either 16 or 32 A.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Ampere
         * </ul>
         */
        MAX_INTENSITY_SOCKET(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Only operably on a master board if load balancing is enabled.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Unknown
         * </ul>
         */
        STATIC_MAX_INTENSITY_CLUSTER(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current current load currently drawn on Phase 1.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: float
         * <li>Unit: Ampere
         * </ul>
         */
        STATION_INTENSITY_PHASE_X(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current current load currently drawn on Phase 2.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: float
         * <li>Unit: Ampere
         * </ul>
         */
        STATION_INTENSITY_PHASE_2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current current load currently drawn on Phase 3.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: float
         * <li>Unit: Ampere
         * </ul>
         */
        STATION_INTENSITY_PHASE_3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
        /**
         * TBD.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Watt Hours
         * </ul>
         */
        STATION_INTENSITY_MSB(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_WRITE)),
        /**
         * TBD.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Watt Hours
         * </ul>
         */
        STATION_INTENSITY_LSB(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_WRITE)),
        /**
         * Total Power Consumption of the EVCS.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Kilo Watt
         * </ul>
         */
        STATION_POWER_TOTAL(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Voltage between L1 and L2.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Volt
         * </ul>
         */
        STN_METER_L1_L2_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Voltage between L2 and L3.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Volt
         * </ul>
         */
        STN_METER_L2_L3_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Voltage between L3 and L1.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Volt
         * </ul>
         */
        STN_METER_L3_L1_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Voltage between L1 and N.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Volt
         * </ul>
         */
        STN_METER_L1_N_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Voltage between L2 and N.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Volt
         * </ul>
         */
        STN_METER_L2_N_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Voltage between L3 and N.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Float
         * <li>Unit: Volt
         * </ul>
         */
        STN_METER_L3_N_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Keep Alive Register for the Remote Managment System. Has to be set to 1 every Cycle.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        REMOTE_CONTROLLER_LIFE_BIT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Register that contains the Information if the EVCS is currently running in Degraded Mode.
         * Value == 2 if Degraded Mode is active
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit: Na
         * </ul>
         */
        DEGRADED_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Historical Error Registers.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit:
         * </ul>
         */
        PREVIOUS_ERROR_0_START_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_0_END_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_0_CODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_1_START_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_1_END_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_1_CODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_2_START_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_2_END_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_2_CODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_3_START_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_3_END_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_3_CODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_4_START_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_4_END_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
        PREVIOUS_ERROR_4_CODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        /**
         * Time between start and stop of the current Charging process. Start with RFID and ends with RFID or command.
         * <ul>
         * <li>Interface: Schneider
         * <li>Type: Integer
         * <li>Unit:
         * </ul>
         */
        SESSION_TIME(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),
        SESSION_TIME_2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }


    /**
     * Gets the Channel for {@link Schneider.ChannelId#CPW_STATE}.
     *
     * @return the Channel
     */
    default Channel<Integer> getCPWStateChannel() {
        return this.channel(ChannelId.CPW_STATE);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#LAST_CHARGE_STATUS}.
     *
     * @return the Channel
     */
    default Channel<Integer> getLastChargeStatusChannel() {
        return this.channel(ChannelId.LAST_CHARGE_STATUS);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#REMOTE_COMMAND_STATUS}.
     *
     * @return the Channel
     */
    default Channel<Integer> getRemoteCommandStatusChannel() {
        return this.channel(ChannelId.REMOTE_COMMAND_STATUS);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#ERROR_STATUS_MSB}.
     *
     * @return the Channel
     */
    default Channel<Integer> getErrorStatusMSBChannel() {
        return this.channel(ChannelId.ERROR_STATUS_MSB);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#ERROR_STATUS_LSB}.
     *
     * @return the Channel
     */
    default Channel<Integer> getErrorStatusLSBChannel() {
        return this.channel(ChannelId.ERROR_STATUS_LSB);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#CHARGE_TIME}.
     *
     * @return the Channel
     */
    default Channel<Integer> getChargeTimeChannel() {
        return this.channel(ChannelId.CHARGE_TIME);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#CHARGE_TIME_2}.
     *
     * @return the Channel
     */
    default Channel<Integer> getChargeTime2Channel() {
        return this.channel(ChannelId.CHARGE_TIME_2);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#REMOTE_COMMAND}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getRemoteCommandChannel() {
        return this.channel(ChannelId.REMOTE_COMMAND);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#MAX_INTENSITY_SOCKET}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getMaxIntensitySocketChannel() {
        return this.channel(ChannelId.MAX_INTENSITY_SOCKET);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STATIC_MAX_INTENSITY_CLUSTER}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getStaticMaxIntensityClusterChannel() {
        return this.channel(ChannelId.STATIC_MAX_INTENSITY_CLUSTER);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STATION_INTENSITY_PHASE_X}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStationIntensityPhaseXChannel() {
        return this.channel(ChannelId.STATION_INTENSITY_PHASE_X);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STATION_INTENSITY_PHASE_2}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStationIntensityPhase2Channel() {
        return this.channel(ChannelId.STATION_INTENSITY_PHASE_2);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STATION_INTENSITY_PHASE_3}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStationIntensityPhase3Channel() {
        return this.channel(ChannelId.STATION_INTENSITY_PHASE_3);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STATION_INTENSITY_MSB}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getStationIntensityMSBChannel() {
        return this.channel(ChannelId.STATION_INTENSITY_MSB);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STATION_INTENSITY_LSB}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getStationIntensityLSBChannel() {
        return this.channel(ChannelId.STATION_INTENSITY_LSB);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STATION_POWER_TOTAL}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStationPowerTotalChannel() {
        return this.channel(ChannelId.STATION_POWER_TOTAL);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STN_METER_L1_L2_VOLTAGE}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStnMeterL1L2VoltageChannel() {
        return this.channel(ChannelId.STN_METER_L1_L2_VOLTAGE);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STN_METER_L2_L3_VOLTAGE}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStnMeterL2L3VoltageChannel() {
        return this.channel(ChannelId.STN_METER_L2_L3_VOLTAGE);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STN_METER_L3_L1_VOLTAGE}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStnMeterL3L1VoltageChannel() {
        return this.channel(ChannelId.STN_METER_L3_L1_VOLTAGE);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STN_METER_L1_N_VOLTAGE}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStnMeterL1NVoltageChannel() {
        return this.channel(ChannelId.STN_METER_L1_N_VOLTAGE);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STN_METER_L2_N_VOLTAGE}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStnMeterL2NVoltageChannel() {
        return this.channel(ChannelId.STN_METER_L2_N_VOLTAGE);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#STN_METER_L3_N_VOLTAGE}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getStnMeterL3NVoltageChannel() {
        return this.channel(ChannelId.STN_METER_L3_N_VOLTAGE);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#REMOTE_CONTROLLER_LIFE_BIT}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getRemoteControllerLifeBitChannel() {
        return this.channel(ChannelId.REMOTE_CONTROLLER_LIFE_BIT);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#DEGRADED_MODE}.
     *
     * @return the Channel
     */
    default Channel<Integer> getDegradedModeChannel() {
        return this.channel(ChannelId.DEGRADED_MODE);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#SESSION_TIME}.
     *
     * @return the Channel
     */
    default Channel<Integer> getSessionTimeChannel() {
        return this.channel(ChannelId.SESSION_TIME);
    }
    /**
     * Gets the Channel for {@link Schneider.ChannelId#SESSION_TIME_2}.
     *
     * @return the Channel
     */
    default Channel<Integer> getSessionTime2Channel() {
        return this.channel(ChannelId.SESSION_TIME_2);
    }
    default int getCPWState() {
        if (this.getCPWStateChannel().value().isDefined()) {
            return this.getCPWStateChannel().value().get();
        } else {
            return this.getCPWStateChannel().getNextValue().orElse(0);
        }

    }
}

