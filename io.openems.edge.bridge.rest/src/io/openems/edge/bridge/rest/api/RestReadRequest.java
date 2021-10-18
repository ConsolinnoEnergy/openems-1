package io.openems.edge.bridge.rest.api;

/**
 * The Interface for the ReadTasks. It allows to setResponses e.g. set the Response of the GET value.
 */
public interface RestReadRequest extends RestRequest {

    /**
     * Called by the Rest Bridge. Sets answer after successful REST Communication.
     *
     * @param success   declares successful communication.
     * @param answer the REST Response from the GET Method.
     */

    void setResponse(boolean success, String answer);
}
