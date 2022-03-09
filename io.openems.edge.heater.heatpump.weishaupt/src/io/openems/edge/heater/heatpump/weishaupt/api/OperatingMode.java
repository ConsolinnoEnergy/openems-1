package io.openems.edge.heater.heatpump.weishaupt.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the operating mode.
 */

public enum OperatingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SUMMER(0, "Summer"), //
	AUTOMATIC(1, "Automatic"), //
	THROTTLING(2, "Holidays, full time throttling (Urlaub)"), //
	NO_THROTTLING(3, "No late night throttling (Party)"), //
	SECOND_HEATER(4, "Second heat generator (2. Waermeerzeuger)"), //
	COOLING(5, "Cooling"); //

	private final int value;
	private final String name;

	private OperatingMode(int value, String name) {
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