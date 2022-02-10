package io.openems.edge.thermometer.api;

import io.openems.common.types.OptionsEnum;

/**
 * Tells if the Temperature within the {@link ThermometerThreshold} is Rising or Falling.
 */
public enum ThermometerState implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    RISING(1, "Rising"),
    FALLING(0, "Falling");

    private final int value;
    private final String name;

    ThermometerState(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Returns ThermometerState depending on given Value. Usually an Internal Method.
     *
     * @param value the value of the ThermometerState
     * @return the ThermometerState of the Value.
     */
    public static ThermometerState getThermometerStateFromInteger(Integer value) {
        ThermometerState stateToReturn = UNDEFINED;
        for (ThermometerState state : ThermometerState.values()) {
            if (state.getValue() == value) {
                stateToReturn = state;
                break;
            }
        }
        return stateToReturn;
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
