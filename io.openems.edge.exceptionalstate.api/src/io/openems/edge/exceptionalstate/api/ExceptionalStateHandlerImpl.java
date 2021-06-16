package io.openems.edge.exceptionalstate.api;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * The concrete Implementation of the ExceptionalState Handler.
 * It allows easier use of the Exceptional State.
 * It checks if the Enable Signal of the Exceptional State is Active, or if it was active before and the Enable Signal is missing
 * allows further running of the exceptional State until the configured time is up.
 *
 *
 */
public class ExceptionalStateHandlerImpl implements ExceptionalStateHandler {
    private int timer;
    private LocalDateTime timestamp;
    private boolean useCyclesNotSeconds;
    private int cycleCounter;
    private boolean exceptionalStateActiveBefore;

    public ExceptionalStateHandlerImpl(int timer, boolean useCyclesNotSeconds) {
        this.timer = timer;
        this.useCyclesNotSeconds = useCyclesNotSeconds;
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
            this.exceptionalStateActiveBefore = true;
            this.resetTimer();
            return exceptionalStateComponent.getExceptionalStateEnableSignalAndReset();
        } else {
            if (this.exceptionalStateActiveBefore
                    && this.checkTimeIsUp() == false) {
                return true;
            } else {
                this.exceptionalStateActiveBefore = false;
                return false;
            }
        }
    }

    private void resetTimer() {
        if (this.useCyclesNotSeconds) {
            this.cycleCounter = 0;
        } else {
            this.timestamp = LocalDateTime.now();
        }
    }

    private boolean checkTimeIsUp() {
        if (this.useCyclesNotSeconds) {
            return this.cycleCounter >= this.timer;
        } else {
            return ChronoUnit.SECONDS.between(this.timestamp, LocalDateTime.now()) >= this.timer;
        }
    }

    @Override
    public void increaseCycleCounter() {
        if (this.cycleCounter < this.timer) {   // Overflow protection.
            this.cycleCounter++;
        }
    }
}
