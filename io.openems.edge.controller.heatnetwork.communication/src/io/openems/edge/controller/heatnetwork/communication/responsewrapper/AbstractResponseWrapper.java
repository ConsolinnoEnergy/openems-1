package io.openems.edge.controller.heatnetwork.communication.responsewrapper;

public abstract class AbstractResponseWrapper implements ResponseWrapper{
    protected String activeValue;
    protected String passiveValue;

    public AbstractResponseWrapper(String activeValue, String passiveValue) {
        this.activeValue = activeValue;
        this.passiveValue = passiveValue;
    }

}
