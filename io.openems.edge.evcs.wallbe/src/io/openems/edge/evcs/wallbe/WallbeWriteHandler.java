package io.openems.edge.evcs.wallbe;

import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Status;

/**
 * This WriteHandler writes the Values from the Internal Channels that where retrieved over Modbus into the correct OpenEms Channels.
 * External READ_ONLY Register -> Internal OpenEms
 */
public class WallbeWriteHandler {
    private final WallbeImpl parent;

    WallbeWriteHandler(WallbeImpl parent) {
        this.parent = parent;
    }

    void run() {
        this.setPhaseCount();
        this.parent._setChargePower(this.parent.getApparentPower());
        this.parent._setChargingType(ChargingType.AC);
        this.parent._setEnergySession(this.parent.getEnergy() * 1000);
        this.setStatus();
    }

    private void setStatus() {
        String status = this.parent.getWallbeStatus();
        switch (status) {
            case "A":
                this.parent._setStatus(Status.NOT_READY_FOR_CHARGING);
                break;
            case "B":
                this.parent._setStatus(Status.READY_FOR_CHARGING);
                break;
            case "C":
            case "D":
                this.parent._setStatus(Status.CHARGING);
                break;
            case "E":
            case "F":
                this.parent._setStatus(Status.ERROR);
                break;
        }
    }

    /**
     * Writes the Amount of Phases in the Phase channel.
     */
    private void setPhaseCount() {
        int phases = 0;

        if (this.parent.getCurrentL1() >= 1) {
            phases += 1;
        }
        if (this.parent.getCurrentL2() >= 1) {
            phases += 1;
        }
        if (this.parent.getCurrentL3() >= 1) {
            phases += 1;
        }
        this.parent._setPhases(phases);

    }

}


