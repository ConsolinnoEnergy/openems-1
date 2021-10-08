package io.openems.edge.heater.heatpump.tecalor.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the operating mode.
 */

public enum OperatingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ANTIFREEZE(0, "Antifreeze protection only (Notbetrieb)"), //
	STANDBY(1, "Standby mode (Bereitschaftsbetrieb)"), //
	PROGRAM_MODE(2, "Program mode (Programmbetrieb)"), //
	COMFORT_MODE(3, "Comfort mode (Komfortbetrieb)"), //
	ECO_MODE(4, "ECO mode (ECO-Betrieb)"), //
	DOMESTIC_HOT_WATER(5, "Domestic hot water (Warmwasserbetrieb)"); //

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