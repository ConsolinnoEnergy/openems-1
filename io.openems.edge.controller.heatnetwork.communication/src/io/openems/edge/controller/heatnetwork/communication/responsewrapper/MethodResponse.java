package io.openems.edge.controller.heatnetwork.communication.responsewrapper;

import io.openems.edge.controller.heatnetwork.communication.request.api.MethodTypes;

public interface MethodResponse {
    MethodTypes getMethod();

    String getActiveValue();

    String getPassiveValue();
}
