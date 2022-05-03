package io.openems.edge.heater.heatpump.weishaupt.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the operating mode.
 */

public enum FlowTempRegulationMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OUTSIDE_TEMP(0, "Outside temperature & heating curve"), //
	MANUAL(1, "Manual set point"), //
	ROOM_TEMP(2, "Room temperature"); //

	private final int value;
	private final String name;

	private FlowTempRegulationMode(int value, String name) {
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
	public static FlowTempRegulationMode valueOf(int value) {
		switch (value) {
			case 0:
				return FlowTempRegulationMode.OUTSIDE_TEMP;
			case 1:
				return FlowTempRegulationMode.MANUAL;
			case 2:
				return FlowTempRegulationMode.ROOM_TEMP;
			default:
				return FlowTempRegulationMode.UNDEFINED;
		}
	}
}