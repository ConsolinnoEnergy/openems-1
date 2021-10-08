package io.openems.edge.heater.heatpump.heliotherm.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.api.Heater;

public interface HeatpumpHeliotherm extends Heater {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers, read only. The register address is in the channel name, so IR0 means input register 0.
        // Signed 16 bit, unless stated otherwise.

        //IR12_FLOW_TEMPERATUR -> Heater, FLOW_TEMPERATURE, d°C. Unit at heatpump is the same.

        //IR13_RETURN_TEMPERATUR -> Heater, RETURN_TEMPERATURE, d°C. Unit at heatpump is the same.

        /**
         * Buffer temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Dezidegree Celsius
         * </ul>
         */
        IR14_BUFFER_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Heatpump running indicator.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        IR25_HEATPUMP_RUNNING(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

        /**
         * Error indicator.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        IR26_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

        /**
         * Verdichter Drehzahl ist vom Webinterface begrenzt, kann dort eingestellt werden. Default ist 15 - 60%.
         * Unit from heatpump is %*10e-1, so watch the conversion!
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR29_READ_VERDICHTER_DREHZAHL(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),

        /**
         * COP, coefficient of performance. Handbuch sagt "Faktor 0,1".
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR30_COP(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * EVU Freigabe, read.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        IR32_EVU_FREIGABE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

        /**
         * Read the temperature set point the device is currently using.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Dezidegree Celsius
         * </ul>
         */
        IR34_READ_TEMP_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * If the pump is running, what function made the request.
         * 0 - not running
         * 10 - cooling
         * 20 - heating
         * 30 - warm water
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        IR41_RUN_REQUEST_TYPE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Current electric power consumption. Unsigned double word. Integer in Java is 32bit, so still fits in an int.
         * Unit from heatpump is Watt.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Watt
         * </ul>
         */
        IR70_71_CURRENT_ELECTRIC_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),

        /**
         * Current thermal power. Unsigned double word. Integer in Java is 32bit, so still fits in an int. Unit from
         * heatpump is kW * 10e-1, watch the conversion!
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Kilowatt
         * </ul>
         */
        //IR74_75_CURRENT_THERMAL_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),
        // Heater, -> EFFECTIVE_HEATING_POWER

        // Holding Registers, read/write. The register address is in the channel name, so HR0 means holding register 0.
        // Signed 16 bit, unless stated otherwise.

        /**
         * Operating mode. Unsigned. Allowed to write 0 - 7, 8 - 10 is only used as an indicator.
         * 0 = AUS
         * 1 = Automatik
         * 2 = Kühlen
         * 3 = Sommer
         * 4 = Dauerbetrieb
         * 5 = Absenkung
         * 6 = Urlaub
         * 7 = Party
         * 8 = Ausheizen
         * 9 = EVU Sperre
         * 10 = Hauptschalter aus
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR100_OPERATING_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Set point temperature. Depends on settings which temperature it is. Can be return temp or buffer temp. The
         * buffer temp is a value that can also be received via Modbus, so any temperature reading can be used as
         * control factor. Unit is d°C, at heatpump it is the same.
         * Hier kann nicht Heater:SET_POINT_TEMPERATURE genommen werden, da die Modbus writes an die Pumpe
         * kontrolliert werden müssen (zyklisch senden alle 5s, aber nicht öfter).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Dezidegree Celsius
         * </ul>
         */
        HR102_SET_POINT_TEMPERATUR(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Use set point temperature control mode. Unsigned.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR103_USE_SET_POINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Use set point power percent control mode / Use set point electric power control mode. Unsigned.
         * Control modes power percent and electric power are mutually exclusive. Which is used depends on heatpump
         * configuration. This register is then the toggle for whichever control mode the heatpump is set to.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR117_USE_POWER_CONTROL(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Set point electric power consumption.
         * This needs configuration of some settings in the heatpump to work.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Watt
         * </ul>
         */
        HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)),

        /**
         * Verdichterdrehzahl (=Leistung, also power percent) ist vom Webinterface begrenzt, kann dort eingestellt
         * werden. Default ist 15 - 60%.
         * Im Webinterface muss PV Modus Off eingestellt sein damit nach Drehzahl geregelt werden kann.
         * Unit at heatpump is %*10e-1, watch the conversion!
         * Hier kann nicht Heater:SET_POINT_POWER_PERCENT genommen werden, da die Modbus writes an die Pumpe
         * kontrolliert werden müssen (zyklisch senden alle 5s, aber nicht öfter).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Percent
         * </ul>
         */
        HR126_SET_POINT_VERDICHTERDREHZAHL_PERCENT(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),

        // Nicht sicher ob das gebraucht wird:
        //HR127_EXTERNE_ANFORDERUNG

        /**
         * Reset error. Unsigned.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR128_RESET_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Send the outside temperature value to the heatpump.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Dezidegree Celsius
         * </ul>
         */
        HR129_OUTSIDE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Use the outside temperature value provided via Modbus. Unsigned.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR130_USE_MODBUS_OUTSIDE_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Send the buffer temperature value to the heatpump. Can be used to send any temperature value that should be
         * used as control parameter.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Dezidegree Celsius
         * </ul>
         */
        HR131_BUFFER_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Use the buffer temperature value provided via Modbus. Unsigned.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR132_USE_MODBUS_BUFFER_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Send EVU Freigabe vie Modbus. false = stop heatpump, true = heatpump allowed to run.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR149_EVU_FREIGABE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Use the EVU Freigabe value sent via Modbus.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR150_USE_MODBUS_EVU_FREIGABE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),


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
         * Status message of the heatpump.
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
     * Gets the Channel for {@link ChannelId#IR14_BUFFER_TEMPERATURE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getBufferTemperatureChannel() {
        return this.channel(ChannelId.IR14_BUFFER_TEMPERATURE);
    }

    /**
     * Read the buffer temperature. Unit is dezidegree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getBufferTemperature() { return this.getBufferTemperatureChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR25_HEATPUMP_RUNNING}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getHeatpumpRunningIndicatorChannel() {
        return this.channel(ChannelId.IR25_HEATPUMP_RUNNING);
    }

    /**
     * Heatpump running indicator.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getHeatpumpRunningIndicator() { return this.getHeatpumpRunningIndicatorChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR26_ERROR}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getErrorIndicatorChannel() {
        return this.channel(ChannelId.IR26_ERROR);
    }

    /**
     * Error indicator.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getErrorIndicator() { return this.getErrorIndicatorChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR29_READ_VERDICHTER_DREHZAHL}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getVerdichterDrehzahlChannel() {
        return this.channel(ChannelId.IR29_READ_VERDICHTER_DREHZAHL);
    }

    /**
     * Get the current Verdichter Drehzahl. Unit is percent.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getVerdichterDrehzahl() { return this.getVerdichterDrehzahlChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR30_COP}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCopChannel() {
        return this.channel(ChannelId.IR30_COP);
    }

    /**
     * Get the current coefficient of performance.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCop() { return this.getCopChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR32_EVU_FREIGABE}.
     *
     * @return the Channel
     */
    public default BooleanReadChannel getEvuFreigabeIndicatorChannel() {
        return this.channel(ChannelId.IR32_EVU_FREIGABE);
    }

    /**
     * EVU Freigabe status. false = pump not allowed to run.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getEvuFreigabeIndicator() { return this.getEvuFreigabeIndicatorChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR34_READ_TEMP_SET_POINT}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getSetPointTemperatureIndicatorChannel() {
        return this.channel(ChannelId.IR34_READ_TEMP_SET_POINT);
    }

    /**
     * Read the temperature set point the device is currently using. Unit is dezidegree Celsius.
     * Depends on settings which temperature it is. Can be return temp or buffer temp. The buffer temp is a value that
     * can also be received via Modbus, so any temperature reading can be used as control factor.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getSetPointTemperatureIndicator() { return this.getSetPointTemperatureIndicatorChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR41_RUN_REQUEST_TYPE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getRunRequestTypeChannel() {
        return this.channel(ChannelId.IR41_RUN_REQUEST_TYPE);
    }

    /**
     * If the pump is running, what function made the request.
     * 0 - not running
     * 10 - cooling
     * 20 - heating
     * 30 - warm water
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getRunRequestType() { return this.getRunRequestTypeChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR70_71_CURRENT_ELECTRIC_POWER}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getCurrentElectricPowerChannel() {
        return this.channel(ChannelId.IR70_71_CURRENT_ELECTRIC_POWER);
    }

    /**
     * Get the current electric power consumption. Unit is watt.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getCurrentElectricPower() { return this.getCurrentElectricPowerChannel().value(); }

    /**
     * Gets the Channel for {@link ChannelId#IR74_75_CURRENT_THERMAL_POWER}.
     *
     * @return the Channel
     */
    //public default IntegerReadChannel getCurrentThermalPowerChannel() { return this.channel(ChannelId.IR74_75_CURRENT_THERMAL_POWER); }

    /**
     * Get the current thermal power. Unit is kilowatt.
     *
     * @return the Channel {@link Value}
     */
    //public default Value<Integer> getCurrentThermalPower() { return this.getCurrentThermalPowerChannel().value(); }


    // Holding Registers. Read/write.
    // Writes to the heatpump should be sent every 5s, but not more often than that. Because of this, all write channels
    // mapped to holding registers have their setters marked as internal method.

    /**
     * Gets the Channel for {@link ChannelId#HR100_OPERATING_MODE}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHr100OperatingModeChannel() {
        return this.channel(ChannelId.HR100_OPERATING_MODE);
    }

    /**
     * Operating mode. Allowed to write 0 - 7, 8 - 10 is only used as an indicator.
     * 0 = AUS
     * 1 = Automatik
     * 2 = Kühlen
     * 3 = Sommer
     * 4 = Dauerbetrieb
     * 5 = Absenkung
     * 6 = Urlaub
     * 7 = Party
     * 8 = Ausheizen
     * 9 = EVU Sperre
     * 10 = Hauptschalter aus
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHr100OperatingMode() { return this.getHr100OperatingModeChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setHr100OperatingMode(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getHr100OperatingModeChannel().setNextWriteValue(value);
    }

    /**
     * Internal method.
     */
    public default void _setHr100OperatingMode(int value) throws OpenemsError.OpenemsNamedException {
        this.getHr100OperatingModeChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR102_SET_POINT_TEMPERATUR}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHr102SetPointTemperatureChannel() {
        return this.channel(ChannelId.HR102_SET_POINT_TEMPERATUR);
    }

    /**
     * Get the temperature set point sent to the device. Should be the same as getSetPointTemperatureIndicator().
     * Depends on settings which temperature it is. Can be return temp or buffer temp. The buffer temp is a value that
     * can also be received via Modbus, so any temperature reading can be used as control factor. Unit is d°C.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHr102SetPointTemperature() { return this.getHr102SetPointTemperatureChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setHr102SetPointTemperature(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getHr102SetPointTemperatureChannel().setNextWriteValue(value);
    }

    /**
     * Internal method.
     */
    public default void _setHr102SetPointTemperature(int value) throws OpenemsError.OpenemsNamedException {
        this.getHr102SetPointTemperatureChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR103_USE_SET_POINT_TEMPERATURE}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHr103UseSetPointTemperatureChannel() {
        return this.channel(ChannelId.HR103_USE_SET_POINT_TEMPERATURE);
    }

    /**
     * Is set point temperature control mode set?
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHr103UseSetPointTemperature() { return this.getHr103UseSetPointTemperatureChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setHr103UseSetPointTemperature(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getHr103UseSetPointTemperatureChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR117_USE_POWER_CONTROL}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHr117UsePowerControlChannel() {
        return this.channel(ChannelId.HR117_USE_POWER_CONTROL);
    }

    /**
     * Use set point power percent control mode / Use set point electric power control mode.
     * Control modes power percent and electric power are mutually exclusive. Which is used depends on heatpump
     * configuration. This register is then the toggle for whichever control mode the heatpump is set to.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHr117UsePowerControl() { return this.getHr117UsePowerControlChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setHr117UsePowerControl(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getHr117UsePowerControlChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHr125SetPointElectricPowerChannel() {
        return this.channel(ChannelId.HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION);
    }

    /**
     * Get the set point electric power consumption. Unit is watt.
     * This control mode needs configuration of some settings in the heatpump to work.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHr125SetPointElectricPower() { return this.getHr125SetPointElectricPowerChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setHr125SetPointElectricPower(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getHr125SetPointElectricPowerChannel().setNextWriteValue(value);
    }

    /**
     * Internal method.
     */
    public default void _setHr125SetPointElectricPower(int value) throws OpenemsError.OpenemsNamedException {
        this.getHr125SetPointElectricPowerChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR126_SET_POINT_VERDICHTERDREHZAHL_PERCENT}.
     *
     * @return the Channel
     */
    public default DoubleWriteChannel getHr126SetPointVerdichterdrehzahlPercentChannel() {
        return this.channel(ChannelId.HR126_SET_POINT_VERDICHTERDREHZAHL_PERCENT);
    }

    /**
     * Get Verdichterdrehzahl (=Leistung, also power percent) ist vom Webinterface begrenzt, kann dort eingestellt
     * werden. Default ist 15 - 60%.
     * Im Webinterface muss PV Modus Off eingestellt sein damit nach Drehzahl geregelt werden kann.
     * Unit is percent.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Double> getHr126SetPointVerdichterdrehzahlPercent() { return this.getHr126SetPointVerdichterdrehzahlPercentChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setHr126SetPointVerdichterdrehzahlPercent(Double value) throws OpenemsError.OpenemsNamedException {
        this.getHr126SetPointVerdichterdrehzahlPercentChannel().setNextWriteValue(value);
    }

    /**
     * Internal method.
     */
    public default void _setHr126SetPointVerdichterdrehzahlPercent(double value) throws OpenemsError.OpenemsNamedException {
        this.getHr126SetPointVerdichterdrehzahlPercentChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR128_RESET_ERROR}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getHr128ResetErrorChannel() {
        return this.channel(ChannelId.HR128_RESET_ERROR);
    }

    /**
     * Status of reset error register.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getHr128ResetError() { return this.getHr128ResetErrorChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setHr128ResetError(Boolean value) throws OpenemsError.OpenemsNamedException {
        this.getHr128ResetErrorChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR129_OUTSIDE_TEMPERATURE}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHr129OutsideTemperatureChannel() {
        return this.channel(ChannelId.HR129_OUTSIDE_TEMPERATURE);
    }

    /**
     * Get the outside temperature reading sent to the device. Unit is dezidegree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHr129OutsideTemperature() { return this.getHr129OutsideTemperatureChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setHr129OutsideTemperature(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getHr129OutsideTemperatureChannel().setNextWriteValue(value);
    }

    /**
     * Internal method.
     */
    public default void _setHr129OutsideTemperature(int value) throws OpenemsError.OpenemsNamedException {
        this.getHr129OutsideTemperatureChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR130_USE_MODBUS_OUTSIDE_TEMPERATURE}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getUseModbusOutsideTemperatureChannel() {
        return this.channel(ChannelId.HR130_USE_MODBUS_OUTSIDE_TEMPERATURE);
    }

    /**
     * Get the setting of the device to use the outside temperature value sent via Modbus.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getUseModbusOutsideTemperatureSetting() { return this.getUseModbusOutsideTemperatureChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setUseModbusOutsideTemperatureSetting(Boolean value) throws OpenemsError.OpenemsNamedException {
        this.getUseModbusOutsideTemperatureChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR131_BUFFER_TEMPERATURE}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getHr131BufferTemperatureChannel() {
        return this.channel(ChannelId.HR131_BUFFER_TEMPERATURE);
    }

    /**
     * Get the buffer temperature reading sent to the device. Unit is dezidegree Celsius.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getHr131BufferTemperature() { return this.getHr131BufferTemperatureChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setHr131BufferTemperature(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getHr131BufferTemperatureChannel().setNextWriteValue(value);
    }

    /**
     * Internal method.
     */
    public default void _setHr131BufferTemperature(int value) throws OpenemsError.OpenemsNamedException {
        this.getHr131BufferTemperatureChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR132_USE_MODBUS_BUFFER_TEMPERATURE}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getUseModbusBufferTemperatureChannel() {
        return this.channel(ChannelId.HR132_USE_MODBUS_BUFFER_TEMPERATURE);
    }

    /**
     * Get the setting of the device to use the buffer temperature value sent via Modbus.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Boolean> getUseModbusBufferTemperatureSetting() { return this.getUseModbusBufferTemperatureChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setUseModbusBufferTemperatureSetting(Boolean value) throws OpenemsError.OpenemsNamedException {
        this.getUseModbusBufferTemperatureChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR149_EVU_FREIGABE}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getEvuFreigabeChannel() {
        return this.channel(ChannelId.HR149_EVU_FREIGABE);
    }

    /**
     * Get the setting for EVU Freigabe. false = device is not allowed to run.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getEvuFreigabe() { return this.getEvuFreigabeChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setEvuFreigabe(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getEvuFreigabeChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR150_USE_MODBUS_EVU_FREIGABE}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel getUseModbusEvuFreigabeChannel() {
        return this.channel(ChannelId.HR150_USE_MODBUS_EVU_FREIGABE);
    }

    /**
     * Get the setting for use Modbus EVU Freigabe. Needs to be true for setEvuFreigabe() to work.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getUseModbusEvuFreigabeSetting() { return this.getUseModbusEvuFreigabeChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setUseModbusEvuFreigabeSetting(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getUseModbusEvuFreigabeChannel().setNextWriteValue(value);
    }


    // Non Modbus channels.

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
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getOperatingMode() { return this.getOperatingModeChannel().value(); }

    /**
     * Operating mode, set point temperature (0) or set point power percent (1). Default is set point power percent.
     */
    public default void setOperatingMode(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getOperatingModeChannel().setNextWriteValue(value);
    }

    /**
     * Operating mode, set point temperature (0) or set point power percent (1). Default is set point power percent.
     */
    public default void setOperatingMode(int value) throws OpenemsError.OpenemsNamedException {
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
     *
     * @return the Channel {@link Value}
     */
    public default Value<String> getStatusMessage() { return this.getStatusMessageChannel().value(); }

    /**
     * Internal method.
     */
    public default void _setStatusMessage(String value) {
        this.getStatusMessageChannel().setNextValue(value);
    }
}
