package io.openems.edge.heater.chp.wolf.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the operating mode.
 */

public enum OperatingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ELECTRIC_POWER(0, "Electric power"), //
	FEED_IN_MANAGEMENT(1, "Feed-in management"), //
	RESERVE(2, "Reserve"); //

	private final int value;
	private final String name;

	OperatingMode(int value, String name) {
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
	public static OperatingMode valueOf(int value) {
		switch (value) {
			case 0:
				return OperatingMode.ELECTRIC_POWER;
			case 1:
				return OperatingMode.FEED_IN_MANAGEMENT;
			case 2:
				return OperatingMode.RESERVE;
			default:
				return OperatingMode.UNDEFINED;
		}
	}
}