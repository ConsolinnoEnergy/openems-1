package io.openems.edge.meter.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.GenericModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a Symmetric Meter.
 *
 * <p>
 * <ul>
 * <li>Negative ActivePower and ConsumptionActivePower represent Consumption,
 * i.e. power that is 'leaving the system', e.g. feed-to-grid
 * <li>Positive ActivePower and ProductionActivePower represent Production, i.e.
 * power that is 'entering the system', e.g. buy-from-grid
 * </ul>
 */
public interface SymmetricMeterModbusGeneric extends GenericModbusComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Frequency.
         *
         * <ul>
         * <li>Interface: Meter Symmetric
         * <li>Type: Integer
         * <li>Unit: mHz
         * <li>Range: only positive values
         * </ul>
         */
        FREQUENCY_LONG(Doc.of(OpenemsType.LONG)),
        FREQUENCY_DOUBLE(Doc.of(OpenemsType.INTEGER)),

        /**
         * Minimum Ever Active Power.
         *
         * <ul>
         * <li>Interface: Meter Symmetric
         * <li>Type: Integer
         * <li>Unit: W
         * <li>Range: negative or '0'
         * <li>Implementation Note: value is automatically derived from ACTIVE_POWER
         * </ul>
         */
        MIN_ACTIVE_POWER_LONG(Doc.of(OpenemsType.LONG)),
        MIN_ACTIVE_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Maximum Ever Active Power.
         *
         * <ul>
         * <li>Interface: Meter Symmetric
         * <li>Type: Integer
         * <li>Unit: W
         * <li>Range: positive or '0'
         * <li>Implementation Note: value is automatically derived from ACTIVE_POWER
         * </ul>
         */
        MAX_ACTIVE_POWER_LONG(Doc.of(OpenemsType.LONG)),
        MAX_ACTIVE_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Active Power.
         *
         * <ul>
         * <li>Interface: Meter Symmetric
         * <li>Type: Integer
         * <li>Unit: W
         * <li>Range: negative values for Consumption (power that is 'leaving the
         * system', e.g. feed-to-grid); positive for Production (power that is 'entering
         * the system')
         * </ul>
         */
        ACTIVE_POWER_LONG(Doc.of(OpenemsType.LONG)),
        ACTIVE_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Reactive Power.
         *
         * <ul>
         * <li>Interface: Meter Symmetric
         * <li>Type: Integer
         * <li>Unit: var
         * <li>Range: negative values for Consumption (power that is 'leaving the
         * system', e.g. feed-to-grid); positive for Production (power that is 'entering
         * the system')
         * </ul>
         */
        REACTIVE_POWER_LONG(Doc.of(OpenemsType.LONG)),
        REACTIVE_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Active Production Energy.
         *
         * <ul>
         * <li>Interface: Meter Symmetric
         * <li>Type: Integer
         * <li>Unit: Wh
         * </ul>
         */
        ACTIVE_PRODUCTION_ENERGY_LONG(Doc.of(OpenemsType.LONG)),
        ACTIVE_PRODUCTION_ENERGY_DOUBLE(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Active Consumption Energy.
         *
         * <ul>
         * <li>Interface: Meter Symmetric
         * <li>Type: Integer
         * <li>Unit: Wh
         * </ul>
         */
        ACTIVE_CONSUMPTION_ENERGY_LONG(Doc.of(OpenemsType.LONG)),
        ACTIVE_CONSUMPTION_ENERGY_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Voltage.
         *
         * <ul>
         * <li>Interface: Meter Symmetric
         * <li>Type: Integer
         * <li>Unit: mV
         * </ul>
         */
        VOLTAGE_LONG(Doc.of(OpenemsType.LONG)),
        VOLTAGE_DOUBLE(Doc.of(OpenemsType.DOUBLE)),
        /**
         * Current.
         *
         * <ul>
         * <li>Interface: Meter Symmetric
         * <li>Type: Integer
         * <li>Unit: mA
         * </ul>
         */
        CURRENT_LONG(Doc.of(OpenemsType.LONG)),
        CURRENT_DOUBLE(Doc.of(OpenemsType.DOUBLE));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Gets the Channel for {@link ChannelId#FREQUENCY_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getFrequencyChannelLong() {
        return this.channel(ChannelId.FREQUENCY_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#FREQUENCY_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getFrequencyChannelDouble() {
        return this.channel(ChannelId.FREQUENCY_DOUBLE);
    }

    default Channel<?> _hasFrequency() {
        return GenericModbusComponent.getValueDefinedChannel(this._getFrequencyChannelLong(),
                this._getFrequencyChannelDouble());
    }


    /**
     * Gets the Channel for {@link ChannelId#MIN_ACTIVE_POWER_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getMinActivePowerChannelLong() {
        return this.channel(ChannelId.MIN_ACTIVE_POWER_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#MIN_ACTIVE_POWER_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getMinActivePowerChannelDouble() {
        return this.channel(ChannelId.MIN_ACTIVE_POWER_DOUBLE);
    }

    default Channel<?> _hasMinActivePower() {
        return GenericModbusComponent.getValueDefinedChannel(this._getMinActivePowerChannelLong(),
                this._getMinActivePowerChannelDouble());
    }


    /**
     * Gets the Channel for {@link ChannelId#MAX_ACTIVE_POWER_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getMaxActivePowerChannelLong() {
        return this.channel(ChannelId.MAX_ACTIVE_POWER_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#MAX_ACTIVE_POWER_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getMaxActivePowerChannelDouble() {
        return this.channel(ChannelId.MAX_ACTIVE_POWER_DOUBLE);
    }

    default Channel<?> _hasMaxActivePower() {
        return GenericModbusComponent.getValueDefinedChannel(this._getMaxActivePowerChannelLong(),
                this._getMaxActivePowerChannelDouble());
    }


    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_POWER_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getActivePowerChannelLong() {
        return this.channel(ChannelId.ACTIVE_POWER_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_POWER_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getActivePowerChannelDouble() {
        return this.channel(ChannelId.ACTIVE_POWER_DOUBLE);
    }


    default Channel<?> _hasActivePower() {
        return GenericModbusComponent.getValueDefinedChannel(this._getActivePowerChannelLong(),
                this._getActivePowerChannelDouble());
    }


    /**
     * Gets the Channel for {@link ChannelId#REACTIVE_POWER_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getReactivePowerChannelLong() {
        return this.channel(ChannelId.REACTIVE_POWER_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#REACTIVE_POWER_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getReactivePowerChannelDouble() {
        return this.channel(ChannelId.REACTIVE_POWER_DOUBLE);
    }

    default Channel<?> _hasReactivePower() {
        return GenericModbusComponent.getValueDefinedChannel(this._getReactivePowerChannelLong(),
                this._getReactivePowerChannelDouble());
    }


    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getActiveProductionEnergyChannelLong() {
        return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_PRODUCTION_ENERGY_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getActiveProductionEnergyChannelDouble() {
        return this.channel(ChannelId.ACTIVE_PRODUCTION_ENERGY_DOUBLE);
    }

    default Channel<?> _hasActiveProductionEnergy() {
        return GenericModbusComponent.getValueDefinedChannel(this._getActiveProductionEnergyChannelLong(),
                this._getActiveProductionEnergyChannelDouble());
    }


    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getActiveConsumptionEnergyChannelLong() {
        return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#ACTIVE_CONSUMPTION_ENERGY_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getActiveConsumptionEnergyChannelDouble() {
        return this.channel(ChannelId.ACTIVE_CONSUMPTION_ENERGY_DOUBLE);
    }

    default Channel<?> _hasActiveConsumptionEnergy() {
        return GenericModbusComponent.getValueDefinedChannel(this._getActiveConsumptionEnergyChannelLong(),
                this._getActiveConsumptionEnergyChannelDouble());
    }

    /**
     * Gets the Channel for {@link ChannelId#VOLTAGE_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getVoltageChannelLong() {
        return this.channel(ChannelId.VOLTAGE_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#VOLTAGE_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getVoltageChannelDouble() {
        return this.channel(ChannelId.VOLTAGE_DOUBLE);
    }

    default Channel<?> _hasVoltage() {
        return GenericModbusComponent.getValueDefinedChannel(this._getVoltageChannelLong(),
                this._getVoltageChannelDouble());
    }


    /**
     * Gets the Channel for {@link ChannelId#CURRENT_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getCurrentChannelLong() {
        return this.channel(ChannelId.CURRENT_LONG);
    }

    /**
     * Gets the Channel for {@link ChannelId#CURRENT_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getCurrentChannelDouble() {
        return this.channel(ChannelId.CURRENT_DOUBLE);
    }

    default Channel<?> _hasCurrent() {
        return GenericModbusComponent.getValueDefinedChannel(this._getCurrentChannelLong(),
                this._getCurrentChannelDouble());
    }

}
