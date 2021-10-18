package io.openems.edge.apartmentmodule.api;

/**
 * Valve Direction. Important for Configuration.
 * If a Valve is of {@link ValveType#ONE_MOTOR_ONE_DIRECTION} -> if the directional Relay is set to "On" ->
 * Either it Closes or opens a Valve.
 */
public enum ValveDirection {
    ACTIVATION_DIRECTIONAL_EQUALS_OPENING, ACTIVATION_DIRECTIONAL_EQUALS_CLOSING;
}
