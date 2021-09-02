package io.openems.edge.bridge.genibus.api.task;

/**
 * Interface for Genibus write tasks.
 */
public interface GenibusWriteTask {

    /**
     * Set the ’sendGet’ counter.
     * @param value the ’sendGet’ counter.
     */
    void setSendGet(int value);

    /**
     * Get the ’sendGet’ counter.
     * @return the ’sendGet’ counter.
     */
    int getSendGet();
}
