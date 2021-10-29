package io.openems.edge.heater.heatpump.heliotherm.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.api.Heater;

public interface HeatpumpHeliotherm extends Heater {


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers, read only. The register address is in the channel name, so IR0 means input register 0.
        // Signed 16 bit, unless stated otherwise.

        /**
         * Outside temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR10_OUTSIDE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        //IR12_FLOW_TEMPERATUR -> Heater, FLOW_TEMPERATURE, d°C. Unit at heat pump is the same.

        //IR13_RETURN_TEMPERATUR -> Heater, RETURN_TEMPERATURE, d°C. Unit at heat pump is the same.

        /**
         * Storage tank temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR14_STORAGE_TANK_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Heat pump running indicator.
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
         * Compressor speed.
         * Is limited by the webinterface setting. Default is 15 - 60%.
         * Unit from heat pump is %*10e-1, so watch the conversion!
         * <ul>
         *      <li> Type: Double
         * </ul>
         */
        IR29_READ_COMPRESSOR_SPEED(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),

        /**
         * COP, coefficient of performance. Manual says "Factor 0,1".
         * <ul>
         *      <li> Type: Double
         * </ul>
         */
        IR30_COP(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY)),

        /**
         * Demand side management (DSM) indicator (EVU Freigabe).
         * ’true’ = heat pump is allowed to run, ’false’ = heat pump blocked.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        IR32_DSM_INDICATOR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

        /**
         * Read the temperature set point the device is currently using.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        IR34_READ_TEMP_SET_POINT(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * If the pump is running, what function is requesting it.
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
        IR70_71_ELECTRIC_POWER_CONSUMPTION(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),

        /*
         * Current thermal power. Unsigned double word. Unit from heat pump is kW * 10e-1, watch the conversion!
         * <ul>
         *      <li> Type: Double
         *      <li> Unit: Kilowatt
         * </ul>
         */
        //IR74_75_CURRENT_THERMAL_POWER(Doc.of(OpenemsType.DOUBLE).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),
        // Heater, -> EFFECTIVE_HEATING_POWER


        /* Holding Registers, read/write. The register address is in the channel name, so HR0 means holding register 0.
           Signed 16 bit, unless stated otherwise.
           This heat pump has the special characteristic that modbus writes should be sent cyclical, but not more
           frequent than every 5 seconds. To hide this from the user, there are two channels for each holding register.
           The first channel is the one that should be used to send commands. The secondary channel with the name
           ’Modbus’ in it is the one actually mapped to the modbus register. It checks every 6 seconds for writes in the
           first channel and sends them if present. */

        /**
         * Operating mode. Unsigned. Allowed to write 0 - 7, 8 - 10 is only used as an indicator.
         * 0 = Off
         * 1 = Automatic
         * 2 = Cooling
         * 3 = Summer
         * 4 = Always on (Dauerbetrieb)
         * 5 = Setback mode (Absenkung)
         * 6 = Holidays, full time setback (Urlaub)
         * 7 = No night setback (Party)
         * 8 = Bake out mode (Ausheizen)
         * 9 = Electric supplier block (EVU Sperre)
         * 10 = Main switch off (Hauptschalter aus)
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR100_OPERATING_MODE(Doc.of(OperatingMode.values()).accessMode(AccessMode.READ_WRITE)),
        HR100_MODBUS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        /**
         * Set point temperature. Depends on settings which temperature it is. Can be return temperature or storage tank
         * temperature.
         * The value for the storage tank temperature can be sent to the heat pump via modbus write. As such, any
         * temperature reading can be mapped to the storage tank temperature and used as a control variable.
         * Needs register 103 set to true, otherwise this setting is ignored.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        //HR102_SET_POINT_TEMPERATURE -> Heater, SET_POINT_TEMPERATURE d°C. Unit at heat pump is the same.
        HR102_MODBUS(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Use set point temperature control mode.
         * Using temperature control mode disables the heat pump reacting to EnableSignal and ExceptionalState.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR103_USE_SET_POINT_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        HR103_MODBUS(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Use set point power percent control mode / Use set point electric power control mode.
         * Control modes power percent and electric power consumption are mutually exclusive. Which is used depends on
         * heat pump configuration. This register is then the toggle for whichever control mode the heat pump is set to.
         * Currently this flag is set automatically!
         * If the power set point (power percent or consumption) is > 0 this is set to true, otherwise false.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR117_USE_POWER_CONTROL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        HR117_MODBUS(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Set point electric power consumption.
         * This control mode only works when the heat pump is configured to ’photovoltaic mode - Modbus RTU/TCP’.
         * Needs register 117 set to true, otherwise this setting is ignored.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Watt
         * </ul>
         */
        HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)),
        HR125_MODBUS(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_WRITE)),

        /**
         * Compressor speed in percent, roughly equivalent to heating power percent.
         * Solar power mode must be set to ’off’ in the webinterface for this to be usable.
         * Range limited by setting in the webinterface, limit can be changed there. Default range is 15% - 60%.
         * Needs register 117 set to true, otherwise this setting is ignored.
         * Unit at heat pump is per mill (10e-3), watch the conversion!
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Percent
         * </ul>
         */
        //HR126_SET_POINT_COMPRESSOR_SPEED_PERCENT -> Heater, SET_POINT_HEATING_POWER_PERCENT.
        HR126_MODBUS(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH).accessMode(AccessMode.READ_WRITE)),

        // Not sure if this is needed:
        //HR127_REQUEST_BY_EXTERNAL

        /**
         * Reset error.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR128_RESET_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        HR128_MODBUS(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Send the outside temperature value to the heat pump.
         * Needs register 130 set to true, otherwise this setting is ignored.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR129_OUTSIDE_TEMPERATURE_SEND(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),
        HR129_MODBUS(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Use the outside temperature value provided via Modbus to register 129.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR130_USE_MODBUS_OUTSIDE_TEMPERATURE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        HR130_MODBUS(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Send the storage tank temperature value to the heat pump.
         * The heat pump can be set to use this value as a control parameter in temperature control mode.
         * Can be used to send any temperature reading, meaning any temperature reading can be the control parameter.
         * Needs register 132 set to true, otherwise this setting is ignored.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Decimal degree Celsius
         * </ul>
         */
        HR131_STORAGE_TANK_TEMPERATURE_SEND(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),
        HR131_MODBUS(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

        /**
         * Use the storage tank temperature value sent via Modbus to register 131.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR132_USE_MODBUS_SENT_STORAGE_TANK_TEMP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        HR132_MODBUS(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Demand side management (DSM) switch (EVU Freigabe).
         * ’true’ = heat pump is allowed to run, ’false’ = stop heat pump.
         * Needs register 150 set to true, otherwise this setting is ignored.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR149_DSM_SWITCH(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        HR149_MODBUS(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Use the DSM setting (EVU Freigabe) sent to register 149.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        HR150_USE_MODBUS_DSM_SWITCH(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        HR150_MODBUS(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));

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
     * Gets the Channel for {@link ChannelId#IR10_OUTSIDE_TEMPERATURE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOutsideTemperatureChannel() {
        return this.channel(ChannelId.IR10_OUTSIDE_TEMPERATURE);
    }

    /**
     * Read the outside temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR10_OUTSIDE_TEMPERATURE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOutsideTemperature() {
        return this.getOutsideTemperatureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR14_STORAGE_TANK_TEMPERATURE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStorageTankTemperatureChannel() {
        return this.channel(ChannelId.IR14_STORAGE_TANK_TEMPERATURE);
    }

    /**
     * Read the storage tank temperature. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR14_STORAGE_TANK_TEMPERATURE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getStorageTankTemperature() {
        return this.getStorageTankTemperatureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR25_HEATPUMP_RUNNING}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getHeatpumpRunningIndicatorChannel() {
        return this.channel(ChannelId.IR25_HEATPUMP_RUNNING);
    }

    /**
     * Heat pump running indicator.
     * See {@link ChannelId#IR25_HEATPUMP_RUNNING}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getHeatpumpRunningIndicator() {
        return this.getHeatpumpRunningIndicatorChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR26_ERROR}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getErrorIndicatorChannel() {
        return this.channel(ChannelId.IR26_ERROR);
    }

    /**
     * Error indicator.
     * See {@link ChannelId#IR26_ERROR}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getErrorIndicator() {
        return this.getErrorIndicatorChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR29_READ_COMPRESSOR_SPEED}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getCompressorSpeedChannel() {
        return this.channel(ChannelId.IR29_READ_COMPRESSOR_SPEED);
    }

    /**
     * Get the current compressor speed. Unit is percent.
     * See {@link ChannelId#IR29_READ_COMPRESSOR_SPEED}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getCompressorSpeed() {
        return this.getCompressorSpeedChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR30_COP}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getCopChannel() {
        return this.channel(ChannelId.IR30_COP);
    }

    /**
     * Get the current coefficient of performance.
     * See {@link ChannelId#IR30_COP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getCop() {
        return this.getCopChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR32_DSM_INDICATOR}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getDsmIndicatorChannel() {
        return this.channel(ChannelId.IR32_DSM_INDICATOR);
    }

    /**
     * Demand side management (DSM) indicator (EVU Freigabe).
     * ’true’ = heat pump is allowed to run, ’false’ = heat pump blocked.
     * See {@link ChannelId#IR32_DSM_INDICATOR}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getDsmIndicator() {
        return this.getDsmIndicatorChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR34_READ_TEMP_SET_POINT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getTemperatureSetPointIndicatorChannel() {
        return this.channel(ChannelId.IR34_READ_TEMP_SET_POINT);
    }

    /**
     * Read the temperature set point the device is currently using. Unit is decimal degree Celsius.
     * See {@link ChannelId#IR34_READ_TEMP_SET_POINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getTemperatureSetPointIndicator() {
        return this.getTemperatureSetPointIndicatorChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR41_RUN_REQUEST_TYPE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getRunRequestTypeChannel() {
        return this.channel(ChannelId.IR41_RUN_REQUEST_TYPE);
    }

    /**
     * If the pump is running, what function is requesting it.
     * 0 - not running
     * 10 - cooling
     * 20 - heating
     * 30 - warm water
     * See {@link ChannelId#IR41_RUN_REQUEST_TYPE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getRunRequestType() {
        return this.getRunRequestTypeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR70_71_ELECTRIC_POWER_CONSUMPTION}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getElectricPowerConsumptionChannel() {
        return this.channel(ChannelId.IR70_71_ELECTRIC_POWER_CONSUMPTION);
    }

    /**
     * Get the current electric power consumption. Unit is watt.
     * See {@link ChannelId#IR70_71_ELECTRIC_POWER_CONSUMPTION}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getElectricPowerConsumption() {
        return this.getElectricPowerConsumptionChannel().value();
    }


    /* Holding Registers. Read/write.
       Writes to the heat pump should be sent every 5s, but not more often than that. Because of this, there are two
       write channels for every register. The primary one is not mapped to a register and can be written to without
       restrictions. The secondary one is mapped to a register and gets the write values from the primary channel every
       6 seconds and sends them to the heat pump. */

    /**
     * Gets the Channel for {@link ChannelId#HR100_OPERATING_MODE}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr100OperatingModeChannel() {
        return this.channel(ChannelId.HR100_OPERATING_MODE);
    }

    /**
     * Get the operating mode. Allowed to write 0 - 7, 8 - 10 is only used as an indicator.
     * 0 = Off
     * 1 = Automatic
     * 2 = Cooling
     * 3 = Summer
     * 4 = Always on (Dauerbetrieb)
     * 5 = Setback mode (Absenkung)
     * 6 = Holidays, full time setback (Urlaub)
     * 7 = No night setback (Party)
     * 8 = Bake out mode (Ausheizen)
     * 9 = Demand side management block (EVU Sperre)
     * 10 = Main switch off (Hauptschalter aus)
     * See {@link ChannelId#HR100_OPERATING_MODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHr100OperatingMode() {
        return this.getHr100OperatingModeChannel().value();
    }

    /**
     * Set the operating mode. Allowed to write 0 - 7, 8 - 10 is only used as an indicator.
     * 0 = Off
     * 1 = Automatic
     * 2 = Cooling
     * 3 = Summer
     * 4 = Always on (Dauerbetrieb)
     * 5 = Setback mode (Absenkung)
     * 6 = Holidays, full time setback (Urlaub)
     * 7 = No night setback (Party)
     * 8 = Bake out mode (Ausheizen)
     * 9 = Demand side management block (EVU Sperre)
     * 10 = Main switch off (Hauptschalter aus)
     * See {@link ChannelId#HR100_OPERATING_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr100OperatingMode(Integer value) throws OpenemsNamedException {
        if (value <= 7 && value >= 0) {
            this.getHr100OperatingModeChannel().setNextWriteValue(value);
        }
    }

    /**
     * Set the operating mode. Allowed to write 0 - 7, 8 - 10 is only used as an indicator.
     * 0 = Off
     * 1 = Automatic
     * 2 = Cooling
     * 3 = Summer
     * 4 = Always on (Dauerbetrieb)
     * 5 = Setback mode (Absenkung)
     * 6 = Holidays, full time setback (Urlaub)
     * 7 = No night setback (Party)
     * 8 = Bake out mode (Ausheizen)
     * 9 = Demand side management block (EVU Sperre)
     * 10 = Main switch off (Hauptschalter aus)
     * See {@link ChannelId#HR100_OPERATING_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr100OperatingMode(int value) throws OpenemsNamedException {
        if (value <= 7 && value >= 0) {
            this.getHr100OperatingModeChannel().setNextWriteValue(value);
        }
    }

    /**
     * Set the operating mode. Allowed to write 0 - 7, 8 - 10 is only used as an indicator.
     * 0 = Off
     * 1 = Automatic
     * 2 = Cooling
     * 3 = Summer
     * 4 = Always on (Dauerbetrieb)
     * 5 = Setback mode (Absenkung)
     * 6 = Holidays, full time setback (Urlaub)
     * 7 = No night setback (Party)
     * 8 = Bake out mode (Ausheizen)
     * 9 = Demand side management block (EVU Sperre)
     * 10 = Main switch off (Hauptschalter aus)
     * See {@link ChannelId#HR100_OPERATING_MODE}.
     *
     * @param mode the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr100OperatingMode(OperatingMode mode) throws OpenemsNamedException {
        if (mode != null && mode.getValue() <= 7 && mode.getValue() >= 0) {
            this.getHr100OperatingModeChannel().setNextWriteValue(mode.getValue());
        }
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR100_MODBUS}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr100ModbusChannel() {
        return this.channel(ChannelId.HR100_MODBUS);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR102_MODBUS}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr102ModbusChannel() {
        return this.channel(ChannelId.HR102_MODBUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR103_USE_SET_POINT_TEMPERATURE}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr103UseSetPointTemperatureChannel() {
        return this.channel(ChannelId.HR103_USE_SET_POINT_TEMPERATURE);
    }

    /**
     * Get the ’use set point temperature’ control mode setting.
     * Using temperature control mode disables the heat pump reacting to EnableSignal and ExceptionalState.
     * Gets the Channel for {@link ChannelId#HR103_USE_SET_POINT_TEMPERATURE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getHr103UseSetPointTemperature() {
        return this.getHr103UseSetPointTemperatureChannel().value();
    }

    /**
     * Set the ’use set point temperature’ control mode setting.
     * Using temperature control mode disables the heat pump reacting to EnableSignal and ExceptionalState.
     * See {@link ChannelId#HR103_USE_SET_POINT_TEMPERATURE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr103UseSetPointTemperature(Boolean value) throws OpenemsNamedException {
        this.getHr103UseSetPointTemperatureChannel().setNextWriteValue(value);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR103_MODBUS}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr103ModbusChannel() {
        return this.channel(ChannelId.HR103_MODBUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR117_USE_POWER_CONTROL}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr117UsePowerControlChannel() {
        return this.channel(ChannelId.HR117_USE_POWER_CONTROL);
    }

    /**
     * Get the setting for ’use power control’.
     * This is either ’power percent control mode’ or ’power consumption control mode’, depending on the heat pump
     * configuration.
     * Currently this flag is set automatically!
     * If the power set point (power percent or consumption) is > 0 this is set to true, otherwise false.
     * See {@link ChannelId#HR117_USE_POWER_CONTROL}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getHr117UsePowerControl() {
        return this.getHr117UsePowerControlChannel().value();
    }

    /**
     * Currently disabled!
     * Set the setting for ’use power control’.
     * This is either ’power percent control mode’ or ’power consumption control mode’, depending on the heat pump
     * configuration.
     * See {@link ChannelId#HR117_USE_POWER_CONTROL}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr117UsePowerControl(Boolean value) throws OpenemsNamedException {
        this.getHr117UsePowerControlChannel().setNextWriteValue(value);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR117_MODBUS}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr117ModbusChannel() {
        return this.channel(ChannelId.HR117_MODBUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr125SetPointElectricPowerChannel() {
        return this.channel(ChannelId.HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION);
    }

    /**
     * Get the set point electric power consumption. Unit is watt.
     * This control mode only works when the heat pump is configured to ’photovoltaic mode - Modbus RTU/TCP’.
     * See {@link ChannelId#HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHr125SetPointElectricPower() {
        return this.getHr125SetPointElectricPowerChannel().value();
    }

    /**
     * Set the set point electric power consumption. Unit is watt.
     * This control mode only works when the heat pump is configured to ’photovoltaic mode - Modbus RTU/TCP’.
     * See {@link ChannelId#HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr125SetPointElectricPower(Integer value) throws OpenemsNamedException {
        this.getHr125SetPointElectricPowerChannel().setNextWriteValue(value);
    }

    /**
     * Set the set point electric power consumption. Unit is watt.
     * This control mode only works when the heat pump is configured to ’photovoltaic mode - Modbus RTU/TCP’.
     * See {@link ChannelId#HR125_SET_POINT_ELECTRIC_POWER_CONSUMPTION}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr125SetPointElectricPower(int value) throws OpenemsNamedException {
        this.getHr125SetPointElectricPowerChannel().setNextWriteValue(value);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR125_MODBUS}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr125ModbusChannel() {
        return this.channel(ChannelId.HR125_MODBUS);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR126_MODBUS}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr126ModbusChannel() {
        return this.channel(ChannelId.HR126_MODBUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR128_RESET_ERROR}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr128ResetErrorChannel() {
        return this.channel(ChannelId.HR128_RESET_ERROR);
    }

    /**
     * Get the status of the ’reset error’ register.
     * See {@link ChannelId#HR128_RESET_ERROR}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getHr128ResetError() {
        return this.getHr128ResetErrorChannel().value();
    }

    /**
     * Set the ’reset error’ register.
     * See {@link ChannelId#HR128_RESET_ERROR}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr128ResetError(Boolean value) throws OpenemsNamedException {
        this.getHr128ResetErrorChannel().setNextWriteValue(value);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR128_MODBUS}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr128ModbusChannel() {
        return this.channel(ChannelId.HR128_MODBUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR129_OUTSIDE_TEMPERATURE_SEND}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr129OutsideTemperatureSendChannel() {
        return this.channel(ChannelId.HR129_OUTSIDE_TEMPERATURE_SEND);
    }

    /**
     * Get the outside temperature value that is sent to the device. Unit is decimal degree Celsius.
     * Needs register 130 set to true, otherwise this value is ignored.
     * See {@link ChannelId#HR129_OUTSIDE_TEMPERATURE_SEND}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHr129OutsideTemperatureSend() {
        return this.getHr129OutsideTemperatureSendChannel().value();
    }

    /**
     * Set the outside temperature value that is sent to the device. Unit is decimal degree Celsius.
     * Needs register 130 set to true, otherwise this value is ignored.
     * See {@link ChannelId#HR129_OUTSIDE_TEMPERATURE_SEND}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr129OutsideTemperatureSend(Integer value) throws OpenemsNamedException {
        this.getHr129OutsideTemperatureSendChannel().setNextWriteValue(value);
    }

    /**
     * Set the outside temperature value that is sent to the device. Unit is decimal degree Celsius.
     * Needs register 130 set to true, otherwise this value is ignored.
     * See {@link ChannelId#HR129_OUTSIDE_TEMPERATURE_SEND}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr129OutsideTemperatureSend(int value) throws OpenemsNamedException {
        this.getHr129OutsideTemperatureSendChannel().setNextWriteValue(value);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR129_MODBUS}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr129ModbusChannel() {
        return this.channel(ChannelId.HR129_MODBUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR130_USE_MODBUS_OUTSIDE_TEMPERATURE}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr130UseModbusOutsideTemperatureSettingChannel() {
        return this.channel(ChannelId.HR130_USE_MODBUS_OUTSIDE_TEMPERATURE);
    }

    /**
     * Get the heat pump setting for ’use value in register 129 as the outside temperature’.
     * See {@link ChannelId#HR130_USE_MODBUS_OUTSIDE_TEMPERATURE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getHr130UseModbusOutsideTemperatureSetting() {
        return this.getHr130UseModbusOutsideTemperatureSettingChannel().value();
    }

    /**
     * Set the heat pump to use the temperature value sent to register 129 as the outside temperature.
     * ’true’ - use the value in register 129.
     * ’false’ - use the value from the connected temperature sensor.
     * See {@link ChannelId#HR130_USE_MODBUS_OUTSIDE_TEMPERATURE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr130UseModbusOutsideTemperatureSetting(Boolean value) throws OpenemsNamedException {
        this.getHr130UseModbusOutsideTemperatureSettingChannel().setNextWriteValue(value);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR130_MODBUS}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr130ModbusChannel() {
        return this.channel(ChannelId.HR130_MODBUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR131_STORAGE_TANK_TEMPERATURE_SEND}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr131StorageTankTemperatureSendChannel() {
        return this.channel(ChannelId.HR131_STORAGE_TANK_TEMPERATURE_SEND);
    }

    /**
     * Get the storage tank temperature value sent to the device. Unit is decimal degree Celsius.
     * Needs register 132 set to true, otherwise this setting is ignored.
     * See {@link ChannelId#HR131_STORAGE_TANK_TEMPERATURE_SEND}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHr131StorageTankTemperatureSend() {
        return this.getHr131StorageTankTemperatureSendChannel().value();
    }

    /**
     * Set the storage tank temperature value that is sent to the heat pump. Unit is decimal degree Celsius.
     * The heat pump can be set to use this value as a control parameter in temperature control mode.
     * Can be used to send any temperature reading, meaning any temperature reading can be the control parameter.
     * Needs register 132 set to true, otherwise this setting is ignored.
     * See {@link ChannelId#HR131_STORAGE_TANK_TEMPERATURE_SEND}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr131StorageTankTemperature(Integer value) throws OpenemsNamedException {
        this.getHr131StorageTankTemperatureSendChannel().setNextWriteValue(value);
    }

    /**
     * Set the storage tank temperature value that is sent to the heat pump. Unit is decimal degree Celsius.
     * The heat pump can be set to use this value as a control parameter in temperature control mode.
     * Can be used to send any temperature reading, meaning any temperature reading can be the control parameter.
     * Needs register 132 set to true, otherwise this setting is ignored.
     * See {@link ChannelId#HR131_STORAGE_TANK_TEMPERATURE_SEND}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr131StorageTankTemperature(int value) throws OpenemsNamedException {
        this.getHr131StorageTankTemperatureSendChannel().setNextWriteValue(value);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR131_MODBUS}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHr131ModbusChannel() {
        return this.channel(ChannelId.HR131_MODBUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR132_USE_MODBUS_SENT_STORAGE_TANK_TEMP}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr132UseModbusStorageTankTemperatureSettingChannel() {
        return this.channel(ChannelId.HR132_USE_MODBUS_SENT_STORAGE_TANK_TEMP);
    }

    /**
     * Get the heat pump setting for ’use value in register 131 as the storage tank temperature’.
     * See {@link ChannelId#HR132_USE_MODBUS_SENT_STORAGE_TANK_TEMP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getHr132UseModbusStorageTankTemperatureSetting() {
        return this.getHr132UseModbusStorageTankTemperatureSettingChannel().value();
    }

    /**
     * Set the heat pump to use the temperature value sent to register 131 as the storage tank temperature.
     * ’true’ - use the value in register 131.
     * ’false’ - use the value from the connected temperature sensor.
     * See {@link ChannelId#HR132_USE_MODBUS_SENT_STORAGE_TANK_TEMP}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr132UseModbusStorageTankTemperatureSetting(Boolean value) throws OpenemsNamedException {
        this.getHr132UseModbusStorageTankTemperatureSettingChannel().setNextWriteValue(value);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR132_MODBUS}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr132ModbusChannel() {
        return this.channel(ChannelId.HR132_MODBUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR149_DSM_SWITCH}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr149DsmSwitchChannel() {
        return this.channel(ChannelId.HR149_DSM_SWITCH);
    }

    /**
     * Gets the demand side management (DSM) status (EVU Freigabe).
     * ’true’ = heat pump is allowed to run, ’false’ = stop heat pump.
     * Needs register 150 set to true, otherwise this setting is ignored.
     * See {@link ChannelId#HR149_DSM_SWITCH}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getHr149DsmSwitch() {
        return this.getHr149DsmSwitchChannel().value();
    }

    /**
     * Sets the demand side management (DSM) status (EVU Freigabe).
     * ’true’ = heat pump is allowed to run, ’false’ = stop heat pump.
     * Needs register 150 set to true, otherwise this setting is ignored.
     * See {@link ChannelId#HR149_DSM_SWITCH}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr149DsmSwitch(Boolean value) throws OpenemsNamedException {
        this.getHr149DsmSwitchChannel().setNextWriteValue(value);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR149_MODBUS}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr149ModbusChannel() {
        return this.channel(ChannelId.HR149_MODBUS);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR150_USE_MODBUS_DSM_SWITCH}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr150UseModbusDsmSwitchChannel() {
        return this.channel(ChannelId.HR150_USE_MODBUS_DSM_SWITCH);
    }

    /**
     * Get the heat pump setting for ’use the DSM setting (EVU Freigabe) sent to register 149’.
     * See {@link ChannelId#HR150_USE_MODBUS_DSM_SWITCH}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getHr150UseModbusDsmSwitch() {
        return this.getHr150UseModbusDsmSwitchChannel().value();
    }

    /**
     * Sets the heat pump to use the DSM setting (EVU Freigabe) sent to register 149.
     * See {@link ChannelId#HR150_USE_MODBUS_DSM_SWITCH}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHr150UseModbusDsmSwitch(Boolean value) throws OpenemsNamedException {
        this.getHr150UseModbusDsmSwitchChannel().setNextWriteValue(value);
    }

    /**
     * For internal use only!
     * Gets the Channel for {@link ChannelId#HR150_MODBUS}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getHr150ModbusChannel() {
        return this.channel(ChannelId.HR150_MODBUS);
    }
}
