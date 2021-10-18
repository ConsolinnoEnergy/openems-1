package io.openems.edge.evcs.alfen.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Alfen extends OpenemsComponent {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Current State of the Meter.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Short
         * <li>Unit:
         * </ul>
         */
        METER_STATE(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Timestamp of the last Meter State.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Long
         * <li>Unit:
         * </ul>
         */
        METER_LAST_VALUE_TIMESTAMP(Doc.of(OpenemsType.LONG).unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Configured Type of the Meter.
         * 0:RTU, 1:TCP/IP, 2:UDP, 3:P1, 4:other
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Short
         * <li>Unit:
         * </ul>
         */
        METER_TYPE(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Voltage between L1 and N.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:Volt
         * </ul>
         */
        VOLTAGE_PHASE_L1_N(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Voltage between L2 and N.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:Volt
         * </ul>
         */
        VOLTAGE_PHASE_L2_N(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Voltage between L3 and N.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:Volt
         * </ul>
         */
        VOLTAGE_PHASE_L3_N(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Voltage between L1 and L2.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:Volt
         * </ul>
         */
        VOLTAGE_PHASE_L1_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Voltage between L2 and L3.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:Volt
         * </ul>
         */
        VOLTAGE_PHASE_L2_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Voltage between L3 and L1.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:Volt
         * </ul>
         */
        VOLTAGE_PHASE_L3_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on N.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:Ampere
         * </ul>
         */
        CURRENT_N(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on L1.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:Ampere
         * </ul>
         */
        CURRENT_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on L2.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:Ampere
         * </ul>
         */
        CURRENT_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on L3.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:Ampere
         * </ul>
         */
        CURRENT_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current current sum.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:Ampere
         * </ul>
         */
        CURRENT_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Power Factor on L1.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:
         * </ul>
         */
        POWER_FACTOR_L1(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Power Factor on L2.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:
         * </ul>
         */
        POWER_FACTOR_L2(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Power Factor on L3.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:
         * </ul>
         */
        POWER_FACTOR_L3(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Power Factor sum.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit:
         * </ul>
         */
        POWER_FACTOR_SUM(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Frequency of the Station.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Hertz
         * </ul>
         */
        FREQUENCY(Doc.of(OpenemsType.FLOAT).unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
        /**
         * Real Power drawn on L1.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Watt
         * </ul>
         */
        REAL_POWER_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Real Power drawn on L2.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Watt
         * </ul>
         */
        REAL_POWER_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Real Power drawn on L3.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Watt
         * </ul>
         */
        REAL_POWER_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Sum of Real power drawn on all three phases.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Watt
         * </ul>
         */
        REAL_POWER_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Apparent Power drawn on L1.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Volt Ampere
         * </ul>
         */
        APPARENT_POWER_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Apparent Power drawn on L2.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Volt Ampere
         * </ul>
         */
        APPARENT_POWER_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Apparent Power drawn on L3.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Volt Ampere
         * </ul>
         */
        APPARENT_POWER_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Apparent Power sum.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Volt Ampere
         * </ul>
         */
        APPARENT_POWER_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Reactive Power drawn on L1.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Volt Ampere reactive
         * </ul>
         */
        REACTIVE_POWER_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Reactive Power drawn on L2.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Volt Ampere reactive
         * </ul>
         */
        REACTIVE_POWER_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Reactive Power drawn on L3.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Volt Ampere reactive
         * </ul>
         */
        REACTIVE_POWER_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Reactive Power sum.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Volt Ampere reactive
         * </ul>
         */
        REACTIVE_POWER_SUM(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE_REACTIVE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Real Energy delivered to L1.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Watt Hours
         * </ul>
         */
        REAL_ENERGY_DELIVERED_L1(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Real Energy delivered to L2.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Watt Hours
         * </ul>
         */
        REAL_ENERGY_DELIVERED_L2(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Real Energy delivered to L3.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Watt Hours
         * </ul>
         */
        REAL_ENERGY_DELIVERED_L3(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Real Energy delivered sum.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Watt Hours
         * </ul>
         */
        REAL_ENERGY_DELIVERED_SUM(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Real Energy consumed on L1.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Watt Hours
         * </ul>
         */
        REAL_ENERGY_CONSUMED_L1(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Real Energy consumed on L2.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Watt Hours
         * </ul>
         */
        REAL_ENERGY_CONSUMED_L2(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Real Energy consumed on L3.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Watt Hours
         * </ul>
         */
        REAL_ENERGY_CONSUMED_L3(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Real Energy consumed sum.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Watt Hours
         * </ul>
         */
        REAL_ENERGY_CONSUMED_SUM(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Apparent Energy on L1.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Volt Ampere Hours
         * </ul>
         */
        APPARENT_ENERGY_L1(Doc.of(OpenemsType.DOUBLE).unit(Unit.VOLT_AMPERE_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Apparent Energy on L2.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Volt Ampere Hours
         * </ul>
         */
        APPARENT_ENERGY_L2(Doc.of(OpenemsType.DOUBLE).unit(Unit.VOLT_AMPERE_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Apparent Energy on L3.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Volt Ampere Hours
         * </ul>
         */
        APPARENT_ENERGY_L3(Doc.of(OpenemsType.DOUBLE).unit(Unit.VOLT_AMPERE_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Apparent Energy sum.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Volt Ampere Hours
         * </ul>
         */
        APPARENT_ENERGY_SUM(Doc.of(OpenemsType.DOUBLE).unit(Unit.VOLT_AMPERE_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Reactive Energy on L1.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Volt Ampere Reactive Hours
         * </ul>
         */
        REACTIVE_ENERGY_L1(Doc.of(OpenemsType.DOUBLE).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Reactive Energy on L2.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Volt Ampere Reactive Hours
         * </ul>
         */
        REACTIVE_ENERGY_L2(Doc.of(OpenemsType.DOUBLE).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Reactive Energy on L3.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Volt Ampere Reactive Hours
         * </ul>
         */
        REACTIVE_ENERGY_L3(Doc.of(OpenemsType.DOUBLE).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Reactive Energy sum.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Double
         * <li>Unit: Volt Ampere Reactive Hours
         * </ul>
         */
        REACTIVE_ENERGY_SUM(Doc.of(OpenemsType.DOUBLE).unit(Unit.VOLT_AMPERE_REACTIVE_HOURS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Availability of the Station.
         * 1: Operative, 2:Inoperative
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Short
         * <li>Unit:
         * </ul>
         */
        AVAILABILITY(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        /**
         * TBD.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:String
         * <li>Unit:
         * </ul>
         */
        MODE_3_STATE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        /**
         * Actual Max Current applied on the Station.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Ampere
         * </ul>
         */
        ACTUAL_APPLIED_MAX_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Remaining time before fall back to safe current.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Integer
         * <li>Unit: Seconds
         * </ul>
         */
        MODBUS_SLAVE_MAX_CURRENT_VALID_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),
        /**
         * Max Current applied over Modbus.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Ampere
         * </ul>
         */
        MODBUS_SLAVE_MAX_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Safe Current used for the Load Balancing ( if active ).
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Float
         * <li>Unit: Ampere
         * </ul>
         */
        ACTIVE_LOAD_BALANCING_SAFE_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * TBD.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Short
         * <li>Unit:
         * </ul>
         */
        MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Amount of phases, the Station should charge with.
         * Either 1 or 3.
         * <ul>
         * <li>Interface: Alfen
         * <li>Type:Short
         * <li>Unit:
         * </ul>
         */

        CHARGE_PHASES(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_WRITE)),
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
     * Gets the Channel for {@link Alfen.ChannelId#METER_STATE}.
     *
     * @return the Channel
     */
    default Channel<Short> getMeterStateChannel() {
        return this.channel(ChannelId.METER_STATE);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#METER_STATE}.
     *
     * @return the value
     */
    default short getMeterState() {
        Channel<Short> channel = this.getMeterStateChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#METER_LAST_VALUE_TIMESTAMP}.
     *
     * @return the Channel
     */
    default Channel<Long> getMeterLastValueTimestampChannel() {
        return this.channel(ChannelId.METER_LAST_VALUE_TIMESTAMP);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#METER_LAST_VALUE_TIMESTAMP}.
     *
     * @return the value
     */
    default long getMeterLastValueTimestamp() {
        Channel<Long> channel = this.getMeterLastValueTimestampChannel();
        return channel.value().orElse(channel.getNextValue().orElse((long) 0));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#METER_TYPE}.
     *
     * @return the Channel
     */
    default Channel<Short> getMeterTypeChannel() {
        return this.channel(ChannelId.METER_TYPE);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#METER_TYPE}.
     *
     * @return the value
     */
    default short getMeterType() {
        Channel<Short> channel = this.getMeterTypeChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#VOLTAGE_PHASE_L1_N}.
     *
     * @return the Channel
     */
    default Channel<Float> getVoltageL1NChannel() {
        return this.channel(ChannelId.VOLTAGE_PHASE_L1_N);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#VOLTAGE_PHASE_L1_N}.
     *
     * @return the value
     */
    default float getVoltageL1N() {
        Channel<Float> channel = this.getVoltageL1NChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#VOLTAGE_PHASE_L2_N}.
     *
     * @return the Channel
     */
    default Channel<Float> getVoltageL2NChannel() {
        return this.channel(ChannelId.VOLTAGE_PHASE_L2_N);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#VOLTAGE_PHASE_L2_N}.
     *
     * @return the value
     */
    default float getVoltageL2N() {
        Channel<Float> channel = this.getVoltageL2NChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#VOLTAGE_PHASE_L3_N}.
     *
     * @return the Channel
     */
    default Channel<Float> getVoltageL3NChannel() {
        return this.channel(ChannelId.VOLTAGE_PHASE_L3_N);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#VOLTAGE_PHASE_L3_N}.
     *
     * @return the value
     */
    default float getVoltageL3N() {
        Channel<Float> channel = this.getVoltageL3NChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#VOLTAGE_PHASE_L1_L2}.
     *
     * @return the Channel
     */
    default Channel<Float> getVoltageL1L2Channel() {
        return this.channel(ChannelId.VOLTAGE_PHASE_L1_L2);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#VOLTAGE_PHASE_L1_L2}.
     *
     * @return the value
     */
    default float getVoltageL1L2() {
        Channel<Float> channel = this.getVoltageL1L2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#VOLTAGE_PHASE_L2_L3}.
     *
     * @return the Channel
     */
    default Channel<Float> getVoltageL2L3Channel() {
        return this.channel(ChannelId.VOLTAGE_PHASE_L2_L3);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#VOLTAGE_PHASE_L2_L3}.
     *
     * @return the value
     */
    default float getVoltageL2L3() {
        Channel<Float> channel = this.getVoltageL2L3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#VOLTAGE_PHASE_L3_L1}.
     *
     * @return the Channel
     */
    default Channel<Float> getVoltageL3L1Channel() {
        return this.channel(ChannelId.VOLTAGE_PHASE_L3_L1);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#VOLTAGE_PHASE_L3_L1}.
     *
     * @return the value
     */
    default float getVoltageL3L1() {
        Channel<Float> channel = this.getVoltageL3L1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#CURRENT_N}.
     *
     * @return the Channel
     */
    default Channel<Float> getCurrentNChannel() {
        return this.channel(ChannelId.CURRENT_N);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#CURRENT_N}.
     *
     * @return the value
     */
    default float getCurrentN() {
        Channel<Float> channel = this.getCurrentNChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#CURRENT_L1}.
     *
     * @return the Channel
     */
    default Channel<Float> getCurrentL1Channel() {
        return this.channel(ChannelId.CURRENT_L1);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#CURRENT_L1}.
     *
     * @return the value
     */
    default float getCurrentL1() {
        Channel<Float> channel = this.getCurrentL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#CURRENT_L2}.
     *
     * @return the Channel
     */
    default Channel<Float> getCurrentL2Channel() {
        return this.channel(ChannelId.CURRENT_L2);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#CURRENT_L2}.
     *
     * @return the value
     */
    default float getCurrentL2() {
        Channel<Float> channel = this.getCurrentL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#CURRENT_L3}.
     *
     * @return the Channel
     */
    default Channel<Float> getCurrentL3Channel() {
        return this.channel(ChannelId.CURRENT_L3);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#CURRENT_L3}.
     *
     * @return the value
     */
    default float getCurrentL3() {
        Channel<Float> channel = this.getCurrentL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#CURRENT_SUM}.
     *
     * @return the Channel
     */
    default Channel<Float> getCurrentSumChannel() {
        return this.channel(ChannelId.CURRENT_SUM);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#CURRENT_SUM}.
     *
     * @return the value
     */
    default float getCurrentSum() {
        Channel<Float> channel = this.getCurrentSumChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#POWER_FACTOR_L1}.
     *
     * @return the Channel
     */
    default Channel<Float> getPowerFactorL1Channel() {
        return this.channel(ChannelId.POWER_FACTOR_L1);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#POWER_FACTOR_L1}.
     *
     * @return the value
     */
    default float getPowerFactorL1() {
        Channel<Float> channel = this.getPowerFactorL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#POWER_FACTOR_L2}.
     *
     * @return the Channel
     */
    default Channel<Float> getPowerFactorL2Channel() {
        return this.channel(ChannelId.POWER_FACTOR_L2);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#POWER_FACTOR_L2}.
     *
     * @return the value
     */
    default float getPowerFactorL2() {
        Channel<Float> channel = this.getPowerFactorL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#POWER_FACTOR_L3}.
     *
     * @return the Channel
     */
    default Channel<Float> getPowerFactorL3Channel() {
        return this.channel(ChannelId.POWER_FACTOR_L3);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#POWER_FACTOR_L3}.
     *
     * @return the value
     */
    default float getPowerFactorL3() {
        Channel<Float> channel = this.getPowerFactorL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#POWER_FACTOR_SUM}.
     *
     * @return the Channel
     */
    default Channel<Float> getPowerFactorSumChannel() {
        return this.channel(ChannelId.POWER_FACTOR_SUM);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#POWER_FACTOR_SUM}.
     *
     * @return the value
     */
    default float getPowerFactorSum() {
        Channel<Float> channel = this.getPowerFactorSumChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#FREQUENCY}.
     *
     * @return the Channel
     */
    default Channel<Float> getFrequencyChannel() {
        return this.channel(ChannelId.FREQUENCY);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#FREQUENCY}.
     *
     * @return the value
     */
    default float getFrequency() {
        Channel<Float> channel = this.getFrequencyChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REAL_POWER_L1}.
     *
     * @return the Channel
     */
    default Channel<Float> getRealPowerL1Channel() {
        return this.channel(ChannelId.REAL_POWER_L1);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REAL_POWER_L1}.
     *
     * @return the value
     */
    default float getRealPowerL1() {
        Channel<Float> channel = this.getRealPowerL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REAL_POWER_L2}.
     *
     * @return the Channel
     */
    default Channel<Float> getRealPowerL2Channel() {
        return this.channel(ChannelId.REAL_POWER_L2);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REAL_POWER_L2}.
     *
     * @return the value
     */
    default float getRealPowerL2() {
        Channel<Float> channel = this.getRealPowerL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REAL_POWER_L3}.
     *
     * @return the Channel
     */
    default Channel<Float> getRealPowerL3Channel() {
        return this.channel(ChannelId.REAL_POWER_L3);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REAL_POWER_L3}.
     *
     * @return the value
     */
    default float getRealPowerL3() {
        Channel<Float> channel = this.getRealPowerL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REAL_POWER_SUM}.
     *
     * @return the Channel
     */
    default Channel<Float> getRealPowerSumChannel() {
        return this.channel(ChannelId.REAL_POWER_SUM);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REAL_POWER_SUM}.
     *
     * @return the value
     */
    default float getRealPowerSum() {
        Channel<Float> channel = this.getRealPowerSumChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#APPARENT_POWER_L1}.
     *
     * @return the Channel
     */
    default Channel<Float> getApparentPowerL1Channel() {
        return this.channel(ChannelId.APPARENT_POWER_L1);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#APPARENT_POWER_L1}.
     *
     * @return the value
     */
    default float getApparentPowerL1() {
        Channel<Float> channel = this.getApparentPowerL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#APPARENT_POWER_L2}.
     *
     * @return the Channel
     */
    default Channel<Float> getApparentPowerL2Channel() {
        return this.channel(ChannelId.APPARENT_POWER_L2);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#APPARENT_POWER_L2}.
     *
     * @return the value
     */
    default float getApparentPowerL2() {
        Channel<Float> channel = this.getApparentPowerL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#APPARENT_POWER_L3}.
     *
     * @return the Channel
     */
    default Channel<Float> getApparentPowerL3Channel() {
        return this.channel(ChannelId.APPARENT_POWER_L3);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#APPARENT_POWER_L3}.
     *
     * @return the value
     */
    default float getApparentPowerL3() {
        Channel<Float> channel = this.getApparentPowerL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#APPARENT_POWER_SUM}.
     *
     * @return the Channel
     */
    default Channel<Float> getApparentPowerSumChannel() {
        return this.channel(ChannelId.APPARENT_POWER_SUM);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#APPARENT_POWER_SUM}.
     *
     * @return the value
     */
    default float getApparentPowerSum() {
        Channel<Float> channel = this.getApparentPowerSumChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REACTIVE_POWER_L1}.
     *
     * @return the Channel
     */
    default Channel<Float> getReactivePowerL1Channel() {
        return this.channel(ChannelId.REACTIVE_POWER_L1);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REACTIVE_POWER_L1}.
     *
     * @return the value
     */
    default float getReactivePowerL1() {
        Channel<Float> channel = this.getReactivePowerL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REACTIVE_POWER_L2}.
     *
     * @return the Channel
     */
    default Channel<Float> getReactivePowerL2Channel() {
        return this.channel(ChannelId.REACTIVE_POWER_L2);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REACTIVE_POWER_L2}.
     *
     * @return the value
     */
    default float getReactivePowerL2() {
        Channel<Float> channel = this.getReactivePowerL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REACTIVE_POWER_L3}.
     *
     * @return the Channel
     */
    default Channel<Float> getReactivePowerL3Channel() {
        return this.channel(ChannelId.REACTIVE_POWER_L3);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REACTIVE_POWER_L3}.
     *
     * @return the value
     */
    default float getReactivePowerL3() {
        Channel<Float> channel = this.getReactivePowerL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REACTIVE_POWER_SUM}.
     *
     * @return the Channel
     */
    default Channel<Float> getReactivePowerSumChannel() {
        return this.channel(ChannelId.REACTIVE_POWER_SUM);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REACTIVE_POWER_SUM}.
     *
     * @return the value
     */
    default float getReactivePowerSum() {
        Channel<Float> channel = this.getReactivePowerSumChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REAL_ENERGY_DELIVERED_L1}.
     *
     * @return the Channel
     */
    default Channel<Double> getRealEnergyDeliveredL1Channel() {
        return this.channel(ChannelId.REAL_ENERGY_DELIVERED_L1);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REAL_ENERGY_DELIVERED_L1}.
     *
     * @return the value
     */
    default double getRealEnergyDeliveredL1() {
        Channel<Double> channel = this.getRealEnergyDeliveredL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REAL_ENERGY_DELIVERED_L2}.
     *
     * @return the Channel
     */
    default Channel<Double> getRealEnergyDeliveredL2Channel() {
        return this.channel(ChannelId.REAL_ENERGY_DELIVERED_L2);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REAL_ENERGY_DELIVERED_L2}.
     *
     * @return the value
     */
    default double getRealEnergyDeliveredL2() {
        Channel<Double> channel = this.getRealEnergyDeliveredL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REAL_ENERGY_DELIVERED_L3}.
     *
     * @return the Channel
     */
    default Channel<Double> getRealEnergyDeliveredL3Channel() {
        return this.channel(ChannelId.REAL_ENERGY_DELIVERED_L3);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REAL_ENERGY_DELIVERED_L3}.
     *
     * @return the value
     */
    default double getRealEnergyDeliveredL3() {
        Channel<Double> channel = this.getRealEnergyDeliveredL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REAL_ENERGY_DELIVERED_SUM}.
     *
     * @return the Channel
     */
    default Channel<Double> getRealEnergyDeliveredSumChannel() {
        return this.channel(ChannelId.REAL_ENERGY_DELIVERED_SUM);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REAL_ENERGY_DELIVERED_SUM}.
     *
     * @return the value
     */
    default double getRealEnergyDeliveredSum() {
        Channel<Double> channel = this.getRealEnergyDeliveredSumChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REAL_ENERGY_CONSUMED_L1}.
     *
     * @return the Channel
     */
    default Channel<Double> getRealEnergyConsumedL1Channel() {
        return this.channel(ChannelId.REAL_ENERGY_CONSUMED_L1);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REAL_ENERGY_CONSUMED_L1}.
     *
     * @return the value
     */
    default double getRealEnergyConsumedL1() {
        Channel<Double> channel = this.getRealEnergyConsumedL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REAL_ENERGY_CONSUMED_L2}.
     *
     * @return the Channel
     */
    default Channel<Double> getRealEnergyConsumedL2Channel() {
        return this.channel(ChannelId.REAL_ENERGY_CONSUMED_L2);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REAL_ENERGY_CONSUMED_L2}.
     *
     * @return the value
     */
    default double getRealEnergyConsumedL2() {
        Channel<Double> channel = this.getRealEnergyConsumedL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REAL_ENERGY_CONSUMED_L3}.
     *
     * @return the Channel
     */
    default Channel<Double> getRealEnergyConsumedL3Channel() {
        return this.channel(ChannelId.REAL_ENERGY_CONSUMED_L3);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REAL_ENERGY_CONSUMED_L3}.
     *
     * @return the value
     */
    default double getRealEnergyConsumedL3() {
        Channel<Double> channel = this.getRealEnergyConsumedL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REAL_ENERGY_CONSUMED_SUM}.
     *
     * @return the Channel
     */
    default Channel<Double> getRealEnergyConsumedSumChannel() {
        return this.channel(ChannelId.REAL_ENERGY_CONSUMED_SUM);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REAL_ENERGY_CONSUMED_SUM}.
     *
     * @return the value
     */
    default double getRealEnergyConsumedSum() {
        Channel<Double> channel = this.getRealEnergyConsumedSumChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#APPARENT_ENERGY_L1}.
     *
     * @return the Channel
     */
    default Channel<Double> getApparentEnergyL1Channel() {
        return this.channel(ChannelId.APPARENT_ENERGY_L1);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#APPARENT_ENERGY_L1}.
     *
     * @return the value
     */
    default double getApparentEnergyL1() {
        Channel<Double> channel = this.getApparentEnergyL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#APPARENT_ENERGY_L2}.
     *
     * @return the Channel
     */
    default Channel<Double> getApparentEnergyL2Channel() {
        return this.channel(ChannelId.APPARENT_ENERGY_L2);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#APPARENT_ENERGY_L2}.
     *
     * @return the value
     */
    default double getApparentEnergyL2() {
        Channel<Double> channel = this.getApparentEnergyL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#APPARENT_ENERGY_L3}.
     *
     * @return the Channel
     */
    default Channel<Double> getApparentEnergyL3Channel() {
        return this.channel(ChannelId.APPARENT_ENERGY_L3);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#APPARENT_ENERGY_L3}.
     *
     * @return the value
     */
    default double getApparentEnergyL3() {
        Channel<Double> channel = this.getApparentEnergyL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#APPARENT_ENERGY_SUM}.
     *
     * @return the Channel
     */
    default Channel<Double> getApparentEnergySumChannel() {
        return this.channel(ChannelId.APPARENT_ENERGY_SUM);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#APPARENT_ENERGY_SUM}.
     *
     * @return the value
     */
    default double getApparentEnergySum() {
        Channel<Double> channel = this.getApparentEnergySumChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REACTIVE_ENERGY_L1}.
     *
     * @return the Channel
     */
    default Channel<Double> getReactiveEnergyL1Channel() {
        return this.channel(ChannelId.REACTIVE_ENERGY_L1);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REACTIVE_ENERGY_L1}.
     *
     * @return the value
     */
    default double getReactiveEnergyL1() {
        Channel<Double> channel = this.getReactiveEnergyL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REACTIVE_ENERGY_L2}.
     *
     * @return the Channel
     */
    default Channel<Double> getReactiveEnergyL2Channel() {
        return this.channel(ChannelId.REACTIVE_ENERGY_L2);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REACTIVE_ENERGY_L2}.
     *
     * @return the value
     */
    default double getReactiveEnergyL2() {
        Channel<Double> channel = this.getReactiveEnergyL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REACTIVE_ENERGY_L3}.
     *
     * @return the Channel
     */
    default Channel<Double> getReactiveEnergyL3Channel() {
        return this.channel(ChannelId.REACTIVE_ENERGY_L3);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REACTIVE_ENERGY_L3}.
     *
     * @return the value
     */
    default double getReactiveEnergyL3() {
        Channel<Double> channel = this.getReactiveEnergyL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#REACTIVE_ENERGY_SUM}.
     *
     * @return the Channel
     */
    default Channel<Double> getReactiveEnergySumChannel() {
        return this.channel(ChannelId.REACTIVE_ENERGY_SUM);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#REACTIVE_ENERGY_SUM}.
     *
     * @return the value
     */
    default double getReactiveEnergySum() {
        Channel<Double> channel = this.getReactiveEnergySumChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.d));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#AVAILABILITY}.
     *
     * @return the Channel
     */
    default Channel<Short> getAvailabilityChannel() {
        return this.channel(ChannelId.AVAILABILITY);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#AVAILABILITY}.
     *
     * @return the value
     */
    default short getAvailability() {
        Channel<Short> channel = this.getAvailabilityChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#MODE_3_STATE}.
     *
     * @return the Channel
     */
    default Channel<String> getMode3StateChannel() {
        return this.channel(ChannelId.MODE_3_STATE);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#MODE_3_STATE}.
     *
     * @return the value
     */
    default String getMode3State() {
        Channel<String> channel = this.getMode3StateChannel();
        return channel.value().orElse(channel.getNextValue().orElse(""));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#ACTUAL_APPLIED_MAX_CURRENT}.
     *
     * @return the Channel
     */
    default Channel<Float> getActualAppliedMaxCurrentChannel() {
        return this.channel(ChannelId.ACTUAL_APPLIED_MAX_CURRENT);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#ACTUAL_APPLIED_MAX_CURRENT}.
     *
     * @return the value
     */
    default float getActualAppliedMaxCurrent() {
        Channel<Float> channel = this.getActualAppliedMaxCurrentChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#MODBUS_SLAVE_MAX_CURRENT_VALID_TIME}.
     *
     * @return the Channel
     */
    default Channel<Integer> getModbusSlaveMaxCurrentValidTimeChannel() {
        return this.channel(ChannelId.MODBUS_SLAVE_MAX_CURRENT_VALID_TIME);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#MODBUS_SLAVE_MAX_CURRENT_VALID_TIME}.
     *
     * @return the value
     */
    default int getModbusSlaveMaxCurrentValidTime() {
        Channel<Integer> channel = this.getModbusSlaveMaxCurrentValidTimeChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#MODBUS_SLAVE_MAX_CURRENT}.
     *
     * @return the Channel
     */
    default WriteChannel<Float> getModbusSlaveMaxCurrentChannel() {
        return this.channel(ChannelId.MODBUS_SLAVE_MAX_CURRENT);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#MODBUS_SLAVE_MAX_CURRENT}.
     *
     * @return the value
     */
    default float getModbusSlaveMaxCurrent() {
        WriteChannel<Float> channel = this.getModbusSlaveMaxCurrentChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Sets a command into the RemoteCommand register. See
     * {@link Alfen.ChannelId#MODBUS_SLAVE_MAX_CURRENT}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setModbusSlaveMaxCurrent(float value) throws OpenemsError.OpenemsNamedException {
        WriteChannel<Float> channel = this.getModbusSlaveMaxCurrentChannel();
        channel.setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#ACTIVE_LOAD_BALANCING_SAFE_CURRENT}.
     *
     * @return the Channel
     */
    default Channel<Float> getActiveLoadBalancingSafeCurrentChannel() {
        return this.channel(ChannelId.ACTIVE_LOAD_BALANCING_SAFE_CURRENT);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#ACTIVE_LOAD_BALANCING_SAFE_CURRENT}.
     *
     * @return the value
     */
    default float getActiveLoadBalancingSafeCurrent() {
        Channel<Float> channel = this.getActiveLoadBalancingSafeCurrentChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR}.
     *
     * @return the Channel
     */
    default Channel<Short> getModbusSlaveReceivedSetpointAccountedForChannel() {
        return this.channel(ChannelId.MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR}.
     *
     * @return the value
     */
    default short getModbusSlaveReceivedSetpointAccountedFor() {
        Channel<Short> channel = this.getModbusSlaveReceivedSetpointAccountedForChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Gets the Channel for {@link Alfen.ChannelId#CHARGE_PHASES}.
     *
     * @return the Channel
     */
    default WriteChannel<Short> getChargePhasesChannel() {
        return this.channel(ChannelId.CHARGE_PHASES);
    }

    /**
     * Gets the Value of {@link Alfen.ChannelId#CHARGE_PHASES}.
     *
     * @return the value
     */
    default short getChargePhases() {
        WriteChannel<Short> channel = this.getChargePhasesChannel();
        return channel.value().orElse(channel.getNextValue().orElse((short) 0));
    }

    /**
     * Sets a command into the RemoteCommand register. See
     * {@link Alfen.ChannelId#CHARGE_PHASES}.
     *
     * @param value the next write value
     * @throws OpenemsError.OpenemsNamedException on error
     */
    default void setChargePhases(short value) throws OpenemsError.OpenemsNamedException {
        WriteChannel<Short> channel = this.getChargePhasesChannel();
        channel.setNextWriteValue(value);
    }
}

