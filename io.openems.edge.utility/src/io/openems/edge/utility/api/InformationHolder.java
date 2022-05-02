package io.openems.edge.utility.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This Nature receives an EnableSignal.
 * When the EnableSignal is set to true, the active Value will be written to the current Value.
 * When the EnableSignal is false, it stores the activeValue for a period of a configured deltaTime (depending on the {@link io.openems.edge.timer.api.Timer}).
 * After the deltaTime is up, the inactiveValue will be written to the current value.
 * Whenever the Component is Enabled/Active the "IsActive" Channel will be set to true.
 */

public interface InformationHolder extends OpenemsComponent {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * The EnableSignal. When this is true, the ActiveValue will be set.
         * <ul>
         * <li>Interface: InformationHolder
         * <li>Type: Boolean
         * </ul>
         */
        ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * The ActiveValue. This will be set when either the EnableSignal is true, or a configured deltaTime x is not up yet.
         * <ul>
         * <li>Interface: InformationHolder
         * <li>Type: String
         * </ul>
         */
        ACTIVE_VALUE(Doc.of(OpenemsType.STRING)),
        /**
         * The inactive Value. This will be set when the EnableSignal is false and a configured deltaTime x is up.
         * <ul>
         * <li>Interface: InformationHolder
         * <li>Type: String
         * </ul>
         */
        INACTIVE_VALUE(Doc.of(OpenemsType.STRING)),
        /**
         * State information. This will be set to true when either the EnableSignal is true, or a configured deltaTime x is not up yet.
         * <ul>
         * <li>Interface: InformationHolder
         * <li>Type: Boolean
         * </ul>
         */
        IS_ACTIVE(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * The CurrentValue. The current value that is hold by the component.
         * <ul>
         * <li>Interface: InformationHolder
         * <li>Type: String
         * </ul>
         */
        CURRENT_VALUE(Doc.of(OpenemsType.STRING));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }


    /**
     * Get the {@link ChannelId#ENABLE_SIGNAL} channel.
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> getEnableSignalChannel() {
        return this.channel(ChannelId.ENABLE_SIGNAL);
    }

    /**
     * Get the {@link ChannelId#ACTIVE_VALUE} channel.
     *
     * @return the channel.
     */

    default Channel<String> getActiveValueChannel() {
        return this.channel(ChannelId.ACTIVE_VALUE);
    }

    /**
     * Get the {@link ChannelId#INACTIVE_VALUE} channel.
     *
     * @return the channel.
     */
    default Channel<String> getInactiveValueChannel() {
        return this.channel(ChannelId.INACTIVE_VALUE);
    }

    /**
     * Get the {@link ChannelId#CURRENT_VALUE} channel.
     *
     * @return the channel.
     */
    default Channel<String> getCurrentValue() {
        return this.channel(ChannelId.CURRENT_VALUE);
    }

    /**
     * Get the {@link ChannelId#IS_ACTIVE} channel.
     *
     * @return the channel.
     */

    default Channel<Boolean> isActive() {
        return this.channel(ChannelId.IS_ACTIVE);
    }
}

