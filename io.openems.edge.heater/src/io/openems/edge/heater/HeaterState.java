package io.openems.edge.heater;

public enum HeaterState {
    OFFLINE, AWAIT, PREHEAT, RUNNING, ERROR, WARNING, UNDEFINED, PRE_COOL;


    public static boolean contains(String requestetState) {
        for (HeaterState heaterState : HeaterState.values()) {
            if (heaterState.name().equals(requestetState)) {
                return true;
            }
        }
        return false;
    }
}
