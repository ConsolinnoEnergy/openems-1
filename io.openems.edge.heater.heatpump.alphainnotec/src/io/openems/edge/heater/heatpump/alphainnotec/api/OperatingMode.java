package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the operating mode.
 */

public enum OperatingMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	HEIZBETRIEB(0, "Heizbetrieb"), //
	TRINKWARMWASSER(1, "Trinkwarmwasser"), //
	SCHWIMMBAD(2, "Schwimmbad"), //
	EVU_SPERRE(3, "EVU-Sperre"), //
	ABTAUEN(4, "Abtauen"), //
	OFF(5, "Off"), //
	EXTERNE_ENERGIEQUELLE(6, "Externe Energiequelle"), //
	KUEHLUNG(7, "Kühlung"); //

	private int value;
	private String name;

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