package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of run clearance.
 */

public enum Clearance implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	BLOCKED(0, "Blocked (Sperre)"), //
	CLEARANCE_1_COMPRESSOR(1, "Clearance 1 compressor (Freigabe 1 Verdichter)"), //
	CLEARANCE_2_COMPRESSORS(2, "Clearance 2 compressors (Freigabe 2 Verdichter)"); //

	private int value;
	private String name;

	private Clearance(int value, String name) {
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