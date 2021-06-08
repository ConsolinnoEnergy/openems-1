package io.openems.edge.hydraulic.minmax;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class MinRoutine implements MinMaxRoutine {


    @Override
    public int executeRoutine(List<Integer> channelValues) {
        AtomicInteger previousMin = new AtomicInteger(Integer.MAX_VALUE);
        channelValues.forEach(entry -> {
            previousMin.set(Math.min(previousMin.get(), entry));
        });
        return previousMin.get();
    }
}
