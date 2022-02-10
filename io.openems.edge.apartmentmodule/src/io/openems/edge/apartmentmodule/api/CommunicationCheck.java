package io.openems.edge.apartmentmodule.api;

import io.openems.common.types.OptionsEnum;

/**
 * Tells the ApartmentModule if the AM Connection is still up.
 */
public enum CommunicationCheck implements OptionsEnum {
    UNDEFINED(-1, "Undefined"), //
    WAITING(0, "Waiting for signal"), //
    RECEIVED(1, "Signal received"); //

    private final int value;
    private final String name;

    private CommunicationCheck(int value, String name) {
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