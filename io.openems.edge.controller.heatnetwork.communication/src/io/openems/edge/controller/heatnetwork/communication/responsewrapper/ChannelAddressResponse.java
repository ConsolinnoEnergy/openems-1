package io.openems.edge.controller.heatnetwork.communication.responsewrapper;

import io.openems.common.exceptions.OpenemsError;

/**
 * Helper Interface/Class for the CommunicationMaster to react to a Response using a Channel easier.
 *
 */
public interface ChannelAddressResponse {
    /**
     * Activates the Response, by setting the active Value. All the Information are coming from the
     * Config of the HeatNetworkMaster.
     *
     * @throws OpenemsError.OpenemsNamedException if channel won't be found.
     */
    void activateResponse() throws OpenemsError.OpenemsNamedException;

    /**
     * Deactivates the Response, by setting the active Value. All the Information are coming from the
     * Config of the HeatNetworkMaster.
     *
     * @throws OpenemsError.OpenemsNamedException if channel won't be found.
     */
    void passiveResponse() throws OpenemsError.OpenemsNamedException;
}
