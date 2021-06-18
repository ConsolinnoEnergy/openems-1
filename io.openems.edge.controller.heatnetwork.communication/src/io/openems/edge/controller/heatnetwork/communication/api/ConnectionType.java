package io.openems.edge.controller.heatnetwork.communication.api;

/**
 * Possible Connection Types.
 */
public enum ConnectionType {
    REST//, MQTT, WMBUS, I2C, SPI, WEBSOCKET, MODBUS, MBUS;
    ;

    public static boolean contains(String requestedConnection) {

        for (ConnectionType connectionType : ConnectionType.values()) {
            if (connectionType.name().equals(requestedConnection)) {
                return true;
            }
        }
        return false;
    }
}
