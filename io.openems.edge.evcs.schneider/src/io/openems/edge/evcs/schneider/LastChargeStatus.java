package io.openems.edge.evcs.schneider;

public enum LastChargeStatus {
    CIRCUIT_BREAKER_ENABLED(0),
    OK(1),
    ENDED_BY_CLUSTER_MANAGER_LOSS(2),
    END_OF_CHARGE_IN_SM3(3),
    COMMUNICATION_ERROR(4),
    DISCONNECTION_CABLE(5),
    DISCONNECTION_EV(6),
    SHORTCUT(7),
    OVERLOAD(8),
    CANCELED_BY_SUPERVISOR(9),
    VENTILATION_NOT_ALLOWED(10),
    UNEXPECTED_CONTACTOR_OPEN(11),
    SIMPLIFIED_MODE_3_NOT_ALLOWED(12),
    POWER_SUPPLY_INTERNAL_ERROR(13),
    UNEXPECTED_PLUG_UNLOCK(14),
    DEFAULT_NB_PHASES(15),
    DI_DEFAULT_SURGE_ARRESTOR(69),
    DI_DEFAULT_ANTI_INTRUSION(70),
    DI_DEFAULT_SHUTTER_UNLOCK(73),
    DI_DEFAULT_FLSI(74),
    DI_DEFAULT_EMERGENCY_STOP(75),
    DI_DEFAULT_UNDERVOLTAGE(76),
    DI_DEFAULT_CI(77),
    OTHER(254),
    UNDEFINED(255);
    private final int value;

    private LastChargeStatus(int value) {
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
