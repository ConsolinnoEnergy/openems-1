package io.openems.edge.consolinno.evcs.limiter;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface powerLimitChannel extends OpenemsComponent {

    /**
     * <ul>
     * <li>Interface:
     * <li>Type:
     * <li>Unit:
     * </ul>
     */
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        POWER_LIMIT(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    default Channel<Integer> getPowerLimitChannel() {
        return this.channel(ChannelId.POWER_LIMIT);
    }

    default int getPowerLimitValue() {
        if (this.getPowerLimitChannel().value().isDefined()) {
            return this.getPowerLimitChannel().value().get();
        } else if (this.getPowerLimitChannel().getNextValue().isDefined()) {
            return this.getPowerLimitChannel().getNextValue().orElse(0);
        } else {
            return -1;
        }
    }
}

