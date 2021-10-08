package io.openems.edge.heater.gasboiler.buderus.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.api.Heater;

/**
 * Channels for the Buderus gas boiler.
 */
public interface GasBoilerBuderus extends Heater {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers, read only. The register address is in the channel name, so IR0 means input register 0.
        // Unsigned 16 bit, unless stated otherwise.

        //IR384_STRATEGY_RETURN_TEMPERATURE -> Does not work. Use IR8003_RETURN_TEMP_TANK1.

        //IR385_STRATEGY_FLOW_TEMPERATURE -> Use IR8001_FLOW_TEMP_TANK1.

        /**
         * Status strategy.
         * 0 - Unknown (Unbekannt)
         * 1 - Warning (Warnung)
         * 2 - Error (Stoerung)
         * 3 - OK
         * 4 - Not active (Nicht aktiv)
         * 5 - Critical (Kritisch)
         * 6 - No info (Keine Info)
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR386_STATUS_STRATEGY(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        //IR387_STRATEGY_READ_EFFECTIVE_POWER_PERCENT -> Heater, READ_EFFECTIVE_POWER_PERCENT. Fällt weg. Nimm Wert vom Kessel.

        /**
         * Who requested heater to turn on.
         * 0 - Not active (Nicht aktiv)
         * 1 - Controller (Regelgerät)
         * 2 - Internal (Intern)
         * 3 - Manual operation (Manueller Betrieb)
         * 4 - External (Extern)
         * 5 - Internal + external (Intern + Extern)
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR390_RUNREQUEST_INITIATOR(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Strategy bitblock.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR394_STRATEGY_BITBLOCK(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Maximum flow temperature requested (Maximale Vorlauftemperatur angefordert).
         * Modbus value is degree Celsius (NOT decidegree), watch the conversion! Signed.
         * Not exactly sure what this does.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR395_MAX_FLOW_TEMP_REQUESTED(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Error register 1. Doubleword.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR476_ERROR_REGISTER1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Error register 2. Doubleword.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR478_ERROR_REGISTER2(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Error register 3. Doubleword.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR480_ERROR_REGISTER3(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Error register 4. Doubleword.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR482_ERROR_REGISTER4(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        //IR8001_FLOW_TEMP_BOILER1 -> Heater, FLOW_TEMPERATURE. d°C, signed.

        /**
         * Flow temperature rate of change (Aenderungsgeschwindigkeit). Unit is deci Kelvin / min, signed.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Deci Kelvin / min
         * </ul>
         */
        IR8002_FLOW_TEMP_RATE_OF_CHANGE_BOILER1(Doc.of(OpenemsType.INTEGER).unit(Unit.DECI_KELVIN_PER_MINUTE).accessMode(AccessMode.READ_ONLY)),

        //IR8003_RETURN_TEMP_BOILER1 -> Heater, RETURN_TEMPERATURE. d°C, signed.

        //IR8004_EFFECTIVE_POWER_BOILER1 -> Heater, EFFECTIVE_HEATING_POWER_PERCENT. %, unsigned.

        /**
         * Heater at load limit boiler 1 (Wärmeerzeuger in Lastbegrenzung Kessel 1).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Percent
         * </ul>
         */
        IR8005_HEATER_AT_LOAD_LIMIT_BOILER1(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),

        //IR8006_OPERATING_TEMPERATURE -> doesn't give a value.

        /**
         * Maximum power boiler 1 (Maximale Leistung Kessel 1).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Kilowatt
         * </ul>
         */
        IR8007_MAXIMUM_POWER_BOILER1(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),

        /**
         * Minimum power boiler 1 (Minimale Leistung Kessel 1). Unit is percent!
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Percent
         * </ul>
         */
        IR8008_MINIMUM_POWER_PERCENT_BOILER1(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),

        /**
         * Maximum flow temperature boiler 1 (Maximale Vorlauftemp Kessel 1). 
         * Modbus value is degree Celsius (NOT decidegree), watch the conversion! Unsigned.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR8011_MAXIMUM_FLOW_TEMP_BOILER1(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Status boiler 1.
         * 0 - Unknown (Unbekannt)
         * 1 - Warning (Warnung)
         * 2 - Error (Stoerung)
         * 3 - OK
         * 4 - Not active (Nicht aktiv)
         * 5 - Critical (Kritisch)
         * 6 - No info (Keine Info)
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR8012_STATUS_BOILER1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Bitblock boiler 1.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR8013_BITBLOCK_BOILER1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Requested temperature set point boiler 1 (Angeforderte Sollwertemperatur Kessel 1). 
         * Modbus value is degree Celsius (NOT decidegree), watch the conversion! Unsigned.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decidegree Celsius
         * </ul>
         */
        IR8015_REQUESTED_TEMPERATURE_SETPOINT_BOILER1(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Set point power percent boiler 1 (Sollwert Leistung Kessel 1, %).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Percent
         * </ul>
         */
        IR8016_SETPOINT_POWER_PERCENT_BOILER1(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),

        /**
         * Pressure boiler 1. Decibar, signed.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decibar
         * </ul>
         */
        IR8017_PRESSURE_BOILER1(Doc.of(OpenemsType.INTEGER).unit(Unit.DECI_BAR).accessMode(AccessMode.READ_ONLY)),

        /**
         * Error code boiler 1 (Fehlercode Kessel 1).
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR8018_ERROR_CODE_BOILER1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Display error code boiler 1 (Fehleranzeigecode im Display Kessel 1).
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR8019_DISPLAY_ERROR_CODE_BOILER1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Number of starts boiler 1 (Anzahl Starts Kessel 1). Doubleword.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR8021_NUMBER_OF_STARTS_BOILER1(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Running time boiler 1 (Betriebszeit Kessel 1). Doubleword.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Minutes
         * </ul>
         */
        IR8023_RUNNING_TIME_BOILER1(Doc.of(OpenemsType.INTEGER).unit(Unit.MINUTE).accessMode(AccessMode.READ_ONLY)),

        // Holding Registers, read/write. The register address is in the channel name, so HR0 means holding register 0.
        // Unsigned 16 bit, unless stated otherwise.

        /**
         * Heart beat, value that is written to the device.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR0_HEARTBEAT_IN(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Heart beat, value that has been received by the device. If "Heart Beat" is activated in the device
         * configuration, a value needs to be constantly written to HR0_HEARTBEAT_IN. If the value is received by the
         * device, it will appear in HR1_HEARTBEAT_OUT for confirmation. The value written to HR0_HEARTBEAT_IN needs to
         * be different than the last value. The suggested algorithm is to read the value from HR1_HEARTBEAT_OUT,
         * increment it and then send it to HR0_HEARTBEAT_IN. With an overflow protection, to start again with 1 if the
         * counter has reached a certain threshold (remember 16 bit limitation).
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR1_HEARTBEAT_OUT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        //HR400_SET_POINT_FLOW_TEMPERATUR -> Heater, SET_POINT_TEMPERATURE, d°C. Unit at heater is °C, not d°C, so watch the conversion.

        //HR401_SET_POINT_POWER_PERCENT -> Heater, SET_POINT_POWER_PERCENT, %. Unit at heater is also %, a value of 50 in the channel means 50%.

        /**
         * Give the heater run permission.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR402_RUN_PERMISSION(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Register containing command bits.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR405_COMMAND_BITS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),


        // Non Modbus channels

        /**
         * Operation mode, set point temperature (0) or set point power percent (1). Default is set point power percent.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        OPERATING_MODE(Doc.of(OperatingMode.values()).accessMode(AccessMode.READ_WRITE)
                .onInit(channel -> { //
                    // on each Write to the channel -> set the value
                    ((EnumWriteChannel) channel).onSetNextWrite(value -> {
                        channel.setNextValue(value);
                    });
                })),

        /**
         * Status message of the heater.
         * <ul>
         *      <li> Type: String
         * </ul>
         */
        STATUS_MESSAGE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    // Input Registers. Read only.

    /**
     * Gets the Channel for {@link ChannelId#IR386_STATUS_STRATEGY}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR386StatusStrategyChannel() {
        return this.channel(ChannelId.IR386_STATUS_STRATEGY);
    }

    /**
     * Status strategy.
     * 0 - Unknown (Unbekannt)
     * 1 - Warning (Warnung)
     * 2 - Error (Stoerung)
     * 3 - OK
     * 4 - Not active (Nicht aktiv)
     * 5 - Critical (Kritisch)
     * 6 - No info (Keine Info)
     * See {@link ChannelId#IR386_STATUS_STRATEGY}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR386StatusStrategy() {
        return this.getIR386StatusStrategyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR390_RUNREQUEST_INITIATOR}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR390RunrequestInitiatorChannel() {
        return this.channel(ChannelId.IR390_RUNREQUEST_INITIATOR);
    }

    /**
     * Who requested heater to turn on.
     * 0 - Not active (Nicht aktiv)
     * 1 - Controller (Regelgerät)
     * 2 - Internal (Intern)
     * 3 - Manual operation (Manueller Betrieb)
     * 4 - External (Extern)
     * 5 - Internal + external (Intern + Extern)
     * See {@link ChannelId#IR390_RUNREQUEST_INITIATOR}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR390RunrequestInitiator() {
        return this.getIR390RunrequestInitiatorChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR394_STRATEGY_BITBLOCK}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR394StrategyBitblockChannel() {
        return this.channel(ChannelId.IR394_STRATEGY_BITBLOCK);
    }

    /**
     * Strategy bitblock.
     * See {@link ChannelId#IR394_STRATEGY_BITBLOCK}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR394StrategyBitblock() {
        return this.getIR394StrategyBitblockChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR395_MAX_FLOW_TEMP_REQUESTED}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR395MaxFlowTempRequestedChannel() {
        return this.channel(ChannelId.IR395_MAX_FLOW_TEMP_REQUESTED);
    }

    /**
     * Maximum flow temperature requested (Maximale Vorlauftemperatur angefordert).
     * Not exactly sure what this does.
     * See {@link ChannelId#IR395_MAX_FLOW_TEMP_REQUESTED}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR395MaxFlowTempRequested() {
        return this.getIR395MaxFlowTempRequestedChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR476_ERROR_REGISTER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR476ErrorRegister1Channel() {
        return this.channel(ChannelId.IR476_ERROR_REGISTER1);
    }

    /**
     * Error register 1.
     * See {@link ChannelId#IR476_ERROR_REGISTER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR476ErrorRegister1() {
        return this.getIR476ErrorRegister1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR478_ERROR_REGISTER2}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR478ErrorRegister2Channel() {
        return this.channel(ChannelId.IR478_ERROR_REGISTER2);
    }

    /**
     * Error register 2.
     * See {@link ChannelId#IR478_ERROR_REGISTER2}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR478ErrorRegister2() {
        return this.getIR478ErrorRegister2Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR480_ERROR_REGISTER3}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR480ErrorRegister3Channel() {
        return this.channel(ChannelId.IR480_ERROR_REGISTER3);
    }

    /**
     * Error register 3.
     * See {@link ChannelId#IR480_ERROR_REGISTER3}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR480ErrorRegister3() {
        return this.getIR480ErrorRegister3Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR482_ERROR_REGISTER4}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR482ErrorRegister4Channel() {
        return this.channel(ChannelId.IR482_ERROR_REGISTER4);
    }

    /**
     * Error register 4.
     * See {@link ChannelId#IR482_ERROR_REGISTER4}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR482ErrorRegister4() {
        return this.getIR482ErrorRegister4Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8002_FLOW_TEMP_RATE_OF_CHANGE_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR8002FlowTempRateOfChangeChannel() {
        return this.channel(ChannelId.IR8002_FLOW_TEMP_RATE_OF_CHANGE_BOILER1);
    }

    /**
     * Flow temperature rate of change (Aenderungsgeschwindigkeit). Unit is deci Kelvin / min.
     * See {@link ChannelId#IR8002_FLOW_TEMP_RATE_OF_CHANGE_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR8002FlowTempRateOfChange() {
        return this.getIR8002FlowTempRateOfChangeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8005_HEATER_AT_LOAD_LIMIT_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getIR8005HeaterAtLoadLimitChannel() {
        return this.channel(ChannelId.IR8005_HEATER_AT_LOAD_LIMIT_BOILER1);
    }

    /**
     * Heater at load limit boiler 1 (Wärmeerzeuger in Lastbegrenzung Kessel 1), unit is %.
     * See {@link ChannelId#IR8005_HEATER_AT_LOAD_LIMIT_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getIR8005HeaterAtLoadLimit() {
        return this.getIR8005HeaterAtLoadLimitChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8007_MAXIMUM_POWER_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getMaximumPowerBoiler1Channel() {
        return this.channel(ChannelId.IR8007_MAXIMUM_POWER_BOILER1);
    }

    /**
     * Get the maximum thermal output of Kessel 1, unit is kW.
     * See {@link ChannelId#IR8007_MAXIMUM_POWER_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getMaximumPowerBoiler1() {
        return this.getMaximumPowerBoiler1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8008_MINIMUM_POWER_PERCENT_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getMinimumPowerPercentBoiler1Channel() {
        return this.channel(ChannelId.IR8008_MINIMUM_POWER_PERCENT_BOILER1);
    }

    /**
     * Get the minimum thermal output of Kessel 1 in percent.
     * See {@link ChannelId#IR8008_MINIMUM_POWER_PERCENT_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getMinimumPowerPercentBoiler1() {
        return this.getMinimumPowerPercentBoiler1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8011_MAXIMUM_FLOW_TEMP_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getMaximumFlowTempBoiler1Channel() {
        return this.channel(ChannelId.IR8011_MAXIMUM_FLOW_TEMP_BOILER1);
    }

    /**
     * Get the maximum flow temperature of boiler 1, unit is decidegree Celsius.
     * See {@link ChannelId#IR8011_MAXIMUM_FLOW_TEMP_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getMaximumFlowTempBoiler1() {
        return this.getMaximumFlowTempBoiler1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8012_STATUS_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getStatusBoiler1Channel() {
        return this.channel(ChannelId.IR8012_STATUS_BOILER1);
    }

    /**
     * Status boiler 1.
     * 0 - Unknown (Unbekannt)
     * 1 - Warning (Warnung)
     * 2 - Error (Stoerung)
     * 3 - OK
     * 4 - Not active (Nicht aktiv)
     * 5 - Critical (Kritisch)
     * 6 - No info (Keine Info)
     * See {@link ChannelId#IR8012_STATUS_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getStatusBoiler1() {
        return this.getStatusBoiler1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8013_BITBLOCK_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getBitblockBoiler1Channel() {
        return this.channel(ChannelId.IR8013_BITBLOCK_BOILER1);
    }

    /**
     * Bitblock boiler 1.
     * See {@link ChannelId#IR8013_BITBLOCK_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getBitblockBoiler1() {
        return this.getBitblockBoiler1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8015_REQUESTED_TEMPERATURE_SETPOINT_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRequestedTemperatureSetPointBoiler1Channel() {
        return this.channel(ChannelId.IR8015_REQUESTED_TEMPERATURE_SETPOINT_BOILER1);
    }

    /**
     * Requested temperature set point boiler 1, unit is decidegree Celsius.
     * See {@link ChannelId#IR8015_REQUESTED_TEMPERATURE_SETPOINT_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRequestedTemperatureSetPointBoiler1() {
        return this.getRequestedTemperatureSetPointBoiler1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8016_SETPOINT_POWER_PERCENT_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRequestedPowerPercentSetPointBoiler1Channel() {
        return this.channel(ChannelId.IR8016_SETPOINT_POWER_PERCENT_BOILER1);
    }

    /**
     * Requested power percent set point Kessel 1, unit is percent.
     * See {@link ChannelId#IR8016_SETPOINT_POWER_PERCENT_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRequestedPowerPercentSetPointBoiler1() {
        return this.getRequestedPowerPercentSetPointBoiler1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8017_PRESSURE_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getPressureBoiler1Channel() {
        return this.channel(ChannelId.IR8017_PRESSURE_BOILER1);
    }

    /**
     * Pressure Kessel 1, unit is deci Bar.
     * See {@link ChannelId#IR8017_PRESSURE_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getPressureBoiler1() {
        return this.getPressureBoiler1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8018_ERROR_CODE_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getErrorCodeBoiler1Channel() {
        return this.channel(ChannelId.IR8018_ERROR_CODE_BOILER1);
    }

    /**
     * Error code boiler 1.
     * See {@link ChannelId#IR8018_ERROR_CODE_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getErrorCodeBoiler1() {
        return this.getErrorCodeBoiler1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8019_DISPLAY_ERROR_CODE_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getErrorCodeDisplayBoiler1Channel() {
        return this.channel(ChannelId.IR8019_DISPLAY_ERROR_CODE_BOILER1);
    }

    /**
     * Display error code boiler 1.
     * See {@link ChannelId#IR8019_DISPLAY_ERROR_CODE_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getErrorCodeDisplayBoiler1() {
        return this.getErrorCodeDisplayBoiler1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8021_NUMBER_OF_STARTS_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getNumberOfStartsBoiler1Channel() {
        return this.channel(ChannelId.IR8021_NUMBER_OF_STARTS_BOILER1);
    }

    /**
     * Number of starts boiler 1.
     * See {@link ChannelId#IR8021_NUMBER_OF_STARTS_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getNumberOfStartsBoiler1() {
        return this.getNumberOfStartsBoiler1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR8023_RUNNING_TIME_BOILER1}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRunningTimeBoiler1Channel() {
        return this.channel(ChannelId.IR8023_RUNNING_TIME_BOILER1);
    }

    /**
     * Running time boiler 1, unit is minutes.
     * See {@link ChannelId#IR8023_RUNNING_TIME_BOILER1}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRunningTimeBoiler1() {
        return this.getRunningTimeBoiler1Channel().value();
    }

    // Holding Registers. Read/write.

    /**
     * Gets the Channel for {@link ChannelId#HR0_HEARTBEAT_IN}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHeartBeatInChannel() {
        return this.channel(ChannelId.HR0_HEARTBEAT_IN);
    }

    /**
     * Sets the value to send as hear beat. If "Heart Beat" is activated in the device configuration, a value needs to
     * be constantly written to setHeartBeatIn. If the value is received by the device, it will appear in getHeartBeatOut
     * for confirmation. The value written to setHeartBeatIn needs to be different than the last value. The suggested
     * algorithm is to read the value from getHeartBeatOut, increment it and then send it to setHeartBeatIn. With an
     * overflow protection, to start again with 1 if the counter has reached a certain threshold (remember 16 bit
     * limitation).
     * See {@link ChannelId#HR0_HEARTBEAT_IN}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setHeartBeatIn(Integer value) throws OpenemsNamedException {
        this.getHeartBeatInChannel().setNextWriteValue(value);
    }

    /**
     * Sets the value to send as hear beat. If "Heart Beat" is activated in the device configuration, a value needs to
     * be constantly written to setHeartBeatIn. If the value is received by the device, it will appear in getHeartBeatOut
     * for confirmation. The value written to setHeartBeatIn needs to be different than the last value. The suggested
     * algorithm is to read the value from getHeartBeatOut, increment it and then send it to setHeartBeatIn. With an
     * overflow protection, to start again with 1 if the counter has reached a certain threshold (remember 16 bit
     * limitation).
     * See {@link ChannelId#HR0_HEARTBEAT_IN}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setHeartBeatIn(int value) throws OpenemsNamedException {
        this.getHeartBeatInChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR1_HEARTBEAT_OUT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getHeartBeatOutChannel() {
        return this.channel(ChannelId.HR1_HEARTBEAT_OUT);
    }

    /**
     * Read the last heart beat value received by the device. If "Heart Beat" is activated in the device configuration,
     * a value needs to be constantly written to setHeartBeatIn. If the value is received by the device, it will appear
     * in getHeartBeatOut for confirmation. The value written to setHeartBeatIn needs to be different than the last
     * value. The suggested algorithm is to read the value from getHeartBeatOut, increment it and then send it to
     * setHeartBeatIn. With an overflow protection, to start again with 1 if the counter has reached a certain threshold
     * (remember 16 bit limitation).
     * See {@link ChannelId#HR1_HEARTBEAT_OUT}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeartBeatOut() {
        return this.getHeartBeatOutChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR402_RUN_PERMISSION}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getRunPermissionChannel() {
        return this.channel(ChannelId.HR402_RUN_PERMISSION);
    }

    /**
     * Give the heater permission to run or not.
     * See {@link ChannelId#HR402_RUN_PERMISSION}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setRunPermission(Boolean value) throws OpenemsNamedException {
        this.getRunPermissionChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR405_COMMAND_BITS}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getCommandBitsChannel() {
        return this.channel(ChannelId.HR405_COMMAND_BITS);
    }

    /**
     * Sets the command bits.
     * See {@link ChannelId#HR405_COMMAND_BITS}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setCommandBits(Integer value) throws OpenemsNamedException {
        this.getCommandBitsChannel().setNextWriteValue(value);
    }

    /**
     * Sets the command bits.
     * See {@link ChannelId#HR405_COMMAND_BITS}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setCommandBits(int value) throws OpenemsNamedException {
        this.getCommandBitsChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#OPERATING_MODE}.
     *
     * @return the Channel
     */
    public default EnumWriteChannel getOperatingModeChannel() {
        return this.channel(ChannelId.OPERATING_MODE);
    }

    /**
     * Get the operating mode.
     * See {@link ChannelId#OPERATING_MODE}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getOperatingMode() {
        return this.getOperatingModeChannel().value();
    }

    /**
     * Operating mode, set point temperature (0) or set point power percent (1). Default is set point power percent.
     * See {@link ChannelId#OPERATING_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setOperatingMode(Integer value) throws OpenemsNamedException {
        this.getOperatingModeChannel().setNextWriteValue(value);
    }

    /**
     * Operating mode, set point temperature (0) or set point power percent (1). Default is set point power percent.
     * See {@link ChannelId#OPERATING_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    public default void setOperatingMode(int value) throws OpenemsNamedException {
        this.getOperatingModeChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#STATUS_MESSAGE}.
     *
     * @return the Channel
     */
    public default StringReadChannel getStatusMessageChannel() {
        return this.channel(ChannelId.STATUS_MESSAGE);
    }

    /**
     * Get the status message.
     * See {@link ChannelId#STATUS_MESSAGE}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<String> getStatusMessage() {
        return this.getStatusMessageChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#STATUS_MESSAGE} Channel.
     * See {@link ChannelId#STATUS_MESSAGE}.
     *
     * @param value the next value
     */
    public default void _setStatusMessage(String value) {
        this.getStatusMessageChannel().setNextValue(value);
    }
}
