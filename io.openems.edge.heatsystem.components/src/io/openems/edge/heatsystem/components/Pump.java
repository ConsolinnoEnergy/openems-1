package io.openems.edge.heatsystem.components;

/**
 *
 */
public interface Pump extends HeatsystemComponent {
    /**
     * Sets the PowerLevel of the Pump. Values between 0-100% can be applied.
     *
     * @param percent the PowerLevel the Pump should be set to.
     */
    void setPowerLevel(double percent);
}
