package io.openems.edge.meter.gasmeter;

/**
 * Stored GasMeter types for easier configuration.
 */
public enum GasMeterType {
    AUTOSEARCH(0,1),
    PAD_PULS_M2(0,1),
    ITRON_CYBLE(4,2);

    int totalConsumptionEnergyAddress;
    int time;

    GasMeterType(int totalConsumptionEnergy,int time) {
        this.totalConsumptionEnergyAddress = totalConsumptionEnergy;
        this.time = time;
    }
    /**
     * Gets the address for the TotalConsumptionEnergyAddress. It's not important if Channel Measured in m³ or kW.
     *
     * @return the address.
     */

    public int getTotalConsumptionEnergyAddress() {
        return this.totalConsumptionEnergyAddress;
    }

    public int getTime(){return this.time;}
}
