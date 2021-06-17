package io.openems.edge.controller.heatnetwork.multipleheatercombined;

/**
 * This class is a Wrapper to save the active value of a Heater / if the heater should run.
 */
class HeaterActiveWrapper {

    private boolean active;


    HeaterActiveWrapper(boolean active) {
        this.active = active;
    }

    HeaterActiveWrapper() {
        this(false);
    }

    /**
     * Should the Heater corresponding to this be activated.
     * Determined previously by {@link ThermometerWrapper}
     *
     * @return {@link #active}
     */

    boolean isActive() {
        return this.active;
    }

    /**
     * Setter for the {@link #active} boolean.
     *
     * @param active true or false determined by {@link ThermometerWrapper}.
     */

    void setActive(boolean active) {
        this.active = active;
    }

}
