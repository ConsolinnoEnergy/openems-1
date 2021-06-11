package io.openems.edge.evcs.alfen.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
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

    default Channel<Float> getCurrentL1Channel() {
        return this.channel(ChannelId.CURRENT_L1);
    }

    default Channel<String> getMeterStateChannel() {
        return this.channel(ChannelId.METER_STATE);
    }


    default float getCurrentL1() {
        Channel<Float> channel = this.getCurrentL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }
}

