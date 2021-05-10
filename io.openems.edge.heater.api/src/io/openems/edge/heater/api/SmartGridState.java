package io.openems.edge.heater.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible smart grid states.
 */

public enum SmartGridState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	LOW(1, "Smart Grid Low"), //
	STANDARD(2, "Standard"), //
	HIGH(3, "Smart Grid High"); //

	private int value;
	private String name;

	private SmartGridState(int value, String name) {
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