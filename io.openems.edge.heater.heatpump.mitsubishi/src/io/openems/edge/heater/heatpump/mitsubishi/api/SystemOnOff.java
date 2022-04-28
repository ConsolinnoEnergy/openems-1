package io.openems.edge.heater.heatpump.mitsubishi.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the operating mode.
 */

public enum SystemOnOff implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	ON(1, "On"), //
	EMERGENCY_MODE(2, "Emergency operation, read only (Notbetrieb)"), //
	TEST_RUN(3, "Test run, read only (Testlauf)"); //

	private final int value;
	private final String name;

	private SystemOnOff(int value, String name) {
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