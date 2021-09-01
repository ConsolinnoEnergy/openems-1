package io.openems.edge.controller.heatnetwork.cooling.multiplecoolercombined;

/**
 * This class is a Wrapper to to save the active value AND check if this cooler should always run on Full power.
 */
class CoolerActiveWrapper {

    private boolean active;

    CoolerActiveWrapper(boolean active) {
        this.active = active;
    }

    CoolerActiveWrapper() {
        this(false);
    }

    /**
     * Check if the corresponding cooler should be active or not.
     * @return true or false
     */
    boolean isActive() {
        return this.active;
    }

    /**
     * Sets if the Cooler should be active or not. Usually set by the MultiCooler
     * @param active the value
     */
    void setActive(boolean active) {
        this.active = active;
    }

}
