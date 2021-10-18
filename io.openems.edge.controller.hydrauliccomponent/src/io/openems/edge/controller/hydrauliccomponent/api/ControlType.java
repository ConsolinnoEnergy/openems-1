package io.openems.edge.controller.hydrauliccomponent.api;

public enum ControlType {
    POSITION, TEMPERATURE;


    public static boolean contains(String request) {
        for (ControlType controlType : ControlType.values()) {
            if (controlType.name().equals(request)) {
                return true;
            }
        }
        return false;
    }
}
