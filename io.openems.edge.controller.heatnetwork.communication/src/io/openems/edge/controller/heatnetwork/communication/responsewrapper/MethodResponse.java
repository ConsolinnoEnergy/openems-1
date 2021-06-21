package io.openems.edge.controller.heatnetwork.communication.responsewrapper;

import io.openems.edge.controller.heatnetwork.communication.request.api.MethodTypes;

/**
 * The Interface of a MethodResponse, it holds a MethodType and the getter for an active/passive value.
 */
public interface MethodResponse {
    /**
     * Get the {@link MethodTypes} to this response, to know what Method to call.
     *
     * @return the {@link MethodTypes}
     */
    MethodTypes getMethod();

    /**
     * Gets the Active Value as a String for the Method.
     *
     * @return the ActiveValue
     */
    String getActiveValue();

    /**
     * Gets the Passive Value as a String for the Method.
     *
     * @return the PassiveValue
     */
    String getPassiveValue();
}
