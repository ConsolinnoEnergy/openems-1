package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the cooling mode.
 */

public enum CoolingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	AUTOMATIC(1, "Automatic"); //

	private final int value;
	private final String name;

	private CoolingMode(int value, String name) {
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
	public static CoolingMode valueOf(int value) {
		switch (value) {
			case 0:
				return CoolingMode.OFF;
			case 1:
				return CoolingMode.AUTOMATIC;
			default:
				return CoolingMode.UNDEFINED;
		}
	}
}