package io.openems.edge.controller.heatnetwork.communication.responsewrapper;

/**
 * The super class of any implementation of a Response. (E.g. Method or ChannelResponse).
 * Contains the active and passive Value for a Response.
 */
public abstract class AbstractResponseWrapper implements ResponseWrapper {
    protected String activeValue;
    protected String passiveValue;

    public AbstractResponseWrapper(String activeValue, String passiveValue) {
        this.activeValue = activeValue;
        this.passiveValue = passiveValue;
    }

}
