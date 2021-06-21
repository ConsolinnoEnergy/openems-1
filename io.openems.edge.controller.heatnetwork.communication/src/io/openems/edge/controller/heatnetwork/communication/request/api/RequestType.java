package io.openems.edge.controller.heatnetwork.communication.request.api;

/**
 * RequestTypes. This ENUM list ist for correct handling/Response. HEAT means e.g. HeatStorage is heating up.
 * If you want to have more RequestTypes/React to Request Types. Add them here.
 * Contains method is implemented to prevent Exceptions.
 */
public enum RequestType {
    HEAT, MORE_HEAT, GENERIC;


    protected static boolean contains(String type) {
        for (RequestType requestType : RequestType.values()) {
            if (requestType.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
