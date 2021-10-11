package io.openems.edge.controller.heatnetwork.apartmentmodule.api;

import io.openems.common.types.OptionsEnum;

/**
 * The State of the ApartmentModuleController.
 */
public enum ApartmentModuleControllerState implements OptionsEnum {
    UNDEFINED(-1, "Undefined"),
    IDLE(0, "Idle"),
    HEAT_PUMP_ACTIVE(1, "HeatPumpActive"),
    EXTRA_HEAT(2, "ExtraHeat"),
    EMERGENCY_ON(20, "EmergencyOn"),
    EMERGENCY_STOP(30, "EmergencyOff");

    private final int value;
    private final String name;


    ApartmentModuleControllerState(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Returns an ApartmentModuleControllerState from an integer Value. Usually called by the Nature itself.
     *
     * @param value the IntegerValue
     * @return the state or if not found undefined.
     */
    public static ApartmentModuleControllerState getStateFromIntValue(int value) {
        ApartmentModuleControllerState stateToReturn = ApartmentModuleControllerState.UNDEFINED;
        for (ApartmentModuleControllerState state : ApartmentModuleControllerState.values()) {
            if (state.getValue() == value) {
                stateToReturn = state;
                break;
            }
        }
        return stateToReturn;
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
