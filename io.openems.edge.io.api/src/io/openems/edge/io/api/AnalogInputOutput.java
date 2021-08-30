package io.openems.edge.io.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
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
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

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
         * Status of Aio in percent.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * <li>Unit: %
         * <li>Range: 0..100
         * </ul>
         */
        AIO_PERCENT_WRITE(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH)),
        AIO_CHECK_PERCENT(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH)),
        /**
         * Value that is being written to Aio.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * </ul>
         */
        AIO_WRITE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        AIO_CHECK_WRITE(Doc.of(OpenemsType.INTEGER));
        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }

    }

    default Channel<Integer> getReadChannel() {
        return this.channel(ChannelId.AIO_READ);
    }

    default int getReadValue() {
        if (this.getReadChannel().value().isDefined()) {
            return this.getReadChannel().value().get();
        } else if (this.getReadChannel().getNextValue().isDefined()) {
            return this.getReadChannel().getNextValue().get();
        }
        return -1;
    }

    default WriteChannel<Integer> setPercentChannel() {
        return this.channel(ChannelId.AIO_PERCENT_WRITE);
    }

    default Channel<Integer> getPercentChannel() {
        return this.channel(ChannelId.AIO_CHECK_PERCENT);
    }

    default float getPercentValue() {
        if (this.getPercentChannel().value().isDefined()) {
            return this.getPercentChannel().value().get().floatValue() / 10;
        } else if (this.getPercentChannel().getNextValue().isDefined()) {
            return this.getPercentChannel().getNextValue().get().floatValue() / 10;
        }
        return -1;
    }

    default WriteChannel<Integer> getWriteChannel() {
        return this.channel(ChannelId.AIO_WRITE);
    }

    default Channel<Integer> getCheckWriteChannel() {
        return this.channel(ChannelId.AIO_CHECK_WRITE);
    }

    default int getWriteValue() {
        if (this.getCheckWriteChannel().value().isDefined()) {
            return this.getCheckWriteChannel().value().get();
        } else if (this.getCheckWriteChannel().getNextValue().isDefined()) {
            return this.getCheckWriteChannel().getNextValue().get();
        }
        return -1;
    }

}
