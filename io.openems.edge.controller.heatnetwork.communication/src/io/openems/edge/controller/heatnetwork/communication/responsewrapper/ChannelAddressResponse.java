package io.openems.edge.controller.heatnetwork.communication.responsewrapper;

import io.openems.common.exceptions.OpenemsError;

public interface ChannelAddressResponse {
    void activateResponse() throws OpenemsError.OpenemsNamedException;
    void passiveResponse() throws OpenemsError.OpenemsNamedException;
}
