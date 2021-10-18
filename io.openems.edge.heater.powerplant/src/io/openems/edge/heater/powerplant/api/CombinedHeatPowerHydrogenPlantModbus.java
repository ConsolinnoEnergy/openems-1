package io.openems.edge.heater.powerplant.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface CombinedHeatPowerHydrogenPlantModbus extends CombinedHeatPowerPlantModbus {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        ENABLE_HYDROGEN_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        HYDROGEN_POWER(Doc.of(OpenemsType.LONG).unit(Unit.KILOWATT)),
        SET_POINT_HYDROGEN_POWER_KW(Doc.of(OpenemsType.LONG).unit(Unit.KILOWATT)),
        SET_POINT_HYDROGEN_PERCENT(Doc.of(OpenemsType.LONG).unit(Unit.KILOWATT));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }
}
