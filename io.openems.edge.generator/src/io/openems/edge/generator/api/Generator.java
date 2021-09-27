package io.openems.edge.generator.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Generator extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * Generator Power.
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        READ_POWER_GENERATOR(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY).unit(Unit.KILOWATT)),
        READ_POWER_PERCENT_GENERATOR(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT)),
        DEFAULT_RUN_POWER(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY).unit(Unit.KILOWATT).onInit(
                channel -> ((DoubleWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        SET_POINT_POWER_GENERATOR(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY).unit(Unit.KILOWATT)),
        SET_POINT_POWER_PERCENT_GENERATOR(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#READ_POWER_GENERATOR}.
     *
     * @return the Channel
     */
    default Channel<Double> getPowerChannelGenerator() {
        return this.channel(ChannelId.READ_POWER_GENERATOR);
    }
    /**
     * Gets the Channel for {@link ChannelId#READ_POWER_GENERATOR}.
     *
     * @return the Channel
     */
    default Channel<Integer> getPowerPercentChannelGenerator() {
        return this.channel(ChannelId.READ_POWER_PERCENT_GENERATOR);
    }

    /**
     * Gets the Channel for {@link ChannelId#SET_POINT_POWER_GENERATOR}.
     *
     * @return the Channel
     */
    default WriteChannel<Double> getSetPointPowerChannelGenerator() {
        return this.channel(ChannelId.SET_POINT_POWER_GENERATOR);
    }
    /**
     * Gets the Channel for {@link ChannelId#SET_POINT_POWER_PERCENT_GENERATOR}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getSetPointPowerPercentChannelGenerator() {
        return this.channel(ChannelId.SET_POINT_POWER_PERCENT_GENERATOR);
    }

    /**
     * Gets the Channel for {@link ChannelId#DEFAULT_RUN_POWER}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getDefaultPower() {
        return this.channel(ChannelId.DEFAULT_RUN_POWER);
    }


}