package io.openems.edge.bridge.genibus.api.task;

/**
 * Interface for Genibus write tasks.
 */
public interface HeadClass4and5 {

    int NO_SET_AVAILABLE = -1;

    /**
     * This method is for write tasks. It is used to see if this task has a SET to send (use ’byteNumber = 0’), as well
     * as to get the unsigned byte value of a SET. Since Java does not have unsigned bytes, the return value is an int
     * of value 0 to 255. The return value ’-1’ means ’no SET available’ or ’not a write task’.
     * If a SET is available depends on the associated write channel. If ’nextWrite’ of the channel is empty (or ’false’
     * in case of a boolean channel), the method returns ’no SET available’.
     * If a SET is available, this method will return the SET as a byte. For tasks with more than one byte, the
     * parameter ’byteNumber’ (0 = hi) is used to collect the different bytes. The number of bytes a task has is
     * available with ’getDataByteSize()’.
     *
     * @param byteNumber which byte of SET to return. When testing if a SET is available, put 0.
     * @return the byte value of the SET if available, ’NO_SET_AVAILABLE’ otherwise.
     */
    default int getByteIfSetAvailable(int byteNumber) {
        return NO_SET_AVAILABLE;
    }

    /**
     * Set the ’ExecuteGetState’ of the task. This tells the Genibus worker if this task should be executed as GET or
     * not.
     * @param value the ’ExecuteGetState’.
     */
    void setExecuteGet(boolean value);

    /**
     * Get the ’sendGet’ counter.
     * @return the ’sendGet’ counter.
     */
    boolean getExecuteGet();
}
