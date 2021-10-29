package io.openems.edge.heater.api;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.value.Value;

/**
 * Possible smart grid states.
 */

public enum SmartGridState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SG1_BLOCKED(1, "Smart Grid state 1: Operation blocked by demand side management (DSM)"), //
	SG2_LOW(2, "Smart Grid state 2: Low energy consumption"), //
	SG3_STANDARD(3, "Smart Grid state 3: Standard"), //
	SG4_HIGH(4, "Smart Grid state 4: High energy consumption"); //

	private final int value;
	private final String name;

	private SmartGridState(int value, String name) {
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
	 * Returns the SmartGridState enum corresponding to the integer value.
	 *
	 * @param value the integer value of the SmartGridState enum
	 * @return the SmartGridState enum
	 */
	public static SmartGridState valueOf(int value) {
		switch (value) {
			case 1:
				return SmartGridState.SG1_BLOCKED;
			case 2:
				return SmartGridState.SG2_LOW;
			case 3:
				return SmartGridState.SG3_STANDARD;
			case 4:
				return SmartGridState.SG4_HIGH;
			default:
				return SmartGridState.UNDEFINED;
		}
	}
}