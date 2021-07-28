package io.openems.edge.exceptionalstate.api;

import io.openems.edge.timer.api.TimerHandler;

import java.util.Optional;

/**
 * The concrete Implementation of the ExceptionalStateHandler. It allows for easy and uniform handling of the
 * ExceptionalState in the component.
 * The ExceptionalState, when active, overrides any other commands to the device. There are two command channels. The
 * first one is the boolean ExceptionalStateEnableSignal, which decides if the ExceptionalState is active or not. The
 * second channel is the integer ExceptionalStateValue, which sets the behaviour of the device when the ExceptionalState
 * is active. For more information see {@link io.openems.edge.exceptionalstate.api.ExceptionalState}.
 * The ExceptionalStateHandler only handles the ExceptionalStateEnableSignal. Since the ExceptionalState is an override,
 * a timer is used as a fallback to end the ExceptionalState upon signal loss. The timer is (re-)started each time
 * 'ExceptionalStateEnableSignal = true' is received. Repeatedly sending 'ExceptionalStateEnableSignal = true' is then
 * needed to keep the ExceptionalState active.
 * The timer can be set to any value and is given as an argument upon initialization of the handler class. It can be set
 * to use seconds or OpenEMS cycles as a measurement unit of time. For more information on the timer, see
 * {@link io.openems.edge.timer.api.Timer}.
 * The ExceptionalStateHandler provides the method exceptionalStateActive() to be called by the component to query
 * whether the ExceptionalState, modified by the shutdown timer, is currently active or not.
 */
public class ExceptionalStateHandlerImpl implements ExceptionalStateHandler {
    private boolean exceptionalStateActiveBefore;
    private final TimerHandler timer;

    // This is the identifier of the timer. Needed to get the timer from TimerHandler.
    private final String exceptionalStateIdentifier;


    public ExceptionalStateHandlerImpl(TimerHandler timer, String exceptionalStateIdentifier) {
        this.timer = timer;
        this.exceptionalStateIdentifier = exceptionalStateIdentifier;
    }

    /**
     * Checks if the ExceptionalState, modified by the shutdown timer, is currently active or not.
     *
     * @param exceptionalStateComponent the Component that implements the {@link ExceptionalState} interface.
     * @return true if ExceptionalState is active or was active before and waitTime is not up.
     */
    @Override
    public boolean exceptionalStateActive(ExceptionalState exceptionalStateComponent) {
        Optional<Boolean> exceptionalStateEnableSignal =
                exceptionalStateComponent.getExceptionalStateEnableSignalChannel().getNextWriteValueAndReset();
        if (exceptionalStateEnableSignal.isPresent()) {
            boolean exceptionalStateActive = exceptionalStateEnableSignal.get();
            this.exceptionalStateActiveBefore = exceptionalStateActive;
            exceptionalStateComponent._setExceptionalStateEnableSignal(exceptionalStateActive);
            this.timer.resetTimer(this.exceptionalStateIdentifier);
            return exceptionalStateActive;
        } else {
            if (this.exceptionalStateActiveBefore
                    && this.timer.checkTimeIsUp(this.exceptionalStateIdentifier) == false) {
                return true;
            } else {
                this.exceptionalStateActiveBefore = false;
                exceptionalStateComponent._setExceptionalStateEnableSignal(false);
                return false;
            }
        }
    }
}
