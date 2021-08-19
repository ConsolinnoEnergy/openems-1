package io.openems.edge.powerplant.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface CombinedHeatPowerPlantModbus extends PowerPlantModbus {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        WMZ_ENERGY_AMOUNT_LONG(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        WMZ_ENERGY_AMOUNT_DOUBLE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),

        WMZ_TEMP_SOURCE_LONG(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SOURCE_DOUBLE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        WMZ_TEMP_SINK_LONG(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SINK_DOUBLE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        WMZ_POWER_LONG(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),
        WMZ_POWER_DOUBLE(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),

        WMZ_GAS_METER_LONG(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        WMZ_GAS_METER_DOUBLE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),

        CURRENT_POWER_LONG(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        CURRENT_POWER_DOUBLE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),

        TARGET_POWER_LONG(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        TARGET_POWER_DOUBLE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),

        HOURS_AFTER_LAST_SERVICE_LONG(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        HOURS_AFTER_LAST_SERVICE_DOUBLE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),

        SECURITY_OFF_EXTERN_LONG(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        SECURITY_OFF_EXTERN_DOUBLE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),

        REQUIRED_ON_EVU(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),

        REQUIRED_ON_EXTERN(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),

        SECURITY_OFF_EVU(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),

        SECURITY_OFF_GRID_FAIL(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),

        ELECTRICITY_ENERGY_PRODUCED(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),

        ELECTRICITY_POWER(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

}
