package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of block / release.
 */

public enum BlockRelease implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	BLOCKED(0, "Block heat pump (Sperre)"), //
	RELEASE_1_COMPRESSOR(1, "Release 1 compressor (Freigabe 1 Verdichter)"), //
	RELEASE_2_COMPRESSORS(2, "Release 2 compressors (Freigabe 2 Verdichter)"); //

	private final int value;
	private final String name;

	private BlockRelease(int value, String name) {
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