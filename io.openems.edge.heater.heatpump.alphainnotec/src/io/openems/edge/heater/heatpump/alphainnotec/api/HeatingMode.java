package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the heating mode.
 */

public enum HeatingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	AUTOMATIC(0, "Automatic"), //
	AUXILIARY_HEATER(1, "Second heat generator (Zusaetzlicher Waermeerzeuger)"), //
	PARTY(2, "No night setback (Party)"), //
	VACATION(3, "Holidays, full time setback (Ferien)"), //
	OFF(4, "Off"); //

	private final int value;
	private final String name;

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