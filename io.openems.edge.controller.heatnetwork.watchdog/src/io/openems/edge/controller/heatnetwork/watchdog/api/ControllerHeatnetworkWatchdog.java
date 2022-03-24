package io.openems.edge.controller.heatnetwork.watchdog.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The Nature of this Component. It stores the {@link ErrorType} and if the Watchdog is active.
 */
public interface ControllerHeatnetworkWatchdog extends OpenemsComponent {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Which {@link ErrorType} is set.
         * <ul>
         * <li>Interface: ControllerHeatnetworkWatchdog
         * <li>Type: ErrorTypes
         * </ul>
         */
        ERROR_TYPE(Doc.of(ErrorType.values()).accessMode(AccessMode.READ_ONLY)),
        /**
         * Is the error active.
         * <ul>
         * <li>Interface: ControllerHeatnetworkWatchdog
         * <li>Type: Boolean
         * </ul>
         */
        ERROR_ACTIVE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Get the {@link ChannelId#ERROR_TYPE} channel.
     * @return the channel
     */
    default EnumReadChannel getErrorTypeChannel() {
        return this.channel(ChannelId.ERROR_TYPE);
    }

    /**
     * Get the {@link ChannelId#ERROR_ACTIVE} channel.
     * @return the channel
     */
    default Channel<Boolean> getErrorActiveChannel() {
        return this.channel(ChannelId.ERROR_ACTIVE);
    }

    /**
     * Internal method to set the {@link ErrorType}.
     * @param errorType the {@link ErrorType}
     */
    default void _setErrorType(ErrorType errorType) {
        if (errorType != null) {
            this.getErrorTypeChannel().setNextValue(errorType.getValue());
        }
    }

    /**
     * Internal method to set the {@link ErrorType}.
     * @param errorType the {@link ErrorType#getValue()}
     */
    default void _setErrorType(int errorType) {
            this.getErrorTypeChannel().setNextValue(errorType);
    }

    /**
     * Internal method to set if the error is active.
     * @param active is the error active.
     */
    default void _setErrorActive(boolean active) {
        this.getErrorActiveChannel().setNextValue(active);
    }

}

