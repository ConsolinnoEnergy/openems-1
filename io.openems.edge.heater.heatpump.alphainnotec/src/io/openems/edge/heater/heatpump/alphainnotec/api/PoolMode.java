package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the pool mode.
 */

public enum PoolMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	AUTOMATIC(0, "Automatic"), //
	VALUE_NOT_USED(1, "Value not used (Wert nicht benutzt)"), //
	PARTY(2, "No night setback (Party)"), //
	VACATION(3, "Holidays, full time setback (Ferien)"), //
	OFF(4, "Off"); //

	private final int value;
	private final String name;

	private PoolMode(int value, String name) {
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
	public static PoolMode valueOf(int value) {
		switch (value) {
			case 0:
				return PoolMode.AUTOMATIC;
			case 1:
				return PoolMode.VALUE_NOT_USED;
			case 2:
				return PoolMode.PARTY;
			case 3:
				return PoolMode.VACATION;
			case 4:
				return PoolMode.OFF;
			default:
				return PoolMode.UNDEFINED;
		}
	}
}