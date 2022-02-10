package io.openems.edge.evcs.mennekes;

import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Status;

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
        this.setStatus();
        this.parent._setChargePower((int) this.parent.getCurrentPower());
        this.parent._setChargingType(ChargingType.AC);
        this.parent._setEnergySession((int) this.parent.getCurrentEnergy());

        this.parent._setActiveConsumptionEnergy((long) this.parent.getCurrentEnergy());
    }

    private void setStatus() {
        switch (this.parent.getVehicleState()) {
            case (1):
                this.parent._setStatus(Status.STARTING);
                break;
            case (2):
                this.parent._setStatus(Status.READY_FOR_CHARGING);
                break;
            case (3):
                this.parent._setStatus(Status.CHARGING);
                break;
            case (4):
                this.parent._setStatus(Status.ERROR);
                break;
            case (5):
                this.parent._setStatus(Status.CHARGING_REJECTED);
        }
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


