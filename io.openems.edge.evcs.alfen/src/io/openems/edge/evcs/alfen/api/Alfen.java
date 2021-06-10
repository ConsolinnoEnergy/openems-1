package io.openems.edge.evcs.alfen.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Alfen extends OpenemsComponent {

    /**
     * <ul>
     * <li>Interface:
     * <li>Type:
     * <li>Unit:
     * </ul>
     */
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        NAME(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        MANUFACTURER(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        MODBUS_TABLE_VERSION(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        FIRMWARE_VERSION(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        PLATFORM_TYPE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        STATION_SERIAL_NUMBER(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        DATE_YEAR(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        DATE_MONTH(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        DATE_DAY(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        TIME_HOUR(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        TIME_MINUTE(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        TIME_SECOND(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        UPTIME(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        TIME_ZONE(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        STATION_ACTIVE_MAX_CURRENT(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        TEMPERATURE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        OCPP_STATE(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        NR_OF_SOCKETS(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        SCN_NAME(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        SCN_SOCKETS(Doc.of(OpenemsType.SHORT).accessMode(AccessMode.READ_ONLY)),
        SCN_TOTAL_CONSUMPTION_PHASE_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        SCN_TOTAL_CONSUMPTION_PHASE_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        SCN_TOTAL_CONSUMPTION_PHASE_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        SCN_ACN_CONSUMPTION_PHASE_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),


        ;
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    default Channel<Float> getScnTotalConsumptionPhaseL1Channel() {
        return this.channel(ChannelId.SCN_TOTAL_CONSUMPTION_PHASE_L1);
    }

    default Channel<String> getNameChannel() {
        return this.channel(ChannelId.NAME);
    }


    default float getScnTotalConsumptionPhaseL1() {
        Channel<Float> channel = this.getScnTotalConsumptionPhaseL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }
}

