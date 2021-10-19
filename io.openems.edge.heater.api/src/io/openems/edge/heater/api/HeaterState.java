package io.openems.edge.heater.api;

import io.openems.common.types.OptionsEnum;

/**
 * The possible states of a heater.
 */

public enum HeaterState implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    BLOCKED_OR_ERROR(0, "Heater operation is blocked by something"), //
    OFF(1, "Off"), //
    STANDBY(2, "Standby, waiting for commands"), //
    STARTING_UP_OR_PREHEAT(3, "Command to heat received, preparing to start heating"),
    RUNNING(4, "Heater is running"); //

    private int value;
    private String name;

    private HeaterState(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Check if the name of the HeaterState matches the given String.
     *
     * @param state name of the HeaterState
     * @return true if HeaterState contains a name like given state.
     */
    public static boolean contains(String state) {
        for (HeaterState currState : HeaterState.values()) {
            if (currState.name.equals(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the value of the HeaterState matches the given value.
     *
     * @param stateValue value of the HeaterState
     * @return true if HeaterState contains a name like given state.
     */
    public static boolean contains(int stateValue) {
        for (HeaterState currState : HeaterState.values()) {
            if (currState.value == stateValue) {
                return true;
            }
        }
        return false;
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