package io.openems.edge.controller.heatnetwork.communication.responsewrapper;

import io.openems.edge.controller.heatnetwork.communication.request.api.MethodTypes;

/**
 * The implementation of the {@link MethodResponse}
 * It holds a MethodType and the getter for an active/passive value.
 */
public class MethodResponseImpl extends AbstractResponseWrapper implements MethodResponse {
    MethodTypes methodTypes;

    public MethodResponseImpl(String methodOrChannelAddress, String activeValue, String passiveValue) {
        super(activeValue, passiveValue);
        if (MethodTypes.contains(methodOrChannelAddress)) {
            this.methodTypes = MethodTypes.valueOf(methodOrChannelAddress);
        }
    }

    /**
     * Get the {@link MethodTypes} to this response, to know what Method to call.
     *
     * @return the {@link MethodTypes}
     */
    @Override
    public MethodTypes getMethod() {
        return this.methodTypes;
    }

    /**
     * Gets the Active Value as a String for the Method.
     *
     * @return the ActiveValue
     */
    @Override
    public String getActiveValue() {
        return activeValue;
    }

    /**
     * Gets the Passive Value as a String for the Method.
     *
     * @return the PassiveValue
     */

    @Override
    public String getPassiveValue() {
        return passiveValue;
    }
}
