package io.openems.edge.meter.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;

/**
 * Generic WaterMeter Modbus Nature extension of Meter Modbus Generic.
 * This Nature will be used by Generic Water Meter components to
 */
public interface WaterMeterModbusGeneric extends MeterModbusGeneric {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Read Water Double.
         *
         * <ul>
         * <li>Interface: WaterMeterModbusGeneric
         * <li>Type: Double
         * </ul>
         */
        READ_WATER_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        /**
         * Read Water Long.
         *
         * <ul>
         * <li>Interface: WaterMeterModbusGeneric
         * <li>Type: Double
         * </ul>
         */
        READ_WATER_LONG(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Get the ReadEnergyLong Channel.
     * Only Call by Generic Meter!
     * The actual Reading Energy will be written into {@link GasMeter#getTotalConsumedEnergyCubicMeterChannel()}.
     *
     * @return the Channel.
     */

    default Channel<Double> _getReadWaterDoubleChannel() {
        return this.channel(ChannelId.READ_WATER_DOUBLE);
    }

    /**
     * Get the ReadEnergyLong Channel.
     * Only Call by Generic Meter!
     * The actual Reading Energy will be written into {@link GasMeter#getTotalConsumedEnergyCubicMeterChannel()}.
     *
     * @return the Channel.
     */

    default Channel<Double> _getReadWaterLongChannel() {
        return this.channel(ChannelId.READ_WATER_LONG);
    }
}

