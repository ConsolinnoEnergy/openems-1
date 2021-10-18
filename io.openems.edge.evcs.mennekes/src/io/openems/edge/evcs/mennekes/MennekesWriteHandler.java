package io.openems.edge.evcs.mennekes;

import io.openems.edge.evcs.api.ChargingType;

/**
 * This WriteHandler writes the Values from the Internal Channels that where retrieved over Modbus into the correct OpenEms Channels.
 * External READ_ONLY Register -> Internal OpenEms
 */
public class MennekesWriteHandler {
    private final MennekesImpl parent;

    MennekesWriteHandler(MennekesImpl parent) {
        this.parent = parent;
    }

    void run() {
        this.setPhaseCount();
        this.parent._setChargePower((int) this.parent.getCurrentPower());
        this.parent._setChargingType(ChargingType.AC);
        this.parent._setEnergySession((int) this.parent.getCurrentEnergy());

        this.parent._setActiveConsumptionEnergy((long) this.parent.getCurrentEnergy());
    }

    /**
     * Writes the Amount of Phases in the Phase channel.
     */
    private void setPhaseCount() {
        int phases = 0;

        if (this.parent.getPowerL1() >= 1) {
            phases += 1;
        }
        if (this.parent.getPowerL2() >= 1) {
            phases += 1;
        }
        if (this.parent.getPowerL3() >= 1) {
            phases += 1;
        }
        this.parent._setPhases(phases);

    }

}


