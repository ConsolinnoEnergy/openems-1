package io.openems.edge.exceptionalstate.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The ExceptionalState Nature. It Provides 2 Basic Channel. The First one is the EnableSignal.
 * That notifies the component if the enable signal is set and the Value Channel, which tells the Component what to do.
 * Example: If a Heater receives the Enable signal and the Value is > 0 -> it activates/starts to heat, otherwise it won't heat.
 * Ignoring all the other controller/enableSignal of the heater interface etc etc.
 */
public interface ExceptionalState extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Exceptional State Enable Signal.
         * This needs to by set periodically to work.
         *
         * <ul>
         * <li>Interface: {@link ExceptionalState}
         * <li>Type: Boolean
         * </ul>
         */
        EXCEPTIONAL_STATE_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        /**
         * Exceptional State Value.
         * The Value that will be applied/used/should be used when an exceptionalState is active.
         *
         * <ul>
         *     <li> Interface: {@link ExceptionalState}
         *     <li> Type: Integer
         * </ul>
         */
        EXCEPTIONAL_STATE_VALUE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).onInit(channel ->
                ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue)));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Get The Enable Signal channel.
     *
     * @return the channel
     */
    default WriteChannel<Boolean> getExceptionalStateEnableChannel() {
        return this.channel(ChannelId.EXCEPTIONAL_STATE_ENABLE_SIGNAL);
    }

    /**
     * The Value Channel.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getExceptionalStateValueChannel() {
        return this.channel(ChannelId.EXCEPTIONAL_STATE_VALUE);
    }

    /**
     * Get the EnableSignal nextWriteValue or else false.
     *
     * @return a boolean.
     */
    default boolean getExceptionalStateEnableSignal() {
        return this.getExceptionalStateEnableChannel().getNextWriteValue().orElse(false);
    }

    /**
     * Get the EnableSignal NextWriteValue and Resets it. If not present set to false.
     *
     * @return a boolean or Else false.
     */
    default boolean getExceptionalStateEnableSignalAndReset() {
        return this.getExceptionalStateEnableChannel().getNextWriteValueAndReset().orElse(false);
    }

    /**
     * Get the Value of the Exceptional State or if nothing is defined -> -1.
     *
     * @return the value or -1 if nothings present.
     */
    default int getExceptionalStateValue() {
        int value = -1;
        WriteChannel<Integer> channel = this.getExceptionalStateValueChannel();
        if (channel.value().isDefined()) {
            value = channel.value().get();
        } else if (channel.getNextValue().isDefined()) {
            value = channel.getNextValue().get();
        } else if (channel.getNextWriteValue().isPresent()) {
            value = channel.getNextWriteValue().get();
        }
        return value;
    }

    /**
     * Sets the EnableSignal.
     *
     * @param value true or false to enable/disable the exceptional state, can also be null
     * @throws OpenemsError.OpenemsNamedException if write fails.
     */
    default void setExceptionalStateEnableSignal(boolean value) throws OpenemsError.OpenemsNamedException {
        this.getExceptionalStateEnableChannel().setNextWriteValueFromObject(value);
    }

    /**
     * Sets the Exceptional State Value.
     *
     * @param value the value that will be used if exceptional state is active.
     * @throws OpenemsError.OpenemsNamedException if write fails
     */
    default void setExceptionalStateValue(int value) throws OpenemsError.OpenemsNamedException {
        this.getExceptionalStateValueChannel().setNextWriteValueFromObject(value);
    }
}
