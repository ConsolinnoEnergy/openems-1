package io.openems.edge.controller.heatnetwork.pump.grundfos.api;


import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pump.grundfos.api.PumpGrundfos;

public interface PumpGrundfosController extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * The control mode of the pump.
         * <ul>
         *      <li> Type: Enum, Integer
         *      <li> Possible values: 0 ... 5
         *      <li> State 0: Constant pressure
         *      <li> State 1: Constant frequency
         *      <li> State 2: Minimum motor curve
         *      <li> State 3: Maximum motor curve
         *      <li> State 4: Auto adapt
         * </ul>
         */
        CONTROL_MODE(Doc.of(ControlModeSetting.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Pump stop command.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        STOP_PUMP(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * The pump pressure set point.
         * <ul>
         *      <li> Type: Double
         *      <li> Unit: Bar
         * </ul>
         */
        PRESSURE_SETPOINT(Doc.of(OpenemsType.DOUBLE).unit(Unit.BAR).accessMode(AccessMode.READ_WRITE)),

        /**
         * The pump motor speed set point.
         * <ul>
         *      <li> Type: Double
         *      <li> Unit: Percent
         * </ul>
         */
        MOTOR_SPEED_SETPOINT(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),

        /**
         * Read only mode switch.
         * <ul>
         *      <li> Type: Boolean
         * </ul>
         */
        READ_ONLY_SETTING(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#CONTROL_MODE}.
     *
     * @return the Channel
     */
    default EnumWriteChannel getControlModeChannel() {
        return channel(ChannelId.CONTROL_MODE);
    }

    /**
     * Gets the control mode of the pump.
     * <ul>
     *      <li> Type: Enum, Integer
     *      <li> Possible values: 0 ... 5
     *      <li> State 0: Constant pressure
     *      <li> State 1: Constant frequency
     *      <li> State 2: Minimum motor curve
     *      <li> State 3: Maximum motor curve
     *      <li> State 4: Auto adapt
     * </ul>
     * See {@link ChannelId#CONTROL_MODE}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Integer> getControlMode() {
        return this.getControlModeChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link PumpGrundfos.ChannelId#CONTROL_MODE} Channel.
     *
     * @param mode the next write value
     */
    default void _setControlMode(ControlModeSetting mode) {
        if (mode != null && mode != ControlModeSetting.UNDEFINED) {
            this.getControlModeChannel().setNextValue(mode.getValue());
        }
    }

    /**
     * Set the control mode of the pump.
     * <ul>
     *      <li> Type: Enum, Integer
     *      <li> Possible values: 0 ... 5
     *      <li> State 0: Constant pressure
     *      <li> State 1: Constant frequency
     *      <li> State 2: Minimum motor curve
     *      <li> State 3: Maximum motor curve
     *      <li> State 4: Auto adapt
     * </ul>
     * See {@link ChannelId#CONTROL_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setControlMode(int value) throws OpenemsNamedException {
        this.getControlModeChannel().setNextWriteValue(value);
    }

    /**
     * Set the control mode of the pump.
     * <ul>
     *      <li> Type: Enum, Integer
     *      <li> Possible values: 0 ... 5
     *      <li> State 0: Constant pressure
     *      <li> State 1: Constant frequency
     *      <li> State 2: Minimum motor curve
     *      <li> State 3: Maximum motor curve
     *      <li> State 4: Auto adapt
     * </ul>
     * See {@link ChannelId#CONTROL_MODE}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setControlMode(Integer value) throws OpenemsNamedException {
        this.getControlModeChannel().setNextWriteValue(value);
    }

    /**
     * Set the control mode of the pump.
     * <ul>
     *      <li> Type: Enum, Integer
     *      <li> Possible values: 0 ... 5
     *      <li> State 0: Constant pressure
     *      <li> State 1: Constant frequency
     *      <li> State 2: Minimum motor curve
     *      <li> State 3: Maximum motor curve
     *      <li> State 4: Auto adapt
     * </ul>
     * See {@link ChannelId#CONTROL_MODE}.
     *
     * @param mode the next write value
     * @throws OpenemsNamedException on error
     */
    default void setControlMode(ControlModeSetting mode) throws OpenemsNamedException {
        if (mode != null && mode != ControlModeSetting.UNDEFINED) {
            this.getControlModeChannel().setNextWriteValue(mode.getValue());
        }
    }

    /**
     * Gets the Channel for {@link ChannelId#STOP_PUMP}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getStopPumpChannel() {
        return channel(ChannelId.STOP_PUMP);
    }

    /**
     * Gets the state of the pump stop command.
     * See {@link ChannelId#STOP_PUMP}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getStopPump() {
        return this.getStopPumpChannel().value();
    }

    /**
     * Turn the pump on or off. True = on, false = off.
     * See {@link ChannelId#STOP_PUMP}.
     *
     * @param value the next write value
     */
    default void _setStopPump(Boolean value) {
        this.getStopPumpChannel().setNextValue(value);
    }

    /**
     * Turn the pump on or off. True = on, false = off.
     * See {@link ChannelId#STOP_PUMP}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setStopPump(Boolean value) throws OpenemsNamedException {
        this.getStopPumpChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#PRESSURE_SETPOINT}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getPressureSetpointChannel() {
        return channel(ChannelId.PRESSURE_SETPOINT);
    }

    /**
     * Gets the pump pressure set point, unit is bar.
     * See {@link ChannelId#PRESSURE_SETPOINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getPressureSetpoint() {
        return this.getPressureSetpointChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#PRESSURE_SETPOINT} Channel.
     *
     * @param value the next value
     */
    default void _setPressureSetpoint(Double value) {
        this.getPressureSetpointChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#PRESSURE_SETPOINT} Channel.
     *
     * @param value the next value
     */
    default void _setPressureSetpoint(double value) {
        this.getPressureSetpointChannel().setNextValue(value);
    }

    /**
     * Set the pump pressure set point, unit is bar.
     * See {@link ChannelId#PRESSURE_SETPOINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setPressureSetpoint(double value) throws OpenemsNamedException {
        this.getPressureSetpointChannel().setNextWriteValue(value);
    }

    /**
     * Set the pump pressure set point, unit is bar.
     * See {@link ChannelId#PRESSURE_SETPOINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setPressureSetpoint(Double value) throws OpenemsNamedException {
        this.getPressureSetpointChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#MOTOR_SPEED_SETPOINT}.
     *
     * @return the Channel
     */
    default DoubleWriteChannel getMotorSpeedSetpointChannel() {
        return channel(ChannelId.MOTOR_SPEED_SETPOINT);
    }

    /**
     * Gets the pump motor speed set point, unit is percent.
     * See {@link ChannelId#MOTOR_SPEED_SETPOINT}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Double> getMotorSpeedSetpoint() {
        return this.getMotorSpeedSetpointChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#MOTOR_SPEED_SETPOINT} Channel.
     *
     * @param value the next value
     */
    default void _setMotorSpeedSetpoint(Double value) {
        this.getMotorSpeedSetpointChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#MOTOR_SPEED_SETPOINT} Channel.
     *
     * @param value the next value
     */
    default void _setMotorSpeedSetpoint(double value) {
        this.getMotorSpeedSetpointChannel().setNextValue(value);
    }

    /**
     * Set the pump motor speed set point, unit is percent. Writing a value of 50 means 50%.
     * See {@link ChannelId#MOTOR_SPEED_SETPOINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setMotorSpeedSetpoint(double value) throws OpenemsNamedException {
        this.getMotorSpeedSetpointChannel().setNextWriteValue(value);
    }

    /**
     * Set the pump motor speed set point, unit is percent. Writing a value of 50 means 50%.
     * See {@link ChannelId#MOTOR_SPEED_SETPOINT}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setMotorSpeedSetpoint(Double value) throws OpenemsNamedException {
        this.getMotorSpeedSetpointChannel().setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#READ_ONLY_SETTING}.
     *
     * @return the Channel
     */
    default BooleanWriteChannel getReadOnlySettingChannel() {
        return channel(ChannelId.READ_ONLY_SETTING);
    }

    /**
     * Gets the setting of the read only mode. True = on, false = off.
     * See {@link ChannelId#READ_ONLY_SETTING}.
     *
     * @return the Channel {@link Value}
     */
    default Value<Boolean> getReadOnlySetting() {
        return this.getReadOnlySettingChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#READ_ONLY_SETTING} Channel.
     *
     * @param value the next value
     */
    default void _setReadOnlySetting(boolean value) {
        this.getReadOnlySettingChannel().setNextValue(value);
    }

    /**
     * Set the read only mode setting. True = on, false = off.
     * See {@link ChannelId#READ_ONLY_SETTING}.
     *
     * @param value the next write value
     * @throws OpenemsNamedException on error
     */
    default void setReadOnlySetting(boolean value) throws OpenemsNamedException {
        this.getReadOnlySettingChannel().setNextWriteValue(value);
    }
}


