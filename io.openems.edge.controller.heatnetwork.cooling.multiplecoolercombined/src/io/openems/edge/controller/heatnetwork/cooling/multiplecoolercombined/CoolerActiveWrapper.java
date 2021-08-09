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

    boolean isActive() {
        return this.active;
    }

    void setActive(boolean active) {
        this.active = active;
    }

}
