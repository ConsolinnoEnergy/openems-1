package io.openems.edge.controller.heatnetwork.communication.api;


import io.openems.edge.bridge.rest.api.RestRemoteDevice;
import io.openems.edge.controller.heatnetwork.communication.request.api.RequestType;


/**
 * The RestRequest Interface. It contains Methods to get the Request and the Callback as well as the RequestType.
 */
public interface RestRequest extends Request {
    /**
     * This Method called by the CommunicationMaster, is needed to check if a Request is available.
     * E.g. if this Contains a "1" a Request is available and awaits a CallbackValue.
     *
     * @return the {@link RestRemoteDevice} responsible for the Request.
     */
    RestRemoteDevice getRequest();

    /**
     * This method is called by the CommunicationMaster/Manager, to respond to a Request.
     * In this case, the Response is set via Rest.
     *
     * @return the {@link RestRemoteDevice} responsible for the Request.
     */
    RestRemoteDevice getCallbackRequest();

    /**
     * Get the RequestType, called by the CommunicationMaster, to know how to react after a RestRequest is set to "1".
     *
     * @return the {@link RequestType}.
     */
    RequestType getRequestType();


}
