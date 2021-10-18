package io.openems.edge.io.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a Aio (Analog-Input-Output) Module.
 *
 * <p>
 * The Content of the Channels is dependent on the Configuration
 * <ul>
 * <li>AIO_READ contains the Digital Output
 * <li>AIO_PERCENT contains the Percent Value of the Configured Value
 * <li>AIO_CHECK_WRITE contains the Value that is written as a debug read
 * </ul>
 */

public interface AnalogInputOutput extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Status of Aio, depends on Configuration.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * </ul>
         */
        AIO_READ(Doc.of(OpenemsType.INTEGER)),
        /**
         * Set Status of Aio in percent.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * <li>Unit: %
         * <li>Range: 0..100
         * </ul>
         */
        AIO_PERCENT_WRITE(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH)),
        /**
         * Status of Aio in percent.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * <li>Unit: %
         * <li>Range: 0..100
         * </ul>
         */
        AIO_CHECK_PERCENT(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH)),
        /**
         * Set Value that is being written to Aio.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * </ul>
         */
        AIO_WRITE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Value that is being written to Aio.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * </ul>
         */
        AIO_CHECK_WRITE(Doc.of(OpenemsType.INTEGER));
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
     * Get the Channel for {@link ChannelId#AIO_READ}.
     *
     * @return the channel.
     */
    default Channel<Integer> getReadChannel() {
        return this.channel(ChannelId.AIO_READ);
    }

    /**
     * Get the Value of the {@link ChannelId#AIO_READ} or else -1.
     *
     * @return the Value.
     */
    default int getReadValue() {
        if (this.getReadChannel().value().isDefined()) {
            return this.getReadChannel().value().get();
        } else if (this.getReadChannel().getNextValue().isDefined()) {
            return this.getReadChannel().getNextValue().get();
        }
        return -1;
    }

    /**
     * Get the Channel for {@link ChannelId#AIO_CHECK_PERCENT}.
     *
     * @return the channel.
     */
    default Channel<Integer> getPercentChannel() {
        return this.channel(ChannelId.AIO_CHECK_PERCENT);
    }

    /**
     * Get the Value of the {@link ChannelId#AIO_CHECK_PERCENT} in Percent or else -1.
     *
     * @return the Value in Percent.
     */
    default float getPercentValue() {
        if (this.getPercentChannel().value().isDefined()) {
            return this.getPercentChannel().value().get().floatValue() / 10;
        } else if (this.getPercentChannel().getNextValue().isDefined()) {
            return this.getPercentChannel().getNextValue().get().floatValue() / 10;
        }
        return -1;
    }

    /**
     * Get the Channel for {@link ChannelId#AIO_WRITE}.
     *
     * @return the channel.
     */
    default WriteChannel<Integer> getWriteChannel() {
        return this.channel(ChannelId.AIO_WRITE);
    }

    /**
     * Get the Channel for {@link ChannelId#AIO_CHECK_WRITE}.
     *
     * @return the channel.
     */
    default Channel<Integer> getCheckWriteChannel() {
        return this.channel(ChannelId.AIO_CHECK_WRITE);
    }

    /**
     * Get the Value of the {@link ChannelId#AIO_CHECK_WRITE} or else -1.
     *
     * @return the Value in Percent.
     */
    default int getWriteValue() {
        if (this.getCheckWriteChannel().value().isDefined()) {
            return this.getCheckWriteChannel().value().get();
        } else if (this.getCheckWriteChannel().getNextValue().isDefined()) {
            return this.getCheckWriteChannel().getNextValue().get();
        }
        return -1;
    }

    /**
     * Sets the Value of the Write Channel.
     *
     * @param value the value that has to be set
     */
    default void setWrite(int value) throws OpenemsError.OpenemsNamedException {
        this.getWriteChannel().setNextWriteValue(value);
    }

    /**
     * Returns Channel for the Output Percent Register.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> setPercentChannel() {
        return this.channel(ChannelId.AIO_PERCENT_WRITE);
    }

    /**
     * Returns the Value of the Percent Output Channel.
     *
     * @return the Value
     */
    default int getSetPercentValue() {
        if (this.setPercentChannel().value().isDefined()) {
            return this.setPercentChannel().value().get();
        } else if (this.setPercentChannel().getNextValue().isDefined()) {
            return this.setPercentChannel().getNextValue().get();
        }
        return -1;
    }

    /**
     * Sets the Value of the Percent Channel.
     *
     * @param percent the value that has to be set
     */
    default void setPercent(int percent) throws OpenemsError.OpenemsNamedException {
        this.setPercentChannel().setNextWriteValue(percent);
    }


}
