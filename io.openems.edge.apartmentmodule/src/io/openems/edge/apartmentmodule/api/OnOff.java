package io.openems.edge.apartmentmodule.api;

import io.openems.common.types.OptionsEnum;

/**
 * The OnOff State of the Relays.
 */
public enum OnOff implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	ON(1, "On"); //

	private final int value;
	private final String name;

	private OnOff(int value, String name) {
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