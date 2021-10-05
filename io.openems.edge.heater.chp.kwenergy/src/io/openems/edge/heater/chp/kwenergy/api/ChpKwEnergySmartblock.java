package io.openems.edge.heater.chp.kwenergy.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.heater.api.Chp;

/**
 * Channels for the KW Energy Smartblock chp.
 */
public interface ChpKwEnergySmartblock extends Chp {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Holding Registers, read/write. The register address is in the channel name, so HR0 means holding register 0.
        // Unsigned 16 bit, unless stated otherwise.

        // This CHP maps all values to holding registers no matter if they are read/write or read only. The following
        // registers are read only values.

        /**
         * Error bits 1 - 16.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR0_ERROR_BITS_1_to_16(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Status bits 1 - 16.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR16_STATUS_BITS_1_to_16(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Status bits 65 - 80.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR20_STATUS_BITS_65_to_80(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Engine temperature.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Dezidegree Celsius
         * </ul>
         */
        HR24_ENGINE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DECIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        // HR25_RETURN_TEMPERATURE -> Heater, RETURN_TEMPERATURE, d°C. Value from CHP is same unit.

        // HR26_FLOW_TEMPERATURE -> Heater, FLOW_TEMPERATURE, d°C. Value from CHP is same unit.

        /**
         * Engine rotations per minute. Watch the conversion, the value coming from the device is rpm*10!
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: RPM
         * </ul>
         */
        HR31_ENGINE_RPM(Doc.of(OpenemsType.INTEGER).unit(Unit.ROTATION_PER_MINUTE).accessMode(AccessMode.READ_ONLY)),

        /**
         * Effective electric power. Watch the conversion, the value coming from the device is kW*10!
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Kilowatt*10
         * </ul>
         */
        HR34_EFFECTIVE_ELECTRIC_POWER(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * CHP model/type identifier.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR48_CHP_MODEL(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Operating hours. Double word value (HR62 high, HR63 low).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Hours
         * </ul>
         */
        HR62_OPERATING_HOURS(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR).accessMode(AccessMode.READ_ONLY)),

        /**
         * Counter of engine starts. Double word value (HR64 high, HR65 low).
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR64_ENGINE_START_COUNTER(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Produced active energy of the CHP (Wirkarbeit). Double word value (HR70 high, HR71 low).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Kilowatt hours
         * </ul>
         */
        HR70_ACTIVE_ENERGY(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT_HOURS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Maintenance interval 1.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Hours
         * </ul>
         */
        HR72_MAINTENANCE_INTERVAL1(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR).accessMode(AccessMode.READ_ONLY)),

        /**
         * Maintenance interval 2.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Hours
         * </ul>
         */
        HR73_MAINTENANCE_INTERVAL2(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR).accessMode(AccessMode.READ_ONLY)),

        /**
         * Maintenance interval 3.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Hours
         * </ul>
         */
        HR74_MAINTENANCE_INTERVAL3(Doc.of(OpenemsType.INTEGER).unit(Unit.HOUR).accessMode(AccessMode.READ_ONLY)),

        /**
         * Produced heat energy of the CHP (Waermemenge). Double word value (HR75 high, HR76 low).
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Kilowatt
         * </ul>
         */
        HR75_PRODUCED_HEAT(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),

        /**
         * Operating mode of the CHP. Unfortunately, information provided by the manual is wrong, so cannot parse what
         * the transmitted code means.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR81_OPERATING_MODE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Set point of the CHP power.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: Kilowatt
         * </ul>
         */
        HR82_POWER_SETPOINT(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),

        /**
         * Handshake counter out.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR108_HANDSHAKE_OUT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),


        // This CHP maps all values to holding registers no matter if they are read/write or read only. The following
        // registers are read/write values.

        /**
         * Command bits 1 - 16.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR109_COMMAND_BITS_1_to_16(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),

        // HR111_SET_POINT_POWER_PERCENT -> Heater, SET_POINT_POWER_PERCENT, percent. Value from CHP is percent*10, watch the conversion!

        /**
         * Grid power draw (Netzbezugswert). Watch the conversion, the value in the device is kW*10!
         * <ul>
         *      <li> Type: Double
         *      <li> Unit: Kilowatt
         * </ul>
         */
        HR112_GRID_POWER_DRAW(Doc.of(OpenemsType.DOUBLE).unit(Unit.KILOWATT).accessMode(AccessMode.READ_WRITE)),

        /**
         * Handshake counter in.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */
        HR119_HANDSHAKE_IN(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),


        // Non Modbus channels

        /**
         * Status of the CHP.
         * <ul>
         *      <li> Type: String
         * </ul>
         */
        STATUS_MESSAGE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),

        /**
         * Control mode of the CHP.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: -1, 0 ... 2
         *      <li> State -1: Undefined
         *      <li> State 0: Control mode power percent
         *      <li> State 1: Control mode electric power
         *      <li> State 2: Control mode consumption
         * </ul>
         */
        CONTROL_MODE(Doc.of(ControlMode.values()).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((EnumWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),

        // Channels for telemetry

        /**
         * Error status of CHP. False = no error.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        CHP_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

        /**
         * Release status of CHP. False = blocked.
         * Mapped to HR16, bit 4 ("Anforderung steht an").
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        CHP_RELEASE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

        /**
         * Engine status of CHP. False = off.
         * Mapped to HR16, bit 7 ("Motor laeuft").
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        CHP_ENGINE_RUNNING(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));


        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }


    /**
     * Gets the Channel for {@link ChannelId#HR0_ERROR_BITS_1_to_16}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getErrorBits1to16Channel() {
        return this.channel(ChannelId.HR0_ERROR_BITS_1_to_16);
    }

    /**
     * Gets the error bits 1 - 16.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getErrorBits1to16() {
        return this.getErrorBits1to16Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR16_STATUS_BITS_1_to_16}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStatusBits1to16Channel() {
        return this.channel(ChannelId.HR16_STATUS_BITS_1_to_16);
    }

    /**
     * Gets the status bits 1 - 16.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getStatusBits1to16() {
        return this.getStatusBits1to16Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR20_STATUS_BITS_65_to_80}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getStatusBits65to80Channel() {
        return this.channel(ChannelId.HR20_STATUS_BITS_65_to_80);
    }

    /**
     * Gets the status bits 65 - 80.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getStatusBits65to80() {
        return this.getStatusBits65to80Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR24_ENGINE_TEMPERATURE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getEngineTemperatureChannel() {
        return this.channel(ChannelId.HR24_ENGINE_TEMPERATURE);
    }

    /**
     * Gets the engine temperature.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getEngineTemperature() {
        return this.getEngineTemperatureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR31_ENGINE_RPM}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getEngineRpmChannel() {
        return this.channel(ChannelId.HR31_ENGINE_RPM);
    }

    /**
     * Gets the engine RPM.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getEngineRpm() {
        return this.getEngineRpmChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR34_EFFECTIVE_ELECTRIC_POWER}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getModbusEffectiveElectricPowerChannel() {
        return this.channel(ChannelId.HR34_EFFECTIVE_ELECTRIC_POWER);
    }

    /**
     * Gets the Modbus value effective electric power. Unit is kW * 10
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getModbusEffectiveElectricPower() {
        return this.getModbusEffectiveElectricPowerChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR48_CHP_MODEL}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getChpModelChannel() {
        return this.channel(ChannelId.HR48_CHP_MODEL);
    }

    /**
     * Gets the CHP model/type identifier.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getChpModel() {
        return this.getChpModelChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR62_OPERATING_HOURS}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOperatingHoursChannel() {
        return this.channel(ChannelId.HR62_OPERATING_HOURS);
    }

    /**
     * Gets the operating hours.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOperatingHours() {
        return this.getOperatingHoursChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR64_ENGINE_START_COUNTER}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getEngineStartCounterChannel() {
        return this.channel(ChannelId.HR64_ENGINE_START_COUNTER);
    }

    /**
     * Gets the counter for the engine starts.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getEngineStartCounter() {
        return this.getEngineStartCounterChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR70_ACTIVE_ENERGY}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getActiveEnergyChannel() {
        return this.channel(ChannelId.HR70_ACTIVE_ENERGY);
    }

    /**
     * Gets the produced active energy of the CHP (Wirkarbeit).
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getActiveEnergy() {
        return this.getActiveEnergyChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR72_MAINTENANCE_INTERVAL1}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getMaintenanceInterval1Channel() {
        return this.channel(ChannelId.HR72_MAINTENANCE_INTERVAL1);
    }

    /**
     * Gets the maintenance interval 1. Unit is hours.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getMaintenanceInterval1() {
        return this.getMaintenanceInterval1Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR73_MAINTENANCE_INTERVAL2}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getMaintenanceInterval2Channel() {
        return this.channel(ChannelId.HR73_MAINTENANCE_INTERVAL2);
    }

    /**
     * Gets the maintenance interval 2. Unit is hours.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getMaintenanceInterval2() {
        return this.getMaintenanceInterval2Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR74_MAINTENANCE_INTERVAL3}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getMaintenanceInterval3Channel() {
        return this.channel(ChannelId.HR74_MAINTENANCE_INTERVAL3);
    }

    /**
     * Gets the maintenance interval 3. Unit is hours.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getMaintenanceInterval3() {
        return this.getMaintenanceInterval3Channel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR75_PRODUCED_HEAT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getProducedHeatChannel() {
        return this.channel(ChannelId.HR75_PRODUCED_HEAT);
    }

    /**
     * Gets the produced heat energy of the CHP (Waermemenge). Unit is kilowatt.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getProducedHeat() {
        return this.getProducedHeatChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR81_OPERATING_MODE}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getOperatingModeChannel() {
        return this.channel(ChannelId.HR81_OPERATING_MODE);
    }

    /**
     * Gets the operating mode. (Can't parse code, info in manual is wrong.)
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getOperatingMode() {
        return this.getOperatingModeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR82_POWER_SETPOINT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getPowerSetpointChannel() {
        return this.channel(ChannelId.HR82_POWER_SETPOINT);
    }

    /**
     * Gets the set point of the CHP power.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getPowerSetpoint() {
        return this.getPowerSetpointChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR108_HANDSHAKE_OUT}.
     *
     * @return the Channel
     */
    default IntegerReadChannel getHandshakeOutChannel() {
        return this.channel(ChannelId.HR108_HANDSHAKE_OUT);
    }

    /**
     * Gets the handshake counter coming from the device.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHandshakeOut() {
        return this.getHandshakeOutChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#HR109_COMMAND_BITS_1_to_16}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getCommandBits1to16Channel() {
        return this.channel(ChannelId.HR109_COMMAND_BITS_1_to_16);
    }

//    Makes no sense to read the command bits.
//    /**
//     * Gets the command bits 1 - 16.
//     *
//     * @return the Channel {@link Value}
//     */
//    default Value<Integer> getCommandBits1to16() {
//        return this.getCommandBits1to16Channel().value();
//    }

    /**
     * Sets the command bits 1 - 16.
     * See {@link ChannelId#HR109_COMMAND_BITS_1_to_16}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setCommandBits1to16(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getCommandBits1to16Channel().setNextWriteValue(value);
    }

    /**
     * Sets the command bits 1 - 16.
     * See {@link ChannelId#HR109_COMMAND_BITS_1_to_16}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setCommandBits1to16(int value) throws OpenemsError.OpenemsNamedException {
        this.getCommandBits1to16Channel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR112_GRID_POWER_DRAW}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getGridPowerDrawSetpointChannel() {
        return this.channel(ChannelId.HR112_GRID_POWER_DRAW);
    }

    /**
     * Gets the grid power draw set point.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getGridPowerDrawSetpoint() {
        return this.getGridPowerDrawSetpointChannel().value();
    }

    /**
     * Sets grid power draw set point.
     * See {@link ChannelId#HR112_GRID_POWER_DRAW}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setGridPowerDrawSetpoint(Double value) throws OpenemsError.OpenemsNamedException {
        this.getGridPowerDrawSetpointChannel().setNextWriteValue(value);
    }

    /**
     * Sets grid power draw set point.
     * See {@link ChannelId#HR112_GRID_POWER_DRAW}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setGridPowerDrawSetpoint(double value) throws OpenemsError.OpenemsNamedException {
        this.getGridPowerDrawSetpointChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#HR119_HANDSHAKE_IN}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getHandshakeInChannel() {
        return this.channel(ChannelId.HR119_HANDSHAKE_IN);
    }

    /**
     * Gets the handshake counter going to the device.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getHandshakeIn() {
        return this.getHandshakeInChannel().value();
    }

    /**
     * Sets the handshake counter going to the device.
     * See {@link ChannelId#HR119_HANDSHAKE_IN}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHandshakeIn(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getHandshakeInChannel().setNextWriteValue(value);
    }

    /**
     * Sets the handshake counter going to the device.
     * See {@link ChannelId#HR119_HANDSHAKE_IN}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setHandshakeIn(int value) throws OpenemsError.OpenemsNamedException {
        this.getHandshakeInChannel().setNextWriteValue(value);
    }

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

    /**
     * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
     *
     * @return the Channel
     */
    default EnumWriteChannel getControlModeChannel() {
        return this.channel(ChannelId.CONTROL_MODE);
    }

    /**
     * Gets the control mode of the CHP.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 2
     *      <li> State -1: Undefined
     *      <li> State 0: Control mode power percent
     *      <li> State 1: Control mode electric power
     *      <li> State 2: Control mode consumption
     * </ul>
     * See {@link ChannelId#CONTROL_MODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getControlMode() {
        return this.getControlModeChannel().value();
    }

    /**
     * Sets the control mode of the CHP.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 2
     *      <li> State -1: Undefined
     *      <li> State 0: Control mode power percent
     *      <li> State 1: Control mode electric power
     *      <li> State 2: Control mode consumption
     * </ul>
     * See {@link ChannelId#CONTROL_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setControlMode(int value) throws OpenemsError.OpenemsNamedException {
        this.getControlModeChannel().setNextWriteValue(value);
    }

    /**
     * Sets the control mode of the CHP.
     * <ul>
     *      <li> Type: Integer
     *      <li> Possible values: -1, 0 ... 2
     *      <li> State -1: Undefined
     *      <li> State 0: Control mode power percent
     *      <li> State 1: Control mode electric power
     *      <li> State 2: Control mode consumption
     * </ul>
     * See {@link ChannelId#CONTROL_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setControlMode(Integer value) throws OpenemsError.OpenemsNamedException {
        this.getControlModeChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#CHP_ERROR}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getChpErrorChannel() {
        return this.channel(ChannelId.CHP_ERROR);
    }

    /**
     * Gets the error status. False = no error.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getChpError() {
        return this.getChpErrorChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#CHP_ERROR} Channel.
     *
     * @param value the next value
     */
    default void _setChpError(Boolean value) {
        this.getChpErrorChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#CHP_RELEASE}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getChpReleaseChannel() {
        return this.channel(ChannelId.CHP_RELEASE);
    }

    /**
     * Gets the chp release status. False = blocked.
     * Mapped to HR16, bit 4 ("Anforderung steht an").
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getChpRelease() {
        return this.getChpReleaseChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#CHP_RELEASE} Channel.
     *
     * @param value the next value
     */
    default void _setChpRelease(Boolean value) {
        this.getChpReleaseChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#CHP_ENGINE_RUNNING}.
     *
     * @return the Channel
     */
    default BooleanReadChannel getChpEngineRunningChannel() {
        return this.channel(ChannelId.CHP_ENGINE_RUNNING);
    }

    /**
     * Gets the engine status of the CHP. False = off.
     * Mapped to HR16, bit 7 ("Motor laeuft").
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getChpEngineRunning() {
        return this.getChpEngineRunningChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#CHP_ENGINE_RUNNING} Channel.
     *
     * @param value the next value
     */
    default void _setChpEngineRunning(Boolean value) {
        this.getChpEngineRunningChannel().setNextValue(value);
    }
}
