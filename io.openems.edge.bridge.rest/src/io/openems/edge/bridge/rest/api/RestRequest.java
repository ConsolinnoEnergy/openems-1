package io.openems.edge.bridge.rest.api;

/**
 * The Basic Interface for the RestRequest.
 */
public interface RestRequest {

    /**
     * Returns the Request. (ComponentId/ChannelId) For the Bridge.
     *
     * @return the ComponentId/ChannelId String.
     */
    String getRequest();

    /**
     * Gets the Id of the Configured Component.
     *
     * @return the Id String.
     */
    String getDeviceId();

}
