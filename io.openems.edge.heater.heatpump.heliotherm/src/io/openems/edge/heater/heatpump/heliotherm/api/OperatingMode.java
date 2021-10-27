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
}