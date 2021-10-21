package io.openems.edge.evcs.alfen;

import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Status;

/**
 * This WriteHandler writes the Values from the Internal Channels that where retrieved over Modbus into the correct OpenEms Channels.
 * External READ_ONLY Register -> Internal OpenEms
 */
public class AlfenWriteHandler {
    private final AlfenImpl parent;


    AlfenWriteHandler(AlfenImpl parent) {
        this.parent = parent;
    }

    void run() {
        this.setPhaseCount();
        this.setStatus();
        this.parent._setChargePower((int) this.parent.getApparentPowerSum());
        this.parent._setChargingType(ChargingType.AC);
        this.parent._setEnergySession((int) this.parent.getRealEnergyConsumedSum());
    }

    private void setStatus() {
        if (this.parent.getCurrentSum() > 0) {
            this.parent._setStatus(Status.CHARGING);
        } else {
            this.parent._setStatus(Status.UNDEFINED);
        }
    }

    /**
     * Writes the Amount of Phases in the Phase channel.
     */
    private void setPhaseCount() {
        int phases = 0;

        if (this.parent.getApparentPowerL1() >= 1) {
            phases += 1;
        }
        if (this.parent.getApparentPowerL2() >= 1) {
            phases += 1;
        }
        if (this.parent.getApparentPowerL3() >= 1) {
            phases += 1;
        }
        this.parent._setPhases(phases);

    }

}
