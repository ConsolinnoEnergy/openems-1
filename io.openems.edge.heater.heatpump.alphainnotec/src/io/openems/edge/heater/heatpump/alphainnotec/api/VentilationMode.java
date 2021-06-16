package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the ventilation mode.
 */

public enum VentilationMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	AUTOMATIC(0, "Automatic"), //
	PARTY(1, "No late night throttling (Party)"), //
	VACATION(2, "Vacation, full time throttling (Ferien)"), //
	OFF(3, "Off"); //

	private int value;
	private String name;

	private VentilationMode(int value, String name) {
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