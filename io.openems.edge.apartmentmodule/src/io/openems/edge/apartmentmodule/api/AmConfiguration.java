package io.openems.edge.apartmentmodule.api;

import io.openems.common.types.OptionsEnum;

/**
 * Check if the ApartmentModule is a Top or Bottom ApartmentModule.
 * Depends on the configuration.
 */
public enum AmConfiguration implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    BOTTOM(0, "Bottom"), //
    TOP(1, "Top"); //

    private final int value;
    private final String name;

    AmConfiguration(int value, String name) {
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