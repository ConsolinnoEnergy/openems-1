package io.openems.edge.controller.heatnetwork.pump.grundfos.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible operating modes of the Grundfos pump.
 */

public enum ControlModeSetting implements OptionsEnum  {
	UNDEFINED(-1, "Undefined"), //
	CONST_PRESSURE(0, "Constant pressure"), //
	CONST_FREQUENCY(1, "Constant frequency"), //
	MIN_MOTOR_CURVE(2, "Minimum motor curve"), //
	MAX_MOTOR_CURVE(3, "Maximum motor curve"), //
	AUTO_ADAPT(4, "Auto adapt");

	private final int value;
	private final String name;

	ControlModeSetting(int value, String name) {
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
	public static ControlModeSetting valueOf(int value) {
		switch (value) {
			case 0:
				return ControlModeSetting.CONST_PRESSURE;
			case 1:
				return ControlModeSetting.CONST_FREQUENCY;
			case 2:
				return ControlModeSetting.MIN_MOTOR_CURVE;
			case 3:
				return ControlModeSetting.MAX_MOTOR_CURVE;
			case 4:
				return ControlModeSetting.AUTO_ADAPT;
			default:
				return ControlModeSetting.UNDEFINED;
		}
	}
}
