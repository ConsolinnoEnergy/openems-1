package io.openems.edge.utility.api;

import io.openems.edge.utility.minmax.MinMaxToChannel;

import java.util.List;

/**
 * The MinMaxRoutine interface.
 * Get's a List of Integer and return the Max/Min value.
 * The Implementing classes: {@link MinRoutine} and {@link MaxRoutine} use this interface to help out e.g. the
 * {@link MinMaxToChannel}
 */
public interface MinMaxRoutine {
    /**
     * Executes the Routine (Min/Max Value) of the given List.
     *
     * @param channelValues the Values of the Channel that were read.
     * @return the min/Max Value
     */
    int executeRoutine(List<Integer> channelValues);
}
