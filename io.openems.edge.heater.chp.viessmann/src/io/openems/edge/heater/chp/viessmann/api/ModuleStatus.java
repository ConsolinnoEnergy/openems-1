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

	private int value;
	private String name;

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
		ModuleStatus returnEnum = ModuleStatus.UNDEFINED;
		switch (value) {
			case 0:
				returnEnum = ModuleStatus.OFF;
				break;
			case 1:
				returnEnum = ModuleStatus.READY;
				break;
			case 2:
				returnEnum = ModuleStatus.START;
				break;
			case 3:
				returnEnum = ModuleStatus.RUNNING;
				break;
			case 4:
				returnEnum = ModuleStatus.DISTURBANCE;
				break;
		}
		return returnEnum;
	}
}