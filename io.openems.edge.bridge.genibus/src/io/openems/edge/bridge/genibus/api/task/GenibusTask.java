package io.openems.edge.bridge.genibus.api.task;


import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.common.taskmanager.ManagedTask;

/**
 * The basic interface for a Genibus task. A task links an OpenEMS channel to a Genibus data item.
 * This interface contains the getters and setters needed by the Genibus bridge to map the data from the Genibus device
 * to the OpenEMS channel.
 */
public interface GenibusTask extends ManagedTask {

    /**
     * Allocate a byte from a response telegram to this task. For multi byte tasks, call this method for each byte in
     * the order hi to lo. Once all bytes are allocated, the data is processed and the result put in ’nextValue’ of the
     * associated OpenEMS channel.
     *
     * @param data the response byte from the Genibus device for this task.
     */
    void processResponse(byte data);

    /**
     * Gets the address of the Genibus data item associated with this task. A Genibus data item is identified by two
     * numbers: (headerNumber, address). Example: ref_rem (5, 1)
     *
     * @return the address.
     */
    byte getAddress();

    /**
     * Gets the headerNumber of the Genibus data item associated with this task. A Genibus data item is identified by two
     * numbers: (headerNumber, address). Example: ref_rem (5, 1)
     *
     * @return the header.
     */
    int getHeader();

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
    void setOneByteInformation(int vi, int bo, int sif);

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
    void setFourByteInformation(int vi, int bo, int sif, byte zeroSignAndUnitIndex, byte scaleFactorZeroOrHigh, byte scaleFactorRangeOrLow);

    /**
     * If INFO for this task has been stored or not.
     *
     * @return true for yes and false for no.
     */
    boolean informationDataAvailable();

    /**
     * Reset the ’informationAvailable’ boolean to false, causing the GenibusWorker to request INFO again for this task.
     */
    void resetInfo();

    /**
     * Sets the Genibus device this task belongs to.
     *
     * @param pumpDevice the Genibus device.
     */
    void setGenibusDevice(PumpDevice pumpDevice);

    /**
     * Get the byte count of this task. An 8 bit task is 1 byte, a 16 bit task is 2 byte, etc.
     *
     * @return the number of bytes.
     */
    int getDataByteSize();

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
    void setApduIdentifier(int identifier);

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
    int getApduIdentifier();

    /**
     * Get the parsed contents of INFO as a string. Will contain the data type, unit and scaling (if available).
     *
     * @return the parsed contents of INFO as a string.
     */
    String printInfo();
}
