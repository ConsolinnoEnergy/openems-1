package io.openems.edge.heatsystem.components.valve;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Abstract Valve. It Provides basic Functions, such as updating the PowerLevel or if tthe powerLevel is reached.
 * Its the BaseClass for any Valve that is implemented.
 */
public abstract class AbstractValve extends AbstractOpenemsComponent implements HydraulicComponent, ExceptionalState {
    @Reference
    Cycle cycle;

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
    private static final int OPTIMIZE_FACTOR = 2;

    protected boolean useCheckOutput;

    protected static int EXTRA_BUFFER_TIME = 2000;
    protected static final int TOLERANCE = 5;
    protected static final int MAX_CONFIG_TRIES = 10;
    protected AtomicInteger configTries = new AtomicInteger(0);
    protected boolean configSuccess;

    protected static final int MILLI_SECONDS_TO_SECONDS = 1000;

    protected Double lastMaximum;
    protected Double lastMinimum;
    protected Double maximum = (double) DEFAULT_MAX_POWER_VALUE;
    protected Double minimum = (double) DEFAULT_MIN_POWER_VALUE;
    protected double powerLevelBeforeUpdate;
    protected boolean updateOk = true;
    protected boolean useExceptionalState;
    protected static final String EXCEPTIONAL_STATE_IDENTIFIER = "VALVE_EXCEPTIONAL_STATE_IDENTIFIER";
    protected boolean parentActive;
    private TimerHandler timerHandler;
    protected ExceptionalStateHandler exceptionalStateHandler;
    protected ExceptionalState exceptionalState;
    protected boolean exceptionalStateActive;

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
        this.useExceptionalState = true;
    }


    protected boolean isExceptionalStateActive() {
        if (this.useExceptionalState) {
            return this.exceptionalStateHandler.exceptionalStateActive(this.exceptionalState);
        } else {
            return false;
        }
    }

    /**
     * This Method adapts the current ValvePosition if 2 Conditions apply.
     * First: Check how much percent the Valve can change per cycle.
     * After that check if the Valve can adapt to the FuturePowerLevel with it's current PowerLevelValue
     */
    protected void adaptValveValue() {
        int cycleTime = this.cycle == null ? Cycle.DEFAULT_CYCLE_TIME : this.cycle.getCycleTime();
        double currentPowerLevelValue = this.getPowerLevelValue();
        double futurePowerLevel = this.getFuturePowerLevelValue();
        double percentPossiblePerCycle = cycleTime / (this.secondsPerPercentage * MILLI_SECONDS_TO_SECONDS);
        boolean possibleToAdapt = this.getFuturePowerLevelValue() - currentPowerLevelValue > percentPossiblePerCycle;
        boolean powerLevelOutOfBounce = currentPowerLevelValue - TOLERANCE > this.getFuturePowerLevelValue()
                || currentPowerLevelValue + TOLERANCE < this.getFuturePowerLevelValue();
        if (possibleToAdapt && powerLevelOutOfBounce && currentPowerLevelValue != futurePowerLevel) {
            try {
                this.setPointPowerLevelChannel().setNextWriteValueFromObject(this.getFuturePowerLevelValue());
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't adapt Valve; Value of Valve: " + super.id());
            }
        }
    }


    protected int getExceptionalSateValue() {
        return this.exceptionalState.getExceptionalStateValue();
    }

    protected Value<?> getValueOfOptionalChannel(Channel<?> optionalChannel) {
        return optionalChannel.value().isDefined() ? optionalChannel.value()
                : optionalChannel.getNextValue().isDefined() ? optionalChannel.getNextValue() : null;
    }

    /**
     * Basic Routine that every Valve has to make.
     * First things first -> is an ExceptionalState active -> 1. priority
     * After that,check if the Valve should Reset (0%) -> 2. priority
     * Check if full Power is Forced (100%) -> 3. priority
     * Check if a PowerValue was set
     *
     * @return false if nothing above was done
     */
    protected boolean parentDidRoutine() {
        this.parentActive = true;
        boolean childHasNothingToDo = false;
        this.exceptionalStateActive = this.isExceptionalStateActive();
        if (this.exceptionalStateActive) {
            this.setPowerLevel(this.getExceptionalSateValue());
            childHasNothingToDo = true;
        } else if (this.shouldReset()) {
            this.reset();
            childHasNothingToDo = true;
        } else if (this.getForceFullPowerAndResetChannel()) {
            this.forceOpen();
            childHasNothingToDo = true;
        } else {
            int setPointPowerLevelValue = this.setPointPowerLevelValue();
            if (setPointPowerLevelValue >= DEFAULT_MIN_POWER_VALUE) {
                this.setPowerLevel(setPointPowerLevelValue);
                childHasNothingToDo = true;
            }
        }

        this.updatePowerLevel();
        this.parentActive = false;
        return childHasNothingToDo;
    }

    /**
     * Sets the PowerLevel of the Valve and calls the ChangeByPercentage method that is implemented by the extending Classes.
     *
     * @param setPoint the setPoint that will be changed to a percent value that a valve has to be adapted to.
     */
    public void setPowerLevel(double setPoint) {
        setPoint -= this.getPowerLevelValue();
        if (this.changeByPercentage(setPoint)) {
            this.setPointPowerLevelChannel().setNextValue(-1);
        }
    }

    /**
     * Changes Valve Position by incoming percentage.
     * Warning, only executes if valve is not busy! (was not forced to open/close)
     * Depending on + or - it changes the current State to open/close it more. Switching the relays on/off does
     * not open/close the valve instantly but slowly. The time it takes from completely closed to completely
     * open is entered in the config. Partial open state of x% is then archived by switching the relay on for
     * time-to-open * x%, or the appropriate amount of time depending on initial state.
     * Sets the Future PowerLevel; ValveManager calls further Methods to refresh true % state
     *
     * @param percentage adjusting the current powerLevel in % points. Meaning if current state is 10%, requesting
     *                   changeByPercentage(20) will change the state to 30%.
     *                   <p>
     *                   If the Valve is busy return false
     *                   otherwise: save the current PowerLevel to the old one and overwrite the new one.
     *                   Then it will check how much time is needed to adjust the position of the valve.
     *                   If percentage is neg. valve needs to be closed (further)
     *                   else it needs to open (further).
     *                   </p>
     */

    protected double calculateCurrentPowerLevelAndSetTime(double percentage) {
        double currentPowerLevel = this.getPowerLevelValue();
        this.getLastPowerLevelChannel().setNextValue(currentPowerLevel);
        this.maximum = this.getMaxAllowedValue();
        this.minimum = this.getMinAllowedValue();
        if (this.maxMinValid() == false) {
            this.minimum = null;
            this.maximum = null;
        }
        currentPowerLevel += percentage;
        if (this.maximum != null && this.maximum < currentPowerLevel) {
            currentPowerLevel = this.maximum;
        } else if (this.lastMaximum != null && this.lastMaximum < currentPowerLevel) {
            currentPowerLevel = this.lastMaximum;
        } else if (currentPowerLevel >= DEFAULT_MAX_POWER_VALUE) {
            currentPowerLevel = DEFAULT_MAX_POWER_VALUE;
        } else if (this.minimum != null && this.minimum > currentPowerLevel) {
            currentPowerLevel = this.minimum;
        } else if (this.lastMinimum != null && this.lastMinimum > currentPowerLevel) {
            currentPowerLevel = this.lastMinimum;
        }
        //Set goal Percentage for future reference
        this.futurePowerLevelChannel().setNextValue(Math.round(currentPowerLevel));
        //if same power level do not change and return --> relays is not always powered
        if (getLastPowerLevelChannel().getNextValue().get() == currentPowerLevel) {
            this.isChanging = false;
            return -1;
        }
        //Calculate the Time to Change the Valve
        if (Math.abs(percentage) >= DEFAULT_MAX_POWER_VALUE) {
            this.timeChannel().setNextValue(DEFAULT_MAX_POWER_VALUE * this.secondsPerPercentage);
        } else {
            this.timeChannel().setNextValue(Math.abs(percentage) * this.secondsPerPercentage);
        }
        return currentPowerLevel;
    }

    /**
     * A Method to help extending classes determine if they are allowed to Accept the Force Request.
     * Additionally it applies the future PowerLevel, the Time needed and updates the PowerLevel.
     *
     * @return true if allowed to force. Otherwise false.
     */

    protected boolean parentForceClose() {

        if (this.exceptionalStateActive == false) {
            this.isForced = true;
            this.isChanging = true;
            this.isClosing = true;
            this.futurePowerLevelChannel().setNextValue(DEFAULT_MIN_POWER_VALUE);
            this.timeChannel().setNextValue(DEFAULT_MAX_POWER_VALUE * this.secondsPerPercentage);
            this.getIsBusyChannel().setNextValue(true);
            //Making sure to wait the correct time even if it is already closing.
            this.timeStampValveInitial = -1;
            this.updatePowerLevel();
            return true;
        }
        this.log.info("Couldn't Force Close ExceptionalState is Active! " + super.id());
        return false;
    }

    /**
     * A Method to help extending classes determine if they are allowed to Accept the Force Request.
     * Additionally it applies the future PowerLevel, the Time needed and updates the PowerLevel.
     *
     * @return true if allowed to force. Otherwise false.
     */
    protected boolean parentForceOpen() {

        if (this.exceptionalStateActive == false) {
            this.isForced = true;
            this.futurePowerLevelChannel().setNextValue(DEFAULT_MAX_POWER_VALUE);
            this.timeChannel().setNextValue(DEFAULT_MAX_POWER_VALUE * this.secondsPerPercentage);

            this.getIsBusyChannel().setNextValue(true);
            this.isChanging = true;
            this.isClosing = false;
            this.timeStampValveCurrent = -1;
            this.updatePowerLevel();
            return true;
        }
        this.log.info("Couldn't Force Open ExceptionalState is Active! " + super.id());
        return false;
    }

    /**
     * A Method to help extending classes determine if they are allowed change their Valve by a certain Percent value.
     * If the Parent is active, the change is always valid (knows the priority order) otherwise check for forcing (readyToChange)
     * or if the exceptionalState is active. As well as check for 0 percentage change.
     *
     * @param percentage the percentAmount that will be added/substracted from the current PowerLevel
     * @return false if the Request ist valid
     */

    protected boolean changeInvalid(double percentage) {

        return this.parentActive == false && (this.readyToChange() == false || this.exceptionalStateActive) || percentage == DEFAULT_MIN_POWER_VALUE;
    }
}
