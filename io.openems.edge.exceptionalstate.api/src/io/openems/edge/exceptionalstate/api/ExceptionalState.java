package io.openems.edge.exceptionalstate.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The ExceptionalState Nature. The ExceptionalState, when active, overrides any other commands to the device.
 * There are two channels. The first one is ExceptionalStateEnableSignal, which decides if the ExceptionalState is
 * active or not. The second channel is ExceptionalStateValue, which sets the behaviour of the device when the
 * ExceptionalState is active.
 * The basic interpretation of the ExceptionalStateValue is that 0 <= off, and any value > 0 means on.
 * If a device has the ability to regulate output power, the advanced implementation is that the ExceptionalStateValue
 * is interpreted as a power percent setting. So a value of 50 would mean 'turn the device on with 50% power setting'
 * and a value >= 100 is 'turn on with full power'.
 * If a device can not not do the full range of 1-100% (a CHP for example can only go as low as 50% power), a value
 * lower than that minimum but >0 will mean 'turn on with power as low as possible'. Only a value <= 0 will turn the
 * device off.
 * Components implementing the ExceptionalState should use the ExceptionalStateHandlerImpl to process the
 * ExceptionalStateEnableSignal (see {@link io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl}).
 */
public interface ExceptionalState extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Write: activate (true) or deactivate (false) the ExceptionalState.
         * Read: The ExceptionalState is active (true) or not (false).
         * Components implementing the ExceptionalState should use the ExceptionalStateHandlerImpl to process the
         * ExceptionalStateEnableSignal (see {@link io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl}).
         * This way the handling of the ExceptionalStateEnableSignal is the same in all devices.
         * The ExceptionalStateHandlerImpl fetches the nextWriteValue of this channel with getNextWriteValueAndReset().
         * If the collected value is ’true’, the ExceptionalState is activated and a configurable timer is started. As
         * long as the timer has not finished counting down, the ExceptionalState stays active. When the timer runs out,
         * the ExceptionalStateHandlerImpl stops the ExceptionalState.
         * To keep the ExceptionalState active, ’true’ must be regularly written in the nextWriteValue of this channel.
         *
         * <ul>
         * <li>Interface: {@link ExceptionalState}
         * <li>Type: Boolean
         * </ul>
         */
        EXCEPTIONAL_STATE_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * The ExceptionalStateValue controls the behaviour of the device when the ExceptionalState is active.
         * The basic interpretation of the ExceptionalStateValue is that 0 <= off, and any value > 0 means on.
         * If a device has the ability to regulate output power, the advanced implementation is that the
         * ExceptionalStateValue is interpreted as a power percent setting. So a value of 50 would mean 'turn the
         * device on with 50% power setting' and a value >= 100 is 'turn on with full power'.
         * If a device can not not do the full range of 1-100% (a CHP for example can only go as low as 50% power),
         * a value lower than that minimum but >0 will mean 'turn on with power as low as possible'. Only a
         * value <= 0 will turn the device off.
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
     * Gets the Channel for {@link ChannelId#EXCEPTIONAL_STATE_ENABLE_SIGNAL}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getExceptionalStateEnableSignalChannel() {
        return this.channel((ChannelId.EXCEPTIONAL_STATE_ENABLE_SIGNAL));
    }

    /**
     * Gets the ExceptionalStateEnableSignal, indicating if the ExceptionalState is active (true) or not (false).
     * See {@link ChannelId#EXCEPTIONAL_STATE_ENABLE_SIGNAL}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getExceptionalStateEnableSignal() {
        return this.getExceptionalStateEnableSignalChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#EXCEPTIONAL_STATE_ENABLE_SIGNAL}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setExceptionalStateEnableSignal(Boolean value) {
        this.getExceptionalStateEnableSignalChannel().setNextValue(value);
    }

    /**
     * Activate the ExceptionalState (regularly write true) or deactivate it (write false).
     * When ’true’ is written, the ExceptionalState is activated and a configurable timer is started. Writing ’true’
     * again will reset the timer. When no further command is received, the ExceptionalState will deactivate when the
     * timer runs out (signal loss fallback).
     * See {@link ChannelId#EXCEPTIONAL_STATE_ENABLE_SIGNAL}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setExceptionalStateEnableSignal(Boolean value) throws OpenemsNamedException {
        this.getExceptionalStateEnableSignalChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#EXCEPTIONAL_STATE_VALUE}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getExceptionalStateValueChannel() {
        return this.channel(ChannelId.EXCEPTIONAL_STATE_VALUE);
    }

    /**
     * Get the ExceptionalStateValue or if nothing is defined -> -1.
     * See {@link ChannelId#EXCEPTIONAL_STATE_VALUE}.
     *
     * @return the Channel {@link Value} or -1 if nothing is defined.
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
     * Sets the ExceptionalStateValue.
     * The ExceptionalStateValue controls the behaviour of the device when the ExceptionalState is active.
     * The basic interpretation of the ExceptionalStateValue is that 0 <= off, and any value > 0 means on.
     * If a device has the ability to regulate output power, the advanced implementation is that the
     * ExceptionalStateValue is interpreted as a power percent setting. So a value of 50 would mean 'turn the
     * device on with 50% power setting' and a value >= 100 is 'turn on with full power'.
     * If a device can not not do the full range of 1-100% (a CHP for example can only go as low as 50% power),
     * a value lower than that minimum but >0 will mean 'turn on with power as low as possible'. Only a
     * value <= 0 will turn the device off.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setExceptionalStateValue(int value) throws OpenemsNamedException {
        this.getExceptionalStateValueChannel().setNextWriteValue(value);
    }

    /**
     * Sets the ExceptionalStateValue.
     * The ExceptionalStateValue controls the behaviour of the device when the ExceptionalState is active.
     * The basic interpretation of the ExceptionalStateValue is that 0 <= off, and any value > 0 means on.
     * If a device has the ability to regulate output power, the advanced implementation is that the
     * ExceptionalStateValue is interpreted as a power percent setting. So a value of 50 would mean 'turn the
     * device on with 50% power setting' and a value >= 100 is 'turn on with full power'.
     * If a device can not not do the full range of 1-100% (a CHP for example can only go as low as 50% power),
     * a value lower than that minimum but >0 will mean 'turn on with power as low as possible'. Only a
     * value <= 0 will turn the device off.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setExceptionalStateValue(Integer value) throws OpenemsNamedException {
        this.getExceptionalStateValueChannel().setNextWriteValue(value);
    }
}
