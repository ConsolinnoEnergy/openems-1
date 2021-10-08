package io.openems.edge.utility.minmax;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Equivalent to the {@link MaxRoutine} this returns the Min Value of a List of Integer.
 */
public class MinRoutine implements MinMaxRoutine {

    /**
     * Executes the Routine (Min/Max Value) of the given List.
     *
     * @param channelValues the Values of the Channel that were read.
     * @return the min/Max Value
     */
    @Override
    public int executeRoutine(List<Integer> channelValues) {
        AtomicInteger previousMin = new AtomicInteger(Integer.MAX_VALUE);
        channelValues.forEach(entry -> {
            previousMin.set(Math.min(previousMin.get(), entry));
        });
        return previousMin.get();
    }
}
