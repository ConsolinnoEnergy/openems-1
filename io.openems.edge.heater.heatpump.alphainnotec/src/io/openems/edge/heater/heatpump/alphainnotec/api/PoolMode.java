package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the pool mode.
 */

public enum PoolMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	AUTOMATIK(0, "Automatik"), //
	WERT_NICHT_BENUTZT(1, "Wert nicht benutzt"), //
	PARTY(2, "Party"), //
	FERIEN(3, "Ferien"), //
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