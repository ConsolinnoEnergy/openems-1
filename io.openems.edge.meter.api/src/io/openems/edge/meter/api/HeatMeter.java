package io.openems.edge.meter.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;

/**
 * A HeatMeter, an expansion of the Meter interface.
 */
public interface HeatMeter extends Meter {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {



        /**
         * Return Temp.
         *
         * <ul>
         * <li>Interface: HeatMeter
         * <li>Type: Float
         * <li>Unit: DegreeCelsius
         * </ul>
         */
        RETURN_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }


    }

    /**
     * Get the Return Temp Channel of this HeatMeter.
     *
     * @return the Channel
     */
    default Channel<Integer> getReturnTempChannel() {
        return this.channel(ChannelId.RETURN_TEMP);
    }


}
