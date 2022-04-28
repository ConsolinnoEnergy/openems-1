package io.openems.edge.meter.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.GenericModbusComponent;

/**
 * Represents an Asymmetric Meter.
 * <p>
 * - Negative ActivePowerL1/L2/L3 and ConsumptionActivePowerL1/L2/L3 represent
 * Consumption, i.e. power that is 'leaving the system', e.g. feed-to-grid
 * <p>
 * - Positive ActivePowerL1/L2/L3 and ProductionActivePowerL1/L2/L3 represent
 * Production, i.e. power that is 'entering the system', e.g. buy-from-grid
 */
public interface AsymmetricMeterModbusGeneric extends SymmetricMeterModbusGeneric {

  String POWER_DOC_TEXT = "Negative values for Consumption; positive for Production";

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Active Power L1.
         *
         * <ul>
         * <li>Interface: Meter Asymmetric
         * <li>Type: Integer
         * <li>Unit: W
         * <li>Range: negative values for Consumption (power that is 'leaving the
         * system', e.g. feed-to-grid); positive for Production (power that is 'entering
         * the system')
         * </ul>
         */
        ACTIVE_POWER_L1_LONG(Doc.of(OpenemsType.LONG)),
        ACTIVE_POWER_L1_DOUBLE(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Active Power L2.
         *
         * <ul>
         * <li>Interface: Meter Asymmetric
         * <li>Type: Integer
         * <li>Unit: W
         * <li>Range: negative values for Consumption (power that is 'leaving the
         * system', e.g. feed-to-grid); positive for Production (power that is 'entering
         * the system')
         * </ul>
         */
        ACTIVE_POWER_L2_LONG(Doc.of(OpenemsType.LONG)),
        ACTIVE_POWER_L2_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Active Power L3.
         *
         * <ul>
         * <li>Interface: Meter Asymmetric
         * <li>Type: Integer
         * <li>Unit: W
         * <li>Range: negative values for Consumption (power that is 'leaving the
         * system', e.g. feed-to-grid); positive for Production (power that is 'entering
         * the system')
         * </ul>
         */
        ACTIVE_POWER_L3_LONG(Doc.of(OpenemsType.LONG)),
        ACTIVE_POWER_L3_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Reactive Power L1.
         *
         * <ul>
         * <li>Interface: Meter Asymmetric
         * <li>Type: Integer
         * <li>Unit: var
         * <li>Range: negative values for Consumption (power that is 'leaving the
         * system', e.g. feed-to-grid); positive for Production (power that is 'entering
         * the system')
         * </ul>
         */
        REACTIVE_POWER_L1_LONG(Doc.of(OpenemsType.LONG)),
        REACTIVE_POWER_L1_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Reactive Power L2.
         *
         * <ul>
         * <li>Interface: Meter Asymmetric
         * <li>Type: Integer
         * <li>Unit: var
         * <li>Range: negative values for Consumption (power that is 'leaving the
         * system', e.g. feed-to-grid); positive for Production (power that is 'entering
         * the system')
         * </ul>
         */
        REACTIVE_POWER_L2_LONG(Doc.of(OpenemsType.LONG)),
        REACTIVE_POWER_L2_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Reactive Power L3.
         *
         * <ul>
         * <li>Interface: Meter Asymmetric
         * <li>Type: Integer
         * <li>Unit: var
         * <li>Range: negative values for Consumption (power that is 'leaving the
         * system', e.g. feed-to-grid); positive for Production (power that is 'entering
         * the system')
         * </ul>
         */
        REACTIVE_POWER_L3_LONG(Doc.of(OpenemsType.LONG)),
        REACTIVE_POWER_L3_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Voltage L1.
         *
         * <ul>
         * <li>Interface: Meter Asymmetric
         * <li>Type: Integer
         * <li>Unit: mV
         * </ul>
         */
        VOLTAGE_L1_LONG(Doc.of(OpenemsType.LONG)),
        VOLTAGE_L1_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Voltage L2.
         *
         * <ul>
         * <li>Interface: Meter Asymmetric
         * <li>Type: Integer
         * <li>Unit: mV
         * </ul>
         */
        VOLTAGE_L2_LONG(Doc.of(OpenemsType.LONG)),
        VOLTAGE_L2_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Voltage L3.
         *
         * <ul>
         * <li>Interface: Meter Asymmetric
         * <li>Type: Integer
         * <li>Unit: mV
         * </ul>
         */
        VOLTAGE_L3_LONG(Doc.of(OpenemsType.LONG)),
        VOLTAGE_L3_DOUBLE(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Current L1.
         *
         * <ul>
         * <li>Interface: Meter Asymmetric
         * <li>Type: Integer
         * <li>Unit: mA
         * </ul>
         */
        CURRENT_L1_LONG(Doc.of(OpenemsType.LONG)),
        CURRENT_L1_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Current L2.
         *
         * <ul>
         * <li>Interface: Meter Asymmetric
         * <li>Type: Integer
         * <li>Unit: mA
         * </ul>
         */
        CURRENT_L2_LONG(Doc.of(OpenemsType.LONG)),
        CURRENT_L2_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Current L3.
         *
         * <ul>
         * <li>Interface: Meter Asymmetric
         * <li>Type: Integer
         * <li>Unit: mA
         * </ul>
         */
        CURRENT_L3_LONG(Doc.of(OpenemsType.LONG)),
        CURRENT_L3_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L1_LONG}.
     *
     * @return the Channel
     */
    default Channel<Integer> _getActivePowerL1ChannelLong() {
        return this.channel(ChannelId.ACTIVE_POWER_L1_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L1_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getActivePowerL1ChannelDouble() {
        return this.channel(ChannelId.ACTIVE_POWER_L1_DOUBLE);
    }

    default Channel<?> _hasActivePowerL1() {
        return GenericModbusComponent.getValueDefinedChannel(this._getActivePowerL1ChannelLong(),
                this._getActivePowerL1ChannelDouble());
    }

    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L2_LONG}.
     *
     * @return the Channel
     */
    default Channel<Integer> _getActivePowerL2ChannelLong() {
        return this.channel(ChannelId.ACTIVE_POWER_L2_LONG);
    }
    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L2_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getActivePowerL2ChannelDouble() {
        return this.channel(ChannelId.ACTIVE_POWER_L2_DOUBLE);
    }

    default Channel<?> _hasActivePowerL2() {
        return GenericModbusComponent.getValueDefinedChannel(this._getActivePowerL2ChannelLong(),
                this._getActivePowerL2ChannelDouble());
    }

    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L3_LONG}.
     *
     * @return the Channel
     */
    default Channel<Integer> _getActivePowerL3ChannelLong() {
        return this.channel(ChannelId.ACTIVE_POWER_L3_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L3_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getActivePowerL3ChannelDouble() {
        return this.channel(ChannelId.ACTIVE_POWER_L3_DOUBLE);
    }

    default Channel<?> _hasActivePowerL3() {
        return GenericModbusComponent.getValueDefinedChannel(this._getActivePowerL3ChannelLong(),
                this._getActivePowerL3ChannelDouble());
    }


    /**
     * Gets the Channel for {@link ChannelId#REACTIVE_POWER_L1_LONG}.
     *
     * @return the Channel
     */
    default Channel<Integer> _getReactivePowerL1ChannelLong() {
        return this.channel(ChannelId.REACTIVE_POWER_L1_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#REACTIVE_POWER_L1_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getReactivePowerL1ChannelDouble() {
        return this.channel(ChannelId.REACTIVE_POWER_L1_DOUBLE);
    }

    default Channel<?> _hasReactivePowerL1() {
        return GenericModbusComponent.getValueDefinedChannel(this._getReactivePowerL1ChannelLong(),
                this._getReactivePowerL1ChannelDouble());
    }

    /**
     * Gets the Channel for {@link ChannelId#REACTIVE_POWER_L2_LONG}.
     *
     * @return the Channel
     */
    default Channel<Integer> _getReactivePowerL2ChannelLong() {
        return this.channel(ChannelId.REACTIVE_POWER_L2_LONG);
    }
    /**
     * Gets the Channel for {@link ChannelId#REACTIVE_POWER_L2_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getReactivePowerL2ChannelDouble() {
        return this.channel(ChannelId.REACTIVE_POWER_L2_DOUBLE);
    }

    default Channel<?> _hasReactivePowerL2() {
        return GenericModbusComponent.getValueDefinedChannel(this._getReactivePowerL2ChannelLong(),
                this._getReactivePowerL2ChannelDouble());
    }

    /**
     * Gets the Channel for {@link ChannelId#REACTIVE_POWER_L3_LONG}.
     *
     * @return the Channel
     */
    default Channel<Integer> _getReactivePowerL3ChannelLong() {
        return this.channel(ChannelId.REACTIVE_POWER_L3_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#REACTIVE_POWER_L3_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getReactivePowerL3ChannelDouble() {
        return this.channel(ChannelId.REACTIVE_POWER_L3_DOUBLE);
    }

    default Channel<?> _hasReactivePowerL3() {
        return GenericModbusComponent.getValueDefinedChannel(this._getReactivePowerL3ChannelLong(),
                this._getReactivePowerL3ChannelDouble());
    }


    /**
     * Gets the Channel for {@link ChannelId#VOLTAGE_L1_LONG}.
     *
     * @return the Channel
     */
    default Channel<Integer> _getVoltageL1ChannelLong() {
        return this.channel(ChannelId.VOLTAGE_L1_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#VOLTAGE_L1_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getVoltageL1ChannelDouble() {
        return this.channel(ChannelId.VOLTAGE_L1_DOUBLE);
    }


    default Channel<?> _hasVoltageL1() {
        return GenericModbusComponent.getValueDefinedChannel(this._getVoltageL1ChannelLong(),
                this._getVoltageL1ChannelDouble());
    }



    /**
     * Gets the Channel for {@link ChannelId#VOLTAGE_L2_LONG}.
     *
     * @return the Channel
     */
    default Channel<Integer> _getVoltageL2ChannelLong() {
        return this.channel(ChannelId.VOLTAGE_L2_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#VOLTAGE_L2_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getVoltageL2ChannelDouble() {
        return this.channel(ChannelId.VOLTAGE_L2_DOUBLE);
    }


    default Channel<?> _hasVoltageL2() {
        return GenericModbusComponent.getValueDefinedChannel(this._getVoltageL2ChannelLong(),
                this._getVoltageL2ChannelDouble());
    }

    /**
     * Gets the Channel for {@link ChannelId#VOLTAGE_L3_LONG}.
     *
     * @return the Channel
     */
    default Channel<Integer> _getVoltageL3ChannelLong() {
        return this.channel(ChannelId.VOLTAGE_L3_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#VOLTAGE_L3_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getVoltageL3ChannelDouble() {
        return this.channel(ChannelId.VOLTAGE_L3_DOUBLE);
    }


    default Channel<?> _hasVoltageL3() {
        return GenericModbusComponent.getValueDefinedChannel(this._getVoltageL3ChannelLong(),
                this._getVoltageL3ChannelDouble());
    }


    /**
     * Gets the Channel for {@link ChannelId#CURRENT_L1_LONG}.
     *
     * @return the Channel
     */
    default Channel<Integer> _getCurrentL1ChannelLong() {
        return this.channel(ChannelId.CURRENT_L1_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#CURRENT_L1_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getCurrentL1ChannelDouble() {
        return this.channel(ChannelId.CURRENT_L1_DOUBLE);
    }

    default Channel<?> _hasCurrentL1() {
        return GenericModbusComponent.getValueDefinedChannel(this._getCurrentL1ChannelLong(),
                this._getCurrentL1ChannelDouble());
    }

    /**
     * Gets the Channel for {@link ChannelId#CURRENT_L2_LONG}.
     *
     * @return the Channel
     */
    default Channel<Integer> _getCurrentL2ChannelLong() {
        return this.channel(ChannelId.CURRENT_L2_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#CURRENT_L2_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getCurrentL2ChannelDouble() {
        return this.channel(ChannelId.CURRENT_L2_DOUBLE);
    }

    default Channel<?> _hasCurrentL2() {
        return GenericModbusComponent.getValueDefinedChannel(this._getCurrentL2ChannelLong(),
                this._getCurrentL2ChannelDouble());
    }

    /**
     * Gets the Channel for {@link ChannelId#CURRENT_L3_LONG}.
     *
     * @return the Channel
     */
    default Channel<Integer> _getCurrentL3ChannelLong() {
        return this.channel(ChannelId.CURRENT_L3_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#CURRENT_L3_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getCurrentL3ChannelDouble() {
        return this.channel(ChannelId.CURRENT_L3_DOUBLE);
    }

    default Channel<?> _hasCurrentL3() {
        return GenericModbusComponent.getValueDefinedChannel(this._getCurrentL3ChannelLong(),
                this._getCurrentL1ChannelDouble());
    }




}
