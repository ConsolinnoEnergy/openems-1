package io.openems.edge.heater.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * A generalized interface for smart grid operation of a heat pump. Vendor specific interfaces should extend this
 * interface. Vendor specific implementations of Smart Grid state should be mapped to this channel.
 */

public interface HeatpumpSmartGrid extends Heater {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Smart Grid state of the heat pump.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: -1, 1 ... 4
		 *      <li> State -1: Undefined
         *      <li> State 1: Electric supplier block
         *      <li> State 2: Low energy consumption
         *      <li> State 3: Standard
		 *      <li> State 4: High energy consumption
         * </ul>
         */
        SMART_GRID_STATE(Doc.of(SmartGridState.values()).accessMode(AccessMode.READ_WRITE));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    // ToDO: Schauen, ob man das auch als EnumChannel machen kann.

    /**
     * Gets the Channel for {@link ChannelId#SMART_GRID_STATE}.
     *
     * @return the Channel
     */
    default IntegerWriteChannel getSmartGridStateChannel() {
        return this.channel(ChannelId.SMART_GRID_STATE);
    }
    
    /**
     * Get the Smart Grid state of the heat pump.
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
    public default Value<Integer> getSmartGridState() {
		return this.getSmartGridStateChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SMART_GRID_STATE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSmartGridState(int value) {
		this.getSmartGridStateChannel().setNextValue(value);
	}
	
	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#SMART_GRID_STATE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setSmartGridState(Integer value) {
		this.getSmartGridStateChannel().setNextValue(value);
	}
	
	/**
	 * Set the Smart Grid state of the heat pump.
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
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSmartGridState(Integer value) throws OpenemsNamedException {
		this.getSmartGridStateChannel().setNextWriteValue(value);
	}

	/**
	 * Set the Smart Grid state of the heat pump.
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
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSmartGridState(int value) throws OpenemsNamedException {
		this.getSmartGridStateChannel().setNextWriteValue(value);
	}
}
