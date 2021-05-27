package io.openems.edge.consolinno.evcs.limiter;

class EvcsOnHold {
    private final int power;
    private final int timestamp;
    private final int phases;

    /**
     * This Object contains the information to Identify a waiting EVCS(this is an internal Object and is designed to
     * be used in a Map where the key is the id.
     *
     * @param power     last known power request
     * @param timestamp time when the EVCS was turned off
     * @param phases    amount of phases the EVCS has
     */
    public EvcsOnHold(int power, int timestamp, int phases) {
        this.power = power;
        this.timestamp = timestamp;
        this.phases = phases;
    }

    public int getPower() {
        return this.power;
    }

    public int getTimestamp() {
        return this.timestamp;
    }

    public int getPhases() {
        return this.phases;
    }
}
