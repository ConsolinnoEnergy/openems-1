package io.openems.edge.meter.mbus.electricity;

public enum ElectricityMeterModel {
    //ElectricityMeter Types with their address for Mbus
    ABB_B23_113(0,-1,-1,1,2, 3, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1),
    ELTAKO_DSZ15DM(0,2,17,6,10,14,18,7,11,15,4,8,12,5,9,13);


    int totalConsumptionEnergy1;
    int totalConsumptionEnergy2;
    int activePower;
    int activePowerL1;
    int activePowerL2;
    int activePowerL3;
    int reactivePower;
    int reactivePowerL1;
    int reactivePowerL2;
    int reactivePowerL3;
    int voltageL1;
    int voltageL2;
    int voltageL3;
    int amperageL1;
    int amperageL2;
    int amperageL3;


    ElectricityMeterModel(int totalConsumptionEnergy1Address, int totalConsumptionEnergy2Address, int activePowerAddress, int activePowerL1Address, int activePowerL2Address, int activePowerL3Address, int reactivePowerAddress, int reactivePowerL1Address, int reactivePowerL2Address, int reactivePowerL3Address, int voltageL1Address, int voltageL2Address, int voltageL3Address, int amperageL1Address, int amperageL2Address, int amperageL3Address) {
        this.totalConsumptionEnergy1 = totalConsumptionEnergy1Address;
        this.totalConsumptionEnergy2 = totalConsumptionEnergy2Address;
        this.activePower = activePowerAddress;
        this.activePowerL1 = activePowerL1Address;
        this.activePowerL2 = activePowerL2Address;
        this.activePowerL3 = activePowerL3Address;
        this.reactivePower = reactivePowerAddress;
        this.reactivePowerL1 = reactivePowerL1Address;
        this.reactivePowerL2 = reactivePowerL2Address;
        this.reactivePowerL3 = reactivePowerL3Address;
        this.voltageL1 = voltageL1Address;
        this.voltageL2 = voltageL2Address;
        this.voltageL3 = voltageL3Address;
        this.amperageL1 = amperageL1Address;
        this.amperageL2 = amperageL2Address;
        this.amperageL3 = amperageL3Address;
    }


    public int getTotalConsumptionEnergy1Address() {
        return totalConsumptionEnergy1;
    }

    public int getTotalConsumptionEnergy2Address() {
        return totalConsumptionEnergy2;
    }

    public int getActivePowerAddress() {
        return activePower;
    }

    public int getActivePowerL1Address() {
        return activePowerL1;
    }

    public int getActivePowerL2Address() {
        return activePowerL2;
    }

    public int getActivePowerL3Address() {
        return activePowerL3;
    }

    public int getReactivePowerAddress() {
        return reactivePower;
    }
    public int getReactivePowerL1Address() {
        return reactivePowerL1;
    }

    public int getReactivePowerL2Address() {
        return reactivePowerL2;
    }

    public int getReactivePowerL3Address() {
        return reactivePowerL3;
    }

    public int getVoltageL1Address() {
        return voltageL1;
    }

    public int getVoltageL2Address() {
        return voltageL2;
    }

    public int getVoltageL3Address() {
        return voltageL3;
    }

    public int getAmperageL1Address() {
        return amperageL1;
    }

    public int getAmperageL2Address() {
        return amperageL2;
    }

    public int getAmperageL3Address() {
        return amperageL3;
    }

}