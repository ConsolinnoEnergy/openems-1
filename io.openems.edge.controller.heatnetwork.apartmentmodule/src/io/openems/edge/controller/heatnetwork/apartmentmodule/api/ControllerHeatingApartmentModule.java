package io.openems.edge.controller.heatnetwork.apartmentmodule.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The Nature of the ApartmentModuleController.
 * <p>
 * This Apartment Module Controller is a slightly Complex Controller, that allows to Monitor ApartmentCords combined with a Pump and
 * e.g. HydraulicLineHeater.
 * The Logic of the Controller will be explained within the Implementation.
 * This Nature stores SetPoint values as well as  ApartmentModuleController States and Emergency Values.
 * The implementation will react accordingly to the Emergencies that have occurred.
 */
public interface ControllerHeatingApartmentModule extends OpenemsComponent {

    int CHECK_MISSING_COMPONENTS_TIME = 60;
    String CHECK_MISSING_COMPONENT_IDENTIFIER = "CONTROLLER_HEATING_APARTMENT_MODULE_CHECK_MISSING";

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Set the Temperature to look out for.
         * E.g. check your threshold thermometer if this temperature is above/below reference temperature.
         *
         * <ul>
         * <li>Interface: Thermometer
         * <li>Type: Integer
         * <li>Unit: dezidegree celsius
         * </ul>
         */
        SET_POINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE).onInit(
                        channel -> ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue)
                )),
        /**
         * Set the PowerLevel of Pump.
         * If Requests are available --> activate pump with given PowerLevel
         */
        SET_POINT_PUMP_POWER_LEVEL(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((DoubleWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * The State of the Controller.
         */
        CONTROLLER_APARTMENT_STATE(Doc.of(ApartmentModuleControllerState.values())),

        /**
         * If this Flag is set-->Pump starts. Except if EmergencyStop is enabled.
         */
        EMERGENCY_PUMP_START((Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE))),
        /**
         * All ResponseFlags will be Set. Except if EmergencyStop is enabled.
         */
        EMERGENCY_ENABLE_EVERY_RESPONSE((Doc.of(OpenemsType.BOOLEAN)).accessMode(AccessMode.READ_WRITE)),
        /**
         * Stops this controller.
         */

        EMERGENCY_STOP((Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    //--------------- SET POINT TEMPERATURE ------------------//


    /**
     * Returns the Channel for {@link ChannelId#SET_POINT_TEMPERATURE}.
     *
     * @return the channel
     */
    default WriteChannel<Integer> getSetPointTemperatureChannel() {
        return this.channel(ChannelId.SET_POINT_TEMPERATURE);
    }

    /**
     * Set the SetPointTemperature for this Controller.
     *
     * @param temperature the temperature in dC
     */
    default void setSetPointTemperature(int temperature) {
        this.getSetPointTemperatureChannel().setNextValue(temperature);
    }

    /**
     * Get the SetPointTemperatureChannel.
     *
     * @return the setPoint
     */
    default int getSetPointTemperature() {

        Integer setPointTemperature = (Integer) this.getValueOfChannel(this.getSetPointTemperatureChannel());
        if (setPointTemperature == null) {
            setPointTemperature = (Integer) this.getNextValueOfChannel(this.getSetPointTemperatureChannel());
        }
        if (setPointTemperature == null) {
            setPointTemperature = Integer.MIN_VALUE;
        }
        return setPointTemperature;
    }
    //----------------------------------------------------------------------//

    //--------------- SET_POINT_PUMP_POWER_LEVEL ------------------//

    /**
     * Returns the Channel for {@link ChannelId#SET_POINT_PUMP_POWER_LEVEL}.
     *
     * @return the channel
     */
    default WriteChannel<Double> getSetPointPowerLevelChannel() {
        return this.channel(ChannelId.SET_POINT_PUMP_POWER_LEVEL);
    }

    /**
     * Set the SetPointPowerLevel for this Controller.
     *
     * @param powerLevel the powerLevel in percent
     */
    default void setSetPointPowerLevel(double powerLevel) {
        this.getSetPointPowerLevelChannel().setNextValue(powerLevel);
    }

    /**
     * Get the SetPointPowerLevel.
     *
     * @return the setPoint
     */
    default double getSetPointPowerLevel() {

        Double setPointPower = (Double) this.getValueOfChannel(this.getSetPointPowerLevelChannel());
        if (setPointPower == null) {
            setPointPower = (Double) this.getNextValueOfChannel(this.getSetPointPowerLevelChannel());
        }
        if (setPointPower == null) {
            setPointPower = (double) Double.MIN_EXPONENT;
        }
        return setPointPower;
    }
    //----------------------------------------------------------------------//
    //--------------- STATE ------------------//

    /**
     * Returns the Channel for {@link ChannelId#CONTROLLER_APARTMENT_STATE}.
     *
     * @return the channel
     */
    default Channel<ApartmentModuleControllerState> getControllerStateChannel() {
        return this.channel(ChannelId.CONTROLLER_APARTMENT_STATE);
    }

    /**
     * Get the ApartmentModuleControllerState.
     *
     * @return the state
     */
    default ApartmentModuleControllerState getControllerState() {
        ApartmentModuleControllerState currentState = ApartmentModuleControllerState.getStateFromIntValue((Integer) this.getValueOfChannel(this.getControllerStateChannel()));
        if (currentState.isUndefined()) {
            currentState = ApartmentModuleControllerState.getStateFromIntValue((Integer) this.getNextValueOfChannel(this.getControllerStateChannel()));
        }
        return currentState;
    }

    /**
     * Set the State for this Controller.
     *
     * @param state the Apartment Module Controller state
     */
    default void setState(ApartmentModuleControllerState state) {
        this.getControllerStateChannel().setNextValue(state);
    }

    //----------------------------------------------------------------------//
    //--------------- EMERGENCY_PUMP_START ------------------//

    /**
     * Returns the Channel for {@link ChannelId#EMERGENCY_PUMP_START}.
     *
     * @return the channel
     */
    default WriteChannel<Boolean> getEmergencyPumpStartChannel() {
        return this.channel(ChannelId.EMERGENCY_PUMP_START);
    }

    //----------------------------------------------------------------------//
    //--------------- EMERGENCY_ENABLE_EVERY_RESPONSE ------------------//

    /**
     * Returns the Channel for {@link ChannelId#EMERGENCY_ENABLE_EVERY_RESPONSE}.
     *
     * @return the channel
     */
    default WriteChannel<Boolean> getEmergencyEnableEveryResponseChannel() {
        return this.channel(ChannelId.EMERGENCY_ENABLE_EVERY_RESPONSE);
    }

    //----------------------------------------------------------------------//
    //--------------- EMERGENCY_STOP ------------------//

    /**
     * Returns the Channel for {@link ChannelId#EMERGENCY_STOP}.
     *
     * @return the channel
     */
    default WriteChannel<Boolean> getEmergencyStopChannel() {
        return this.channel(ChannelId.EMERGENCY_STOP);
    }
    //----------------------------------------------------------------------//

    /**
     * Get the Current Value of a Channel.
     *
     * @param requestedValue the Channel
     * @return the value.
     */
    default Object getValueOfChannel(Channel<?> requestedValue) {
        if (requestedValue.value().isDefined()) {
            return requestedValue.value().get();
        } else {
            return null;
        }
    }

    /**
     * Get the next Value of a Channel.
     *
     * @param requestedValue the Channel
     * @return the value.
     */
    default Object getNextValueOfChannel(Channel<?> requestedValue) {
        if (requestedValue.getNextValue().isDefined()) {
            return requestedValue.getNextValue().get();
        }
        return null;
    }

}
