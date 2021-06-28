package io.openems.edge.exceptionalstate.api;

/**
 * The Interface of the Exceptional State Handler. It allows for easier use of the Exceptional State.
 * The handler includes a timer that is used to delay leaving the Exceptional State in case of a signal loss.
 *
 * It checks if the Enable Signal of the Exceptional State is active, or if it was active before and the Enable Signal
 * is missing allows further running of the exceptional State until the configured Time is up (look up {@link io.openems.edge.timer.api.Timer}
 * for more information, how a Timer works.
 */
public interface ExceptionalStateHandler {

    /**
     * Checks if the ExceptionalState is Active.
     *
     * @param exceptionalStateComponent the Component that implements the {@link ExceptionalState} interface.
     * @return true if ExceptionalState is Active or was active before and waitTime is not up.
     */
    boolean exceptionalStateActive(ExceptionalState exceptionalStateComponent);
}
