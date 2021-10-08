package io.openems.edge.heater.analogue;

/**
 * The ControlType. Determines which Channel for a new SetPoint PowerValue will be checked, as well as
 * Conversion of the SetPoint Value. (Percent to KW or KW to Percent)
 */
public enum ControlType {
    PERCENT, KW
}
