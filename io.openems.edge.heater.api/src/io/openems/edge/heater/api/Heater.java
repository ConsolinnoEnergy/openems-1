package io.openems.edge.heater.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * A generalized interface for a heater.
 * Contains the most important functions that should be available on all heaters, allowing a vendor agnostic
 * implementation. Vendor specific interfaces should extend this interface.
 * This interface is designed to use the EnableSignal channel to switch the heater on or off.
 * Warning: Not all functionality is supported by every device using this interface.
 */

public interface Heater extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

    	// read/write channels

    	/**
         * Write: Turn the heater on (true) or off (write nothing).
         * Read: The heater is running (true) or not (false).
		 * It is recommended to use the EnableSignalHandlerImpl in the component using the Heater interface (see
		 * {@link io.openems.edge.heater.api.EnableSignalHandlerImpl}). This way the handling of the EnableSignal is the
		 * same in all Heater devices.
		 * The EnableSignalHandlerImpl fetches the nextWriteValue of this channel with getNextWriteValueAndReset(). If
		 * the collected value is ’true’, the heater is turned on and a configurable timer is started. As long as the
		 * timer has not finished counting down, the heater stays on. When the timer runs out, the heater stops heating.
		 * To keep the heater heating, ’true’ must be regularly written in the nextWriteValue of this channel.
		 * It is not needed to write ’false’ to turn off the heater. Simply stop writing ’true’, and the heater will
		 * turn off after the timer runs out. This way multiple controllers can turn on the heater without needing a
		 * controller hierarchy.
		 * Writing ’false’ is possible (interpreted as ’no value’), but this will overwrite any ’true’ another controller
		 * may have written that cycle.
		 *
		 * <ul>
		 *     <li> Interface: Heater
		 *     <li> Type: Boolean
		 * </ul>
         */
    	ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

		/**
		 * The temperature set point. This usually means the flow temperature set point.
		 *
		 * <ul>
		 *     <li> Interface: Heater
		 *     <li> Type: Integer
		 *     <li> Unit: dezidegree Celsius
		 * </ul>
		 */
		SET_POINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),

		/**
		 * The heating power set point.
		 *
		 * <ul>
		 *     <li> Interface: Heater
		 *     <li> Type: Integer
		 *     <li> Unit: kilowatt
		 * </ul>
		 */
		SET_POINT_HEATING_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).accessMode(AccessMode.READ_WRITE)),

		/**
		 * The heating power set point in percent. A value of 50 in the channel means 50%.
		 *
		 * <ul>
		 *     <li> Interface: Heater
		 *     <li> Type: Double
		 *     <li> Unit: percent
		 * </ul>
		 */
		SET_POINT_HEATING_POWER_PERCENT(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),

		// read only channels

		/**
         * Temperature value of the outgoing hot water.
		 *
		 * <ul>
		 *     <li> Interface: Heater
		 *     <li> Type: Integer
		 *     <li> Unit: dezidegree Celsius
		 * </ul>
         */
		FLOW_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Temperature value of the water return flow.
		 *
		 * <ul>
		 *     <li> Interface: Heater
		 *     <li> Type: Integer
		 *     <li> Unit: dezidegree Celsius
		 * </ul>
         */
        RETURN_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

		/**
		 * Effective (current) heating power.
		 *
		 * <ul>
		 *     <li> Interface: Heater
		 *     <li> Type: Integer
		 *     <li> Unit: kilowatt
		 * </ul>
		 */
		EFFECTIVE_HEATING_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT).accessMode(AccessMode.READ_ONLY)),

		/**
		 * Effective (current) heating power in percent. A value of 50 in the channel means 50%.
		 *
		 * <ul>
		 *     <li> Interface: Heater
		 *     <li> Type: Double
		 *     <li> Unit: percent
		 * </ul>
		 */
		EFFECTIVE_HEATING_POWER_PERCENT(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),

		/**
		 * Warning message of the heater. Contains "No warning" when everything is fine.
		 *
		 * <ul>
		 *     <li> Interface: Heater
		 *     <li> Type: String
		 * </ul>
		 */
		WARNING_MESSAGE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),

        /**
         * Error message of the heater. Contains "No error" when everything is fine.
		 *
		 * <ul>
		 *     <li> Interface: Heater
		 *     <li> Type: String
		 * </ul>
         */
        ERROR_MESSAGE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),

        /**
         * Possible states of the heater.
		 *
		 * <ul>
		 *     <li> Interface: Heater
		 *     <li> Type: Integer
		 *     <li> Possible values: -1 ... 4
		 *     <li> State -1: UNDEFINED - Undefined
		 *     <li> State 0: BLOCKED - Heater operation is blocked by something
		 *     <li> State 1: OFF - Off
		 *     <li> State 2: STANDBY - Standby, waiting for commands
		 *     <li> State 3: STARTING_UP_OR_PREHEAT - Command to heat received, preparing to start heating
		 *     <li> State 4: HEATING - Heater is heating
		 * </ul>
         */
        HEATER_STATE(Doc.of(HeaterState.values()).accessMode(AccessMode.READ_ONLY));
    	
        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#ENABLE_SIGNAL}.
     *
     * @return the Channel
     */
    public default BooleanWriteChannel getEnableSignalChannel() {
        return this.channel((ChannelId.ENABLE_SIGNAL));
    }
    
    /**
	 * Gets the enable signal, indicating if the heater is running (true) or not (false).
	 * See {@link ChannelId#ENABLE_SIGNAL}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Boolean> getEnableSignal() {
		return this.getEnableSignalChannel().value();
	}
	
	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ENABLE_SIGNAL}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setEnableSignal(Boolean value) {
		this.getEnableSignalChannel().setNextValue(value);
	}
	
	/**
	 * Turn the heater on (regularly write true) or off (write nothing).
	 * When ’true’ is written, the heater is turned on and a configurable timer is started. Writing ’true’ again will
	 * reset the timer. As long as the timer has not finished counting down, the heater stays on. When the timer runs
	 * out, the heater stops heating. To keep the heater heating, ’true’ must be regularly written.
	 * It is not needed to write ’false’ to turn off the heater. This way multiple controllers can turn on the heater
	 * without needing a controller hierarchy.
	 * Writing ’false’ is possible (interpreted as ’no value’), but this will overwrite any ’true’ another controller
	 * may have written that cycle.
	 * See {@link ChannelId#ENABLE_SIGNAL}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setEnableSignal(Boolean value) throws OpenemsNamedException {
		this.getEnableSignalChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_POINT_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getTemperatureSetpointChannel() {
		return this.channel((ChannelId.SET_POINT_TEMPERATURE));
	}

	/**
	 * Gets the temperature set point. This usually means the flow temperature set point. Unit is dezidegree Celsius.
	 * See {@link ChannelId#SET_POINT_TEMPERATURE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTemperatureSetpoint() {
		return this.getTemperatureSetpointChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SET_POINT_TEMPERATURE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureSetpoint(int value) {
		this.getTemperatureSetpointChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SET_POINT_TEMPERATURE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setTemperatureSetpoint(Integer value) {
		this.getTemperatureSetpointChannel().setNextValue(value);
	}

	/**
	 * Sets the temperature set point. This usually means the flow temperature set point. Unit is dezidegree Celsius.
	 * See {@link ChannelId#SET_POINT_TEMPERATURE}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setTemperatureSetpoint(int value) throws OpenemsNamedException {
		this.getTemperatureSetpointChannel().setNextWriteValue(value);
	}

	/**
	 * Sets the temperature set point. This usually means the flow temperature set point. Unit is dezidegree Celsius.
	 * See {@link ChannelId#SET_POINT_TEMPERATURE}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setTemperatureSetpoint(Integer value) throws OpenemsNamedException {
		this.getTemperatureSetpointChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_POINT_HEATING_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getHeatingPowerSetpointChannel() {
		return this.channel((ChannelId.SET_POINT_HEATING_POWER));
	}

	/**
	 * Gets the heating power set point. Unit is kilowatt.
	 * See {@link ChannelId#SET_POINT_HEATING_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHeatingPowerSetpoint() {
		return this.getHeatingPowerSetpointChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SET_POINT_HEATING_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHeatingPowerSetpoint(int value) {
		this.getHeatingPowerSetpointChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SET_POINT_HEATING_POWER}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHeatingPowerSetpoint(Integer value) {
		this.getHeatingPowerSetpointChannel().setNextValue(value);
	}

	/**
	 * Sets the heating power set point. Unit is kilowatt.
	 * See {@link ChannelId#SET_POINT_HEATING_POWER}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingPowerSetpoint(int value) throws OpenemsNamedException {
		this.getHeatingPowerSetpointChannel().setNextWriteValue(value);
	}

	/**
	 * Sets the heating power set point. Unit is kilowatt.
	 * See {@link ChannelId#SET_POINT_HEATING_POWER}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingPowerSetpoint(Integer value) throws OpenemsNamedException {
		this.getHeatingPowerSetpointChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_POINT_HEATING_POWER_PERCENT}.
	 *
	 * @return the Channel
	 */
	public default DoubleWriteChannel getHeatingPowerPercentSetpointChannel() {
		return this.channel((ChannelId.SET_POINT_HEATING_POWER_PERCENT));
	}

	/**
	 * Gets the heating power percent set point. Unit is percent. A value of 50 in the channel means 50%.
	 * See {@link ChannelId#SET_POINT_HEATING_POWER_PERCENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Double> getHeatingPowerPercentSetpoint() {
		return this.getHeatingPowerPercentSetpointChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SET_POINT_HEATING_POWER_PERCENT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHeatingPowerPercentSetpoint(double value) {
		this.getHeatingPowerPercentSetpointChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SET_POINT_HEATING_POWER_PERCENT}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHeatingPowerPercentSetpoint(Double value) {
		this.getHeatingPowerPercentSetpointChannel().setNextValue(value);
	}

	/**
	 * Sets the heating power percent set point. Unit is percent. A value of 50 in the channel means 50%.
	 * See {@link ChannelId#SET_POINT_HEATING_POWER_PERCENT}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingPowerPercentSetpoint(double value) throws OpenemsNamedException {
		this.getHeatingPowerPercentSetpointChannel().setNextWriteValue(value);
	}

	/**
	 * Sets the heating power percent set point. Unit is percent. A value of 50 in the channel means 50%.
	 * See {@link ChannelId#SET_POINT_HEATING_POWER_PERCENT}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setHeatingPowerPercentSetpoint(Double value) throws OpenemsNamedException {
		this.getHeatingPowerPercentSetpointChannel().setNextWriteValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#FLOW_TEMPERATURE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getFlowTemperatureChannel() {
        return this.channel(ChannelId.FLOW_TEMPERATURE);
    }
    
    /**
	 * Gets the temperature value of the outgoing hot water in dezidegree Celsius.
	 * See {@link ChannelId#FLOW_TEMPERATURE}.
	 *
	 * @return the Channel {@link Value}
	 */
    public default Value<Integer> getFlowTemperature() {
		return this.getFlowTemperatureChannel().value();
	}
	
	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#FLOW_TEMPERATURE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setFlowTemperature(Integer value) {
		this.getFlowTemperatureChannel().setNextValue(value);
	}
	
	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#FLOW_TEMPERATURE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setFlowTemperature(int value) {
		this.getFlowTemperatureChannel().setNextValue(value);
	}

    /**
     * Gets the Channel for {@link ChannelId#RETURN_TEMPERATURE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getReturnTemperatureChannel() {
        return this.channel(ChannelId.RETURN_TEMPERATURE);
    }
    
    /**
     * Get the rewind temperature in dezidegree Celsius.
	 * See {@link ChannelId#RETURN_TEMPERATURE}.
	 *
	 * @return the Channel {@link Value}
     */
    public default Value<Integer> getReturnTemperature() {
		return this.getReturnTemperatureChannel().value();
	}
    
    /**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RETURN_TEMPERATURE}
	 * Channel.
	 *
	 * @param value the next value
	 */
    public default void _setReturnTemperature(Integer value) {
		this.getReturnTemperatureChannel().setNextValue(value);
	}
	
	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#RETURN_TEMPERATURE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setReturnTemperature(int value) {
		this.getReturnTemperatureChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#WARNING_MESSAGE}.
	 *
	 * @return the Channel
	 */
	public default StringReadChannel getWarningMessageChannel() {
		return this.channel(ChannelId.WARNING_MESSAGE);
	}

	/**
	 * Warning message of the heater. Contains "No warning" when everything is fine.
	 * See {@link ChannelId#WARNING_MESSAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<String> getWarningMessage() {
		return this.getWarningMessageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#WARNING_MESSAGE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setWarningMessage(String value) {
		this.getWarningMessageChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#ERROR_MESSAGE}.
	 *
	 * @return the Channel
	 */
	public default StringReadChannel getErrorMessageChannel() {
		return this.channel(ChannelId.ERROR_MESSAGE);
	}

	/**
	 * Error message of the heater. Contains "No error" when everything is fine.
	 * See {@link ChannelId#ERROR_MESSAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<String> getErrorMessage() {
		return this.getErrorMessageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#ERROR_MESSAGE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setErrorMessage(String value) {
		this.getErrorMessageChannel().setNextValue(value);
	}

	/**
     * Gets the Channel for {@link ChannelId#HEATER_STATE}.
     *
     * @return the Channel
     */
	public default IntegerReadChannel getHeaterStateChannel() {
        return this.channel(ChannelId.HEATER_STATE);
    }
	
    /**
	 * Gets the state of the heater.
	 *
	 * <ul>
	 *     <li> Type: Integer
	 *     <li> Possible values: -1 ... 4
	 *     <li> State -1: UNDEFINED - Undefined
	 *     <li> State 0: BLOCKED - Heater operation is blocked by something
	 *     <li> State 1: OFF - Off
	 *     <li> State 2: STANDBY - Standby, waiting for commands
	 *     <li> State 3: STARTING_UP_OR_PREHEAT - Command to heat received, preparing to start heating
	 *     <li> State 4: HEATING - Heater is heating
	 * </ul>
	 * See {@link ChannelId#HEATER_STATE}.
     *
	 * @return the Channel {@link Value}
     */
    public default Value<Integer> getHeaterState() {
		return this.getHeaterStateChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#HEATER_STATE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setHeaterState(int value) {
		this.getHeaterStateChannel().setNextValue(value);
	}

    /**
	 * Internal method to set the 'nextValue' on {@link ChannelId#HEATER_STATE}
	 * Channel.
	 *
	 * @param value the next value
	 */
    public default void _setHeaterState(Integer value) {
		this.getHeaterStateChannel().setNextValue(value);
	}

}
