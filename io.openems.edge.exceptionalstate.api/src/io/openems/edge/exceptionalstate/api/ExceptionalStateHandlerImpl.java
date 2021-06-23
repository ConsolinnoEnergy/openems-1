package io.openems.edge.exceptionalstate.api;

import io.openems.edge.timer.api.TimerHandler;

public class ExceptionalStateHandlerImpl implements ExceptionalStateHandler {
    private final TimerHandler timer;
    private final String exceptionalStateIdentifier;
    private boolean exceptionalStateActiveBefore;

    public ExceptionalStateHandlerImpl(TimerHandler timer, String exceptionalStateIdentifier) {
        this.timer = timer;
        this.exceptionalStateIdentifier = exceptionalStateIdentifier;
    }

    @Override
    public boolean exceptionalStateActive(ExceptionalState exceptionalState) {
        if (exceptionalState.getExceptionalStateEnableChannel().getNextWriteValue().isPresent()) {
            this.exceptionalStateActiveBefore = true;
            this.timer.resetTimer(this.exceptionalStateIdentifier);
            return exceptionalState.getExceptionalStateEnableSignalAndReset();
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
