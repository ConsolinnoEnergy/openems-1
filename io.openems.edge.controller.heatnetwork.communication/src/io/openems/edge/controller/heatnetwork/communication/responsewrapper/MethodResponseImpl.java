package io.openems.edge.controller.heatnetwork.communication.responsewrapper;

import io.openems.edge.controller.heatnetwork.communication.request.api.MethodTypes;

public class MethodResponseImpl extends AbstractResponseWrapper implements MethodResponse {
    MethodTypes methodTypes;
    public MethodResponseImpl(String methodOrChannelAddress, String activeValue, String passiveValue) {
        super(activeValue, passiveValue);
        if(MethodTypes.contains(methodOrChannelAddress)){
            this.methodTypes = MethodTypes.valueOf(methodOrChannelAddress);
        }
    }

    @Override
    public MethodTypes getMethod() {
        return this.methodTypes;
    }
    @Override
    public String getActiveValue() {
        return activeValue;
    }
    @Override
    public String getPassiveValue() {
        return passiveValue;
    }
}
