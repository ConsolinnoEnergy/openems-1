package io.openems.edge.bridge.genibus.api.task;

import io.openems.edge.bridge.genibus.api.PumpDevice;

/**
 * Description of what this class does.
 */

public abstract class AbstractPumpTask implements GenibusTask {

    double unitCalc;
    String unitString;
    UnitTable unitTable;
    private final byte address;
    private final int headerNumber;
    //Scale information Factor
    int sif;
    //Value interpretation
    //
    boolean vi;
    int range = 254;
    //Byte Order 0 = HighOrder 1 = Low Order
    //
    boolean bo;
    int unitIndex = -66;
    int scaleFactorHighOrder;
    int scaleFactorLowOrder;
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
     * @param unitIndex             index Number for the Unit of the task.
     * @param scaleFactorRangeOrLow range scale factor or low order byte.
     * @param scaleFactorZeroOrHigh either Zero scale factor or factor for high order byte
     *
     *                              <p> Unit calc depends on the unitString ---> unitCalc needed for default Channel Unit.
     *                              </p>
     */
    @Override
    public void setFourByteInformation(int vi, int bo, int sif, byte unitIndex, byte scaleFactorZeroOrHigh, byte scaleFactorRangeOrLow) {
        this.setOneByteInformation(vi, bo, sif);
        this.unitIndex = (unitIndex & 127);
        if (sif == 3) {
            this.scaleFactorHighOrder = Byte.toUnsignedInt(scaleFactorZeroOrHigh);
            this.scaleFactorLowOrder = Byte.toUnsignedInt(scaleFactorRangeOrLow);
        } else {
            this.zeroScaleFactor = Byte.toUnsignedInt(scaleFactorZeroOrHigh);
            this.rangeScaleFactor = Byte.toUnsignedInt(scaleFactorRangeOrLow);
        }
        if (this.unitIndex > 0) {
            this.unitString = this.unitTable.getInformationData().get(this.unitIndex);

            if (this.unitString != null) {
                switch (this.unitString) {
                    // All pressure units are converted to bar

                    case "Celsius/10":
                    case "bar/10":
                    case "m":   // <- this is a pressure unit = bar/10
                    case "Ampere*0.1":
                    case "0.1*m³/h":
                    case "10%":
                        this.unitCalc = 0.1;
                        break;

                    case "Kelvin/100":
                    case "diff-Kelvin/100":
                    case "bar/100":
                    case "m/10":
                    case "kPa":
                    case "0.01*Hz":
                    case "1%":
                        this.unitCalc = 0.01;
                        break;

                    case "bar/1000":
                    case "m/100":
                    case "0.1%":
                        this.unitCalc = 0.001;
                        break;

                    case "0.01%":
                        this.unitCalc = 0.0001;
                        break;

                    case "ppm":
                    case "m/10000":
                        this.unitCalc = 0.000001;
                        break;

                    case "psi":
                        this.unitCalc = 0.06895;
                        break;

                    case "psi*10":
                        this.unitCalc = 0.6895;
                        break;

                    case "2*Hz":
                        this.unitCalc = 2.0;
                        break;

                    case "2.5*Hz":
                        this.unitCalc = 2.5;
                        break;

                    case "5*m³/h":
                        this.unitCalc = 5.0;
                        break;

                    case "Watt*10":
                    case "10*m³/h":
                        this.unitCalc = 10.0;
                        break;

                    case "Watt*100":
                        this.unitCalc = 100.0;
                        break;

                    case "kW":
                        this.unitCalc = 1000.0;
                        break;

                    case "kW*10":
                        this.unitCalc = 10000.0;
                        break;

                    case "Celsius":
                    case "Fahrenheit":  // <- conversion to °C in PumpReadTask.java
                    case "Kelvin":      // <- conversion to °C in PumpReadTask.java
                    case "diff-Kelvin":
                    case "Watt":
                    case "bar":
                    case "m*10":
                    case "m³/h":
                    case "Hz":
                    default:
                        this.unitCalc = 1.0;
                }

                if ((unitIndex & 128) == 128) {
                    this.unitCalc = this.unitCalc * (-1);
                }
            }
        }

        // Extract pressure sensor interval from h (used to be h_diff (2, 23), but that does not work with pump MGE).
        if (this.headerNumber == 2 && this.address == 37) {
            this.pumpDevice.setPressureSensorMinBar(this.zeroScaleFactor * this.unitCalc);
            this.pumpDevice.setPressureSensorRangeBar(this.rangeScaleFactor * this.unitCalc);
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
                    returnString.append("extended precision, min: ").append(Math.pow(256, exponent) * (256 * this.scaleFactorHighOrder + this.scaleFactorLowOrder));
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
