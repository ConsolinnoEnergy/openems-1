package io.openems.edge.bridge.rest.api;

import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationException;

import java.util.Map;

/**
 * This interface allows Devices to add and Remove their RestRequests. This enables the remote communication with a
 * different OpenEms via REST. One Request handles one Channel. Either read a ChannelValue or write into a Channel.
 */
public interface RestBridge extends OpenemsComponent  {
    /**
     * Adds the RestRequest to the tasks map.
     *
     * @param id      identifier == remote device Id usually from Remote Device config
     * @param request the RestRequest created by the Remote Device.
     * @throws ConfigurationException if the id is already in the Map.
     */

    void addRestRequest(String id, RestRequest request) throws ConfigurationException;

    /**
     * Removes a Remote device from the Bridge.
     * Usually called by RestRemote Component on deactivation or when the Bridge itself deactivates.
     *
     * @param deviceId the deviceId to Remove.
     */
    void removeRestRemoteDevice(String deviceId);

    /**
     * Get all requests from the RestBridge.
     * @return all of the stored RestRequests
     */
    Map<String, RestRequest> getAllRequests();

    /**
     * Is the Connection OK (Test Get request) Not ideal but it works.
     *
     * @return a boolean if connection is Ok.
     */

    boolean connectionOk();
}
