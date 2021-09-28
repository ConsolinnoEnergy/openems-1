package io.openems.edge.heater.api;

import io.openems.common.types.OptionsEnum;

/**
 * The possible states of a heater.
 */

public enum HeaterState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	BLOCKED(0, "Heater operation is blocked by something"), //
	OFF(1, "Off"), //
	STANDBY(2, "Standby, waiting for commands"), //
	STARTING_UP_OR_PREHEAT(3, "Command to heat received, preparing to start heating"),
	HEATING(4, "Heater is heating"); //

	private final int value;
	private final String name;

	private HeaterState(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}
	
	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

	/**
	 * Returns the enum state corresponding to the integer value.
	 *
	 * @param value the integer value of the enum
	 * @return the enum state
	 */
	public static HeaterState valueOf(int value) {
		HeaterState returnEnum = HeaterState.UNDEFINED;
		switch (value) {
			case 0:
				returnEnum = HeaterState.BLOCKED;
				break;
			case 1:
				returnEnum = HeaterState.OFF;
				break;
			case 2:
				returnEnum = HeaterState.STANDBY;
				break;
			case 3:
				returnEnum = HeaterState.STARTING_UP_OR_PREHEAT;
				break;
			case 4:
				returnEnum = HeaterState.HEATING;
				break;
		}
		return returnEnum;
	}
}