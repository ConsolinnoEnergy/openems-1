package io.openems.edge.controller.heatnetwork.communication.request.api;

/**
 * RequestTypes. This ENUM list ist for correct handling/Response. HEAT means e.g. HeatStorage is heating up.
 * If you want to have more RequestTypes/React to Request Types. Add them here.
 * Contains method is implemented to prevent Exceptions.
 */
public enum RequestType {
    HEAT, MORE_HEAT, GENERIC;

    /**
     * The Contains Method, to check if the Given String is a Name of a possible RequestType.
     *
     * @param type the possible name of the RequestType
     * @return true if the Type is a valid name.
     */
    public static boolean contains(String type) {
        for (RequestType requestType : RequestType.values()) {
            if (requestType.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}