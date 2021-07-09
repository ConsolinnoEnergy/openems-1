package io.openems.edge.condition.applier;

/**
 * The SupportedData will be used in future versions. When the ConditionApplier is in further development.
 */
public enum SupportedDataType {
    INTEGER, BOOLEAN, STRING, FLOAT, DOUBLE;

    static boolean contains(String type) {
        for (SupportedDataType supportedDataType : SupportedDataType.values()) {
            if (supportedDataType.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
