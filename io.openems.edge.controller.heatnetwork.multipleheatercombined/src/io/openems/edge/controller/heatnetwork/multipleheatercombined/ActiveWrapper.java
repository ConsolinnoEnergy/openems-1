package io.openems.edge.controller.heatnetwork.multipleheatercombined;

/**
 * This class is a Wrapper to to save the active value AND check if this heater should always run on Full power.
 */
class ActiveWrapper {

    private boolean active;


    ActiveWrapper(boolean active) {
        this.active = active;
    }

    ActiveWrapper() {
        this(false);
    }

    boolean isActive() {
        return this.active;
    }

    void setActive(boolean active) {
        this.active = active;
    }

}
