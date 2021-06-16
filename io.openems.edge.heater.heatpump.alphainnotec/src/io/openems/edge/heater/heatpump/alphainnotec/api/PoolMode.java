package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the pool mode.
 */

public enum PoolMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	AUTOMATIC(0, "Automatic"), //
	VALUE_NOT_USED(1, "Value not used (Wert nicht benutzt)"), //
	PARTY(2, "No late night throttling (Party)"), //
	VACATION(3, "Vacation, full time throttling (Ferien)"), //
	OFF(4, "Off"); //

	private int value;
	private String name;

	private PoolMode(int value, String name) {
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