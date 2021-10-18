package io.openems.edge.evcs.wallbe;

public enum WallbeStatus {
    EVCS_FREE('A'),
    EV_CONNECTED('B'),
    CHARGING('C'),
    CHARGING2('D'),
    ERROR('E'),
    ERROR2('F');

    private final char value;

    private WallbeStatus(char value) {
        this.value = value;

    }

    /**
     * Get Error Value.
     *
     * @return Value
     */
    public int getValue() {
        return this.value;
    }
}
