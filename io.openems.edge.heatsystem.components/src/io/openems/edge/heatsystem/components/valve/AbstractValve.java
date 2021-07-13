package io.openems.edge.heatsystem.components.valve;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heatsystem.components.Valve;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractValve extends AbstractOpenemsComponent implements Valve, ExceptionalState {
    protected final Logger log = LoggerFactory.getLogger(AbstractValve.class);

    protected double secondsPerPercentage;
    protected long timeStampValveInitial;
    protected long timeStampValveCurrent = -1;
    //if true updatePowerlevel
    protected boolean isChanging = false;
    //if true --> subtraction in updatePowerLevel else add
    protected boolean isClosing = false;
    protected boolean wasAlreadyReset = false;
    protected boolean isForced;
    protected static int EXTRA_BUFFER_TIME = 2000;
    protected static final int TOLERANCE = 5;
    protected static final int MAX_CONFIG_TRIES = 10;
    protected AtomicInteger configTries = new AtomicInteger(0);

    protected static final int MILLI_SECONDS_TO_SECONDS = 1000;

    protected Double lastMaximum;
    protected Double lastMinimum;
    protected Double maximum = (double) DEFAULT_MAX_POWER_VALUE;
    protected Double minimum = (double) DEFAULT_MIN_POWER_VALUE;
    protected double powerLevelBeforeUpdate;
    protected boolean updateOk = true;
    protected boolean useExceptionalState;
    protected static final String EXCEPTIONAL_STATE_IDENTIFIER = "VALVE_EXCEPTIONAL_STATE_IDENTIFIER";
    private TimerHandler timerHandler;
    protected ExceptionalStateHandler exceptionalStateHandler;
    protected ExceptionalState exceptionalState;
    protected boolean configSuccess;

    protected AbstractValve(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                            io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }


    // --------------- READY TO CHANGE AND CHANGE BY PERCENTAGE ------------ //

    /**
     * Ready To Change is always true except if the Valve was forced to open/close and the Time to close/open the
     * Valve completely is not over.
     */
    @Override
    public boolean readyToChange() {
        if (this.isForced) {
            long currentTime = this.getMilliSecondTime();
            if (currentTime - this.timeStampValveInitial
                    >= ((this.timeNeeded() * MILLI_SECONDS_TO_SECONDS) + EXTRA_BUFFER_TIME)) {
                this.getIsBusyChannel().setNextValue(false);
                this.wasAlreadyReset = false;
                this.isForced = false;
                return true;
            } else {
                return false;
            }
        }
        if (this.isChanging == false) {
            this.timeStampValveCurrent = -1;
        }
        return true;
    }


    // --------- UTILITY -------------//

    /**
     * get Current Time in Ms.
     *
     * @return currentTime in Ms.
     */

    protected long getMilliSecondTime() {
        long time = System.nanoTime();
        return TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
    }
    //--------------UPDATE POWERLEVEL AND POWER LEVEL REACHED---------------//

    /**
     * Update PowerLevel by getting elapsed Time and check how much time has passed.
     * Current PowerLevel and new Percentage is added together and rounded to 3 decimals.
     */


    protected void updatePowerLevel() {
        //Only Update PowerLevel if the Valve is Changing
        if (this.isChanging()) {
            long elapsedTime = this.getMilliSecondTime();
            //If it's the first update of PowerLevel
            if (this.timeStampValveCurrent == -1) {
                //only important for ForceClose/Open
                this.timeStampValveInitial = elapsedTime;
                //First time in change
                elapsedTime = 0;
                this.powerLevelBeforeUpdate = this.getPowerLevelValue();

                //was updated before
            } else {
                elapsedTime -= this.timeStampValveCurrent;
            }
            this.timeStampValveCurrent = this.getMilliSecondTime();
            double percentIncrease = elapsedTime / (this.secondsPerPercentage * 1000);
            if (this.isClosing) {
                percentIncrease *= -1;
            }
            //Round the calculated PercentIncrease of current PowerLevel and percentIncrease to 3 decimals
            Double powerLevel = this.getPowerLevelValue();
            double truncatedDouble = powerLevel == null ? 0 : BigDecimal.valueOf(powerLevel + percentIncrease)
                    .setScale(3, RoundingMode.HALF_UP)
                    .doubleValue();
            if (truncatedDouble > DEFAULT_MAX_POWER_VALUE) {
                truncatedDouble = DEFAULT_MAX_POWER_VALUE;
            } else if (truncatedDouble < DEFAULT_MIN_POWER_VALUE) {
                truncatedDouble = DEFAULT_MIN_POWER_VALUE;
            }
            this.getPowerLevelChannel().setNextValue(truncatedDouble);
            if (this.updateOk) {
                this.powerLevelBeforeUpdate = truncatedDouble;
            }
        }
    }

    /**
     * Check if Valve has reached the set-point and shuts down Relays if true. (No further opening and closing of Valve)
     *
     * @return is powerLevelReached
     */
    @Override
    public boolean powerLevelReached() {
        boolean reached = true;
        if (this.isChanging()) {
            reached = false;
            Double powerLevel = this.getPowerLevelValue();
            Double futurePowerLevel = this.getFuturePowerLevelValue();
            if (powerLevel != null) {
                if (this.isClosing) {
                    reached = powerLevel <= futurePowerLevel;
                } else {
                    reached = powerLevel >= futurePowerLevel;
                }
            }
        }
        //ReadyToChange always True except if it is forced
        reached = reached && this.readyToChange();
        if (reached) {
            this.isChanging = false;
            this.timeStampValveCurrent = -1;
        }
        return reached;
    }


    /**
     * Is Changing --> Is closing/Opening.
     *
     * @return isChanging
     */
    @Override
    public boolean isChanging() {
        return this.isChanging;
    }

    /**
     * Called by ValveManager to check if this Valve should be reset.
     *
     * @return shouldReset.
     */

    public boolean shouldReset() {
        if (this.wasAlreadyReset) {
            return false;
        } else {
            return this.getResetValueAndResetChannel();
        }
    }

    /**
     * Checks if the max and Min Values are correct and not bizarre that leads to bugs and errors.
     *
     * @return validation.
     */

    protected boolean maxMinValid() {
        if (this.maximum == null) {
            this.maximum = (double) DEFAULT_MAX_POWER_VALUE;
        }
        if (this.minimum == null) {
            this.minimum = (double) DEFAULT_MIN_POWER_VALUE;
        }
        return (this.maximum >= this.minimum && this.maximum > 0 && this.minimum >= 0);
    }


    @Override
    public String debugLog() {
        if (this.getPowerLevelChannel().value().isDefined()) {
            String name = "";
            if (!super.alias().equals("")) {
                name = super.alias();
            } else {
                name = super.id();
            }
            return "Valve: " + name + ": " + this.getPowerLevelChannel().value().toString() + "\n";
        } else {
            return "\n";
        }
    }

    protected void createExcpetionalStateHandler(String timerId, int maxTime,
                                                 ComponentManager cpm, ExceptionalState exceptionalState) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        TimerHandler timerHandler = new TimerHandlerImpl(super.id(), cpm);
        if (this.timerHandler != null) {
            this.timerHandler.removeComponent();
        }
        this.timerHandler = timerHandler;
        this.timerHandler.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerId, maxTime);
        this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timerHandler, EXCEPTIONAL_STATE_IDENTIFIER);
        this.exceptionalState = exceptionalState;
    }


    protected boolean isExceptionalStateActive() {
        if (this.useExceptionalState) {
            return this.exceptionalStateHandler.exceptionalStateActive(this.exceptionalState);
        } else {
            return false;
        }
    }

    protected int getExceptionalSateValue() {
        return this.exceptionalState.getExceptionalStateValue();
    }
}
