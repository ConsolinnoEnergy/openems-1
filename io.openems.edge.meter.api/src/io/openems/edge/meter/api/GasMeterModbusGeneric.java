package io.openems.edge.meter.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;

public interface GasMeterModbusGeneric extends MeterModbusGeneric {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {



        /**
         * The Percolation of the GasMeter.
         *
         * <ul>
         * <li>Interface: GasMeterModbusGeneric
         * <li>Type: Long
         * </ul>
         */
        PERCOLATION_LONG(Doc.of(OpenemsType.LONG)),

        /**
         * The Percolation of the GasMeter.
         *
         * <ul>
         * <li>Interface: GasMeterModbusGeneric
         * <li>Type: Double
         * </ul>
         */
        PERCOLATION_DOUBLE(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Total Consumed Energy Cubic Meter.
         * <ul>
         *     <li>Interface: GasMeter
         *     <li>Type: Integer
         *     <li>Unit: CubicMeter
         * </ul>
         */

        READ_ENERGY_LONG(Doc.of(OpenemsType.LONG)),

        /**
         * Total Consumed Energy Cubic Meter.
         * <ul>
         *     <li>Interface: GasMeter
         *     <li>Type: Integer
         *     <li>Unit: CubicMeter
         * </ul>
         */

        READ_ENERGY_DOUBLE(Doc.of(OpenemsType.DOUBLE)),

        /**
         * Flow Temperature in Degree Celsius.
         * <ul>
         *     <li>Interface: GasMeter
         *     <li>Type: Integer
         *     <li>Unit: Degree Celsius
         * </ul>
         */

        FLOW_TEMP_LONG(Doc.of(OpenemsType.LONG)),

        /**
         * Flow Temperature in Degree Celsius.
         * <ul>
         *     <li>Interface: GasMeter
         *     <li>Type: Integer
         *     <li>Unit: Degree Celsius
         * </ul>
         */

        FLOW_TEMP_DOUBLE(Doc.of(OpenemsType.DOUBLE));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }


    }

    /**
     * Get the ReadingPercolationDouble Channel.
     * Only Call by Generic Meter!
     * The actual Percolation will be written into {@link GasMeter#getPercolationChannel()} ()}.
     *
     * @return the Channel.
     */

    default Channel<Double> getPercolationDoubleChannel() {
        return this.channel(ChannelId.PERCOLATION_DOUBLE);
    }

    /**
     * Get the ReadEnergyLong Channel.
     * Only Call by Generic Meter!
     * The actual Energy will be written into {@link GasMeter#getTotalConsumedEnergyCubicMeterChannel()} ()}.
     *
     * @return the Channel.
     */

    default Channel<Long> getReadPercolationLongChannel() {
        return this.channel(ChannelId.PERCOLATION_LONG);
    }

    /**
     * Get the ReadingPercolationLong Channel.
     * Only Call by Generic Meter!
     * The actual Reading Power will be written into {@link HeatMeter#getReadingPowerChannel()}.
     *
     * @return the Channel.
     */

    default Channel<Double> getReadPercolationDoubleChannel() {
        return this.channel(ChannelId.PERCOLATION_DOUBLE);
    }

    /**
     * Get the ReadEnergyLong Channel.
     * Only Call by Generic Meter!
     * The actual Reading Energy will be written into {@link GasMeter#getTotalConsumedEnergyCubicMeterChannel()}.
     *
     * @return the Channel.
     */

    default Channel<Long> getReadEnergyLongChannel() {
        return this.channel(ChannelId.READ_ENERGY_LONG);
    }

    /**
     * Get the ReadEnergyDouble Channel.
     * Only Call by Generic Meter!
     * The actual Reading Energy will be written into {@link GasMeter#getTotalConsumedEnergyCubicMeterChannel()}.
     *
     * @return the Channel.
     */

    default Channel<Double> getReadEnergyDoubleChannel() {
        return this.channel(ChannelId.READ_ENERGY_DOUBLE);
    }


    /**
     * Get the ReadEnergyDouble Channel.
     * Only Call by Generic Meter!
     * The actual Reading Energy will be written into {@link GasMeter#getTotalConsumedEnergyCubicMeterChannel()}.
     *
     * @return the Channel.
     */

    default Channel<Double> getFlowTempDoubleChannel() {
        return this.channel(ChannelId.FLOW_TEMP_DOUBLE);
    }



}

