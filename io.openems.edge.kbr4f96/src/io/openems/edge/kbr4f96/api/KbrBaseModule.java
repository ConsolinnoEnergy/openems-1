package io.openems.edge.kbr4f96.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface KbrBaseModule extends OpenemsComponent {

    boolean moduleCheckout(int number);
    boolean moduleRemove(int number);

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * All Configuration Addresses for the Kbr Device.
         * Should they become necessary in the future, activate them like CONFIG_NEW_SYSTEM_TIME.
         */
        /*
        METERING_VOLTAGE_PRIMARY_TRANSDUCER(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        METERING_VOLTAGE_SECONDARY_TRANSDUCER(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        METERING_CURRENT_PRIMARY_TRANSDUCER(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        METERING_CURRENT_SECONDARY_TRANSDUCER(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        FREQUENCY(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        MEASUREMENT_TIME(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        ATTENUATION_VOLTAGE(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        ATTENUATION_CURRENT(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        SYNC_TYPE(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        TARIFF_CHANGE(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        LOW_TARIFF_ON(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        LOW_TARIFF_OFF(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        DAYLIGHTSAVINGS(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        SUMMER_TO_WINTER(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        WINTER_TO_SUMMER(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        INFINITE_COUNTER_ACTIVE_HT_GAIN(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),
        INFINITE_COUNTER_ACTIVE_NT_GAIN(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),
        INFINITE_COUNTER_BLIND_HT_GAIN(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),
        INFINITE_COUNTER_BLIND_NT_GAIN(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),
         */
        CONFIG_NEW_SYSTEM_TIME(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        /*
        DEFAULT_RESPONSE_TIME(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        BYTE_ORDER(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        ENERGY_TYPE(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        IMPULSE_TYPE(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        IMPULSE_FACTOR(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),
        IMPULSE_TIME(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        RELAY_ONE_PULL_OFFSET(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        RELAY_ONE_PUSH_OFFSET(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        RELAY_TWO_PULL_OFFSET(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        RELAY_TWO_PUSH_OFFSET(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_WRITE)),
        INFINITE_COUNTER_ACTIVE_HT_LOSS(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),
        INFINITE_COUNTER_ACTIVE_NT_LOSS(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),
        INFINITE_COUNTER_BLIND_HT_LOSS(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),
        INFINITE_COUNTER_BLIND_NT_LOSS(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),
         */
        /**
         * Kbr Commands, as specified in the Datasheet.
         */
        COMMAND_RESET_DEVICE(Doc.of(OpenemsType.BOOLEAN)),
        COMMAND_RESET_MAX_VALUES(Doc.of(OpenemsType.BOOLEAN)),
        COMMAND_RESET_MIN_VALUES(Doc.of(OpenemsType.BOOLEAN)),
        COMMAND_SWAP_TO_HT(Doc.of(OpenemsType.SHORT)),
        COMMAND_SWAP_TO_NT(Doc.of(OpenemsType.SHORT)),
        COMMAND_ERASE_FAIL_STATUS(Doc.of(OpenemsType.BOOLEAN)),
        INTERNAL_F001(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_WRITE)),
        INTERNAL_F002(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_WRITE)),
        INTERNAL_F003(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_WRITE)),
        INTERNAL_F004(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_WRITE)),
        INTERNAL_F005(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_WRITE)),
        INTERNAL_F006(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Actual Measuring Channels.
         */
        GRID_FREQUENCY(Doc.of(OpenemsType.FLOAT).unit(Unit.HERTZ)),
        NEUTRAL_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
        AVERAGE_NEUTRAL_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
        TOTAL_ACTIVE_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)),
        TOTAL_IDLE_POWER(Doc.of(OpenemsType.FLOAT)),
        TOTAL_APPARENT_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE)),
        POWER_FACTOR(Doc.of(OpenemsType.FLOAT)),
        ERROR_STATUS(Doc.of(OpenemsType.LONG)),
        SYSTEM_TIME(Doc.of(OpenemsType.LONG)),
        COUNTER_STATE_ACTIVE_POWER_HT_GAIN(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS)),
        COUNTER_STATE_ACTIVE_POWER_NT_GAIN(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS)),
        COUNTER_STATE_IDLE_POWER_HT_GAIN(Doc.of(OpenemsType.DOUBLE)),
        COUNTER_STATE_IDLE_POWER_NT_GAIN(Doc.of(OpenemsType.DOUBLE)),
        COUNTER_STATE_ACTIVE_POWER_HT_LOSS(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS)),
        COUNTER_STATE_ACTIVE_POWER_NT_LOSS(Doc.of(OpenemsType.DOUBLE).unit(Unit.WATT_HOURS)),
        COUNTER_STATE_IDLE_POWER_HT_LOSS(Doc.of(OpenemsType.DOUBLE)),
        COUNTER_STATE_IDLE_POWER_NT_LOSS(Doc.of(OpenemsType.DOUBLE));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    default WriteChannel<Long> getNewConfigSystemTime() {
        return this.channel(ChannelId.CONFIG_NEW_SYSTEM_TIME);
    }

    default Channel<Boolean> getCommandReset() {
        return this.channel(ChannelId.COMMAND_RESET_DEVICE);
    }

    default Channel<Boolean> getCommandMax() {
        return this.channel(ChannelId.COMMAND_RESET_MAX_VALUES);
    }

    default Channel<Boolean> getCommandMin() {
        return this.channel(ChannelId.COMMAND_RESET_MIN_VALUES);
    }

    default Channel<Short> getCommandHT() {
        return this.channel(ChannelId.COMMAND_SWAP_TO_HT);
    }

    default Channel<Short> getCommandNT() {
        return this.channel(ChannelId.COMMAND_SWAP_TO_NT);
    }

    default Channel<Boolean> getCommandEraseFail() {
        return this.channel(ChannelId.COMMAND_ERASE_FAIL_STATUS);
    }

    default WriteChannel<Short> getInternalReset() {
        return this.channel(ChannelId.INTERNAL_F001);
    }

    default WriteChannel<Short> getInternalMax() {
        return this.channel(ChannelId.INTERNAL_F002);
    }

    default WriteChannel<Short> getInternalMin() {
        return this.channel(ChannelId.INTERNAL_F003);
    }

    default WriteChannel<Short> getInternalHT() {
        return this.channel(ChannelId.INTERNAL_F004);
    }

    default WriteChannel<Short> getInternalNT() {
        return this.channel(ChannelId.INTERNAL_F005);
    }

    default WriteChannel<Short> getInternalFail() {
        return this.channel(ChannelId.INTERNAL_F006);
    }

    default Channel<Float> getGridFrequencyChannel() {
        return this.channel(ChannelId.GRID_FREQUENCY);
    }

    default Channel<Float> getNeutralPowerChannel() {
        return this.channel(ChannelId.NEUTRAL_POWER);
    }

    default Channel<Float> getAverageNeutralPowerChannel() {
        return this.channel(ChannelId.AVERAGE_NEUTRAL_POWER);
    }

    default Channel<Float> getWantedActivePowerChannel() {
        return this.channel(ChannelId.TOTAL_ACTIVE_POWER);
    }

    default Channel<Float> getWantedIdlePowerChannel() {
        return this.channel(ChannelId.TOTAL_IDLE_POWER);
    }

    default Channel<Float> getWantedApparentPowerChannel() {
        return this.channel(ChannelId.TOTAL_APPARENT_POWER);
    }

    default Channel<Float> getPowerFactorChannel() {
        return this.channel(ChannelId.POWER_FACTOR);
    }

    default Channel<Long> getErrorStatusChannel() {
        return this.channel(ChannelId.ERROR_STATUS);
    }

    default Channel<Long> getSystemTimeChannel() {
        return this.channel(ChannelId.SYSTEM_TIME);
    }

    default Channel<Double> getCounterStateActivePowerHtGainChannel() {
        return this.channel(ChannelId.COUNTER_STATE_ACTIVE_POWER_HT_GAIN);
    }

    default Channel<Double> getCounterStateActivePowerNtGainChannel() {
        return this.channel(ChannelId.COUNTER_STATE_ACTIVE_POWER_NT_GAIN);
    }

    default Channel<Double> getCounterStateIdlePowerHtGainChannel() {
        return this.channel(ChannelId.COUNTER_STATE_IDLE_POWER_HT_GAIN);
    }

    default Channel<Double> getCounterStateIdlePowerNtGainChannel() {
        return this.channel(ChannelId.COUNTER_STATE_IDLE_POWER_NT_GAIN);
    }

    default Channel<Double> getCounterStateActivePowerHtLossChannel() {
        return this.channel(ChannelId.COUNTER_STATE_ACTIVE_POWER_HT_LOSS);
    }

    default Channel<Double> getCounterStateActivePowerNtLossChannel() {
        return this.channel(ChannelId.COUNTER_STATE_ACTIVE_POWER_NT_LOSS);
    }

    default Channel<Double> getCounterStateIdlePowerHtLossChannel() {
        return this.channel(ChannelId.COUNTER_STATE_IDLE_POWER_HT_LOSS);
    }

    default Channel<Double> getCounterStateIdlePowerNtLossChannel() {
        return this.channel(ChannelId.COUNTER_STATE_IDLE_POWER_NT_LOSS);
    }

}