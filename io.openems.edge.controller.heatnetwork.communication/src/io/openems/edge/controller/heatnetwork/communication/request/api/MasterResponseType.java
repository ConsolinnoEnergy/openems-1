package io.openems.edge.controller.heatnetwork.communication.request.api;

public enum MasterResponseType {
    METHOD, CHANNEL_ADDRESS;



    protected static boolean contains(String type) {
        for (MasterResponseType masterResponseType : MasterResponseType.values()) {
            if (masterResponseType.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
