package io.openems.edge.heater.cluster.api;

import io.openems.common.types.OptionsEnum;

public enum ClusterState implements OptionsEnum {
    UNDEFINED(-1, "Undefined"),
    OFF(0, "Offline"),
    OK(1, "Ok"),
    NOT_ENOUGH_POWER(2, "Not Enough Power");


    private final int value;
    private final String name;

    ClusterState(int value, String name) {
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
