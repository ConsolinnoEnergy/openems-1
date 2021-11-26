package io.openems.edge.bridge.genibus.api.task;


import io.openems.edge.bridge.genibus.api.PumpDevice;
import io.openems.edge.common.taskmanager.ManagedTask;

/**
 * Interface for Genibus tasks.
 */
public interface GenibusTask extends ManagedTask {

    /**
     * This method is used to see if this task has a SET to send, as well as to get the value of a SET. This is tied to
     * the contents of the associated write channel. If "nextWrite" of the channel is empty/false, the method returns
     * "nothing to write" (-1 or -256).
     * The point of "clearChannel" is then to tell the task that it has been added to a telegram (with SET) and the
     * command is executed. The channel is cleared and further getRequests will return "nothing to write", until
     * something is written in the channel again.
     *
     * @param byteCounter how many bytes this task has.
     * @param clearChannel mark the task as executed or not.
     * @return the value of the SET if available, -1 or -256 otherwise.
     */
    default int getRequest(int byteCounter, boolean clearChannel) {
        return -1;
    }

    /**
     * Process the response from the pump device.
     *
     * @param data data sent from the pump device.
     */
    void processResponse(byte data);

    /**
     * Gets the address of the task. The address together with the header is the identifier for a data item in the pump.
     *
     * @return the address.
     */
    byte getAddress();

    /**
     * Gets the header of the task. The header together with the address is the identifier for a data item in the pump.
     *
     * @return the header.
     */
    int getHeader();

    /**
     * If a task has INFO and the INFO is of the one byte type, this is used to store the INFO content with the task.
     *
     * @param vi Value information if set range is 255 else 254. Comes from 5th bit
     * @param bo Byte order information. 0 is high order, 1 is low order byte. 4th bit
     * @param sif Scale information format. 0 = not available, 1= bitwise interpreted value.
     */
    void setOneByteInformation(int vi, int bo, int sif);

    /**
     * If a task has INFO and the INFO is of the four byte type, this is used to store the INFO content with the task.
     *
     * @param vi Value information if set range is 255 else 254. Comes from 5th bit
     * @param bo Byte order information. 0 is high order, 1 is low order byte. 4th bit
     * @param sif Scale information format. 0 = not available, 1= bitwise interpreted value.
     * @param zeroSignAndUnitIndex bit 0-6 are the number to declare what unit it is according to the unit table.
     *                             Bit 7 is the sign of the Zero scale factor (0 positive, 1 negative).
     * @param scaleFactorZeroOrHigh either Zero scale factor or factor for high order byte.
     * @param scaleFactorRangeOrLow range scale factor or low order byte.
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
     * Set the pump device this task belongs to.
     *
     * @param pumpDevice the pump device.
     */
    void setPumpDevice(PumpDevice pumpDevice);

    /**
     * Get the number of bytes this task has in the response telegram.
     *
     * @return the number of bytes.
     */
    int getDataByteSize();

    /**
     * Set the APDU identifier. This is an internal variable of the Genibus bridge used by the GenibusWorker to tell
     * apart the APDUs it is creating for a telegram. When the worker checks if there are enough bytes left for a task,
     * it determines in which APDU it would put the task. This information is stored with the task via this method, so
     * it can be retrieved later when the task is actually added to the telegram.
     *
     * @param identifier the APDU identifier.
     */
    void setApduIdentifier(int identifier);

    /**
     * Get the APDU identifier. This is an internal variable of the Genibus bridge used by the GenibusWorker to tell
     * apart the APDUs it is creating for a telegram. When the worker checks if there are enough bytes left for a task,
     * it determines in which APDU it would put the task. This information is stored with the task and can be retrieved
     * via this method when the task is actually added to a telegram.
     *
     * @return the APDU identifier.
     */
    int getApduIdentifier();

    /**
     * Print the content of INFO for this task to the log. Will print the data type, unit and scaling (if available).
     *
     * @return the parsed contents of INFO as a string.
     */
    String printInfo();

}
