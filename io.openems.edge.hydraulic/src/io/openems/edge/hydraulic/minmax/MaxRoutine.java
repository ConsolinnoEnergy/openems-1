package io.openems.edge.hydraulic.minmax;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

class MaxRoutine implements MinMaxRoutine {
    @Override
    public int executeRoutine(List<Integer> channels) {
        AtomicInteger previousMax = new AtomicInteger(Integer.MIN_VALUE);
        channels.forEach(entry -> previousMax.set(Math.max(previousMax.get(), entry)));
        return previousMax.get();
    }
}
