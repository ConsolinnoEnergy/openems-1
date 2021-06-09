package io.openems.edge.condition.applier;

/**
 * The SupportedOperation, like the SupportedDataType this will be used in Future, when the Condition Applier work will be
 * continued.
 */
public enum SupportedOperation {
    EQUALS, GREATER, LESS;

    static boolean contains(String type) {
        for (SupportedOperation supportedOperation : SupportedOperation.values()) {
            if (supportedOperation.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
