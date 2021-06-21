package io.openems.edge.controller.heatnetwork.communication.request.api;

public enum MethodTypes {
    LOG_INFO, ACTIVATE_PUMP, ACTIVATE_LINE_HEATER;


    protected static boolean contains(String type) {
        for (MethodTypes methodTypes : MethodTypes.values()) {
            if (methodTypes.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
