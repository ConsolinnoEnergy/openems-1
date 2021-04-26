package io.openems.edge.heatsystem.components.valve;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

import io.openems.edge.heatsystem.components.ConfigurationType;
import io.openems.edge.heatsystem.components.PassingChannel;
import io.openems.edge.heatsystem.components.Valve;
import io.openems.edge.relay.api.Relay;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Passing.Valve",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS}
)
public class ValveImpl extends AbstractOpenemsComponent implements OpenemsComponent, Valve, EventHandler {

    private final Logger log = LoggerFactory.getLogger(ValveImpl.class);

    private ChannelAddress openAddress;
    private ChannelAddress closeAddress;

    private Relay openRelay;
    private Relay closeRelay;

    private double secondsPerPercentage;
    private long timeStampValveInitial;
    private long timeStampValveCurrent = -1;
    //if true updatePowerlevel
    private boolean isChanging = false;
    //if true --> subtraction in updatePowerLevel else add
    private boolean isClosing = false;
    private boolean wasAlreadyReset = false;
    private boolean isForced;
    private static int EXTRA_BUFFER_TIME = 2000;

    private static final int BUFFER = 5;

    private Double lastMaximum;
    private Double lastMinimum;
    private Double maximum;
    private Double minimum;
    private Config config;
    private ConfigurationType configurationType;

    @Reference
    ComponentManager cpm;


    public ValveImpl() {
        super(OpenemsComponent.ChannelId.values(), PassingChannel.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activateOrModifiedRoutine(config);
        this.getIsBusy().setNextValue(false);
        this.getPowerLevel().setNextValue(0);
        this.getLastPowerLevel().setNextValue(0);
        this.setPowerLevelPercent().setNextValue(-1);
        this.setGoalPowerLevel().setNextValue(0);
        this.config = config;
        if (config.shouldCloseOnActivation()) {
            this.forceClose();
        }
    }

    private void activateOrModifiedRoutine(Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.configurationType = config.configurationType();
        switch (this.configurationType) {
            case CHANNEL:
                this.openAddress = ChannelAddress.fromString(config.open());
                this.closeAddress = ChannelAddress.fromString(config.close());
                if (this.checkChannelOk() == false) {
                    throw new ConfigurationException("ActivateMethod", "Given Channels are not ok!");
                }
                break;
            case DEVICE:
                if (this.checkDevicesOk(config.open(), config.close()) == false) {
                    throw new ConfigurationException("ActivateMethod", "Given Devices are not ok!");
                }
                break;
        }
        this.secondsPerPercentage = ((double) config.valve_Time() / 100.d);
        this.getTimeNeeded().setNextValue(0);

    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activateOrModifiedRoutine(config);
    }

    private boolean checkDevicesOk(String open, String close) throws OpenemsError.OpenemsNamedException {
        OpenemsComponent relayToApply = this.cpm.getComponent(open);
        if (relayToApply instanceof Relay) {
            this.openRelay = (Relay) relayToApply;
        } else {
            return false;
        }
        relayToApply = this.cpm.getComponent(close);
        if (relayToApply instanceof Relay) {
            this.closeRelay = (Relay) relayToApply;
        } else {
            return false;
        }
        return true;
    }

    private boolean checkChannelOk() throws OpenemsError.OpenemsNamedException {
        Channel<?> openChannel = this.cpm.getChannel(this.openAddress);
        Channel<?> closeChannel = this.cpm.getChannel(this.closeAddress);
        return openChannel instanceof WriteChannel<?> && closeChannel instanceof WriteChannel<?> && openChannel.getType()
                .equals(OpenemsType.BOOLEAN) && closeChannel.getType().equals(OpenemsType.BOOLEAN);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }


    // --------------- READY TO CHANGE AND CHANGE BY PERCENTAGE ------------ //

    /**
     * Ready To Change is always true except if the Valve was forced to open/close and the Time to close/open the
     * Valve completely is not over.
     */
    @Override
    public boolean readyToChange() {
        long currentTime = this.getMilliSecondTime();
        if (this.isForced) {
            if (this.timeStampValveCurrent == -1 || (currentTime - this.timeStampValveInitial)
                    < ((this.getTimeNeeded().getNextValue().get() * 1000) + EXTRA_BUFFER_TIME)) {
                return false;
            }
            this.getIsBusy().setNextValue(false);
            this.shouldForceClose().setNextValue(false);
            this.wasAlreadyReset = false;
            this.isForced = false;
        }
        if (this.isChanging == false) {
            this.timeStampValveCurrent = -1;
        }
        return true;

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
     * @param percentage adjusting the current powerlevel in % points. Meaning if current state is 10%, requesting
     *                   changeByPercentage(20) will change the state to 30%.
     *                   <p>
     *                   If the Valve is busy return false
     *                   otherwise: save the current PowerLevel to the old one and overwrite the new one.
     *                   Then it will check how much time is needed to adjust the position of the valve.
     *                   If percentage is neg. valve needs to be closed (further)
     *                   else it needs to open (further).
     *                   </p>
     */
    @Override
    public boolean changeByPercentage(double percentage) {
        double currentPowerLevel;

        if (this.readyToChange() == false || percentage == 0) {
            return false;
        } else {
            //Setting the oldPowerLevel and adjust the percentage Value
            currentPowerLevel = this.getPowerLevel().value().get();
            this.getLastPowerLevel().setNextValue(currentPowerLevel);
            this.maximum = getMaxValue();
            this.minimum = getMinValue();
            if (this.maxMinValid() == false) {
                this.minimum = null;
                this.maximum = null;
            }
            currentPowerLevel += percentage;
            if (this.maximum != null && this.maximum < currentPowerLevel) {
                currentPowerLevel = this.maximum;
            } else if (this.lastMaximum != null && this.lastMaximum < currentPowerLevel) {
                currentPowerLevel = this.lastMaximum;
            } else if (currentPowerLevel >= 100) {
                currentPowerLevel = 100;
            } else if (this.minimum != null && this.minimum > currentPowerLevel) {
                currentPowerLevel = this.minimum;
            } else if (this.lastMinimum != null && this.lastMinimum > currentPowerLevel) {
                currentPowerLevel = this.lastMinimum;
            }
            //Set goal Percentage for future reference
            this.setGoalPowerLevel().setNextValue(currentPowerLevel);
            //if same power level do not change and return --> relays is not always powered
            if (getLastPowerLevel().getNextValue().get() == currentPowerLevel) {
                this.isChanging = false;
                return false;
            }
            //Calculate the Time to Change the Valve
            if (Math.abs(percentage) >= 100) {
                this.getTimeNeeded().setNextValue(100 * this.secondsPerPercentage);
            } else {
                this.getTimeNeeded().setNextValue(Math.abs(percentage) * this.secondsPerPercentage);
            }
            //Close on negative Percentage and Open on Positive
            this.isChanging = true;
            if (percentage < 0) {
                this.valveClose();
            } else {
                this.valveOpen();
            }
            return true;
        }
    }

    private boolean maxMinValid() {
        return (this.maximum >= this.minimum && this.maximum > 0 && this.minimum >= 0);
    }


    //------------------------------------------------------ //


    //--------------UPDATE POWERLEVEL AND POWER LEVEL REACHED---------------//

    /**
     * Update PowerLevel by getting elapsed Time and check how much time has passed.
     * Current PowerLevel and new Percentage is added together and rounded to 3 decimals.
     */
    @Override
    public void updatePowerLevel() {
        //Only Update PowerLevel if the Valve is Changing
        if (this.isChanging()) {
            long elapsedTime = this.getMilliSecondTime();
            //If it's the first update of PowerLevel
            if (this.timeStampValveCurrent == -1) {
                //only important for ForceClose/Open
                this.timeStampValveInitial = this.getMilliSecondTime();
                //First time in change
                elapsedTime = 0;

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
            double truncatedDouble = this.getPowerLevel().value().isDefined() == false ? 0 : BigDecimal.valueOf(this.getPowerLevel().value().get() + percentIncrease)
                    .setScale(3, RoundingMode.HALF_UP)
                    .doubleValue();
            if (truncatedDouble > 100) {
                truncatedDouble = 100;
            } else if (truncatedDouble < 0) {
                truncatedDouble = 0;
            }
            this.getPowerLevel().setNextValue(truncatedDouble);
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
            if (this.getPowerLevel().value().isDefined() && this.setGoalPowerLevel().getNextValue().isDefined()) {
                if (this.isClosing) {
                    reached = this.getPowerLevel().value().get() <= this.setGoalPowerLevel().getNextValue().get();
                } else {
                    reached = this.getPowerLevel().value().get() >= this.setGoalPowerLevel().getNextValue().get();

                }
            }
        }
        //ReadyToChange always True except
        reached = reached && this.readyToChange();
        if (reached) {
            this.isChanging = false;
            this.timeStampValveCurrent = -1;
            this.shutdownRelays();
        }
        return reached;
    }

    // ------------------------------------------------------------- //

    /**
     * IS Changing --> Is closing/Opening.
     *
     * @return isChanging
     */
    @Override
    public boolean isChanging() {
        return this.isChanging;
    }

    //---------------------RESET------------------------- //

    /**
     * Resets the Valve and forces to close.
     * Was Already Reset prevents multiple forceCloses if Channel not refreshed in time.
     */
    @Override
    public void reset() {
        if (this.wasAlreadyReset == false) {
            this.forceClose();
            this.wasAlreadyReset = true;
        }

    }

    /**
     * Called by ValveManager to check if this Valve should be reset.
     *
     * @return shouldReset.
     */
    @Override
    public boolean shouldReset() {
        if (this.shouldForceClose().getNextValue().isDefined()) {
            return this.shouldForceClose().getNextValue().get();
        }
        return false;
    }


    // ------------ FORCE OPEN AND CLOSE------------------ //

    /**
     * Closes the valve completely, overriding any current valve operation.
     * If a closed valve is all you need, better use this instead of changeByPercentage(-100) as you do not need
     * to check if the valve is busy or not.
     * Usually called to Reset a Valve or ForceClose the Valve on an Error.
     */
    @Override
    public void forceClose() {
        if (this.isForced == false || this.isClosing == false) {
            this.isForced = true;
            this.setGoalPowerLevel().setNextValue(0);
            this.getTimeNeeded().setNextValue(100 * this.secondsPerPercentage);
            this.valveClose();
            this.getIsBusy().setNextValue(true);
            this.isChanging = true;
            this.isClosing = true;
            this.timeStampValveCurrent = -1;
            this.updatePowerLevel();
        }

    }

    /**
     * Opens the valve completely, overriding any current valve operation.
     * If an open valve is all you need, better use this instead of changeByPercentage(100) as you do not need
     * to check if the valve is busy or not.
     */
    @Override
    public void forceOpen() {
        if (this.isForced == false || this.isClosing == true) {
            this.isForced = true;
            this.setGoalPowerLevel().setNextValue(100);
            this.getTimeNeeded().setNextValue(100 * this.secondsPerPercentage);
            this.valveOpen();
            this.getIsBusy().setNextValue(true);
            this.isChanging = true;
            this.isClosing = false;
            this.timeStampValveCurrent = -1;
            this.updatePowerLevel();
        }

    }

    //-------------------------------------------------------------//


    //---------------------ShutDown Relay---------//

    /**
     * Turn off Relay if PowerLevel is reached.
     */
    private void shutdownRelays() {
        this.controlRelays(false, "Open");
        this.controlRelays(false, "Closed");
    }

    // -------------------------------------- //


    // ---------- CLOSE AND OPEN VALVE ------------ //

    /**
     * Closes the valve and sets a time stamp.
     * DO NOT CALL DIRECTLY! Might not work if called directly as the timer for "readyToChange()" is not
     * set properly. Use either "changeByPercentage()" or forceClose / forceOpen.
     */
    private void valveClose() {

        this.controlRelays(false, "Open");
        this.controlRelays(true, "Closed");
        if (this.isClosing == false) {
            this.timeStampValveCurrent = -1;
            this.isClosing = true;
        }

    }

    /**
     * Opens the valve and sets a time stamp.
     * DO NOT CALL DIRECTLY! Might not work if called directly as the timer for "readyToChange()" is not
     * set properly. Use either "changeByPercentage()" or forceClose / forceOpen.
     */
    private void valveOpen() {

        this.controlRelays(false, "Closed");
        this.controlRelays(true, "Open");
        if (this.isClosing == true) {
            this.timeStampValveCurrent = -1;
            this.isClosing = false;
        }
    }
    //-------------------------------------


    /**
     * Controls the relays by typing either activate or not and what relays should be called.
     * DO NOT USE THIS !!!! Exception: ValveManager --> Needs this method if Time is up to set Valve Relays off.
     * If ExceptionHandling --> use forceClose or forceOpen!
     *
     * @param activateOrDeactivate activate or deactivate.
     * @param whichRelays          opening or closing relays ?
     *                             <p>Writes depending if the relays is an opener or closer, the correct boolean.
     *                             if the relays was set false (no power) busy will be false.</p>
     */
    private void controlRelays(boolean activateOrDeactivate, String whichRelays) {
        try {
            switch (whichRelays) {
                case "Open":
                    if (this.configurationType.equals(ConfigurationType.CHANNEL)) {
                        if (this.checkChannelOk()) {
                            WriteChannel<Boolean> openChannel = this.cpm.getChannel(this.openAddress);
                            openChannel.setNextWriteValue(activateOrDeactivate);
                        }
                    } else {
                        this.openRelay.getRelaysWriteChannel().setNextWriteValueFromObject(activateOrDeactivate);
                    }
                    break;

                case "Closed":
                    if (this.configurationType.equals(ConfigurationType.CHANNEL)) {
                        if (this.checkChannelOk()) {
                            WriteChannel<Boolean> closeChannel = this.cpm.getChannel(this.closeAddress);
                            closeChannel.setNextWriteValue(activateOrDeactivate);
                        }
                    } else {
                        this.closeRelay.setRelayStatus(activateOrDeactivate);
                    }
                    break;

            }
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }


    // --------- UTILITY -------------//

    /**
     * get Current Time in Ms.
     *
     * @return currentTime in Ms.
     */

    private long getMilliSecondTime() {
        long time = System.nanoTime();
        return TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
    }

    // ----------------------------- //


    @Override
    public String debugLog() {
        if (this.getPowerLevel().value().isDefined()) {
            String name = "";
            if (!super.alias().equals("")) {
                name = super.alias();
            } else {
                name = super.id();
            }
            return "Valve: " + name + ": " + this.getPowerLevel().value().toString() + "\n";
        } else {
            return "\n";
        }
    }


    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            this.lastMaximum = this.maximum;
            this.lastMinimum = this.minimum;
            this.updatePowerLevel();
            boolean reached = this.powerLevelReached();
            if (reached) {
                this.readyToChange();
            }
            this.updatePowerLevel();
        }
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            boolean maxMin = false;
            if (this.maxValue().getNextWriteValue().isPresent() && this.maxValue().getNextWriteValue().get() + BUFFER < this.getCurrentPowerLevelValue()) {
                this.changeByPercentage(this.maxValue().getNextWriteValue().get() - this.getCurrentPowerLevelValue());
                if (this.maxMinValid()) {
                    maxMin = true;
                } else {
                    maxMin = false;
                }

            } else if (this.minValue().getNextWriteValue().isPresent() && this.minValue().getNextWriteValue().get() + BUFFER > this.getCurrentPowerLevelValue()) {
                this.changeByPercentage(this.minValue().getNextWriteValue().get() - this.getCurrentPowerLevelValue());
                if (this.maxMinValid()) {
                    maxMin = true;
                } else {
                    maxMin = false;
                }
            }


            //next Value bc on short scheduler the value.get() is not quick enough updated
            //Should this be Reset?
            if (this.shouldReset()) {
                this.reset();
                this.shouldForceClose().setNextValue(false);
                this.updatePowerLevel();
            } else {
                //Reacting to SetPowerLevelPercent by REST Request
                if (maxMin == false && this.setPowerLevelPercent().value().isDefined() && this.setPowerLevelPercent().value().get() >= 0) {

                    int changeByPercent = this.setPowerLevelPercent().value().get();
                    //getNextPowerLevel Bc it's the true current state that's been calculated
                    if (this.getPowerLevel().getNextValue().isDefined()) {
                        changeByPercent -= this.getPowerLevel().getNextValue().get();
                    }
                    if (this.changeByPercentage(changeByPercent)) {
                        this.setPowerLevelPercent().setNextValue(-1);
                    }
                }
                //Calculate current % State of Valve
                if (this.powerLevelReached() == false) {
                    this.updatePowerLevel();
                }
            }
        }
    }
}

