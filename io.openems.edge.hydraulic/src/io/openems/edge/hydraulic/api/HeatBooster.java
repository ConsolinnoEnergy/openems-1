package io.openems.edge.hydraulic.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface HeatBooster extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        HEAT_BOOSTER_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    default WriteChannel<Boolean> getHeatBoosterEnableSignalChannel() {
        return this.channel(ChannelId.HEAT_BOOSTER_ENABLE_SIGNAL);
    }

    default void setHeatBoosterEnableSignal(boolean signal) throws OpenemsError.OpenemsNamedException {
        this.getHeatBoosterEnableSignalChannel().setNextWriteValue(signal);
    }

    default boolean getHeatBoosterEnableSignal() {
        return this.getHeatBoosterEnableSignalChannel().getNextWriteValue().orElse(false);
    }

    default void _resetEnableSignal() {
        this.getHeatBoosterEnableSignalChannel().getNextWriteValueAndReset();
    }

    default boolean getHeatBoosterEnableSignalPresent() {
        return this.getHeatBoosterEnableSignalChannel().getNextWriteValue().isPresent();
    }

}
