package io.openems.edge.condition.applier;

/**
 * The SupportedData, this will be used in Future. When the ConditionApplier is in further development.
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
