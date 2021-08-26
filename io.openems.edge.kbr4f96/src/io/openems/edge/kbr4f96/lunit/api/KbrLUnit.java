package io.openems.edge.kbr4f96.lunit.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface KbrLUnit extends OpenemsComponent {
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        VOLTAGE_PH_N(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
        VOLTAGE_PH_PH(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)),
        RAW_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
        AVERAGE_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)),
        APPARENT_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT_AMPERE)),
        ACTIVE_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)),
        IDLE_POWER(Doc.of(OpenemsType.FLOAT)),
        COS_PHI(Doc.of(OpenemsType.FLOAT)),
        POWER_FACTOR(Doc.of(OpenemsType.FLOAT)),
        SPGS_THD(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
        PHASE_ANGLE_U(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE)),

        ;
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
     * Returns the Active Power Channel.
     *
     * @return the Channel
     */
    default Channel<Float> getActivePowerChannel() {
        return this.channel(ChannelId.ACTIVE_POWER);
    }

    /**
     * Returns the Active Power value in W.
     *
     * @return the Active Power as float
     */
    default float getActivePower() {
        return getActivePowerChannel().value().orElse(0.f);
    }

}
