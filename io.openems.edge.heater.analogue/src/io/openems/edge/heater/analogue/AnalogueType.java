package io.openems.edge.heater.analogue;

/**
 * The Analogue Heater Type, determines, which {@link io.openems.edge.heater.analogue.component.AbstractAnalogueHeaterComponent}
 * extending class will be instantiated.
 * Usually determined via config.
 */
public enum AnalogueType {
    PWM, AIO, RELAY, LUCID_CONTROL
}
