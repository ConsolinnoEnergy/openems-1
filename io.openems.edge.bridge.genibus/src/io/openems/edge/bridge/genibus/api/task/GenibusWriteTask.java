package io.openems.edge.bridge.genibus.api.task;

/**
 * Interface for Genibus write tasks.
 */
public interface GenibusWriteTask {

    /**
     * Returns if this task has a SET available. If a SET is available depends on the associated write channel.
     * For head class 3 tasks (commands), this is true if the ’nextWrite’ of the associated channel contains ’true’.
     * For head class 4 and 5 this is true if the ’nextWrite’ of the associated channel is not empty.
     *
     * @return if a SET is available.
     */
    boolean isSetAvailable();

    /**
     * This method clears the ’nextWrite’ of the channel associated with this task. This will make ’isSetAvailable()’
     * return ’false’, until a new value has been written into ’nextWrite’ of the channel.
     * Should be called after this task was added as SET to an APDU, so that the SET is executed just once.
     * Also, if applicable, marks this task as ’get value from Genibus’, so the channel is updated to the new value.
     */
    void clearNextWriteAndUpdateChannel();

}
