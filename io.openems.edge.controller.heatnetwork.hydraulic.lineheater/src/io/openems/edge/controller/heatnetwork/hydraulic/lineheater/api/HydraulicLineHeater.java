package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface HydraulicLineHeater extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * SetPoint Temperature When the Controller should Activate offset to controller.
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li> Unit: Dezidegree Celsius
         * </ul>
         */

        /**
         * Signal to remotely enable the lineheater, need to be set all the time, or it fall back in fallbackmode if activated
         * after a certain time
         * <ul>
         * <li>Interface: HydraulicLineHeaterApi
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */
        ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)), //

        /**
         * Signal if heater is in Fallback mode
         *
         * <ul>
         * <li>Interface: HydraulicLineHeaterApi
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */
        IS_FALLBACK(Doc.of(OpenemsType.BOOLEAN)), //

        /**
         * Signal if heater is running
         *
         * <ul>
         * <li>Interface: HydraulicLineHeaterApi
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */

        IS_RUNNING(Doc.of(OpenemsType.BOOLEAN)),
        /**
         * Maximum value in % the Valve is allowed to be open
         *
         * <ul>
         * <li>Interface: HydraulicLineHeaterApi
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */

        MAX_VALVE_VALUE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((DoubleWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * Minimum value in % the Valve has to be open
         *
         * <ul>
         * <li>Interface: HydraulicLineHeaterApi
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */

        MIN_VALVE_VALUE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((DoubleWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        ));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }


    default Channel<Boolean> isRunning() {
        return this.channel(ChannelId.IS_RUNNING);
    }
    default Channel<Boolean> isFallback() {
        return this.channel(ChannelId.IS_FALLBACK);
    }

    default WriteChannel<Boolean> enableSignal() {
        return this.channel(ChannelId.ENABLE_SIGNAL);
    }

    default WriteChannel<Double> maxValue() {
        return this.channel(ChannelId.MAX_VALVE_VALUE);
    }

    default WriteChannel<Double> minValue() {
        return this.channel(ChannelId.MIN_VALVE_VALUE);
    }


}
