package io.openems.edge.bridge.genibus.api.task;

import io.openems.edge.bridge.genibus.api.PumpDevice;

/**
 * Description of what this class does.
 */

public abstract class AbstractPumpTask implements GenibusTask {

    UnitTable unitTable;
    int genibusUnitIndex = -66;
    double genibusUnitFactor = 1.0;
    String unitString;
    private final byte address;
    private final int headerNumber;
    int sif;    // Scale information factor
    boolean vi; // Value interpretation
    int range = 254;
    boolean bo; // Byte Order, 0 = HighOrder 1 = LowOrder
    int zeroScaleFactorHighOrder;
    int zeroScaleFactorLowOrder;
    int zeroScaleFactor;
    int rangeScaleFactor;
    private boolean informationAvailable = false;
    boolean wasAdded;
    PumpDevice pumpDevice;
    final int dataByteSize;
    private int apduIdentifier;

    public AbstractPumpTask(int address, int headerNumber, String unitString, int dataByteSize) {
        this.address = (byte) address;
        this.headerNumber = headerNumber;
        this.dataByteSize = dataByteSize;
        switch (unitString) {
            case "Standard":
            default:
                this.unitTable = UnitTable.Standard_Unit_Table;
        }

    }

    /**
     * Gets the address of the task. The address together with the header is the identifier for a data item in the pump.
     *
     * @return the address.
     */
    @Override
    public byte getAddress() {
        return this.address;
    }

    /**
     * Gets the header of the task. The header together with the address is the identifier for a data item in the pump.
     *
     * @return the header.
     */
    @Override
    public int getHeader() {
        return this.headerNumber;
    }

    /**
     * If a task has INFO and the INFO is of the one byte type, this is used to store the INFO content with the task.
     *
     * @param vi Value information if set range is 255 else 254. Comes from 5th bit
     * @param bo Byte order information. 0 is high order, 1 is low order byte. 4th bit
     * @param sif Scale information format. 0 = not available, 1= bitwise interpreted value.
     */
    @Override
    public void setOneByteInformation(int vi, int bo, int sif) {
        this.vi = vi != 0;
        this.bo = bo != 0;
        this.sif = sif;
        this.informationAvailable = true;
        if (this.vi) {
            this.range = 255;
        } else {
            this.range = 254;
        }
    }

    /**
     * Gets the Information written in 4 byte from the genibus bridge (handleResponse() method).
     *
     * @param vi                    see OneByteInformation.
     * @param bo                    see OneByteInformation.
     * @param sif                   see OneByteInformation.
     * @param zeroSignAndUnitIndex             index Number for the Unit of the task.
     * @param scaleFactorRangeOrLow range scale factor or low order byte.
     * @param scaleFactorZeroOrHigh either Zero scale factor or factor for high order byte
     *
     *                              <p> Unit calc depends on the unitString ---> unitCalc needed for default Channel Unit.
     *                              </p>
     */
    @Override
    public void setFourByteInformation(int vi, int bo, int sif, byte zeroSignAndUnitIndex, byte scaleFactorZeroOrHigh, byte scaleFactorRangeOrLow) {
        this.setOneByteInformation(vi, bo, sif);
        this.genibusUnitIndex = (zeroSignAndUnitIndex & 0b01111111);
        int zeroSign = 1;
        if ((zeroSignAndUnitIndex & 0b10000000) == 0b10000000) {
            zeroSign = -1;
        }
        if (sif == 3) {
            this.zeroScaleFactorHighOrder = zeroSign * Byte.toUnsignedInt(scaleFactorZeroOrHigh);
            this.zeroScaleFactorLowOrder = zeroSign * Byte.toUnsignedInt(scaleFactorRangeOrLow);
        } else {
            this.zeroScaleFactor = zeroSign * Byte.toUnsignedInt(scaleFactorZeroOrHigh);
            this.rangeScaleFactor = Byte.toUnsignedInt(scaleFactorRangeOrLow);
        }
        if (this.genibusUnitIndex > 0) {
            this.unitString = this.unitTable.getInformationData().get(this.genibusUnitIndex);
            this.genibusUnitFactor = this.unitTable.getGenibusUnitFactor(this.genibusUnitIndex);
        }

        // Extract pressure sensor interval.
        if (this.headerNumber == 2 && this.address == this.pumpDevice.getPressureSensorTaskAddress()) {
            this.pumpDevice.setPressureSensorMinBar(this.zeroScaleFactor * this.genibusUnitFactor);
            this.pumpDevice.setPressureSensorRangeBar(this.rangeScaleFactor * this.genibusUnitFactor);
        }
    }

    @Override
    public boolean informationDataAvailable() {
        return this.informationAvailable;
    }

    @Override
    public void resetInfo() {
        this.informationAvailable = false;
    }

    @Override
    public void setPumpDevice(PumpDevice pumpDevice) {
        this.pumpDevice = pumpDevice;
    }

    @Override
    public int getDataByteSize() {
        return this.dataByteSize;
    }

    @Override
    public void setApduIdentifier(int identifier) {
        this.apduIdentifier = identifier;
    }

    @Override
    public int getApduIdentifier() {
        return this.apduIdentifier;
    }

    @Override
    public String printInfo() {
        StringBuilder returnString = new StringBuilder();
        returnString.append("Task ").append(this.headerNumber).append(", ").append(Byte.toUnsignedInt(this.address)).append(" - ");
        if (this.headerNumber == 7) {
            returnString.append("ASCII");
            return returnString.toString();
        }
        if (this.informationAvailable) {
            returnString.append("Unit: ").append(this.unitString).append(", Format: ").append(this.dataByteSize * 8).append(" bit ");
            switch (this.sif) {
                case 1:
                    returnString.append("bit wise interpreted value");
                    break;
                case 2:
                    returnString.append("scaled value, min: ").append(this.zeroScaleFactor).append(", range: ").append(this.rangeScaleFactor);

                    break;
                case 3:
                    int exponent = this.dataByteSize - 2;
                    if (exponent < 0) {
                        exponent = 0;
                    }
                    returnString.append("extended precision, min: ").append(Math.pow(256, exponent) * (256 * this.zeroScaleFactorHighOrder + this.zeroScaleFactorLowOrder));
                    break;
                case 0:
                default:
                    returnString.append("no scale info available");
                    break;
            }
        } else {
            returnString.append("no INFO yet.");
        }
        return returnString.toString();
    }
}
