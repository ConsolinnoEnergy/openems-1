package io.openems.edge.utility.minmax;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The MaxRoutine compares a List of Integer values and returns the max Value.
 * This helps the {@link MinMaxToChannel}
 */
public class MaxRoutine implements MinMaxRoutine {
    /**
     * Executes the Routine (Min/Max Value) of the given List.
     *
     * @param channelValues the Values of the Channel that were read.
     * @return the min/Max Value
     */
    @Override
    public int executeRoutine(List<Integer> channelValues) {
        AtomicInteger previousMax = new AtomicInteger(Integer.MIN_VALUE);
        channelValues.forEach(entry -> previousMax.set(Math.max(previousMax.get(), entry)));
        return previousMax.get();
    }

}
