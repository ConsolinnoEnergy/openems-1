package io.openems.edge.heatsystem.components.valve;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.aio.api.AioChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.heatsystem.components.HeatsystemComponent;
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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This Component allows a Valve  to be configured and controlled.
 * It either works with 1 Aio/Pwm or 1 ChannelAddresses.
 * It can update it's opening/closing state and shows up the percentage value of itself.
 * To check if the output is correct, you can configure the CheckUp.
 * E.g. The Consolinno Leaflet reads the MCP and it's status, this will be send into the {@link AioChannel#getPercentChannel()} ()}
 * If the value you read is the expected value, everything is ok, otherwise the Components tries to set the expected values again.
 */
@Designate(ocd = ConfigValveOneInput.class, factory = true)
@Component(name = "HeatsystemComponent.Valve.OneInput", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE

)

public class ValveOneInput extends AbstractValve implements OpenemsComponent, Valve, ExceptionalState, EventHandler {

    private final Logger log = LoggerFactory.getLogger(ValveOneInput.class);

    private ChannelAddress outputChannel;
    private ChannelAddress optionalCheckOutputChannel;

    private ConfigValveOneInput config;

    private final AtomicInteger configTries = new AtomicInteger(0);
    private boolean validCheckOutputChannel;
    private boolean powerValueReachedBeforeCheck = false;

    @Reference
    ComponentManager cpm;


    public ValveOneInput() {
        super(OpenemsComponent.ChannelId.values(),
                HeatsystemComponent.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, ConfigValveOneInput config) {
        try {
            super.activate(context, config.id(), config.alias(), config.enabled());
            this.config = config;
            this.activationOrModifiedRoutine(config);
            if (config.useExceptionalState()) {
                super.createExcpetionalStateHandler(config.timerId(), config.maxTime(), this.cpm, this);
            }
            if (config.shouldCloseOnActivation()) {
                this.getPowerLevelChannel().setNextValue(0);
                this.setPointPowerLevelChannel().setNextWriteValueFromObject(0);
                this.forceClose();
            }
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void activationOrModifiedRoutine(ConfigValveOneInput config) throws ConfigurationException {
        try {
            this.outputChannel = ChannelAddress.fromString(config.inputChannel());

            if (config.useCheckChannel()) {
                this.optionalCheckOutputChannel = ChannelAddress.fromString(config.checkChannel());
            } else {
                this.optionalCheckOutputChannel = null;
            }
            this.secondsPerPercentage = ((double) config.timeToOpenValve() / 100.d);
        } catch (OpenemsError.OpenemsNamedException e) {
            throw new ConfigurationException("Activate : " + super.id(), "ChannelAddresses are configured in a wrong Way");
        }
    }

    @Modified
    void modified(ComponentContext context, ConfigValveOneInput config) {
        this.configSuccess = false;
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        try {
            this.activationOrModifiedRoutine(config);
        } catch (ConfigurationException e) {
            //TODO
        }
        this.configSuccess = true;
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public boolean changeByPercentage(double percentage) {
        double currentPowerLevel;
        if (!this.readyToChange() || percentage == 0) {
            return false;
        } else {
            //Setting the oldPowerLevel and adjust the percentage Value
            currentPowerLevel = this.getPowerLevelValue();
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
            } else if (currentPowerLevel >= 100) {
                currentPowerLevel = 100;
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
                return false;
            }
            //Calculate the Time to Change the Valve
            if (Math.abs(percentage) >= 100) {
                this.timeChannel().setNextValue(100 * secondsPerPercentage);
            } else {
                this.timeChannel().setNextValue(Math.abs(percentage) * secondsPerPercentage);
            }
            this.writeToOutputChannel(Math.round(currentPowerLevel));
            boolean prevClosing = this.isClosing;
            this.isClosing = percentage < 0;
            this.isChanging = true;
            if (prevClosing != this.isClosing) {
                this.timeStampValveCurrent = -1;
            }
            return true;
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
            this.futurePowerLevelChannel().setNextValue(0);
            this.timeChannel().setNextValue(100 * secondsPerPercentage);
            this.writeToOutputChannel(0);
            this.getIsBusyChannel().setNextValue(true);
            this.isChanging = true;
            this.isClosing = true;
            this.timeStampValveCurrent = -1;
            this.updatePowerLevel();
        }

    }

    private void writeToOutputChannel(double percent) {
        try {
            Channel<?> channelToWrite = this.cpm.getChannel(this.outputChannel);
            int scaleFactor = channelToWrite.channelDoc().getUnit().equals(Unit.THOUSANDTH) ? 10 : 1;
            if (channelToWrite instanceof WriteChannel<?>) {

                ((WriteChannel<?>) channelToWrite).setNextWriteValueFromObject(percent * scaleFactor);
            } else {
                channelToWrite.setNextValue(percent * scaleFactor);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Couldn't write into Channel! Might not exist: " + this.outputChannel);
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
            this.futurePowerLevelChannel().setNextValue(100);
            this.timeChannel().setNextValue(100 * secondsPerPercentage);
            this.writeToOutputChannel(100);
            this.getIsBusyChannel().setNextValue(true);
            this.isChanging = true;
            this.isClosing = false;
            this.timeStampValveCurrent = -1;
            this.updatePowerLevel();
        }

    }

    @Override
    public boolean powerLevelReached() {
        boolean reached = true;
        if (this.isChanging()) {
            reached = false;
            if (this.getPowerLevelChannel().value().isDefined() && this.futurePowerLevelChannel().value().isDefined()) {
                if (this.isClosing) {
                    reached = this.getPowerLevelValue() <= this.getFuturePowerLevelValue();
                } else {
                    reached = this.getPowerLevelValue() >= this.getFuturePowerLevelValue();
                }
            }
        }
        //ReadyToChange always True except
        reached = reached && this.readyToChange();
        if (reached) {
            isChanging = false;
            timeStampValveCurrent = -1;
            this.powerValueReachedBeforeCheck = true;

        } else {
            this.powerValueReachedBeforeCheck = false;
        }
        return reached;
    }

    /**
     * Check if the expected Value is almost the same as the written Value, if not -> Set Value again.
     *
     * @return true if expected Value is almost the Same (Or if an Error occurred, so the Valve can continue to run)
     */

    private boolean checkOutputIsEqualToGoalPercent() {
        if (this.optionalCheckOutputChannel != null) {
            try {
                //Prepare and get all Values needed.
                Channel<?> optionalChannel = this.cpm.getChannel(this.optionalCheckOutputChannel);
                Value<?> otherChannelValue = optionalChannel.value().isDefined() ? optionalChannel.value() : optionalChannel.getNextValue().isDefined() ? optionalChannel.getNextValue() : null;
                int scaleDownFactorOtherChannel = this.getScaleFactor(optionalChannel);

                Value<Double> goalPowerLevelValue = this.futurePowerLevelChannel().value().isDefined() ? this.futurePowerLevelChannel().value() : null;
                int scaleDownFactorGoalPowerLevel = this.getScaleFactor(this.futurePowerLevelChannel());


                if (otherChannelValue != null && goalPowerLevelValue != null) {
                    if (this.containsOnlyNumbers(otherChannelValue.get().toString()) && this.containsOnlyNumbers(goalPowerLevelValue.get().toString())) {
                        this.validCheckOutputChannel = true;
                        double otherValue = Double.parseDouble(otherChannelValue.get().toString());
                        double goalValue = Double.parseDouble(goalPowerLevelValue.get().toString());
                        if (Math.abs(otherValue / scaleDownFactorOtherChannel - goalValue / scaleDownFactorGoalPowerLevel) > TOLERANCE) {
                            this.getPowerLevelChannel().setNextValue(otherValue / scaleDownFactorOtherChannel);
                            this.setPointPowerLevelChannel().setNextWriteValueFromObject(goalValue / scaleDownFactorGoalPowerLevel);
                            this.isChanging = true;
                            this.timeStampValveCurrent = -1;
                            return false;
                        }
                    } else {
                        this.validCheckOutputChannel = false;
                        this.log.warn("Couldn't compare Values of " + super.id() + " Other Value Channel does not contain only numbers!");
                    }

                } else {
                    return true;
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.error("Couldn't get or write into Channel: Check channelAddress: " + this.optionalCheckOutputChannel);
                return true;
            }

        }
        return true;
    }

    private int getScaleFactor(Channel<?> channel) {
        return channel.channelDoc().getUnit().equals(Unit.THOUSANDTH) ? 10 : 1;
    }

    @Override
    public void reset() {
        this.forceClose();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            if (this.configSuccess == false) {
                try {
                    this.activationOrModifiedRoutine(this.config);
                    this.configSuccess = true;
                } catch (ConfigurationException e) {
                    this.configSuccess = false;
                    if (this.configTries.get() >= MAX_CONFIG_TRIES) {
                        this.log.error("Config is Wrong in : " + super.id());
                    } else {
                        this.configTries.getAndIncrement();
                    }
                }
            } else if (this.powerValueReachedBeforeCheck) {
                boolean check = this.checkOutputIsEqualToGoalPercent();
                if (check == false) {
                    //Set PowerLevel To "correct" Output -> if Output says 10% it is 10%; since it is not timebased in comparison to the ValveTwoRelay
                    try {
                        if (this.optionalCheckOutputChannel != null && this.validCheckOutputChannel) {
                            Channel<?> otherChannel = this.cpm.getChannel(this.optionalCheckOutputChannel);

                            Value<?> captureValueOfChannel = otherChannel.value().isDefined() ? otherChannel.value() : otherChannel.getNextValue().isDefined() ? otherChannel.getNextValue() : null;
                            if (captureValueOfChannel != null) {
                                Unit otherUnit = otherChannel.channelDoc().getUnit();
                                Unit powerLevelUnit = this.getPowerLevelChannel().channelDoc().getUnit();
                                double scaleDownFactor = 1;
                                double value = Double.parseDouble(captureValueOfChannel.get().toString());
                                if (otherUnit.equals(powerLevelUnit) == false) {
                                    //powerLevel is PERCENT but
                                    if (powerLevelUnit.equals(Unit.PERCENT) && otherUnit.equals(Unit.THOUSANDTH)) {
                                        scaleDownFactor = 10;
                                    } else if (powerLevelUnit.equals(Unit.THOUSANDTH) && otherUnit.equals(Unit.PERCENT)) {
                                        scaleDownFactor = 0.1d;
                                    }
                                }
                                this.getPowerLevelChannel().setNextValue(value / scaleDownFactor);
                            }

                        }
                        this.powerValueReachedBeforeCheck = false;
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Couldn't receive optional Check Output Channel! " + super.id());
                    }
                }
            }

        }
    }
}
