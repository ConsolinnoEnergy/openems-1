package io.openems.edge.heatsystem.components.valve;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The Abstract Valve. It Provides basic Functions, such as updating the PowerLevel or if the powerLevel is reached.
 * Its the BaseClass for any Valve that is implemented.
 */
public abstract class AbstractValve extends AbstractOpenemsComponent implements HydraulicComponent, ExceptionalState {

    Cycle cycle;

    protected final Logger log = LoggerFactory.getLogger(AbstractValve.class);

    protected double secondsPerPercentage;
    protected long timeStampValveInitial;
    protected long timeStampValveCurrent = UNINITIALIZED;
    //if true updatePowerlevel
    protected boolean isChanging = false;
    //if true --> subtraction in updatePowerLevel else add
    protected boolean isClosing = false;
    protected boolean wasAlreadyReset = false;
    protected boolean isForced;
    private static final int BUFFER = 5;

    protected boolean useCheckOutput;

    protected static int EXTRA_BUFFER_TIME = 2000;
    protected static final int TOLERANCE = 5;
    protected static final int MAX_CONFIG_TRIES = 10;
    protected AtomicInteger configTries = new AtomicInteger(0);
    protected boolean configSuccess;

    protected static final int MILLI_SECONDS_TO_SECONDS = 1000;
    private static final int UNINITIALIZED = -1;

    protected Double lastMaximum;
    protected Double lastMinimum;
    protected Double maximum = (double) DEFAULT_MAX_POWER_VALUE;
    protected Double minimum = (double) DEFAULT_MIN_POWER_VALUE;
    protected double powerLevelBeforeUpdate;
    protected boolean updateOk = true;
    protected boolean useExceptionalState;
    protected static final String EXCEPTIONAL_STATE_IDENTIFIER = "VALVE_EXCEPTIONAL_STATE_IDENTIFIER";
    protected boolean parentActive;
    protected TimerHandler timerHandler;
    protected ExceptionalStateHandler exceptionalStateHandler;
    protected ExceptionalState exceptionalState;
    protected boolean exceptionalStateActive;
    protected double percentPossiblePerCycle = 1.d;
    protected double percentIncreaseThisRun = 0.0d;

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
            this.timeStampValveCurrent = UNINITIALIZED;
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
        this.percentIncreaseThisRun = 0.0d;
        if (this.isChanging()) {
            long elapsedTime = this.getMilliSecondTime();
            //If it's the first update of PowerLevel
            if (this.timeStampValveCurrent == UNINITIALIZED) {
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
            this.percentIncreaseThisRun = percentIncrease;
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
            this.getPowerLevelChannel().nextProcessImage();
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
            double futurePowerLevel = this.getFuturePowerLevelValue();
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
            this.timeStampValveCurrent = UNINITIALIZED;
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
        return (this.maximum >= this.minimum && this.maximum > DEFAULT_MIN_POWER_VALUE && this.minimum >= DEFAULT_MIN_POWER_VALUE);
    }


    @Override
    public String debugLog() {
        if (this.getPowerLevelChannel().value().isDefined()) {
            String name;
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

    /**
     * Creates the Timer for: Missing Components check, and ExceptionalState support.
     *
     * @param timerId             the timer Id
     * @param maxTime             the Maximum Time fpr the Missing Components check and ExceptionalState check
     * @param cpm                 Component Manager
     * @param exceptionalState    the exceptionalState component (this)
     * @param useExceptionalState should the ExceptionalState be used
     * @throws OpenemsError.OpenemsNamedException if component cannot be found
     * @throws ConfigurationException             if timerId is wrong (component found but wrong instance).
     */
    protected void createTimerHandler(String timerId, int maxTime,
                                      ComponentManager cpm, ExceptionalState exceptionalState, boolean useExceptionalState) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        TimerHandler timerHandler = new TimerHandlerImpl(super.id(), cpm);
        if (this.timerHandler != null) {
            this.timerHandler.removeComponent();
        }
        this.timerHandler = timerHandler;
        this.timerHandler.addOneIdentifier(CHECK_COMPONENT_IDENTIFIER, timerId, WAIT_TIME_CHECK_COMPONENTS);
        if (useExceptionalState) {
            this.timerHandler.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerId, maxTime);
            this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timerHandler, EXCEPTIONAL_STATE_IDENTIFIER);
            this.exceptionalState = exceptionalState;
        }

        this.useExceptionalState = useExceptionalState;
    }

    /**
     * Check if ExceptionalState is active.
     *
     * @return true if active
     */
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
        int cycleTime = Cycle.DEFAULT_CYCLE_TIME;
        if (this.cycle != null) {
            cycleTime = this.cycle.getCycleTime();
        }

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

    /**
     * Gets the exceptionalState Value.
     *
     * @return the value
     */
    protected int getExceptionalSateValue() {
        return this.exceptionalState.getExceptionalStateValue();
    }

    /**
     * Gets the Value of the optional config channel (output/checkoutput).
     *
     * @param optionalChannel the channel
     * @return the Wrapped Value either value or nextValue.
     */
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
            int exceptionalStateValue = this.getExceptionalStateValue();
                //Allow Forcing but only from exceptionalState -> need to revert exceptionalState
            if (exceptionalStateValue == DEFAULT_MIN_EXCEPTIONAL_VALUE) {
                this.exceptionalStateActive = false;
                this.forceClose();
                this.exceptionalStateActive =  true;
            } else if (exceptionalStateValue == DEFAULT_MAX_POWER_VALUE) {
                this.exceptionalStateActive = false;
                this.forceOpen();
                this.exceptionalStateActive = true;
            } else {
                this.setPowerLevel(this.getExceptionalSateValue());
            }
            childHasNothingToDo = true;
        } else if (this.shouldReset()) {
            this.reset();
            childHasNothingToDo = true;
        } else if (this.getForceFullPowerAndResetChannel()) {
            this.forceOpen();
            childHasNothingToDo = true;
        } else {
            if(this.readyToChange()) {
                int setPointPowerLevelValue = this.setPointPowerLevelValue();
                if (setPointPowerLevelValue >= DEFAULT_MIN_POWER_VALUE) {
                    if (this.maxMinValid()) {
                        setPointPowerLevelValue = TypeUtils.fitWithin(this.minimum.intValue(), this.maximum.intValue(), setPointPowerLevelValue);
                    }
                    if(this.setPowerLevel(setPointPowerLevelValue)) {
                        childHasNothingToDo = true;
                    }
                }
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
    public boolean setPowerLevel(double setPoint) {
        setPoint -= this.getPowerLevelValue();
        if (this.changeByPercentage(setPoint)) {
            this.setPointPowerLevelChannel().setNextValue(-1);
            return true;
        }
        return false;
    }


    /**
     * Changes Valve Position by incoming percentage.
     * Warning, only executes if valve is not busy! (was not forced to open/close)
     * Depending on + or - it changes the current State to open/close it more. Switching the relays on/off does
     * not open/close the valve instantly but slowly. The time it takes from completely closed to completely
     * open is entered in the config. Partial open state of x% is then archived by switching the relay on for
     * time-to-open * x%, or the appropriate amount of time depending on initial state.
     * Sets the Future PowerLevel; ValveManager calls further Methods to refresh true % state.
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
     * @return the current PowerLevel
     */
    protected double calculateCurrentPowerLevelAndSetTime(double percentage) {
        double futurePowerLevel = this.getPowerLevelValue();
        this.getLastPowerLevelChannel().setNextValue(futurePowerLevel);
        this.maximum = this.getMaxAllowedValue();
        this.minimum = this.getMinAllowedValue();
        if (this.maxMinValid() == false) {
            this.minimum = null;
            this.maximum = null;
        }
        futurePowerLevel += percentage;
        if (this.maximum != null && this.maximum + BUFFER < futurePowerLevel) {
            futurePowerLevel = this.maximum;
        } else if (this.lastMaximum != null && this.lastMaximum + BUFFER < futurePowerLevel) {
            futurePowerLevel = this.lastMaximum;
        } else if (futurePowerLevel >= DEFAULT_MAX_POWER_VALUE) {
            futurePowerLevel = DEFAULT_MAX_POWER_VALUE;
        } else if (this.minimum != null && this.minimum - BUFFER > futurePowerLevel) {
            futurePowerLevel = this.minimum;
        } else if (this.lastMinimum != null && this.lastMinimum - BUFFER > futurePowerLevel) {
            futurePowerLevel = this.lastMinimum;
        }
        //Set goal Percentage for future reference
        this.futurePowerLevelChannel().setNextValue(Math.round(futurePowerLevel));
        //if same power level do not change and return --> relays is not always powered
        if (getLastPowerLevelChannel().getNextValue().get() == futurePowerLevel) {
            this.isChanging = false;
            return -1;
        }
        //Calculate the Time to Change the Valve
        if (Math.abs(percentage) >= DEFAULT_MAX_POWER_VALUE) {
            this.timeChannel().setNextValue(DEFAULT_MAX_POWER_VALUE * this.secondsPerPercentage);
        } else {
            this.timeChannel().setNextValue(Math.abs(percentage) * this.secondsPerPercentage);
        }
        return futurePowerLevel;
    }

    /**
     * A Method to help extending classes determine if they are allowed to Accept the Force Request.
     * Additionally it applies the future PowerLevel, the Time needed and updates the PowerLevel.
     *
     * @return true if allowed to force. Otherwise false.
     */

    protected boolean parentForceClose() {

        return this.parentForced(true);
    }

    /**
     * A Method to help extending classes determine if they are allowed to Accept the Force Request.
     * Additionally it applies the future PowerLevel, the Time needed and updates the PowerLevel.
     *
     * @return true if allowed to force. Otherwise false.
     */
    protected boolean parentForceOpen() {

        return this.parentForced(false);
    }

    /**
     * A Method to help processing the Force Request.
     * It applies the future PowerLevel, the Time needed to reach the powerLevel (100% time).
     *
     * @param closing is the valve closing?(true) else: False
     * @return success
     */
    private boolean parentForced(boolean closing) {
        if (this.exceptionalStateActive == false) {
            this.isForced = true;
            this.futurePowerLevelChannel().setNextValue(closing ? DEFAULT_MIN_POWER_VALUE : DEFAULT_MAX_POWER_VALUE);
            this.timeChannel().setNextValue(DEFAULT_MAX_POWER_VALUE * this.secondsPerPercentage);

            this.getIsBusyChannel().setNextValue(true);
            this.isChanging = true;
            this.isClosing = closing;
            this.timeStampValveCurrent = UNINITIALIZED;
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
        boolean ableToAdapt = Math.abs(percentage) >= this.percentPossiblePerCycle;
        double currentPowerValue = this.getPowerLevelValue();
        if (currentPowerValue + percentage >= DEFAULT_MAX_POWER_VALUE || currentPowerValue + percentage <= DEFAULT_MIN_POWER_VALUE) {
            ableToAdapt = true;
        }
        //Parent active always valid.
        if(this.parentActive){
            return false;
        } else {
            return !this.readyToChange() || !this.exceptionalStateActive || !ableToAdapt || percentage == DEFAULT_MIN_POWER_VALUE;
        }
    }

    protected void setCycle(Cycle cycle) {
        this.cycle = cycle;
    }
}
