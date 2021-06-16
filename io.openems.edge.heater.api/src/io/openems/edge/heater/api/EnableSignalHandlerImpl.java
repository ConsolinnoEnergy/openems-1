package io.openems.edge.heater.api;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

public class EnableSignalHandlerImpl implements EnableSignalHandler {
    private int timer;
    private LocalDateTime timestamp;
    private boolean useCyclesNotSeconds;
    private int cycleCounter;
    private boolean currentlyEnabled;

    public EnableSignalHandlerImpl(int timer, boolean useCyclesNotSeconds) {
        this.timer = timer;
        this.useCyclesNotSeconds = useCyclesNotSeconds;

    }

    @Override
    public boolean enableSignalActive(Heater heaterChannel) {
        Optional<Boolean> enabledSignal = heaterChannel.getEnableSignalChannel().getNextWriteValueAndReset();

        // If for some reason enabledSignal is false, treat it as no signal.
        if (enabledSignal.isPresent() && enabledSignal.get()) {
            this.currentlyEnabled = true;
            this.resetTimer();
        } else {
            // No value in the Optional or enabledSignal = false.
            // Wait configured amount of time. If isEnabledSignal() does not return true before timer runs out, turn off device.
            if (this.currentlyEnabled) {
                if (this.checkTimeIsUp()) {
                    this.currentlyEnabled = false;
                }
            }
        }
        return this.currentlyEnabled;
    }

    @Override
    public void resetTimer() {
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
