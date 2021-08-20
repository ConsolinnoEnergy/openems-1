package io.openems.edge.powerplant.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;

public interface CombinedHeatPowerPlant extends PowerPlant {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * CHP get working temperature
         * <ul>
         *      <li> Type: Float
         * </ul>
         */

        WMZ_ENERGY_AMOUNT(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY).unit(Unit.KILOWATT_HOURS)),
        WMZ_TEMP_SOURCE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SINK(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
        WMZ_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),
        WMZ_GAS_METER_POWER(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        GAS_KIND(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        CURRENT_POWER(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        TARGET_POWER(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        HOURS_AFTER_LAST_SERVICE(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
        SECURITY_OFF_EXTERN(Doc.of(OpenemsType.FLOAT).accessMode(AccessMode.READ_ONLY)),
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

    /**
     * Gets the Channel for {@link ChannelId#ELECTRICITY_POWER}.
     *
     * @return the Channel
     */
    default Channel<Float> getElectricityPowerChannel() {
        return this.channel(ChannelId.ELECTRICITY_POWER);
    }

    /**
     * Gets the Channel for {@link ChannelId#ELECTRICITY_ENERGY_PRODUCED}.
     *
     * @return the Channel
     */
    default Channel<Float> getElectricityEnergyProducedChannel() {
        return this.channel(ChannelId.ELECTRICITY_ENERGY_PRODUCED);
    }

    /**
     * Gets the Channel for {@link ChannelId#SECURITY_OFF_GRID_FAIL}.
     *
     * @return the Channel
     */
    default Channel<Float> getSecurityOffGridFailChannel() {
        return this.channel(ChannelId.SECURITY_OFF_GRID_FAIL);
    }

    /**
     * Gets the Channel for {@link ChannelId#SECURITY_OFF_EVU}.
     *
     * @return the Channel
     */
    default Channel<Float> getSecurityOffEVUChannel() {
        return this.channel(ChannelId.SECURITY_OFF_EVU);
    }

    /**
     * Gets the Channel for {@link ChannelId#REQUIRED_ON_EXTERN}.
     *
     * @return the Channel
     */
    default Channel<Float> getRequiredOnExternChannel() {
        return this.channel(ChannelId.REQUIRED_ON_EXTERN);
    }

    /**
     * Gets the Channel for {@link ChannelId#REQUIRED_ON_EVU}.
     *
     * @return the Channel
     */
    default Channel<Float> getRequiredOnEVUChannel() {
        return this.channel(ChannelId.REQUIRED_ON_EVU);
    }

    /**
     * Gets the Channel for {@link ChannelId#SECURITY_OFF_EXTERN}.
     *
     * @return the Channel
     */
    default Channel<Float> getSecurityOffExternChannel() {
        return this.channel(ChannelId.SECURITY_OFF_EXTERN);
    }

    /**
     * Gets the Channel for {@link ChannelId#HOURS_AFTER_LAST_SERVICE}.
     *
     * @return the Channel
     */
    default Channel<Float> getHoursAfterLastServiceChannel() {
        return this.channel(ChannelId.HOURS_AFTER_LAST_SERVICE);
    }

    /**
     * Gets the Channel for {@link ChannelId#TARGET_POWER}.
     *
     * @return the Channel
     */
    default Channel<Float> getTargetPowerChannel() {
        return this.channel(ChannelId.TARGET_POWER);
    }

    /**
     * Gets the Channel for {@link ChannelId#CURRENT_POWER}.
     *
     * @return the Channel
     */
    default Channel<Float> getCurrentPowerChannel() {
        return this.channel(ChannelId.CURRENT_POWER);
    }

    /**
     * Gets the Channel for {@link ChannelId#GAS_KIND}.
     *
     * @return the Channel
     */
    default Channel<Float> getGasKindChannel() {
        return this.channel(ChannelId.GAS_KIND);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_GAS_METER_POWER}.
     *
     * @return the Channel
     */
    default Channel<Float> getWmzGasMeterPowerChannel() {
        return this.channel(ChannelId.WMZ_GAS_METER_POWER);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_POWER}.
     *
     * @return the Channel
     */
    default Channel<Float> getWmzPowerChannel() {
        return this.channel(ChannelId.WMZ_POWER);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SINK}.
     *
     * @return the Channel
     */
    default Channel<Float> getWmzTempSinkChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SINK);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_ENERGY_AMOUNT}.
     *
     * @return the Channel
     */
    default Channel<Float> getWmzEnergyAmountChannel() {
        return this.channel(ChannelId.WMZ_ENERGY_AMOUNT);
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SOURCE}.
     *
     * @return the Channel
     */
    default Channel<Float> getWmzTempSourceChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SOURCE);
    }

}
