package io.openems.edge.controller.hydrauliccomponent.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.*;
import io.openems.edge.common.component.OpenemsComponent;

public interface PidHydraulicController extends HydraulicController {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {




        /**
         * Min Temperature.
         * <ul>
         * <li> Min Temperature that has to be reached
         * <li>Type: Integer
         * <li>Unit: Decimal degrees Celsius
         * </ul>
         */

        MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE).onInit(channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                }
        ));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }
    /**
     * Min Temperature you want to reach / check if it can be reached.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> setMinTemperature() {
        return this.channel(ChannelId.MIN_TEMPERATURE);
    }


}