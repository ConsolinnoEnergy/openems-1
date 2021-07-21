package io.openems.edge.meter.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface ModbusMeterGeneric extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Power.
         *
         * <ul>
         * <li>Interface: HeatMeterMbus
         * <li>Type: Integer
         * <li>Unit: Kilowatt
         * </ul>
         */
        POWER_FLOAT(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT)),

        /**
         * Total Consumed Energy.
         *
         * <ul>
         * <li>Interface: HeatMeter
         * <li>Type: Integer
         * <li>Unit: WattHours
         * </ul>
         */
        TOTAL_CONSUMED_ENERGY_FLOAT(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }


    }

    /**
     * Gets the POWER Channel of this Meter.
     *
     * @return the Channel
     */
    default Channel<Float> getPowerFloatChannel() {
        return this.channel(ChannelId.POWER_FLOAT);
    }


    /**
     * Gets the Total Consumed Energy Channel of this Meter.
     *
     * @return the Channel.
     */
    default Channel<Float> getTotalConsumedEnergyFloatChannel() {
        return this.channel(ChannelId.TOTAL_CONSUMED_ENERGY_FLOAT);
    }


}

