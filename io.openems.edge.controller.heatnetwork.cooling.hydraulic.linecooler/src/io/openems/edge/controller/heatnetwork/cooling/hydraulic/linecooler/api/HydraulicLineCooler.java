package io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.thermometer.api.Thermometer;

/**
 * The HydraulicLineCooler. It cools a System by checking a {@link Thermometer} and it's configured SetPoint and opening/closing a Valve after that.
 * It also can be used as a LineReducer by Setting up a MinMax Value to a {@link io.openems.edge.heatsystem.components.Valve}.
 * The HydraulicLineCooler reacts to an EnableSignal by getting the nextWriteValue and resetting it.
 */
public interface HydraulicLineCooler extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Signal to remotely enable the line-cooler, need to be set all the time, or it fall back in Fallback-mode if activated.
         * after a certain time
         * <ul>
         * <li>Interface: HydraulicLineCoolerApi
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */
        ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)), //

        /**
         * Signal if cooler is in Fallback mode.
         *
         * <ul>
         * <li>Interface: HydraulicLineCoolerApi
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */
        IS_FALLBACK(Doc.of(OpenemsType.BOOLEAN)), //

        /**
         * Signal if cooler is running.
         *
         * <ul>
         * <li>Interface: HydraulicLineCoolerApi
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */

        IS_RUNNING(Doc.of(OpenemsType.BOOLEAN)),
        /**
         * Maximum value in % the Valve is allowed to be open.
         *
         * <ul>
         * <li>Interface: HydraulicLineCoolerApi
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */

        MAX_VALVE_VALUE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((DoubleWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * Minimum value in % the Valve has to be open.
         *
         * <ul>
         * <li>Interface: HydraulicLineCoolerApi
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

    /**
     * Get the isRunning Channel.
     *
     * @return the channel.
     */
    default Channel<Boolean> isRunning() {
        return this.channel(ChannelId.IS_RUNNING);
    }

    /**
     * Get the IsFallback Channel.
     *
     * @return the channel.
     */
    default Channel<Boolean> isFallback() {
        return this.channel(ChannelId.IS_FALLBACK);
    }

    /**
     * Get The EnableSignalChannel.
     *
     * @return the channel
     */
    default WriteChannel<Boolean> enableSignal() {
        return this.channel(ChannelId.ENABLE_SIGNAL);
    }

    /**
     * Get the maxValue Channel.
     *
     * @return the channel
     */
    default WriteChannel<Double> maxValue() {
        return this.channel(ChannelId.MAX_VALVE_VALUE);
    }

    /**
     * Get the minValue Channel.
     *
     * @return the channel.
     */
    default WriteChannel<Double> minValue() {
        return this.channel(ChannelId.MIN_VALVE_VALUE);
    }


}
