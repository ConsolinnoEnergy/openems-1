package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the ventilation mode.
 */

public enum VentilationMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	AUTOMATIC(0, "Automatic"), //
	PARTY(1, "No night setback (Party)"), //
	VACATION(2, "Holidays, full time setback (Ferien)"), //
	OFF(3, "Off"); //

	private final int value;
	private final String name;

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

	/**
	 * Returns the enum corresponding to the integer value.
	 *
	 * @param value the integer value of the enum
	 * @return the enum
	 */
	public static VentilationMode valueOf(int value) {
		switch (value) {
			case 0:
				return VentilationMode.AUTOMATIC;
			case 1:
				return VentilationMode.PARTY;
			case 2:
				return VentilationMode.VACATION;
			case 3:
				return VentilationMode.OFF;
			default:
				return VentilationMode.UNDEFINED;
		}
	}
}