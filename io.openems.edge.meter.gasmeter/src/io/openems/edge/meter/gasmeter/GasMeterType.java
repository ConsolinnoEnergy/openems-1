package io.openems.edge.meter.gasmeter;

/**
 * Stored GasMeter types for easier configuration.
 */
public enum GasMeterType {
    PLACEHOLDER(0, 0, 0, 0, 0);

    int powerAddress;
    int percolationAddress;
    int totalConsumptionEnergyAddress;
    int flowTempAddress;
    int returnTempAddress;

    GasMeterType(int power, int percolation, int totalConsumptionEnergy, int flowTemp, int returnTemp) {
        this.powerAddress = power;
        this.percolationAddress = percolation;
        this.totalConsumptionEnergyAddress = totalConsumptionEnergy;

        this.flowTempAddress = flowTemp;
        this.returnTempAddress = returnTemp;
    }

    /**
     * Gets the address for the POWER Channel of the Meter.
     *
     * @return the address.
     */

    public int getPowerAddress() {
        return this.powerAddress;
    }

    /**
     * Get the address for the PERCOLATION Channel of the GasMeter.
     *
     * @return the address.
     */

    public int getPercolationAddress() {
        return this.percolationAddress;
    }

    /**
     * Gets the address for the TotalConsumptionEnergyAddress. It's not important if Channel Measured in m³ or kW.
     *
     * @return the address.
     */

    public int getTotalConsumptionEnergyAddress() {
        return this.totalConsumptionEnergyAddress;
    }

    /**
     * Gets the address for the FlowTemperature.
     *
     * @return the address.
     */

    public int getFlowTempAddress() {
        return this.flowTempAddress;
    }

    /**
     * Get the address for the RETURN_TEMPERATURE.
     *
     * @return the address.
     */

    public int getReturnTempAddress() {
        return this.returnTempAddress;
    }
}
