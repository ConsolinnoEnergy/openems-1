package io.openems.edge.powerplant.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.generator.api.GeneratorModbus;

public interface CombinedHeatPowerPlantModbus extends PowerPlantModbus {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        WMZ_ENERGY_AMOUNT_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        WMZ_ENERGY_AMOUNT_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        WMZ_TEMP_SOURCE_LONG(Doc.of(OpenemsType.LONG).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SOURCE_DOUBLE(Doc.of(OpenemsType.DOUBLE).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        WMZ_TEMP_SINK_LONG(Doc.of(OpenemsType.LONG).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),
        WMZ_TEMP_SINK_DOUBLE(Doc.of(OpenemsType.DOUBLE).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        WMZ_POWER_LONG(Doc.of(OpenemsType.LONG).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),
        WMZ_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),

        WMZ_GAS_METER_POWER_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        WMZ_GAS_METER_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        CURRENT_POWER_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        CURRENT_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        TARGET_POWER_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        TARGET_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        HOURS_AFTER_LAST_SERVICE_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        HOURS_AFTER_LAST_SERVICE_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        SECURITY_OFF_EXTERN_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        SECURITY_OFF_EXTERN_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        REQUIRED_ON_EVU_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        REQUIRED_ON_EVU_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        REQUIRED_ON_EXTERN_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        REQUIRED_ON_EXTERN_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        SECURITY_OFF_EVU_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        SECURITY_OFF_EVU_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        SECURITY_OFF_GRID_FAIL_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        SECURITY_OFF_GRID_FAIL_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        ELECTRICITY_ENERGY_PRODUCED_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        ELECTRICITY_ENERGY_PRODUCED_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        ELECTRICITY_POWER_LONG(Doc.of(OpenemsType.LONG).accessMode(AccessMode.READ_ONLY)),
        ELECTRICITY_POWER_DOUBLE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_ENERGY_AMOUNT_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getWMZEnergyAmountLongChannel() {
        return this.channel(ChannelId.WMZ_ENERGY_AMOUNT_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#WMZ_ENERGY_AMOUNT_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getWMZEnergyAmountDoubleChannel() {
        return this.channel(ChannelId.WMZ_ENERGY_AMOUNT_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a WMZ Energy Amount set. After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasWMZEnergyAmount() {
        return GeneratorModbus.getValueDefinedChannel(this._getWMZEnergyAmountLongChannel(), this._getWMZEnergyAmountDoubleChannel());
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SOURCE_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getWMZTempSourceLongChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SOURCE_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SOURCE_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getWMZTempSourceDoubleChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SOURCE_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a WMZ Temp Source set. After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasWMZTempSource() {
        return GeneratorModbus.getValueDefinedChannel(this._getWMZTempSourceLongChannel(), this._getWMZTempSourceDoubleChannel());
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SINK_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getWMZTempSinkLongChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SINK_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SINK_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getWMZTempSinkDoubleChannel() {
        return this.channel(ChannelId.WMZ_TEMP_SINK_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a WMZ Temp Sink set. After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasWMZTempSink() {
        return GeneratorModbus.getValueDefinedChannel(this._getWMZTempSinkLongChannel(), this._getWMZTempSinkDoubleChannel());
    }

    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SINK_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getWMZPowerLongChannel() {
        return this.channel(ChannelId.WMZ_POWER_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SINK_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getWMZPowerDoubleChannel() {
        return this.channel(ChannelId.WMZ_POWER_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a WMZ Power set.
     * After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasWMZPower() {
        return GeneratorModbus.getValueDefinedChannel(this._getWMZPowerLongChannel(), this._getWMZPowerDoubleChannel());
    }


    /**
     * Gets the Channel for {@link ChannelId#WMZ_TEMP_SINK_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getWMZGasMeterPowerLongChannel() {
        return this.channel(ChannelId.WMZ_GAS_METER_POWER_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#WMZ_GAS_METER_POWER_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getWMZGasMeterPowerDoubleChannel() {
        return this.channel(ChannelId.WMZ_GAS_METER_POWER_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a GasMeter Power set.
     * After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasWMZGasMeterPower() {
        return GeneratorModbus.getValueDefinedChannel(this._getWMZGasMeterPowerLongChannel(), this._getWMZGasMeterPowerDoubleChannel());
    }

    /**
     * Gets the Channel for {@link ChannelId#CURRENT_POWER_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getCurrentPowerLongChannel() {
        return this.channel(ChannelId.CURRENT_POWER_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#CURRENT_POWER_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getCurrentPowerDoubleChannel() {
        return this.channel(ChannelId.CURRENT_POWER_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a GasMeter Power set.
     * After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasCurrentPower() {
        return GeneratorModbus.getValueDefinedChannel(this._getCurrentPowerLongChannel(), this._getCurrentPowerDoubleChannel());
    }



    /**
     * Gets the Channel for {@link ChannelId#TARGET_POWER_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getTargetPowerLongChannel() {
        return this.channel(ChannelId.TARGET_POWER_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#TARGET_POWER_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getTargetPowerDoubleChannel() {
        return this.channel(ChannelId.TARGET_POWER_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a Target Power set.
     * After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasTargetPower() {
        return GeneratorModbus.getValueDefinedChannel(this._getTargetPowerLongChannel(), this._getTargetPowerDoubleChannel());
    }

    /**
     * Gets the Channel for {@link ChannelId#HOURS_AFTER_LAST_SERVICE_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getHoursAfterServiceLongChannel() {
        return this.channel(ChannelId.HOURS_AFTER_LAST_SERVICE_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#HOURS_AFTER_LAST_SERVICE_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getHoursAfterServiceDoubleChannel() {
        return this.channel(ChannelId.HOURS_AFTER_LAST_SERVICE_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a Hours After Service set.
     * After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasHoursAfterService() {
        return GeneratorModbus.getValueDefinedChannel(this._getHoursAfterServiceLongChannel(), this._getHoursAfterServiceDoubleChannel());
    }


    /**
     * Gets the Channel for {@link ChannelId#SECURITY_OFF_EXTERN_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getSecurityOffExternLongChannel() {
        return this.channel(ChannelId.SECURITY_OFF_EXTERN_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#SECURITY_OFF_EXTERN_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getSecurityOffExternDoubleChannel() {
        return this.channel(ChannelId.SECURITY_OFF_EXTERN_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a Security Off Extern set.
     * After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasSecurityOffExtern() {
        return GeneratorModbus.getValueDefinedChannel(this._getSecurityOffExternLongChannel(), this._getSecurityOffExternDoubleChannel());
    }


    /**
     * Gets the Channel for {@link ChannelId#REQUIRED_ON_EVU_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getRequiredOnEVULongChannel() {
        return this.channel(ChannelId.REQUIRED_ON_EVU_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#REQUIRED_ON_EVU_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getRequiredOnEVUDoubleChannel() {
        return this.channel(ChannelId.REQUIRED_ON_EVU_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a Required On EVU set set.
     * After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasRequiredOnEVU() {
        return GeneratorModbus.getValueDefinedChannel(this._getRequiredOnEVULongChannel(), this._getRequiredOnEVUDoubleChannel());
    }


    /**
     * Gets the Channel for {@link ChannelId#REQUIRED_ON_EXTERN_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getRequiredOnExternLongChannel() {
        return this.channel(ChannelId.REQUIRED_ON_EXTERN_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#REQUIRED_ON_EXTERN_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getRequiredOnExternDoubleChannel() {
        return this.channel(ChannelId.REQUIRED_ON_EXTERN_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a Required On Extern set.
     * After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasRequiredOnExtern() {
        return GeneratorModbus.getValueDefinedChannel(this._getRequiredOnExternLongChannel(), this._getRequiredOnExternDoubleChannel());
    }


    /**
     * Gets the Channel for {@link ChannelId#SECURITY_OFF_EVU_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getSecurityOffEVULongChannel() {
        return this.channel(ChannelId.SECURITY_OFF_EVU_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#SECURITY_OFF_EVU_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getSecurityOffEVUDoubleChannel() {
        return this.channel(ChannelId.SECURITY_OFF_EVU_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a Security Off EVU set.
     * After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasSecurityOffEVU() {
        return GeneratorModbus.getValueDefinedChannel(this._getSecurityOffEVULongChannel(), this._getSecurityOffEVUDoubleChannel());
    }



    /**
     * Gets the Channel for {@link ChannelId#SECURITY_OFF_GRID_FAIL_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getSecurityOffGridFailLongChannel() {
        return this.channel(ChannelId.SECURITY_OFF_GRID_FAIL_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#SECURITY_OFF_GRID_FAIL_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getSecurityOffGridFailDoubleChannel() {
        return this.channel(ChannelId.SECURITY_OFF_GRID_FAIL_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a Security Off Grid Fail set.
     * After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasSecurityOffGridFail() {
        return GeneratorModbus.getValueDefinedChannel(this._getSecurityOffGridFailLongChannel(), this._getSecurityOffGridFailDoubleChannel());
    }

    /**
     * Gets the Channel for {@link ChannelId#ELECTRICITY_ENERGY_PRODUCED_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getElectricityEnergyProducedLongChannel() {
        return this.channel(ChannelId.ELECTRICITY_ENERGY_PRODUCED_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#ELECTRICITY_ENERGY_PRODUCED_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getElectricityEnergyProducedDoubleChannel() {
        return this.channel(ChannelId.ELECTRICITY_ENERGY_PRODUCED_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a Security Off Grid Fail set.
     * After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasElectricityEnergyProduced() {
        return GeneratorModbus.getValueDefinedChannel(this._getElectricityEnergyProducedLongChannel(), this._getElectricityEnergyProducedDoubleChannel());
    }



    /**
     * Gets the Channel for {@link ChannelId#ELECTRICITY_ENERGY_PRODUCED_LONG}.
     *
     * @return the Channel
     */
    default Channel<Long> _getElectricityPowerLongChannel() {
        return this.channel(ChannelId.ELECTRICITY_POWER_LONG);
    }


    /**
     * Gets the Channel for {@link ChannelId#ELECTRICITY_ENERGY_PRODUCED_DOUBLE}.
     *
     * @return the Channel
     */
    default Channel<Double> _getElectricityPowerDoubleChannel() {
        return this.channel(ChannelId.ELECTRICITY_POWER_DOUBLE);
    }

    /**
     * Checks if the CombinedHeatPowerPlant has a Electricity Power set.
     * After that the stored value will be written to the actual {@link CombinedHeatPowerPlant}
     * Only call this within the implementing Class.
     *
     * @return the channel that contains the value or else null.
     */
    default Channel<?> _hasElectricityPower() {
        return GeneratorModbus.getValueDefinedChannel(this._getElectricityEnergyProducedLongChannel(), this._getElectricityEnergyProducedDoubleChannel());
    }



}
