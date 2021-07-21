package io.openems.edge.meter.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This Natures is an "Expansion" of the existing {@link Meter}.
 * This Nature is needed by Generic Modbus Meter, that may have Channels, that have different OpenemsTypes
 * and other Units.
 */
public interface MeterModbusGeneric extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * The last timestamp.
         *
         * <ul>
         * <li>Interface: MeterModbusGeneric
         * <li>Type: Double
         * <li>
         * </ul>
         */
        TIMESTAMP_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        /**
         * The last timestamp.
         *
         * <ul>
         * <li>Interface: MeterModbusGeneric
         * <li>Type: Double
         * <li>
         * </ul>
         */
        TIMESTAMP_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }


    }

    /**
     * Gets the Double TimeStamp Channel.
     *
     * @return the Channel.
     */
    default Channel<Double> getTimeStampDoubleChannel() {
        return this.channel(ChannelId.TIMESTAMP_DOUBLE);
    }

    /**
     * Gets the Long TimeStamp Channel.
     *
     * @return the Channel.
     */
    default Channel<Long> getTimeStampLongChannel() {
        return this.channel(ChannelId.TIMESTAMP_LONG);
    }

}

