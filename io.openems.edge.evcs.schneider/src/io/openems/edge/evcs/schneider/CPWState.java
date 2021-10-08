package io.openems.edge.evcs.schneider;

public enum CPWState {
    EVSE_NOT_AVAILABLE(0, "F"),
    EVSE_AVAILABLE(1,"A"),
    PLUG_DETECTED(2,"A+"),
    EV_CONNECTED(4,"B"),
    EV_CONNECTED2(5,"C-"),
    EV_CONNECTED_VENTILATION_REQUIRED(6,"D-"),
    EVSE_READY(7,"B+"),
    EV_READY(8,"C"),
    CHARGING(9,"C+"),
    EV_READY_VENTILATION_REQUIRED(10,"D"),
    CHARGING_VENTILATION_REQUIRED(11,"D+"),
    STOP_CHARGING(12,""),
    ALARM(13,""),
    SHORTCUT(14,"E"),
    DIGITAL_COM_BY_EVSE_STATE(15,"");
    private final int value;
    private final String state;

    private CPWState(int value, String state) {
        this.value = value;
        this.state = state;
    }

    /**
     * Get Status Value.
     *
     * @return Value
     */
    public int getValue() {
        return this.value;
    }

    /**
     * Get Status state.
     *
     * @return Value
     */
    public String getState() {
        return this.state;
    }
}
