package io.openems.edge.controller.heatnetwork.communication.request.api;

/**
 * The MethodTypes supported by the CommunicationMaster.
 * You can Configure Responses to given MethodTypes, those will usually be shown in Config after the first "Save" in
 * the Apache Web Config.
 */
public enum MethodTypes {
    LOG_INFO, ACTIVATE_PUMP, ACTIVATE_LINE_HEATER;

    /**
     * The Contains method to check if a String matches an Enum name.
     *
     * @param type the MethodType as a String
     * @return true if a match is found.
     */

    public static boolean contains(String type) {
        for (MethodTypes methodTypes : MethodTypes.values()) {
            if (methodTypes.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
