package io.openems.edge.heater.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;

/**
 * A generalized interface for smart grid operation of a heat pump. Vendor specific interfaces should extend this
 * interface. Vendor specific implementations of Smart Grid state should be mapped to this channel.
 */

public interface HeatpumpSmartGrid extends Heater {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Smart Grid state of the heat pump.
		 * <ul>
		 * 		<li> Interface: HeatpumpSmartGrid
		 *      <li> Type: Integer
		 *      <li> Possible values: -1, 1 ... 4
		 *      <li> State -1: Undefined
		 *      <li> State 1: Electric supplier block
		 *      <li> State 2: Low energy consumption
		 *      <li> State 3: Standard
		 *      <li> State 4: High energy consumption
		 * </ul>
		 */
		SMART_GRID_STATE(Doc.of(SmartGridState.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * If the heat pump is set to use SMART_GRID_STATE (true) or ENABLE_SIGNAL (false).
		 * <ul>
		 * 		<li> Interface: HeatpumpSmartGrid
		 * 		<li> Type: Boolean
		 * </ul>
         */
        USE_SMART_GRID_STATE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
				channel -> {
					((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue);
				}
		));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#SMART_GRID_STATE}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getSmartGridStateChannel() {
        return this.channel(ChannelId.SMART_GRID_STATE);
    }
    
    /**
     * Get the Smart Grid state of the heat pump. Use ’getSmartGridState().asEnum()’ to get the enum.
     * <ul>
	 *      <li> Type: Integer
	 *      <li> Possible values: -1, 1 ... 4
	 *      <li> State -1: Undefined
	 *      <li> State 1: Electric supplier block
	 *      <li> State 2: Low energy consumption
	 *      <li> State 3: Standard
	 *      <li> State 4: High energy consumption
     * </ul>
	 * See {@link ChannelId#SMART_GRID_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
    default Value<Integer> getSmartGridState() {
		return this.getSmartGridStateChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SMART_GRID_STATE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	default void _setSmartGridState(int value) {
		this.getSmartGridStateChannel().setNextValue(value);
	}
	
	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SMART_GRID_STATE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	default void _setSmartGridState(Integer value) {
		this.getSmartGridStateChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SMART_GRID_STATE}
	 * Channel.
	 *
	 * @param state the next value
	 */
	default void _setSmartGridState(SmartGridState state) {
		if (state != null) {
			this.getSmartGridStateChannel().setNextValue(state.getValue());
		}
	}
	
	/**
	 * Set the Smart Grid state of the heat pump.
	 * <ul>
	 *      <li> Type: Integer
	 *      <li> Possible values: 1 ... 4
	 *      <li> State 1: Electric supplier block
	 *      <li> State 2: Low energy consumption
	 *      <li> State 3: Standard
	 *      <li> State 4: High energy consumption
	 * </ul>
	 * See {@link ChannelId#SMART_GRID_STATE}.
	 * 
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setSmartGridState(Integer value) throws OpenemsNamedException {
		this.getSmartGridStateChannel().setNextWriteValue(value);
	}

	/**
	 * Set the Smart Grid state of the heat pump.
	 * <ul>
	 *      <li> Type: Integer
	 *      <li> Possible values: 1 ... 4
	 *      <li> State 1: Electric supplier block
	 *      <li> State 2: Low energy consumption
	 *      <li> State 3: Standard
	 *      <li> State 4: High energy consumption
	 * </ul>
	 * See {@link ChannelId#SMART_GRID_STATE}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setSmartGridState(int value) throws OpenemsNamedException {
		this.getSmartGridStateChannel().setNextWriteValue(value);
	}

	/**
	 * Set the Smart Grid state of the heat pump.
	 * <ul>
	 *      <li> Type: Integer
	 *      <li> Possible values: 1 ... 4
	 *      <li> State 1: Electric supplier block
	 *      <li> State 2: Low energy consumption
	 *      <li> State 3: Standard
	 *      <li> State 4: High energy consumption
	 * </ul>
	 * See {@link ChannelId#SMART_GRID_STATE}.
	 *
	 * @param state the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setSmartGridState(SmartGridState state) throws OpenemsNamedException {
		if (state != null && state != SmartGridState.UNDEFINED) {
			this.getSmartGridStateChannel().setNextWriteValue(state.getValue());
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#USE_SMART_GRID_STATE}.
	 *
	 * @return the Channel
	 */
	default BooleanWriteChannel getUseSmartGridStateChannel() {
		return this.channel(ChannelId.USE_SMART_GRID_STATE);
	}

	/**
	 * Get the current setting of the heat pump. SMART_GRID_STATE (true) or ENABLE_SIGNAL (false).
	 * See {@link ChannelId#USE_SMART_GRID_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Boolean> getUseSmartGridState() {
		return this.getUseSmartGridStateChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#USE_SMART_GRID_STATE} Channel.
	 *
	 * @param value the next value
	 */
	default void _setUseSmartGridState(Boolean value) {
		this.getUseSmartGridStateChannel().setNextValue(value);
	}

	/**
	 * Set the control mode of the heat pump. SMART_GRID_STATE (true) or ENABLE_SIGNAL (false).
	 * See {@link ChannelId#USE_SMART_GRID_STATE}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	default void setUseSmartGridState(Boolean value) throws OpenemsNamedException {
		this.getUseSmartGridStateChannel().setNextWriteValue(value);
	}
}
