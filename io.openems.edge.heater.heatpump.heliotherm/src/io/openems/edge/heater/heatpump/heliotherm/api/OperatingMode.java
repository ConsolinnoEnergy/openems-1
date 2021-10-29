package io.openems.edge.heater.heatpump.heliotherm.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the operating mode.
 */

public enum OperatingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(0, "Off"), //
	AUTOMATIC(1, "Automatic"), //
	COOLING(2, "Cooling"), //
	SUMMER(3, "Summer"), //
	ALWAYS_ON(4, "Always on (Dauerbetrieb)"), //
	SETBACK(5, "Setback mode (Absenkung)"), //
	VACATION(6, "Holidays, full time setback (Urlaub)"), //
	PARTY(7, "No night setback (Party)"), //
	BAKE_OUT(8, "Bake out mode (Ausheizen)"), //
	DSM_BLOCK(9, "Demand side management block (EVU Sperre)"), //
	MAIN_SWITCH_OFF(10, "Main switch off (Hauptschalter aus)"); //

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
	 * Returns the enum state corresponding to the integer value.
	 *
	 * @param value the integer value of the enum
	 * @return the enum state
	 */
	public static OperatingMode valueOf(int value) {
		switch (value) {
			case 0:
				return OperatingMode.OFF;
			case 1:
				return OperatingMode.AUTOMATIC;
			case 2:
				return OperatingMode.COOLING;
			case 3:
				return OperatingMode.SUMMER;
			case 4:
				return OperatingMode.ALWAYS_ON;
			case 5:
				return OperatingMode.SETBACK;
			case 6:
				return OperatingMode.VACATION;
			case 7:
				return OperatingMode.PARTY;
			case 8:
				return OperatingMode.BAKE_OUT;
			case 9:
				return OperatingMode.DSM_BLOCK;
			case 10:
				return OperatingMode.MAIN_SWITCH_OFF;
			default:
				return OperatingMode.UNDEFINED;
		}
	}
}