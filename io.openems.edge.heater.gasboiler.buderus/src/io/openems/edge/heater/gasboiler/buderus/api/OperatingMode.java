package io.openems.edge.heater.gasboiler.buderus.api;

import io.openems.common.types.OptionsEnum;

public enum OperatingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SET_POINT_TEMPERATURE(0, "Set point temperature"), //
	SET_POINT_POWER_PERCENT(1, "Set point power percent"); //

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
	 * Returns the enum corresponding to the integer value.
	 *
	 * @param value the integer value of the enum
	 * @return the enum
	 */
	public static OperatingMode valueOf(int value) {
		switch (value) {
			case 0:
				return OperatingMode.SET_POINT_TEMPERATURE;
			case 1:
				return OperatingMode.SET_POINT_POWER_PERCENT;
			default:
				return OperatingMode.UNDEFINED;
		}
	}
}