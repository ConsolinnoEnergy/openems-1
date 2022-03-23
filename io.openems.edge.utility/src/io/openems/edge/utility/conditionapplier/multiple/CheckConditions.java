package io.openems.edge.utility.conditionapplier.multiple;

/**
 * This Enum is helping the {@link MultipleBooleanConditionApplierImpl}
 * to determine which CheckCondition is set. Usually setup via {@link ConfigMultipleConditionApplier}
 */
public enum CheckConditions {
    AND, OR, XOR
}
