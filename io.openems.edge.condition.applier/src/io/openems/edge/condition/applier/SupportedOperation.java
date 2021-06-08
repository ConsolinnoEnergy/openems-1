package io.openems.edge.condition.applier;

public enum SupportedOperation {
    EQUALS, GREATER, LESS;

    public static boolean contains(String type) {
        for (SupportedOperation supportedOperation : SupportedOperation.values()) {
            if (supportedOperation.name().equals(type)) {
                return true;
            }
        }
        return false;
    }
}
