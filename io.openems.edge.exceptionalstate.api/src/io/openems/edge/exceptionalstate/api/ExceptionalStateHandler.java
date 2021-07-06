package io.openems.edge.exceptionalstate.api;

/**
 * The interface of the ExceptionalStateHandler. It allows for easy and uniform handling of the ExceptionalState in the
 * component.
 * The ExceptionalState, when active, overrides any other commands to the device. There are two command channels. The
 * first one is the boolean ExceptionalStateEnableSignal, which decides if the ExceptionalState is active or not. The
 * second channel is the integer ExceptionalStateValue, which sets the behaviour of the device when the ExceptionalState
 * is active. For more information see {@link io.openems.edge.exceptionalstate.api.ExceptionalState}.
 * The ExceptionalStateHandler only handles the ExceptionalStateEnableSignal. Since the ExceptionalState is an override,
 * a timer is used as a fallback to end the ExceptionalState upon signal loss. The timer is defined upon initialization
 * (see {@link io.openems.edge.timer.api.Timer}) and (re-)started each time 'ExceptionalStateEnableSignal = true' is
 * received. Repeatedly sending 'ExceptionalStateEnableSignal = true' is then needed to keep the ExceptionalState active.
 * The ExceptionalStateHandler provides the method exceptionalStateActive() to be called by the component to query
 * whether the ExceptionalState, modified by the shutdown timer, is currently active or not.
 */
public interface ExceptionalStateHandler {

    /**
     * Checks if the ExceptionalState, modified by the shutdown timer, is currently active or not.
     *
     * @param exceptionalStateComponent the Component that implements the {@link ExceptionalState} interface.
     * @return true if ExceptionalState is active or was active before and waitTime is not up.
     */
    boolean exceptionalStateActive(ExceptionalState exceptionalStateComponent);
}
