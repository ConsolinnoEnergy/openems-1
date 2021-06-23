package io.openems.edge.exceptionalstate.api;

import io.openems.edge.timer.api.TimerHandler;

/**
 * The concrete Implementation of the ExceptionalState Handler.
 * It allows easier use of the Exceptional State.
 * It checks if the Enable Signal of the Exceptional State is Active, or if it was active before and the Enable Signal is missing
 * allows further running of the exceptional State until the configured Time is up (look up {@link io.openems.edge.timer.api.Timer}
 *
 *
 */
public class ExceptionalStateHandlerImpl implements ExceptionalStateHandler {
    private final TimerHandler timer;
    private final String exceptionalStateIdentifier;
    private boolean exceptionalStateActiveBefore;

    public ExceptionalStateHandlerImpl(TimerHandler timer, String exceptionalStateIdentifier) {
        this.timer = timer;
        this.exceptionalStateIdentifier = exceptionalStateIdentifier;
    }

    /**
     * Checks if the ExceptionalState is Active.
     *
     * @param exceptionalStateComponent the Component that implements the {@link ExceptionalState} interface.
     * @return true if ExceptionalState is Active or was active before and waitTime is not up.
     */
    @Override
    public boolean exceptionalStateActive(ExceptionalState exceptionalStateComponent) {
        if (exceptionalStateComponent.getExceptionalStateEnableChannel().getNextWriteValue().isPresent()) {
            if (exceptionalStateComponent.getExceptionalStateEnableSignalAndReset()) {
                this.exceptionalStateActiveBefore = true;
                this.timer.resetTimer(this.exceptionalStateIdentifier);
                return true;
            } else {
                this.exceptionalStateActiveBefore = false;
                return false;
            }
        } else {
            if (this.exceptionalStateActiveBefore
                    && this.timer.checkTimeIsUp(this.exceptionalStateIdentifier) == false) {
                return true;
            } else {
                this.exceptionalStateActiveBefore = false;
                return false;
            }
        }
    }
}
