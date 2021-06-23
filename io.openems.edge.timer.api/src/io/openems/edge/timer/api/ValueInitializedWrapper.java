package io.openems.edge.timer.api;

import org.joda.time.DateTime;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ValueInitializedWrapper {

    private int maxValue;
    private boolean initialized;
    //only needed by CycleTimer
    private final AtomicInteger counter = new AtomicInteger(0);
    private final AtomicReference<DateTime> initialDateTime = new AtomicReference<>();

    public ValueInitializedWrapper(int maxValue, boolean initialized) {
        this.maxValue = maxValue;
        this.initialized = initialized;
        this.initialDateTime.set(new DateTime());
    }

    public ValueInitializedWrapper(int maxValue) {
        this(maxValue, false);
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
    }

    public AtomicInteger getCounter() {
        return counter;
    }

    public AtomicReference<DateTime> getInitialDateTime() {
        return initialDateTime;
    }
}
