package io.openems.edge.heater.chp.kwenergy.api;

import io.openems.common.types.OptionsEnum;

/**
 * The possible control modes of the CHP.
 */

public enum ControlMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	POWER_PERCENT(0, "Control mode power percent"), //
	POWER(1, "Control mode electric power"), //
	CONSUMPTION(2, "Control mode consumption"); //

	private final int value;
	private final String name;

	private ControlMode(int value, String name) {
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