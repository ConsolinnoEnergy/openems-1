package io.openems.edge.bridge.genibus.api.task;

/**
 * Extended interface for Genibus write tasks of head class 4 and 5.
 */
public interface HeadClass4and5 extends GenibusWriteTask {

    int NO_SET_AVAILABLE = -1;

    /**
     * Returns the byte value for a SET. For a multi byte task, the parameter byteNumber specifies which byte (0 is hi).
     * For 8 bit tasks, byteNumber is 0.
     * The return value is an int of value 0 to 255, or ’NO_SET_AVAILABLE’ if there is no SET available (or the
     * byteNumber is wrong). If a SET is available depends on the associated write channel. If ’nextWrite’ of the channel
     * is empty, then there is no SET available.
     *
     * @param byteNumber which byte of SET to return.
     * @return the byte value of the SET if available, ’NO_SET_AVAILABLE’ otherwise.
     */
    int getByteIfSetAvailable(int byteNumber);

    /**
     * Set the ’ExecuteGetState’ of the task. This tells the Genibus worker if this task should be executed as GET or
     * not.
     *
     * @param value the ’ExecuteGetState’.
     */
    void setExecuteGet(boolean value);

    /**
     * Get the ’ExecuteGetState’ of the task. This tells the Genibus worker if this task should be executed as GET or
     * not.
     *
     * @return the ’ExecuteGetState’.
     */
    boolean getExecuteGet();
}
