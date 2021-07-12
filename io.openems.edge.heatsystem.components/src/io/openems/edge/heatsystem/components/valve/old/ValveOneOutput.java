package io.openems.edge.heatsystem.components.valve.old;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
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
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */

@Designate(ocd = ConfigValveOneOutput.class, factory = true)
@Component(name = "ValveOneOutput", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE

)

public class ValveOneOutput extends AbstractValve implements OpenemsComponent, Valve, ExceptionalState, EventHandler {

    private final Logger log = LoggerFactory.getLogger(ValveOneOutput.class);

    private ChannelAddress outputChannel;
    private ChannelAddress optionalCheckOutputChannel;
    private boolean configSuccess;
    private ExceptionalStateHandler stateHandler;
    private ConfigValveOneOutput config;
    private String oldId;
    // 5% tolerance
    private static final int TOLERANCE = 5;
    private static final int MAX_CONFIG_TRIES = 10;
    private final AtomicInteger configTries = new AtomicInteger(0);
    private boolean validCheckOutputChannel;
    private boolean powerValueReachedBeforeCheck = false;

    @Reference
    ComponentManager cpm;


    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    ManagerValve managerValve;

    public ValveOneOutput() {
        super(ChannelId.values(),
                Valve.ChannelId.values(),
                PassingChannel.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, ConfigValveOneOutput config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.oldId = super.id();
        this.config = config;
        this.activationOrModifiedRoutine(config);
        if (config.useExceptionalState()) {
            super.createExcpetionalStateHandler(config.timerId(), config.maxTime(), this.cpm, this);
        }
        if (config.shouldCloseOnActivation()) {
            try {
                this.setPowerLevelPercent().setNextWriteValueFromObject(0);
                this.forceClose();
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't set Valve to 0");
            }
        }
        this.configSuccess = true;


    }

    private void activationOrModifiedRoutine(ConfigValveOneOutput config) throws ConfigurationException {
        try {
            this.outputChannel = ChannelAddress.fromString(config.outputChannel());

            if (config.useCheckOutput()) {
                this.optionalCheckOutputChannel = ChannelAddress.fromString(config.checkOutputChannel());
            } else {
                this.optionalCheckOutputChannel = null;
            }
            this.managerValve.removeValve(oldId);
            this.managerValve.addValve(super.id(), this);
            this.secondsPerPercentage = ((double) config.timeToOpenValve() / 100.d);
        } catch (OpenemsError.OpenemsNamedException e) {
            throw new ConfigurationException("Activate : " + super.id(), "ChannelAddresses are configured in a wrong Way");
        }
    }

    @Modified
    void modified(ComponentContext context, ConfigValveOneOutput config) throws ConfigurationException {
        configSuccess = false;
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        this.activationOrModifiedRoutine(config);
        this.configSuccess = true;
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
        this.managerValve.removeValve(super.id());
    }

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
            this.setGoalPowerLevel().setNextValue(Math.round(currentPowerLevel));
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
            this.setGoalPowerLevel().setNextValue(0);
            this.getTimeNeeded().setNextValue(100 * secondsPerPercentage);
            this.writeToOutputChannel(0);
            this.getIsBusy().setNextValue(true);
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
            this.log.error("Couldn't write into Channel! Might not exist: " + outputChannel);
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
            this.writeToOutputChannel(100);
            this.getIsBusy().setNextValue(true);
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

                Value<Double> goalPowerLevelValue = this.setGoalPowerLevel().getNextValue().isDefined() ? this.setGoalPowerLevel().getNextValue() : null;
                int scaleDownFactorGoalPowerLevel = this.getScaleFactor(this.setGoalPowerLevel());


                if (otherChannelValue != null && goalPowerLevelValue != null) {
                    if (this.containsOnlyNumbers(otherChannelValue.get().toString()) && this.containsOnlyNumbers(goalPowerLevelValue.get().toString())) {
                        this.validCheckOutputChannel = true;
                        double otherValue = Double.parseDouble(otherChannelValue.get().toString());
                        double goalValue = Double.parseDouble(goalPowerLevelValue.get().toString());
                        if (Math.abs(otherValue / scaleDownFactorOtherChannel - goalValue / scaleDownFactorGoalPowerLevel) > TOLERANCE) {
                            this.getPowerLevel().setNextValue(otherValue / scaleDownFactorOtherChannel);
                            this.setPowerLevelPercent().setNextWriteValueFromObject(goalValue / scaleDownFactorGoalPowerLevel);
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
            } else if (this.configSuccess && this.powerValueReachedBeforeCheck) {
                boolean check = this.checkOutputIsEqualToGoalPercent();
                if (check == false) {
                    //Set PowerLevel To "correct" Output -> if Output says 10% it is 10%; since it is not timebased in comparison to the ValveTwoRelay
                    try {
                        if (this.optionalCheckOutputChannel != null && this.validCheckOutputChannel) {
                            Channel<?> otherChannel = this.cpm.getChannel(this.optionalCheckOutputChannel);

                            Value<?> captureValueOfChannel = otherChannel.value().isDefined() ? otherChannel.value() : otherChannel.getNextValue().isDefined() ? otherChannel.getNextValue() : null;
                            if (captureValueOfChannel != null) {
                                Unit otherUnit = otherChannel.channelDoc().getUnit();
                                Unit powerLevelUnit = this.getPowerLevel().channelDoc().getUnit();
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
                                this.getPowerLevel().setNextValue(value / scaleDownFactor);
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
