package io.openems.edge.utility.conditionapplier;

/**
 * The SupportedOperation, like the SupportedDataType, will be used in future versions, when the Condition Applier work will be
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
