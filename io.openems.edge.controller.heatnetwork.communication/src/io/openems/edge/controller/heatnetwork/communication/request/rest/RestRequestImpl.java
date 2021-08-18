package io.openems.edge.controller.heatnetwork.communication.request.rest;


import io.openems.edge.controller.heatnetwork.communication.api.RestRequest;
import io.openems.edge.controller.heatnetwork.communication.request.api.RequestType;
import io.openems.edge.remote.rest.device.api.RestRemoteDevice;
import org.osgi.service.cm.ConfigurationException;

/**
 * Rest Request Handling a request and callback RestRemoteDevice.
 */
public class RestRequestImpl implements RestRequest {
    //Request input
    private final RestRemoteDevice request;
    //Write to/Signal/Callback/Allow
    private final RestRemoteDevice callback;

    private final RequestType requestType;


    public RestRequestImpl(RestRemoteDevice request, RestRemoteDevice callback, RequestType type) throws ConfigurationException {
        if (request.isRead()) {
            this.request = request;
            this.requestType = type;
        } else {
            throw new ConfigurationException("Request RemoteDevice is not a Read Task", request.getId());
        }
        if (callback.isWrite()) {
            this.callback = callback;
        } else {
            throw new ConfigurationException("Request RemoteDevice is not a Write Task", callback.getId());
        }
    }

    /**
     * This Method called by the CommunicationMaster, is needed to check if a Request is available.
     * E.g. if this Contains a "1" a Request is available and awaits a CallbackValue.
     *
     * @return the {@link RestRemoteDevice} responsible for the Request.
     */
    @Override
    public RestRemoteDevice getRequest() {
        return this.request;
    }

    /**
     * This method is called by the CommunicationMaster/Manager, to respond to a Request.
     * In this case, the Response is set via Rest.
     *
     * @return the {@link RestRemoteDevice} responsible for the Request.
     */
    @Override
    public RestRemoteDevice getCallbackRequest() {
        return this.callback;
    }

    /**
     * Get the RequestType, called by the CommunicationMaster, to know how to react after a RestRequest is set to "1".
     *
     * @return the {@link RequestType}.
     */

    @Override
    public RequestType getRequestType() {
        return this.requestType;
    }

    /**
     * The overwritten Equals method.
     * Equals becomes true, if all RestRemoteDevices and the requestType are identical
     *
     * @param o the object compared to this one.
     * @return true if all RestRemoteDevices and the requestType are identical.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof RestRequestImpl) {
            RestRequestImpl otherObject = (RestRequestImpl) o;
            return otherObject.getRequest().equals(this.getRequest())
                    && otherObject.getCallbackRequest().equals(this.getCallbackRequest())
                    && otherObject.getRequestType().equals(this.requestType);
        }
        return false;
    }
}
