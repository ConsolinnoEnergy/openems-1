package io.openems.edge.pump.grundfos.api;

public enum PumpMode {
	UNKNOWN("unknown"), //
	STOP("Stop"), //
	CONST_FREQU_MAX("Constant frequency - Max"), //
	CONST_FREQU_MIN("Constant frequency - Min"), //
	CONST_PRESSURE("Constant pressure"), //
	PROP_PRESSURE("Proportional pressure"), //
	CONST_FREQU("Constant frequency"), //
	AUTO_ADAPT("AutoAdapt"), //
	CONST_TEMP("Constant temperature"), //
	CLOSED_LOOP_SENSOR_CONTROL("Closed loop sensor control"), //
	CONST_FLOW("Constant flow"), //
	CONST_LEVEL("Constant level"), //
	FLOW_ADAPT("FlowAdapt"), //
	CONST_DIFF_PRESSURE("Constant differential pressure"), //
	CONST_DIFF_TEMP("Constant differential temperature"), //
	HAND_MODE("Hand mode"), //
	AUTO_ADAPT_OR_FLOW_ADAPT("AutoAdapt or FlowAdapt"), //
	OTHER("Other"), //
	TEST("Test");

	private final String name;

	PumpMode(String name) {
		this.name = name;
	}

	/**
	 * Get the name.
	 * @return the name.
	 */
	public String getName() {
		return this.name;
	}
}
