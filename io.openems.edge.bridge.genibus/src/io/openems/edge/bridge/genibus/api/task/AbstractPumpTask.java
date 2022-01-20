package io.openems.edge.bridge.genibus.api.task;

import io.openems.common.channel.Unit;
import io.openems.edge.bridge.genibus.api.PumpDevice;

import java.util.OptionalDouble;

/**
 * This is the base class for Genibus tasks. A task links an OpenEMS channel to a Genibus data item.
 * This class contains the parameters and methods that all the different tasks have in common.
 */

public abstract class AbstractPumpTask implements GenibusTask {

    UnitTable unitTable;
    int genibusUnitIndex = 0; // Initialize as an invalid value. As far as I know 0 is not a valid value in the Genibus unit table.
    double genibusUnitFactor = 1.0;
    String unitString;
    private final int headerNumber; // A Genibus data item is identified by two numbers: (headerNumber, address). Example: ref_rem (5, 1)
    private final byte address;
    int sif;    // Scale information factor
    boolean vi; // Value interpretation
    int range = 254;
    boolean bo; // Byte Order, 0 = HighOrder 1 = LowOrder
    int zeroScaleFactorHighOrder;
    int zeroScaleFactorLowOrder;
    int zeroScaleFactor;
    int rangeScaleFactor;
    private boolean informationAvailable = false;
    PumpDevice pumpDevice;
    final int dataByteSize;
    private int apduIdentifier;

    /**
     * Constructor.
     *
     * @param headerNumber the Genibus data item head class.
     * @param address the Genibus data item address.
     * @param unitTable the Genibus unit table. Currently there is just one unit table, so this does not do anything.
     * @param numberOfBytes the number of bytes of this task. 8 bit = 1, 16 bit = 2, etc.
     */
    public AbstractPumpTask(int headerNumber, int address, String unitTable, int numberOfBytes) {
        this.address = (byte) address;
        this.headerNumber = headerNumber;
        this.dataByteSize = numberOfBytes;
        switch (unitTable) {
            case "Standard":
            default:
                this.unitTable = UnitTable.Standard_Unit_Table;
        }
    }

    /**
     * Gets the address of the Genibus data item associated with this task. A Genibus data item is identified by two
     * numbers: (headerNumber, address). Example: ref_rem (5, 1)
     *
     * @return the address.
     */
    @Override
    public byte getAddress() {
        return this.address;
    }

    /**
     * Gets the headerNumber of the Genibus data item associated with this task. A Genibus data item is identified by two
     * numbers: (headerNumber, address). Example: ref_rem (5, 1)
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
     * @param vi value interpretation.
     *           0: Only values from 0-254 are legal. 255 means “data not available”.
     *           1: All values 0-255 are legal values.
     * @param bo byte order information.
     *           0: High order byte, this is default for all values that are only 8 bit.
     *           1: Low order byte to a 16 bit, 24 bit or 32 bit value.
     * @param sif scale information format.
     *            0: Scale information not available (no UNIT, ZERO or RANGE in reply).
     *            1: Bit wise interpreted value (no UNIT, ZERO or RANGE in reply).
     *            2: Scaled 8/16 bit value (UNIT, ZERO and RANGE in reply).
     *            3: Extended precision, scaled 8/16/24/32 bit value (UNIT and ZERO hi/lo in reply).
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
     * If a task has INFO and the INFO is of the four byte type, this is used to store the INFO content with the task.
     *
     * @param vi value interpretation.
     *           0: Only values from 0-254 are legal. 255 means “data not available”.
     *           1: All values 0-255 are legal values.
     * @param bo byte order information.
     *           0: High order byte, this is default for all values that are only 8 bit.
     *           1: Low order byte to a 16 bit, 24 bit or 32 bit value.
     * @param sif scale information format.
     *            0: Scale information not available (no UNIT, ZERO or RANGE in reply).
     *            1: Bit wise interpreted value (no UNIT, ZERO or RANGE in reply).
     *            2: Scaled 8/16 bit value (UNIT, ZERO and RANGE in reply).
     *            3: Extended precision, scaled 8/16/24/32 bit value (UNIT and ZERO hi/lo in reply).
     * @param zeroSignAndUnitIndex  zero scale factor sign and unit index number of the task.
     * @param scaleFactorRangeOrLow the range scale factor or the lo order byte of the extended precision zero scale factor.
     * @param scaleFactorZeroOrHigh the zero scale factor or the hi order byte of the extended precision zero scale factor.
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
            if (this.unitString.length() == 0) {
                this.unitString = "not yet supported";
            }
            this.genibusUnitFactor = this.unitTable.getGenibusUnitFactor(this.genibusUnitIndex);
        }

        // Extract pressure sensor interval.
        if (this.headerNumber == 2 && this.address == this.pumpDevice.getPressureSensorTaskAddress()) {
            Unit sensorUnit = this.pumpDevice.getSensorUnit();
            OptionalDouble sensorMinOptional = OptionalDouble.empty();
            OptionalDouble sensorRangeOptional = OptionalDouble.empty();;
            if (this.sif == 2) {
                sensorMinOptional = this.unitTable.convertToOpenEmsUnit(this.zeroScaleFactor, this.genibusUnitIndex, sensorUnit);
                sensorRangeOptional = this.unitTable.convertToOpenEmsUnit(this.rangeScaleFactor, this.genibusUnitIndex, sensorUnit);
            } else if (this.sif == 3) {
                /* Speculative. Don't know if sensor data in extended precision can be used for ref_rem mapping.
                   This formula assumes the sensor interval is an 8 bit value. */
                int minWithoutUnit = 256 * this.zeroScaleFactorHighOrder + this.zeroScaleFactorLowOrder;
                int maxWithoutUnit = 256 * this.zeroScaleFactorHighOrder + this.zeroScaleFactorLowOrder + 254;
                sensorMinOptional = this.unitTable.convertToOpenEmsUnit(minWithoutUnit, this.genibusUnitIndex, sensorUnit);
                sensorRangeOptional = this.unitTable.convertToOpenEmsUnit(maxWithoutUnit, this.genibusUnitIndex, sensorUnit);
            }
            if (sensorMinOptional.isPresent() && sensorRangeOptional.isPresent()) {
                this.pumpDevice.setPressureSensorMin(sensorMinOptional.getAsDouble());
                this.pumpDevice.setPressureSensorRange(sensorRangeOptional.getAsDouble());
            }
        }
    }

    /**
     * If INFO for this task has been stored or not.
     *
     * @return true for yes and false for no.
     */
    @Override
    public boolean informationDataAvailable() {
        return this.informationAvailable;
    }

    /**
     * Reset the ’informationAvailable’ boolean to false, causing the GenibusWorker to request INFO again for this task.
     */
    @Override
    public void resetInfo() {
        this.informationAvailable = false;
    }

    /**
     * Sets the Genibus device this task belongs to.
     *
     * @param pumpDevice the Genibus device.
     */
    @Override
    public void setGenibusDevice(PumpDevice pumpDevice) {
        this.pumpDevice = pumpDevice;
    }

    /**
     * Get the byte count of this task. An 8 bit task is 1 byte, a 16 bit task is 2 byte, etc.
     *
     * @return the number of bytes.
     */
    @Override
    public int getDataByteSize() {
        return this.dataByteSize;
    }

    /**
     * Set the APDU identifier. This is an internal variable of the Genibus bridge used by the GenibusWorker to tell
     * apart the APDUs it is creating for a telegram.
     * The APDU identifier is a 3 digit decimal number. The 100 digit is the HeadClass of the apdu, the 10 digit is the
     * operation (0=get, 2=set, 3=info), the 1 digit is a counter starting at 0.
     * Example: 230 would be HeadClass 2 and INFO, first APDU of this type.
     *
     * <p>The last digit, the counter, allows to have more than one APDU of a given type. Since an APDU (request and
     * answer) is limited to 63 bytes, several APDUs of the same type might be needed to fit all tasks.</p>
     *
     * @param identifier the APDU identifier.
     */
    @Override
    public void setApduIdentifier(int identifier) {
        this.apduIdentifier = identifier;
    }

    /**
     * Get the APDU identifier. This is an internal variable of the Genibus bridge used by the GenibusWorker to tell
     * apart the APDUs it is creating for a telegram.
     * The APDU identifier is a 3 digit decimal number. The 100 digit is the HeadClass of the apdu, the 10 digit is the
     * operation (0=get, 2=set, 3=info), the 1 digit is a counter starting at 0.
     * Example: 230 would be HeadClass 2 and INFO, first APDU of this type.
     *
     * <p>The last digit, the counter, allows to have more than one APDU of a given type. Since an APDU (request and
     * answer) is limited to 63 bytes, several APDUs of the same type might be needed to fit all tasks.</p>
     *
     *
     * @return the APDU identifier.
     */
    @Override
    public int getApduIdentifier() {
        return this.apduIdentifier;
    }

    /**
     * Get the parsed contents of INFO as a string. Will contain the data type, unit and scaling (if available).
     *
     * @return the parsed contents of INFO as a string.
     */
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
