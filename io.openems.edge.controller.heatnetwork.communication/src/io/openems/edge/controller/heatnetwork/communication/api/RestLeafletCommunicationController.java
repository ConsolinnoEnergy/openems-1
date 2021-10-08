package io.openems.edge.controller.heatnetwork.communication.api;

import java.util.List;
import java.util.Map;

/**
 * The RestLeafletCommunicationController, an extension of a LeafletCommunicationController.
 * It stores RestRequests and creates a RestRequestManager, that will manage Requests, depending on the {@link ManageType}
 * and the maximum Requests that are allowed at once, as well as the maximum WaitTime.
 */
public interface RestLeafletCommunicationController extends LeafletCommunicationController {
    /**
     * Map that stores all Request. It's Key is a simple int, grouping the collection of {@link RestRequest}s
     * @return the Map with all grouped Requests.
     */
    Map<Integer, List<RestRequest>> getAllRequests();

    /**
     * Gets the {@link RestRequestManager} of this CommunicationController.
     * @return the {@link RestRequestManager}.
     */
    RestRequestManager getRestManager();

    /**
     * Adds RestRequests to allRequests. This is important for the Manager handling the Requests.
     *
     * @param additionalRequests the additional Requests.
     */
    void addRestRequests(Map<Integer, List<RestRequest>> additionalRequests);
}
