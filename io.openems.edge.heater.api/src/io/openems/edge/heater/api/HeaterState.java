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

	private int value;
	private String name;

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
}