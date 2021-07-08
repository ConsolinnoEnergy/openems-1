package io.openems.edge.controller.heatnetwork.communication.api;

import io.openems.common.types.OptionsEnum;

/**
 * Types of FallbackHandling.
 */
public enum FallbackHandling implements OptionsEnum {
    UNDEFINED(-1, "Undefined"),
    HEAT(1, "HEAT"),
    OPEN(2, "OPEN"),
    CLOSE(3, "CLOSE"),
    DEFAULT(4, "DEFAULT");

    private final int value;
    private final String name;


    FallbackHandling(int value, String name) {
        this.value = value;
        this.name = name;
    }

    protected static boolean contains(String handling) {
        for (FallbackHandling fallbackHandling : FallbackHandling.values()) {
            if (fallbackHandling.name().equals(handling)) {
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
