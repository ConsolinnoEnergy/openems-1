package io.openems.edge.meter.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;

public interface HeatMeterModbusGeneric extends MeterModbusGeneric {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Reading Power of the HeatMeter.
         * <ul>
         * <li> Interface: HeatMeter
         * <li> Type: Long
         * </ul>
         */
        READING_POWER_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),

        /**
         * Reading Power of the HeatMeter.
         * <ul>
         * <li> Interface: HeatMeter
         * <li> Type: Dobule
         * </ul>
         */
        READING_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        /**
         * Total Consumed Energy.
         *
         * <ul>
         * <li>Interface: HeatMeter
         * <li>Type: Long
         * </ul>
         */
        READING_ENERGY_LONG(Doc.of(OpenemsType.LONG)),

        /**
         * Total Consumed Energy.
         *
         * <ul>
         * <li>Interface: HeatMeter
         * <li>Type: Integer
         * <li>Unit: WattHours
         * </ul>
         */
        READING_ENERGY_DOUBLE(Doc.of(OpenemsType.DOUBLE)),


        /**
         * Return Temp.
         *
         * <ul>
         * <li>Interface: HeatMeter
         * <li>Type: Float
         * <li>Unit: DegreeCelsius
         * </ul>
         */
        RETURN_TEMP_LONG(Doc.of(OpenemsType.LONG)),

        /**
         * Return Temp.
         *
         * <ul>
         * <li>Interface: HeatMeter
         * <li>Type: Float
         * <li>Unit: DegreeCelsius
         * </ul>
         */
        RETURN_TEMP_DOUBLE(Doc.of(OpenemsType.DOUBLE).unit(Unit.DEZIDEGREE_CELSIUS));


        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Get the ReadingPowerDouble Channel.
     * Only Call by Generic Meter!
     * The actual Reading Power will be written into {@link HeatMeter#getReadingPowerChannel()}.
     *
     * @return the Channel.
     */

    default Channel<Double> _getReadingPowerDoubleChannel() {
        return this.channel(ChannelId.READING_POWER_DOUBLE);
    }

    /**
     * Get the ReadingPowerLong Channel.
     * Only Call by Generic Meter!
     * The actual Reading Power will be written into {@link HeatMeter#getReadingPowerChannel()}.
     *
     * @return the Channel.
     */


    default Channel<Long> _getReadingPowerLongChannel() {
        return this.channel(ChannelId.READING_POWER_LONG);
    }

    /**
     * Get the ReadingEnergyDouble Channel.
     * Only Call by Generic Meter!
     * The actual Reading Power will be written into {@link HeatMeter#getReadingEnergyChannel()}.
     *
     * @return the Channel.
     */

    default Channel<Double> _getReadingEnergyDouble() {
        return this.channel(ChannelId.READING_ENERGY_DOUBLE);
    }

    /**
     * Get the ReadingEnergyLong Channel.
     * Only Call by Generic Meter!
     * The actual Reading Power will be written into {@link HeatMeter#getReadingEnergyChannel()}.
     *
     * @return the Channel.
     */

    default Channel<Long> _getReadingEnergyLong() {
        return this.channel(ChannelId.READING_ENERGY_LONG);
    }

    /**
     * Get the ReturnTempDouble Channel.
     * Only Call by Generic Meter!
     * The actual Reading Power will be written into {@link HeatMeter#getReturnTempChannel()}.
     *
     * @return the Channel.
     */

    default Channel<Double> getReturnTempDoubleChannel() {
        return this.channel(ChannelId.RETURN_TEMP_DOUBLE);
    }

    /**
     * Get the ReturnTempLong Channel.
     * Only Call by Generic Meter!
     * The actual Reading Power will be written into {@link HeatMeter#getReturnTempChannel()}.
     *
     * @return the Channel.
     */

    default Channel<Long> getReturnTempLongChannel() {
        return this.channel(ChannelId.RETURN_TEMP_LONG);
    }

}

