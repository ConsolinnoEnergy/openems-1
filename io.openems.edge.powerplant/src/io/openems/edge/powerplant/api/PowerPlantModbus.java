package io.openems.edge.powerplant.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.generator.api.GeneratorModbus;

public interface PowerPlantModbus extends GeneratorModbus {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * These Channel will be set by a PowerPlant itself. Do not set those in any other way.
         * Use the other channel instead.
         */
        SET_EXTERNAL_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        SET_EXTERNAL_POWER_LEVEL_PERCENT(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_WRITE)),

        /**
         * PowerLevel.
         *
         * <ul>
         * <li>Interface: PassingChannel
         * <li>Type: Double
         * <li> Unit: Percentage
         * </ul>
         */

        POWER_LEVEL_PERCENT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT).onInit(
                channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                }
        )),

        /**
         * PowerLevelKW.
         *
         * <ul>
         * <li>
         * <li>Type: Double
         * <li>Unit: Kw
         * </ul>
         */

        POWER_LEVEL_KW(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.KILOWATT).onInit(
                ch -> {
                    ((IntegerWriteChannel) ch).onSetNextWrite(ch::setNextValue);
                })),

        MAXIMUM_KW(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.KILOWATT)),

        ERROR_OCCURED(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }
}
