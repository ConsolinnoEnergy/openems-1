package io.openems.edge.controller.heatnetwork.communication.request.api;

/**
 * ResponseTypes that may be configurable for the CommunicationMaster.
 * Will be displayed in the Config on the apache web config if saved initially.
 */
public enum MasterResponseType {
    METHOD, CHANNEL_ADDRESS;


    /**
     * Checks if the given String is a Name of a MasterResponseType.
     *
     * @param type the possible Name of a MasterResponseType
     * @return true if any MasterResponseType's name equals the given String.
     */
    public static boolean contains(String type) {
        for (MasterResponseType masterResponseType : MasterResponseType.values()) {
            if (masterResponseType.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
