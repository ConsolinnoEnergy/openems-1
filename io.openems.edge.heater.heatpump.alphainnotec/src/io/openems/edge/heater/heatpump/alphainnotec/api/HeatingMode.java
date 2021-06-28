package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the heating mode.
 */

public enum HeatingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	AUTOMATIC(0, "Automatic"), //
	AUXILIARY_HEATER(1, "Second heat generator (Zusätzlicher Wärmeerzeuger)"), //
	PARTY(2, "No late night throttling (Party)"), //
	VACATION(3, "Holidays, full time throttling (Ferien)"), //
	OFF(4, "Off"); //

	private int value;
	private String name;

	private HeatingMode(int value, String name) {
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