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

	/**
	 * Returns the enum state corresponding to the integer value.
	 *
	 * @param value the integer value of the enum
	 * @return the enum state
	 */
	public static OperatingMode valueOf(int value) {
		switch (value) {
			case 0:
				return OperatingMode.ANTIFREEZE;
			case 1:
				return OperatingMode.STANDBY;
			case 2:
				return OperatingMode.PROGRAM_MODE;
			case 3:
				return OperatingMode.COMFORT_MODE;
			case 4:
				return OperatingMode.ECO_MODE;
			case 5:
				return OperatingMode.DOMESTIC_HOT_WATER;
			default:
				return OperatingMode.UNDEFINED;
		}
	}
}