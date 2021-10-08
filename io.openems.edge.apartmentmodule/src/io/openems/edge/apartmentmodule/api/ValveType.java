package io.openems.edge.apartmentmodule.api;

/**
 * Is the Valve either: A Valve that has one relay for opening(config -> which one is opening) and one for closing
 * or
 * Is has the Valve one Motor and one direction Relay (config -> which one is Direction) where Set the direction Relay to on
 * defines opening or closing the Valve.
 */
public enum ValveType {
    ONE_OPEN_ONE_CLOSE, ONE_MOTOR_ONE_DIRECTION; //no pun intended
}
