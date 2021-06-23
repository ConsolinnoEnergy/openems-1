package io.openems.edge.controller.heatnetwork.communication.request.api;

public enum ConfigPosition {

    REQUEST_TYPE_POSITION(0),
    MASTER_RESPONSE_TYPE_POSITION(1),
    METHOD_OR_CHANNEL_ADDRESS_POSITION(2),
    REQUEST_ACTIVE_VALUE_POSITION(3),
    REQUEST_NOT_ACTIVE_VALUE_POSITION(4);


    private final int value;

    ConfigPosition(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
