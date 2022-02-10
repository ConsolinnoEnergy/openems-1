package io.openems.edge.controller.optimizer;


/**
 * Indices of the Values in the Schedule List.
 */
public enum Index {

    COMPONENT_INDEX(0), //Index of the ID of the Component
    SCHEDULE_INDEX(1), //Index of the Schedule String
    CHANNEL_INDEX(2), //Index of the Channel ID
    ENABLE_CHANNEL_INDEX(3); //Index of the Optional EnableSignal Channel ID
    private final int numVal;

    Index(int numVal) {
        this.numVal = numVal;
    }

    /**
     * Get the Numerical Value behind the indices.
     *
     * @return Integer value.
     */
    public int getNumVal() {
        return this.numVal;
    }

}
