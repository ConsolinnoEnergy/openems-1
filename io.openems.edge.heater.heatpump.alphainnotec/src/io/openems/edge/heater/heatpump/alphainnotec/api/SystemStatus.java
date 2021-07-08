package io.openems.edge.heater.heatpump.alphainnotec.api;

import io.openems.common.types.OptionsEnum;

/**
 * Possible states of the system status.
 */

public enum SystemStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ROOM_HEATING(0, "Room heating mode (Heizbetrieb)"), //
	DOMESTIC_HOT_WATER_HEATING(1, "Domestic hot water heating (Trinkwarmwasser)"), //
	POOL_HEATING(2, "Swimming pool heating (Schwimmbad)"), //
	BLOCKED(3, "Electric supplier block (EVU-Sperre)"), //
	DEFROST(4, "Defrost (Abtauen)"), //
	OFF(5, "Off"), //
	EXTERNAL_ENERGY_SOURCE(6, "External energy source (Externe Energiequelle)"), //
	COOLING(7, "Cooling (Kühlung)"); //

	private int value;
	private String name;

	private SystemStatus(int value, String name) {
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