package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Nature for a HydraulicLineHeater.
 * Other Components or Controller can set up an EnableSignal, the MinValue and the MaxValue.
 */
public interface HydraulicLineController extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Signal to remotely enable the LineHeater. Need to be set all the time, or it fall back in FallbackMode if activated
         * after a certain time.
         * <ul>
         * <li>Interface: HydraulicLineHeater
         * <li>Type: Boolean
         * </ul>
         */
        ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Signal if heater is in Fallback mode.
         *
         * <ul>
         * <li>Interface: HydraulicLineHeaterApi
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */
        IS_FALLBACK(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Maximum value in % the Valve is allowed to be open.
         *
         * <ul>
         * <li>Interface: HydraulicLineHeaterApi
         * <li>Type: Double
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
         * <li>Interface: HydraulicLineHeaterApi
         * <li>Type: Double
         * <li> Unit: none
         * </ul>
         */

        MIN_VALVE_VALUE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((DoubleWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),

        IS_ACTIVE(Doc.of(OpenemsType.BOOLEAN));
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
     * Return the {@link ChannelId#IS_FALLBACK} channel.
     *
     * @return the channel.
     */
    default Channel<Boolean> isFallbackChannel() {
        return this.channel(ChannelId.IS_FALLBACK);
    }

    /**
     * Return the {@link ChannelId#ENABLE_SIGNAL} channel.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> enableSignalChannel() {
        return this.channel(ChannelId.ENABLE_SIGNAL);
    }

    /**
     * Return the {@link ChannelId#MAX_VALVE_VALUE} channel.
     *
     * @return the channel.
     */
    default WriteChannel<Double> maxValueChannel() {
        return this.channel(ChannelId.MAX_VALVE_VALUE);
    }

    /**
     * Return the {@link ChannelId#MIN_VALVE_VALUE} channel.
     *
     * @return the channel.
     */
    default WriteChannel<Double> minValueChannel() {
        return this.channel(ChannelId.MIN_VALVE_VALUE);
    }

    /**
     * Return the {@link ChannelId#IS_ACTIVE} channel.
     *
     * @return the channel.
     */
    default Channel<Boolean> isActiveChannel() {
        return this.channel(ChannelId.IS_ACTIVE);
    }



}
