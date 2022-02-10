package io.openems.edge.apartmentmodule.api;

import io.openems.common.types.OptionsEnum;

/**
 * The Valve Status of the TopAm Valve.
 * This will be set by the AM Implementation.
 */
public enum ValveStatus implements OptionsEnum {
    CLOSED(0, "Closed"),
    OPEN(1, "Open"),
    OPENING(2, "Opening"),
    CLOSING(3, "Closing"),
    ERROR(4, "Error Both Relays are Active"),
    UNDEFINED(-1, "Undefined");

    private final int value;
    private final String name;

    ValveStatus(int value, String name) {
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
