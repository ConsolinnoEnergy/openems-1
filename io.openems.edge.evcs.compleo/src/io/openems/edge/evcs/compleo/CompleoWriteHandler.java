package io.openems.edge.evcs.compleo;

import io.openems.edge.evcs.api.ChargingType;

public class CompleoWriteHandler {
    private final CompleoImpl parent;

    CompleoWriteHandler(CompleoImpl parent) {
        this.parent = parent;
    }

    void run() {
        this.setPhaseCount();
        this.parent._setChargePower(this.parent.getPower() / 100);
        this.parent._setChargingType(ChargingType.AC);
        this.parent._setEnergySession(this.parent.getEnergy() / 100);
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
