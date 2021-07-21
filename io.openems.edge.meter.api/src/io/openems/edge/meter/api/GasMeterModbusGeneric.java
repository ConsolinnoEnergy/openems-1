package io.openems.edge.meter.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;

public interface GasMeterModbusGeneric extends MeterModbusGeneric {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * The Percolation of the Gasmeter.
         *
         * <ul>
         * <li>Interface: GasMeter
         * <li>Type: Integer
         * <li>Unit: CubicMeterPerSecond
         * </ul>
         */
        PERCOLATION(Doc.of(OpenemsType.INTEGER).unit(Unit.CUBICMETER_PER_SECOND)),
        /**
         * Total Consumed Energy Cubic Meter.
         * <ul>
         *     <li>Interface: GasMeter
         *     <li>Type: Integer
         *     <li>Unit: CubicMeter
         * </ul>
         */
        TOTAL_CONSUMED_ENERGY_CUBIC_METER(Doc.of(OpenemsType.INTEGER).unit(Unit.CUBIC_METER)),
        /**
         * Flow Temperature in Degree Celsius.
         * <ul>
         *     <li>Interface: GasMeter
         *     <li>Type: Integer
         *     <li>Unit: Degree Celsius
         * </ul>
         */
        FLOW_TEMP(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)),
        /**
         * Return Temperature in Degree Celsius.
         * <ul>
         *     <li>Interface: GasMeter
         *     <li>Type: Integer
         *     <li>Unit: Degree Celsius
         * </ul>
         */
        RETURN_TEMP(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }


    }

    /**
     * Gets the Percolation of the GasMeter.
     *
     * @return the Channel.
     */
    default Channel<Integer> getPercolationChannel() {
        return this.channel(GasMeter.ChannelId.PERCOLATION);
    }

    /**
     * Gets the Total Consumed Energy.
     *
     * @return the Channel
     */
    default Channel<Integer> getTotalConsumedEnergyCubicMeterChannel() {
        return this.channel(GasMeter.ChannelId.TOTAL_CONSUMED_ENERGY_CUBIC_METER);
    }

    /**
     * Gets the Flow Temperature Channel.
     *
     * @return the Channel
     */
    default Channel<Float> getFlowTempChannel() {
        return this.channel(GasMeter.ChannelId.FLOW_TEMP);
    }

    /**
     * Gets the return Temperature Channel.
     *
     * @return the Channel.
     */
    default Channel<Float> getReturnTemp() {
        return this.channel(GasMeter.ChannelId.RETURN_TEMP);
    }
}

