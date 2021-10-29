package io.openems.edge.heater.chp.viessmann.api;

import io.openems.common.types.OptionsEnum;

/**
 * The possible module status of the CHP.
 */

public enum ModuleStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	READY(1, "Ready"), //
	START(2, "Start"),
	RUNNING(3, "Running"),
	DISTURBANCE(4, "Disturbance"); //

	private final int value;
	private final String name;

	private ModuleStatus(int value, String name) {
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
	public static ModuleStatus valueOf(int value) {
		switch (value) {
			case 0:
				return ModuleStatus.OFF;
			case 1:
				return ModuleStatus.READY;
			case 2:
				return ModuleStatus.START;
			case 3:
				return ModuleStatus.RUNNING;
			case 4:
				return ModuleStatus.DISTURBANCE;
			default:
				return ModuleStatus.UNDEFINED;
		}
	}
}