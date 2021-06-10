package io.openems.edge.consolinno.evcs.limiter;

import org.joda.time.DateTime;

class EvcsOnHold {
    private final int power;
    private final DateTime timestamp;
    private final int phases;

    /**
     * This Object contains the information to Identify a waiting EVCS(this is an internal Object and is designed to
     * be used in a Map where the key is the id.
     *
     * @param power     last known power request
     * @param timestamp time when the EVCS was turned off
     * @param phases    amount of phases the EVCS has
     */
    public EvcsOnHold(int power, DateTime timestamp, int phases) {
        this.power = power;
        this.timestamp = timestamp;
        this.phases = phases;
    }

    public int getPower() {
        return this.power;
    }

    public DateTime getTimestamp() {
        return this.timestamp;
    }

    public int getPhases() {
        if (this.phases == 0) {
            return 1;
        }
        return this.phases;
    }
}
