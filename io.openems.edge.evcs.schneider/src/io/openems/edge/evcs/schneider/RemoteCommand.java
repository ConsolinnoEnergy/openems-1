package io.openems.edge.evcs.schneider;

public enum RemoteCommand {

    ACKNOWLEDGE_COMMAND(0),
    FORCE_STOP_CHARGE(3),
    SUSPEND_CHARGING(4),
    RESTART_CHARGING(5),
    SET_EVCSE_UNAVAILABLE(6),
    SET_EVCSE_AVAILABLE(34)
    ;
    private final int value;

    private RemoteCommand(int value) {
        this.value = value;

    }

    public int getValue() {
        return this.value;
    }
}
