package io.openems.edge.heatsystem.components.valve.old;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.heatsystem.components.HeatsystemComponent;
import io.openems.edge.heatsystem.components.api.PassingChannel;
import io.openems.edge.heatsystem.components.api.Valve;
import io.openems.edge.heatsystem.components.valve.AbstractValve;
import io.openems.edge.manager.valve.api.ManagerValve;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;


@Designate(ocd = ConfigValveTwoRelays.class, factory = true)
@Component(name = "Passing.Valve",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS}
)
public class ValveTwoRelaysImpl extends AbstractValve implements OpenemsComponent, Valve, EventHandler, ExceptionalState {

    private ChannelAddress openAddress;
    private ChannelAddress closeAddress;
    private ChannelAddress checkOpenAddress;
    private ChannelAddress checkClosingAddress;
    private boolean useCheckChannel;

    //if true updatePowerlevel
    //if true --> subtraction in updatePowerLevel else add
    private ConfigValveTwoRelays config;

    @Reference
    ComponentManager cpm;


    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    ManagerValve managerValve;


    public ValveTwoRelaysImpl() {
        super(OpenemsComponent.ChannelId.values(),
                HeatsystemComponent.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, ConfigValveTwoRelays config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        //boolean setupOk = true;
        this.openAddress = ChannelAddress.fromString(config.openChannelAddress());
        this.closeAddress = ChannelAddress.fromString(config.closeChannelAddress());
        this.config = config;

        if (this.checkChannelOk()) {
            super.activate(context, config.id(), config.alias(), config.enabled());
            this.getIsBusy().setNextValue(false);
            this.getPowerLevel().setNextValue(0);
            this.getLastPowerLevel().setNextValue(0);
            this.secondsPerPercentage = ((double) config.valve_Time() / 100.d);
            this.managerValve.addValve(super.id(), this);
            this.getTimeNeeded().setNextValue(0);
            this.setPowerLevelPercent().setNextValue(-1);
            this.setGoalPowerLevel().setNextValue(0);
            if (config.shouldCloseOnActivation()) {
                this.shouldForceClose().setNextValue(true);
            }
        } else {
            throw new ConfigurationException("ActivateMethod", "Given Channels are not ok!");
        }
        if (config.useExceptionalState()) {
            super.createExcpetionalStateHandler(config.timerId(), config.maxTime(), this.cpm, this);
        }

    }

    private boolean checkChannelOk() throws OpenemsError.OpenemsNamedException {
        Channel<?> openChannel = this.cpm.getChannel(openAddress);
        Channel<?> closeChannel = this.cpm.getChannel(closeAddress);

        return openChannel instanceof WriteChannel<?> && closeChannel instanceof WriteChannel<?> && openChannel.getType()
                .equals(OpenemsType.BOOLEAN) && closeChannel.getType().equals(OpenemsType.BOOLEAN);
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        this.managerValve.removeValve(super.id());
    }


    // --------------- READY TO CHANGE AND CHANGE BY PERCENTAGE ------------ //

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

        if (!this.readyToChange() || percentage == 0) {
            return false;
        } else {
            //Setting the oldPowerLevel and adjust the percentage Value
            currentPowerLevel = this.getPowerLevel().value().get();
            this.getLastPowerLevel().setNextValue(currentPowerLevel);
            this.maximum = getMaxValue();
            this.minimum = getMinValue();
            if (maxMinValid() == false) {
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
                this.getTimeNeeded().setNextValue(100 * secondsPerPercentage);
            } else {
                this.getTimeNeeded().setNextValue(Math.abs(percentage) * secondsPerPercentage);
            }
            //Close on negative Percentage and Open on Positive
            if (percentage < 0) {
                isChanging = true;
                valveClose();
            } else {
                isChanging = true;
                valveOpen();
            }
            return true;
        }
    }


    //------------------------------------------------------ //


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
            isChanging = false;
            timeStampValveCurrent = -1;
            shutdownRelays();
        }
        return reached;
    }

    // ------------------------------------------------------------- //


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
            this.getTimeNeeded().setNextValue(100 * secondsPerPercentage);
            valveClose();
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
            this.getTimeNeeded().setNextValue(100 * secondsPerPercentage);
            valveOpen();
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
        controlRelays(false, "Open");
        controlRelays(false, "Closed");
    }

    // -------------------------------------- //


    // ---------- CLOSE AND OPEN VALVE ------------ //

    /**
     * Closes the valve and sets a time stamp.
     * DO NOT CALL DIRECTLY! Might not work if called directly as the timer for "readyToChange()" is not
     * set properly. Use either "changeByPercentage()" or forceClose / forceOpen.
     */
    private void valveClose() {

        controlRelays(false, "Open");
        controlRelays(true, "Closed");
        if (this.isClosing == false) {
            timeStampValveCurrent = -1;
            this.isClosing = true;
        }

    }

    /**
     * Opens the valve and sets a time stamp.
     * DO NOT CALL DIRECTLY! Might not work if called directly as the timer for "readyToChange()" is not
     * set properly. Use either "changeByPercentage()" or forceClose / forceOpen.
     */
    private void valveOpen() {

        controlRelays(false, "Closed");
        controlRelays(true, "Open");
        if (this.isClosing == true) {
            timeStampValveCurrent = -1;
            this.isClosing = false;
        }
    }
    //-------------------------------------


    /**
     * Controls the relays by typing either activate or not and what relays should be called.
     * DO NOT USE THIS !!!! Exception: ValveManager --> Needs this method if Time is up to set Valve Relays off.
     * If ExceptionHandling --> use forceClose or forceOpen!
     *
     * @param activate    activate or deactivate.
     * @param whichRelays opening or closing relays ?
     *                    <p>Writes depending if the relays is an opener or closer, the correct boolean.
     *                    if the relays was set false (no power) busy will be false.</p>
     */
    private void controlRelays(boolean activate, String whichRelays) {
        try {
            switch (whichRelays) {
                case "Open":
                    if (this.checkChannelOk()) {
                        WriteChannel<Boolean> openChannel = this.cpm.getChannel(openAddress);
                        openChannel.setNextWriteValue(activate);
                    }
                    break;

                case "Closed":
                    if (this.checkChannelOk()) {
                        WriteChannel<Boolean> closeChannel = this.cpm.getChannel(closeAddress);
                        closeChannel.setNextWriteValue(activate);
                    }
                    break;

            }
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }


    // ----------------------------- //


    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            this.lastMaximum = this.maximum;
            this.lastMinimum = this.minimum;
            this.checkValveChannelCorrect();
        }
    }

    private void checkValveChannelCorrect() {
        if (this.useCheckChannel) {
            try {
                boolean channelValueIsTrue;
                boolean channelValueIsNotFalse;

                Channel<?> channelToCheckForTrue;
                Channel<?> channelToCheckForFalse;
                if (this.isClosing) {
                    channelToCheckForTrue = this.cpm.getChannel(this.checkClosingAddress);
                    channelToCheckForFalse = this.cpm.getChannel(this.checkOpenAddress);
                } else {
                    channelToCheckForTrue = this.cpm.getChannel(this.checkOpenAddress);
                    channelToCheckForFalse = this.cpm.getChannel(this.checkClosingAddress);
                }
                channelValueIsTrue = this.checkChannelValueIsTrue(channelToCheckForTrue);
                channelValueIsNotFalse = this.checkChannelValueIsTrue(channelToCheckForFalse);
                if (this.isChanging) {
                    if (channelValueIsTrue == false || channelValueIsNotFalse) {
                        updateOk = false;
                        timeStampValveCurrent = -1;
                        this.getPowerLevel().setNextValue(this.powerLevelBeforeUpdate);
                        if (this.isClosing) {
                            valveClose();
                        } else {
                            valveOpen();
                        }
                    } else {
                        updateOk = true;
                    }
                } else {
                    if (channelValueIsTrue) {
                        this.isChanging = true;
                        this.updatePowerLevel();
                        this.shutdownRelays();
                    }
                }

            } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
                this.log.warn("Attention in Valve: " + super.id() + " ChannelToCheck Method failed: " + e.getMessage());
                this.updateOk = false;
            }
        }
    }


    private boolean checkChannelValueIsTrue(Channel<?> channelToCheck) throws ConfigurationException {
        Value<?> value = channelToCheck.value().isDefined() ? channelToCheck.value()
                : channelToCheck.getNextValue().isDefined() ? channelToCheck.getNextValue() : null;
        if (value != null) {
            switch (channelToCheck.getType()) {
                case BOOLEAN:
                    return (Boolean) value.get();
                case SHORT:
                case INTEGER:
                case LONG:
                case FLOAT:
                case DOUBLE:
                    return ((Double) value.get() > 0);
                case STRING:
                    if (this.containsOnlyNumbers(value.get().toString())) {
                        return Double.parseDouble(value.get().toString()) > 0;
                    } else {
                        throw new ConfigurationException("CheckChannelValue: " + super.id() + channelToCheck.toString(), "Channel Cannot be parsed to a Numeric Value");
                    }
                default:
                    throw new ConfigurationException("CheckChannelValue: " + super.id() + channelToCheck.toString(), "ValueType not supported!");
            }
        } else {
            return false;
        }
    }

    /*   @Override
     public void handleEvent(Event event) {
             if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
              this.updatePowerLevel();
              boolean reached = powerLevelReached();
              if (reached) {
                  this.readyToChange();
              }
           }
         if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS)) {
             if (this.setPowerLevelPercent().value().isDefined() && this.setPowerLevelPercent().value().get() >= 0) {
                   if (this.changeByPercentage(changeByPercent)) {
                     try {
                         this.setPowerLevelPercent().setNextWriteValue(-1);
                     } catch (OpenemsError.OpenemsNamedException e) {
                         e.printStackTrace();
                     }
                 }
             }
           }
     } */
}

