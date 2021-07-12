package io.openems.edge.heatsystem.components.valve.old.test;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface TestDeviceValve extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        WRITE_ON_OFF(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        READ_ON_OFF(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
        WRITE_ON_OFF_TWO(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        READ_ON_OFF_TWO(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
        POWER_WRITE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.THOUSANDTH)),
        POWER_READ(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    default WriteChannel<Boolean> getRelaysWriteChannel() {
        return this.channel(ChannelId.WRITE_ON_OFF);
    }

    default Channel<Boolean> getRelaysReadChannel() {
        return this.channel(ChannelId.READ_ON_OFF);
    }

    default WriteChannel<Boolean> getRelaysWriteChannel2() {
        return this.channel(ChannelId.WRITE_ON_OFF_TWO);
    }

    default Channel<Boolean> getRelaysReadChannel2() {
        return this.channel(ChannelId.READ_ON_OFF_TWO);
    }

    default WriteChannel<Integer> getPowerWriteChannel() {
        return this.channel(ChannelId.POWER_WRITE);
    }

    default Channel<Integer> getPowerReadChannel() {
        return this.channel(ChannelId.POWER_READ);
    }
}