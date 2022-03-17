package io.openems.edge.heater.heatpump.weishaupt.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.api.HeatpumpSmartGrid;

/**
 * Channels for the Weishaupt heat pump.
 */

public interface HeatpumpWeishaupt extends HeatpumpSmartGrid {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /* Registers. Manuel doesn't say if they are Input or Holding. Testing revealed they are both input and holding,
           meaning the input registers have the same values as the holding registers.
           The registers in the manual are 0 based, meaning the first register has address 0. OpenEMS is also 0 based.*/

        // Read only

        /**
         * Outside temperature.
         * Modbus data type and unit: float 16, °C
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR1_OUTSIDE_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        //HR2_RETURN_TEMPERATURE -> Heater RETURN_TEMPERATURE. Modbus data type and unit: float 16, °C

        /**
         * Domestic hot water temperature.
         * Modbus data type and unit: float 16, °C
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR3_DOMESTIC_HOT_WATER(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        //HR5_FLOW_TEMPERATURE -> Heater FLOW_TEMPERATURE. Modbus data type and unit: float 16, °C

        /**
         * Status code.
         * Modbus data type: unsigned int 16
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR103_STATUS_CODE(Doc.of(OpenemsType.INTEGER)),

        /**
         * Blocked code (Sperre).
         * Modbus data type: unsigned int 16
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR104_BLOCKED_CODE(Doc.of(OpenemsType.INTEGER)),

        /**
         * Error code.
         * Modbus data type: unsigned int 16
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR105_ERROR_CODE(Doc.of(OpenemsType.INTEGER)),

        // Read/write

        /**
         * Operating mode.
         * Modbus data type: unsigned int 16
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 5
         *      <li> State 0: Summer
         *      <li> State 1: Automatic
         *      <li> State 2: Holidays, full time throttling (Urlaub)
         *      <li> State 3: No late night throttling (Party)
         *      <li> State 4: Second heat generator (2. Waermeerzeuger)
         *      <li> State 5: Cooling
         * </ul>
         */
        HR142_OPERATING_MODE(Doc.of(OperatingMode.values()).accessMode(AccessMode.READ_WRITE)),


        // Non Modbus channels

        /**
         * Status code of the heater parsed to a string.
         * <ul>
         *      <li> Type: String
         * </ul>
         */
        STATUS_MESSAGE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        ;


        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    // Holding (?) Registers. Read only.

    /**
     * Gets the Channel for {@link ChannelId#HR1_OUTSIDE_TEMP}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOutsideTempChannel() {
        return this.channel(ChannelId.HR1_OUTSIDE_TEMP);
    }

    /**
     * Gets the outside temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR1_OUTSIDE_TEMP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOutsideTemp() {
        return this.getOutsideTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR3_DOMESTIC_HOT_WATER}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getDomesticHotWaterTempChannel() {
        return this.channel(ChannelId.HR3_DOMESTIC_HOT_WATER);
    }

    /**
     * Gets the domestic hot water temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#HR3_DOMESTIC_HOT_WATER}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getDomesticHotWaterTemp() {
        return this.getDomesticHotWaterTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR103_STATUS_CODE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStatusCodeChannel() {
        return this.channel(ChannelId.HR103_STATUS_CODE);
    }

    /**
     * Gets the status code.
     * See {@link ChannelId#HR103_STATUS_CODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getStatusCode() {
        return this.getStatusCodeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR104_BLOCKED_CODE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getBlockedCodeChannel() {
        return this.channel(ChannelId.HR104_BLOCKED_CODE);
    }

    /**
     * Gets the blocked code (Sperre).
     * See {@link ChannelId#HR104_BLOCKED_CODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getBlockedCode() {
        return this.getBlockedCodeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR105_ERROR_CODE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getErrorCodeChannel() {
        return this.channel(ChannelId.HR105_ERROR_CODE);
    }

    /**
     * Gets the error code.
     * See {@link ChannelId#HR105_ERROR_CODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getErrorCode() {
        return this.getErrorCodeChannel().value();
    }

    // Read/write

    /**
     * Gets the Channel for {@link ChannelId#HR142_OPERATING_MODE}.
     *
     * @return the Channel
     */
    default EnumWriteChannel getOperatingModeChannel() {
        return this.channel(ChannelId.HR142_OPERATING_MODE);
    }

    /**
     * Get the operating mode.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 5
     *      <li> State 0: Summer
     *      <li> State 1: Automatic
     *      <li> State 2: Holidays, full time throttling (Urlaub)
     *      <li> State 3: No late night throttling (Party)
     *      <li> State 4: Second heat generator (2. Waermeerzeuger)
     *      <li> State 5: Cooling
     * </ul>
     * See {@link ChannelId#HR142_OPERATING_MODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOperatingMode() {
        return this.getOperatingModeChannel().value();
    }

    /**
     * Set the operating mode.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 5
     *      <li> State 0: Summer
     *      <li> State 1: Automatic
     *      <li> State 2: Holidays, full time throttling (Urlaub)
     *      <li> State 3: No late night throttling (Party)
     *      <li> State 4: Second heat generator (2. Waermeerzeuger)
     *      <li> State 5: Cooling
     * </ul>
     * See {@link ChannelId#HR142_OPERATING_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setOperatingMode(int value) throws OpenemsNamedException {
        this.getOperatingModeChannel().setNextWriteValue(value);
    }

    /**
     * Set the operating mode.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 5
     *      <li> State 0: Summer
     *      <li> State 1: Automatic
     *      <li> State 2: Holidays, full time throttling (Urlaub)
     *      <li> State 3: No late night throttling (Party)
     *      <li> State 4: Second heat generator (2. Waermeerzeuger)
     *      <li> State 5: Cooling
     * </ul>
     * See {@link ChannelId#HR142_OPERATING_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setOperatingMode(Integer value) throws OpenemsNamedException {
        this.getOperatingModeChannel().setNextWriteValue(value);
    }


    // Non Modbus channels.

    /**
     * Gets the Channel for {@link ChannelId#STATUS_MESSAGE}.
     *
     * @return the Channel
     */
    default StringReadChannel getStatusMessageChannel() {
        return this.channel(ChannelId.STATUS_MESSAGE);
    }

    /**
     * Gets the status message.
     *
     * @return the Channel {@link Value}
     */
    default Value<String> getStatusMessage() {
        return this.getStatusMessageChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#STATUS_MESSAGE} Channel.
     *
     * @param value the next value
     */
    default void _setStatusMessage(String value) {
        this.getStatusMessageChannel().setNextValue(value);
    }
}
