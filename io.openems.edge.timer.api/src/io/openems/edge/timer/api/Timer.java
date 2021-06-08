package io.openems.edge.timer.api;

import java.util.Map;

public interface Timer {
    /**
     * Resets the Timer for the Component calling this method. Multiple Timer per config are possible.
     * @param id the openemsComponent id
     * @param identifier the identifier the component uses
     */
    void reset(String id, String identifier);

    /**
     * Check if the Time for this Component is up.
     * @param id the OpenemsComponent Id.
     * @param identifier the identifier the component uses.
     * @return true if Time is up.
     */
    boolean checkIsTimeUp(String id, String identifier);

    /**
     * Adds a Component with it's corresponding identifier to Time Map to the Timer.
     * e.g. if a Component ControllerFoo needs 2 separate Timer with identifier Bar and a time of 10 and Bar2 and a time of 20
     * the Map will look something like <Bar, 10>
     *                                  <Bar2, 20>
     * @param id the OpenemsComponent ID
     * @param identifierToTime the Map of the Identifier to a Time/CycleCount
     *
     */
    void addComponentToTimer(String id, Map<String, Integer> identifierToTime);

    /**
     * Removes the Component from the Timer
     * @param id of the Component you want to remove
     */
    void removeComponent(String id);

    /**
     * Adds a Identifier to the Timer
     * @param id the ComponentId
     * @param identifier the identifier
     * @param maxValue the maxValue (max CycleTime or maxTime to wait)
     */
    void addIdentifierToTimer(String id, String identifier, int maxValue);
}
