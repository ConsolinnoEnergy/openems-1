package io.openems.edge.evcs.schneider;

public enum EVCSEError {
    LOST_COMMUNICATION_WITH_RFID_READER(23, 0),
    LOST_COMMUNICATION_WITH_DISPLAY(23, 1),
    CANNOT_CONNECT_TO_MASTER_BOARD(23, 2),
    INCORRECT_PLUG_LOCK_STATE(23, 3),
    INCORRECT_CONTACTOR_STATE(23, 4),
    INCORRECT_SURGE_ARRESTOR_STATE(23, 5),
    INCORRECT_ANTI_INTRUSION_STATE(23, 6),
    CANNOT_CONNECT_TO_US_DAUGHTER_BOARD(23, 7),
    CONFIGURATION_FILE_MISSING(23, 8),
    INCORRECT_SHUTTER_LOCK_STATE(23, 9),
    INCORRECT_CIRCUIT_BREAKER_STATE(23, 10),
    LOST_COMMUNICATION_WITH_POWERMETER(23, 11),
    REMOTE_CONTROLLER_LOST(23, 12),
    INCORRECT_SOCKET_STATE(23, 13),
    INCORRECT_CHARGING_PHASE_NUMBER(23, 14),
    LOST_COMMUNICATION_WITH_CLUSTER_MANAGER(23, 15),
    MODE3_COMMUNICATION_ERROR(24, 0),
    INCORRECT_CABLE_STATE(24, 1),
    DEFAULT_EV_CHARGING_CABLE_DISCONNECTION(24, 2),
    SHORT_CIRCUIT_FP1(24, 3),
    OVERCURRENT(24, 4),
    NO_ENERGY_AVAILABLE_FOR_CHARGING(24, 5);
    private final int address;
    private final int bit;

    private EVCSEError(int address, int bit) {
        this.address = address;
        this.bit = bit;

    }

    /**
     * Get Error Address.
     *
     * @return Value
     */
    public int getAddress() {
        return this.address;
    }

    /**
     * Get Error Bit Position.
     *
     * @return Value
     */
    public int getBit() {
        return this.bit;
    }
}
