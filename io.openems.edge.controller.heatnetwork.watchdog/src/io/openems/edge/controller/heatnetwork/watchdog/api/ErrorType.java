package io.openems.edge.controller.heatnetwork.watchdog.api;

import io.openems.common.types.OptionsEnum;

public enum ErrorType implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    FLOW_TEMP_TOO_LOW(0, "FlowTempTooLow"),
    VALVE_ERROR(1, "ValveError"),
    PUMP_ERROR(2, "PumpError"),
    PROTECTION_WAS_ACTIVE(3, "ProtectionWasActive");
    

    private final int value;
    private final String name;

    ErrorType(int value, String name) {
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
     * Check if the name of the ErrorTypes matches the given String.
     *
     * @param state name of the ErrorTypes
     * @return true if ErrorTypes contains a name like given state.
     */
    public static boolean contains(String state) {
        for (ErrorType currState : ErrorType.values()) {
            if (currState.name.equals(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if the value of the ErrorTypes matches the given value.
     *
     * @param stateValue value of the ErrorTypes
     * @return true if ErrorTypes contains a name like given state.
     */
    public static boolean contains(int stateValue) {
        for (ErrorType currState : ErrorType.values()) {
            if (currState.value == stateValue) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the enum state corresponding to the integer value.
     *
     * @param value the integer value of the enum
     * @return the enum state
     */
    public static ErrorType valueOf(int value) {
        ErrorType returnEnum = ErrorType.UNDEFINED;
        switch (value) {
            case 0:
                returnEnum = ErrorType.FLOW_TEMP_TOO_LOW;
                break;
            case 1:
                returnEnum = ErrorType.VALVE_ERROR;
                break;
            case 2:
                returnEnum = ErrorType.PUMP_ERROR;
                break;
            case 3:
                returnEnum = ErrorType.PROTECTION_WAS_ACTIVE;
                break;
        }
        return returnEnum;
    }

}
