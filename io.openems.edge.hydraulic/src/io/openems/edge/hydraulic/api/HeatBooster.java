package io.openems.edge.hydraulic.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The Nature of a HeatBooster. If the HeatBooster Signal is enabled -> start the HeatBooster.
 * This can be used within a HeatSystem to boost e.g. a HeatPump.
 */
public interface HeatBooster extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Enable Signal.
         *
         * <ul>
         * <li>Interface: {@link HeatBooster}
         * <li>Type: Boolean
         * <li>AccessMode: ReadWrite
         * </ul>
         */

        HEAT_BOOSTER_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));

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
     * Get the HeatBoosterEnableSignalChannel.
     *
     * @return the channel
     */
    default WriteChannel<Boolean> getHeatBoosterEnableSignalChannel() {
        return this.channel(ChannelId.HEAT_BOOSTER_ENABLE_SIGNAL);
    }

    /**
     * Set the HeatBooster Enable Signal.
     *
     * @param signal true or false for the enable Signal
     * @throws OpenemsError.OpenemsNamedException if write fails.
     */
    default void setHeatBoosterEnableSignal(boolean signal) throws OpenemsError.OpenemsNamedException {
        this.getHeatBoosterEnableSignalChannel().setNextWriteValue(signal);
    }

    /**
     * Get the HeatBooster Enable Signal or else false.
     *
     * @return the value or else false.
     */
    default boolean getHeatBoosterEnableSignal() {
        return this.getHeatBoosterEnableSignalChannel().getNextWriteValue().orElse(false);
    }

    /**
     * Resets the enableSignal, usually for internal use.
     */
    default void _resetEnableSignal() {
        this.getHeatBoosterEnableSignalChannel().getNextWriteValueAndReset();
    }

    /**
     * Check if the EnableSignal is Present.
     * @return if the getNextWriteValue is Present.
     */
    default boolean getHeatBoosterEnableSignalPresent() {
        return this.getHeatBoosterEnableSignalChannel().getNextWriteValue().isPresent();
    }

}
