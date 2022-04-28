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
import io.openems.edge.heater.api.Heater;

/**
 * Channels for the Weishaupt heat pump.
 */

public interface HeatpumpWeishaupt extends Heater {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /* Registers. Manual doesn't say if the address is for Input or Holding. Testing revealed the address is for both
           Input and Holding, meaning Input and Holding registers on the same address have the same value. Most values
           are read only, even though they are Holding registers. To avoid confusion, in the code read only values use
           the Input registers, while read/write values use the Holding registers.
           The registers in the manual are 0 based, meaning the first register has address 0. OpenEMS is also 0 based.*/

        // Read only

        /**
         * Outside temperature.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR1_OUTSIDE_TEMP(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        //IR2_RETURN_TEMPERATURE -> Heater RETURN_TEMPERATURE.

        /**
         * Domestic hot water temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR3_DOMESTIC_HOT_WATER(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        //IR5_FLOW_TEMPERATURE -> Heater FLOW_TEMPERATURE.

        /**
         * Operating hours compressor 1.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: Hour
         * </ul>
         */
        IR72_OPERATING_HOURS_COMPRESSOR1(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Operating hours compressor 2.
         * <ul>
         *     <li> Type: Integer
         *     <li> Unit: Hour
         * </ul>
         */
        IR73_OPERATING_HOURS_COMPRESSOR2(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR)),

        /**
         * Status code.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR103_STATUS_CODE(Doc.of(OpenemsType.INTEGER)),

        /**
         * Blocked code (Sperre).
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR104_BLOCKED_CODE(Doc.of(OpenemsType.INTEGER)),

        /**
         * Error code.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR105_ERROR_CODE(Doc.of(OpenemsType.INTEGER)),

        // Read/write

        /**
         * Room temperature set point. Only used when HR247 set to room temperature mode. Min 150 d°C, max 300 d°C.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR46_ROOM_TEMPERATURE_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Operating mode.
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
        HR222_OPERATING_MODE(Doc.of(OperatingMode.values()).accessMode(AccessMode.READ_WRITE)),
        HR222_MODBUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Manual flow temperature set point. Only used when HR247 set to manual mode. Min 180 d°C, max 600 d°C.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        //HR244_SET_POINT_TEMPERATURE -> Heater SET_POINT_TEMPERATURE.
        HR244_MODBUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * 1st heating circuit flow temperature regulation mode. (Regelung 1. Heizkreis)
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 ... 2
         *      <li> State 0: Outside temperature & heating curve
         *      <li> State 1: Manual set point (uses HR244 set point)
         *      <li> State 2: Room temperature
         * </ul>
         */
        HR247_FLOW_TEMP_REGULATION_MODE(Doc.of(FlowTempRegulationMode.values()).accessMode(AccessMode.READ_WRITE)),
        HR247_MODBUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Domestic hot water set point temperature. Min 300 d°C, max as per HR255 setting.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR254_DOMESTIC_HOT_WATER_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),

        /**
         * Domestic hot water set point temperature upper limit, for value of HR254. Min 300 d°C, max 850 d°C.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR255_DOMESTIC_HOT_WATER_SET_POINT_UPPER_LIMIT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS)),


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

    // Input Registers. Read only.

    /**
     * Gets the Channel for {@link ChannelId#IR1_OUTSIDE_TEMP}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOutsideTempChannel() {
        return this.channel(ChannelId.IR1_OUTSIDE_TEMP);
    }

    /**
     * Gets the outside temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR1_OUTSIDE_TEMP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOutsideTemp() {
        return this.getOutsideTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR3_DOMESTIC_HOT_WATER}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getDomesticHotWaterTempChannel() {
        return this.channel(ChannelId.IR3_DOMESTIC_HOT_WATER);
    }

    /**
     * Gets the domestic hot water temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR3_DOMESTIC_HOT_WATER}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getDomesticHotWaterTemp() {
        return this.getDomesticHotWaterTempChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR72_OPERATING_HOURS_COMPRESSOR1}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getHoursCompressor1Channel() {
        return this.channel(ChannelId.IR72_OPERATING_HOURS_COMPRESSOR1);
    }

    /**
     * Gets the operating hours of compressor 1.
     * See {@link ChannelId#IR72_OPERATING_HOURS_COMPRESSOR1}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHoursCompressor1() {
        return this.getHoursCompressor1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR73_OPERATING_HOURS_COMPRESSOR2}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getHoursCompressor2Channel() {
        return this.channel(ChannelId.IR73_OPERATING_HOURS_COMPRESSOR2);
    }

    /**
     * Gets the operating hours of compressor 2.
     * See {@link ChannelId#IR73_OPERATING_HOURS_COMPRESSOR2}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHoursCompressor2() {
        return this.getHoursCompressor2Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR103_STATUS_CODE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStatusCodeChannel() {
        return this.channel(ChannelId.IR103_STATUS_CODE);
    }

    /**
     * Gets the status code.
     * See {@link ChannelId#IR103_STATUS_CODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getStatusCode() {
        return this.getStatusCodeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR104_BLOCKED_CODE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getBlockedCodeChannel() {
        return this.channel(ChannelId.IR104_BLOCKED_CODE);
    }

    /**
     * Gets the blocked code (Sperre).
     * See {@link ChannelId#IR104_BLOCKED_CODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getBlockedCode() {
        return this.getBlockedCodeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR105_ERROR_CODE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getErrorCodeChannel() {
        return this.channel(ChannelId.IR105_ERROR_CODE);
    }

    /**
     * Gets the error code.
     * See {@link ChannelId#IR105_ERROR_CODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getErrorCode() {
        return this.getErrorCodeChannel().value();
    }

    // Holding Registers. Read/write

    /**
     * Gets the Channel for {@link ChannelId#HR46_ROOM_TEMPERATURE_SET_POINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getRoomTemperatureSetpointChannel() {
        return this.channel((ChannelId.HR46_ROOM_TEMPERATURE_SET_POINT));
    }

    /**
     * Gets the room temperature set point. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR46_ROOM_TEMPERATURE_SET_POINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getRoomTemperatureSetpoint() {
        return this.getRoomTemperatureSetpointChannel().value();
    }

    /**
     * Sets the room temperature set point. Unit is dezidegree Celsius. Min 150 d°C, max 300 d°C.
     * The FlowTempRegulationMode must be set to ROOM_TEMP for this to do anything.
     * See {@link ChannelId#HR46_ROOM_TEMPERATURE_SET_POINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setRoomTemperatureSetpoint(int value) throws OpenemsNamedException {
        this.getRoomTemperatureSetpointChannel().setNextWriteValue(value);
    }

    /**
     * Sets the room temperature set point. Unit is dezidegree Celsius. Min 150 d°C, max 300 d°C.
     * The FlowTempRegulationMode must be set to ROOM_TEMP for this to do anything.
     * See {@link ChannelId#HR46_ROOM_TEMPERATURE_SET_POINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setRoomTemperatureSetpoint(Integer value) throws OpenemsNamedException {
        this.getRoomTemperatureSetpointChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR222_OPERATING_MODE}.
     *
     * @return the Channel
     */
    default EnumWriteChannel getOperatingModeChannel() {
        return this.channel(ChannelId.HR222_OPERATING_MODE);
    }

    /**
     * Gets the operating mode.
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
     * See {@link ChannelId#HR222_OPERATING_MODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOperatingMode() {
        return this.getOperatingModeChannel().value();
    }

    /**
     * Sets the operating mode.
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
     * See {@link ChannelId#HR222_OPERATING_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setOperatingMode(int value) throws OpenemsNamedException {
        this.getOperatingModeChannel().setNextWriteValue(value);
    }

    /**
     * Sets the operating mode.
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
     * See {@link ChannelId#HR222_OPERATING_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setOperatingMode(Integer value) throws OpenemsNamedException {
        this.getOperatingModeChannel().setNextWriteValue(value);
    }

    /**
     * Sets the operating mode.
     * See {@link ChannelId#HR222_OPERATING_MODE}.
     *
     * @param mode the next write value
     * @throws OpenemsNamedException on error
     */
    default void setOperatingMode(OperatingMode mode) throws OpenemsNamedException {
        if (mode != null && mode != OperatingMode.UNDEFINED) {
            this.getOperatingModeChannel().setNextWriteValue(mode.getValue());
        }
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR222_MODBUS}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr222ModbusChannel() {
        return this.channel(ChannelId.HR222_MODBUS);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR244_MODBUS}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr244ModbusChannel() {
        return this.channel(ChannelId.HR244_MODBUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR247_FLOW_TEMP_REGULATION_MODE}.
     *
     * @return the Channel
     */
    default EnumWriteChannel getFlowTempRegulationModeChannel() {
        return this.channel(ChannelId.HR247_FLOW_TEMP_REGULATION_MODE);
    }

    /**
     * Gets the flow temperature regulation mode.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 2
     *      <li> State 0: Outside temperature & heating curve
     *      <li> State 1: Manual set point (uses HR244 set point)
     *      <li> State 2: Room temperature
     * </ul>
     * See {@link ChannelId#HR247_FLOW_TEMP_REGULATION_MODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getFlowTempRegulationMode() {
        return this.getFlowTempRegulationModeChannel().value();
    }

    /**
     * Sets the flow temperature regulation mode.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 2
     *      <li> State 0: Outside temperature & heating curve
     *      <li> State 1: Manual set point (uses HR244 set point)
     *      <li> State 2: Room temperature
     * </ul>
     * See {@link ChannelId#HR247_FLOW_TEMP_REGULATION_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setFlowTempRegulationMode(int value) throws OpenemsNamedException {
        this.getFlowTempRegulationModeChannel().setNextWriteValue(value);
    }

    /**
     * Sets the flow temperature regulation mode.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: 0 ... 2
     *      <li> State 0: Outside temperature & heating curve
     *      <li> State 1: Manual set point (uses HR244 set point)
     *      <li> State 2: Room temperature
     * </ul>
     * See {@link ChannelId#HR247_FLOW_TEMP_REGULATION_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setFlowTempRegulationMode(Integer value) throws OpenemsNamedException {
        this.getFlowTempRegulationModeChannel().setNextWriteValue(value);
    }

    /**
     * Sets the flow temperature regulation mode.
     * See {@link ChannelId#HR247_FLOW_TEMP_REGULATION_MODE}.
     *
     * @param mode the next write value
     * @throws OpenemsNamedException on error
     */
    default void setFlowTempRegulationMode(FlowTempRegulationMode mode) throws OpenemsNamedException {
        if (mode != null && mode != FlowTempRegulationMode.UNDEFINED) {
            this.getFlowTempRegulationModeChannel().setNextWriteValue(mode.getValue());
        }
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR247_MODBUS}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr247ModbusChannel() {
        return this.channel(ChannelId.HR247_MODBUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR254_DOMESTIC_HOT_WATER_SET_POINT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHotWaterTempSetpointChannel() {
        return this.channel((ChannelId.HR254_DOMESTIC_HOT_WATER_SET_POINT));
    }

    /**
     * Gets the domestic hot water temperature set point. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR254_DOMESTIC_HOT_WATER_SET_POINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHotWaterTempSetpoint() {
        return this.getHotWaterTempSetpointChannel().value();
    }

    /**
     * Sets the domestic hot water temperature set point. Unit is dezidegree Celsius. Min 300 d°C, max defined by
     * setHotWaterTempSetpointUpperLimit.
     * See {@link ChannelId#HR254_DOMESTIC_HOT_WATER_SET_POINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHotWaterTempSetpoint(int value) throws OpenemsNamedException {
        this.getHotWaterTempSetpointChannel().setNextWriteValue(value);
    }

    /**
     * Sets the domestic hot water temperature set point. Unit is dezidegree Celsius. Min 300 d°C, max defined by
     * setHotWaterTempSetpointUpperLimit.
     * See {@link ChannelId#HR254_DOMESTIC_HOT_WATER_SET_POINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHotWaterTempSetpoint(Integer value) throws OpenemsNamedException {
        this.getHotWaterTempSetpointChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR255_DOMESTIC_HOT_WATER_SET_POINT_UPPER_LIMIT}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHotWaterTempSetpointUpperLimitChannel() {
        return this.channel((ChannelId.HR255_DOMESTIC_HOT_WATER_SET_POINT_UPPER_LIMIT));
    }

    /**
     * Gets the domestic hot water temperature set point upper limit. Unit is dezidegree Celsius.
     * See {@link ChannelId#HR255_DOMESTIC_HOT_WATER_SET_POINT_UPPER_LIMIT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHotWaterTempSetpointUpperLimit() {
        return this.getHotWaterTempSetpointUpperLimitChannel().value();
    }

    /**
     * Sets the domestic hot water temperature set point upper limit. Unit is dezidegree Celsius. Min 300 d°C, max 850 d°C.
     * See {@link ChannelId#HR255_DOMESTIC_HOT_WATER_SET_POINT_UPPER_LIMIT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHotWaterTempSetpointUpperLimit(int value) throws OpenemsNamedException {
        this.getHotWaterTempSetpointUpperLimitChannel().setNextWriteValue(value);
    }

    /**
     * Sets the domestic hot water temperature set point upper limit. Unit is dezidegree Celsius. Min 150 d°C, max 850 d°C.
     * See {@link ChannelId#HR255_DOMESTIC_HOT_WATER_SET_POINT_UPPER_LIMIT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHotWaterTempSetpointUpperLimit(Integer value) throws OpenemsNamedException {
        this.getHotWaterTempSetpointUpperLimitChannel().setNextWriteValue(value);
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
