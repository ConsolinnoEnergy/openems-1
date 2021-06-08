package io.openems.edge.condition.applier;

public enum SupportedDataType {
    INTEGER, BOOLEAN, STRING, FLOAT, DOUBLE;

    public static boolean contains(String type) {
        for (SupportedDataType supportedDataType : SupportedDataType.values()) {
            if (supportedDataType.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
