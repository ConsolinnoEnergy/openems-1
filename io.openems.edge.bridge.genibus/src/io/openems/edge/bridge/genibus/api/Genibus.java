package io.openems.edge.bridge.genibus.api;

/**
 * Interface for the Genibus bridge. Adds or removes a device.
 */

public interface Genibus {

    /**
     * Add a device to the Genibus bridge.
     * @param pumpDevice the device.
     */
    void addDevice(PumpDevice pumpDevice);

    /**
     * Remove a device from the Genibus bridge.
     * @param deviceId the device.
     */
    void removeDevice(String deviceId);

}
