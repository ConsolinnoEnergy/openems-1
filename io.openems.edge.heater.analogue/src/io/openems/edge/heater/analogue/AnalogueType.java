package io.openems.edge.heater.analogue;

import io.openems.edge.heater.analogue.component.AbstractAnalogueHeaterOrCoolerComponent;

/**
 * The Analogue Heater Type, determines, which {@link AbstractAnalogueHeaterOrCoolerComponent}
 * extending class will be instantiated.
 * Usually determined via config.
 */
public enum AnalogueType {
    PWM, AIO, RELAY, LUCID_CONTROL
}
