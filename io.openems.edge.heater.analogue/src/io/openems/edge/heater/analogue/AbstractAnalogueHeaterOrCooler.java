package io.openems.edge.heater.analogue;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.configupdate.ConfigurationUpdate;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.heater.analogue.component.AnalogueHeaterOrCoolerAio;
import io.openems.edge.heater.analogue.component.AnalogueHeaterOrCoolerComponent;
import io.openems.edge.heater.analogue.component.AnalogueHeaterOrCoolerLucidControl;
import io.openems.edge.heater.analogue.component.AnalogueHeaterOrCoolerPwm;
import io.openems.edge.heater.analogue.component.AnalogueHeaterOrCoolerRelay;
import io.openems.edge.heater.analogue.component.ControlType;
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class AbstractAnalogueHeaterOrCooler extends AbstractOpenemsComponent implements OpenemsComponent, Heater {

    ConfigurationAdmin ca;

    ComponentManager cpm;


    protected final Logger log = LoggerFactory.getLogger(AnalogueHeater.class);

    private TimerHandler timer;
    private static final String ENABLE_IDENTIFIER = "ENABLE_SIGNAL_IDENTIFIER";
    private static final String POWER_IDENTIFIER = "POWER_IDENTIFIER";
    private int overwritePower;
    private int maxPowerKw;
    protected boolean configurationSuccess = false;
    private int defaultRunPower;
    private boolean useError;
    private boolean hasError;
    private final List<ChannelAddress> errorList = new ArrayList<>();
    private EnableSignalHandler enableSignalHandler;
    private int minPower;

    private ControlType type = ControlType.PERCENT;


    private final List<AnalogueHeaterOrCoolerComponent> heaterComponent = new ArrayList<>();

    protected AbstractAnalogueHeaterOrCooler(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                                             io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    protected void activate(ComponentContext context, String id, String alias, boolean enabled,
                            boolean useError, String[] errorChannelAddressesList,
                            String[] analogueIds, AnalogueType analogueType, ControlType controlType,
                            int defaultMinPower, String timerId, int maxTimeEnableSignal,
                            int maxTimePowerSignal, int defaultRunPower, int maxPower, ComponentManager cpm, ConfigurationAdmin ca) {
        super.activate(context, id, alias, enabled);
        this.cpm = cpm;
        this.ca = ca;
        try {
            this.activationOrModifiedRoutine(analogueIds, analogueType, controlType, defaultMinPower, timerId, maxTimeEnableSignal,
                    maxTimePowerSignal, defaultRunPower, maxPower, useError, errorChannelAddressesList);
            this._setHeaterState(HeaterState.STANDBY);
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Configuration Apply failed, try again later!");
        }
    }

    void modified(ComponentContext context, String id, String alias, boolean enabled,
                  boolean useError, String[] errorChannelAddressesList,
                  String[] analogueIds, AnalogueType analogueType, ControlType controlType,
                  int defaultMinPower, String timerId, int maxTimeEnableSignal,
                  int maxTimePowerSignal, int defaultRunPower, int maxPower, ComponentManager cpm, ConfigurationAdmin ca) {
        super.modified(context, id, alias, enabled);
        this.cpm = cpm;
        this.ca = ca;
        this.configurationSuccess = false;
        this.heaterComponent.clear();
        try {
            this.activationOrModifiedRoutine(analogueIds, analogueType, controlType, defaultMinPower, timerId, maxTimeEnableSignal,
                    maxTimePowerSignal, defaultRunPower, maxPower, useError, errorChannelAddressesList);
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Configuration Apply failed, try again later!");
        }

    }

    /**
     * This method is an internal method, that will run all basic operations/setups either if the component was activated or modified.
     *
     * @param analogueIds               the analogue Devices applied to a AnalogueHeaterOrCoolingComponent
     * @param analogueType              the analogueType for this Heater
     * @param controlType               the ControlType
     * @param defaultMinPower           the minimum Power that will be applied, when no EnableSignal is present.
     * @param timerId                   the Timer that the Heater will use.
     * @param maxTimeEnableSignal       the maximum Time an EnableSignal is allowed to be absent after EnableSignal was set to true.
     * @param maxTimePowerSignal        the maximum Time a new SetPoint for a PowerValue is allowed to be absent.
     * @param defaultRunPower           the default RunPower, when EnableSignal is true and no SetPoint was set.
     * @param maxPower                  the maximum Power available for this heater.
     * @param useError                  use Error input to determine if everything is ok with the analogue heater
     * @param errorChannelAddressesList the list of errorChannel. If either is true -> error -> Heater/cooler cannot work
     * @throws OpenemsError.OpenemsNamedException if a component couldn't be found
     * @throws ConfigurationException             if a component could be found but was not the correct instance of XYZ.
     */
    protected void activationOrModifiedRoutine(String[] analogueIds, AnalogueType analogueType, ControlType controlType,
                                               int defaultMinPower, String timerId, int maxTimeEnableSignal,
                                               int maxTimePowerSignal, int defaultRunPower, int maxPower, boolean useError, String[] errorChannelAddressesList) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.heaterComponent.clear();
        this.errorList.clear();
        Arrays.stream(analogueIds).forEach(id -> {
            try {
                switch (analogueType) {
                    case PWM:
                        this.heaterComponent.add(new AnalogueHeaterOrCoolerPwm(this.cpm, id, this.maxPowerKw, controlType, defaultMinPower));
                        break;
                    case AIO:
                        this.heaterComponent.add(new AnalogueHeaterOrCoolerAio(this.cpm, id, this.maxPowerKw, controlType, defaultMinPower));
                        break;
                    case RELAY:
                        this.heaterComponent.add(new AnalogueHeaterOrCoolerRelay(this.cpm, id, this.maxPowerKw, controlType, defaultMinPower));
                        break;
                    case LUCID_CONTROL:
                        this.heaterComponent.add(new AnalogueHeaterOrCoolerLucidControl(this.cpm, id, this.maxPowerKw, controlType, defaultMinPower));
                        break;
                }
            } catch (Exception ignored) {
                this.log.error("Unable to get Components. Check Config!");
            }
        });
        this.useError = useError;
        if (this.useError) {
            Arrays.stream(errorChannelAddressesList).forEach(errorChannelAddress -> {
                try {
                    ChannelAddress address = ChannelAddress.fromString(errorChannelAddress);
                    if (!this.errorList.contains(address)) {
                        this.errorList.add(address);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn(this.id() + ": ChannelAddress is wrong! " + errorChannelAddress + " is not a valid");
                }
            });
        }

        if (this.timer != null) {
            this.timer.removeComponent();
        }
        this.timer = new TimerHandlerImpl(this.id(), this.cpm);
        this.timer.addOneIdentifier(ENABLE_IDENTIFIER, timerId, maxTimeEnableSignal);
        this.timer.addOneIdentifier(POWER_IDENTIFIER, timerId, maxTimePowerSignal);
        this.enableSignalHandler = new EnableSignalHandlerImpl(this.timer, ENABLE_IDENTIFIER);
        this.overwritePower = defaultRunPower;
        this.getDefaultActivePowerChannel().setNextValue(defaultRunPower);
        this.getDefaultMinPowerChannel().setNextValue(defaultMinPower);
        this.maxPowerKw = maxPower;
        this.type = controlType;
        this.defaultRunPower = defaultRunPower;
        this.minPower = defaultMinPower;
        this.configurationSuccess = true;
    }

    protected void handleEvent(Event event) {
        if (this.isEnabled() && this.configurationSuccess) {
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
                //Check for New Default Values
                this.updateConfig();
            } else if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
                this.hasErrorCheck();
                if (this.enableSignalHandler.deviceShouldBeHeating(this) && !this.hasError) {
                    this._setHeaterState(HeaterState.RUNNING);
                    this.heaterComponent.forEach(component -> {
                        try {
                            component.startProcess(this.powerToApply());
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.error("Unable to start heating!");
                        }
                    });
                } else {
                    if (this.hasError == false) {
                        this._setHeaterState(HeaterState.STANDBY);
                    }
                    this.getEnableSignalChannel().getNextWriteValueAndReset();
                    this.getEnableSignalChannel().setNextValue(false);
                    this.heaterComponent.forEach(component -> {
                        try {
                            component.stopProcess();
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.error("Unable to stop heating!");
                        }
                    });
                }
                this.heaterComponent.stream().findAny().ifPresent(entry -> {
                    try {
                        this.updatePowerChannel(entry.getCurrentPowerApplied());
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Couldn't apply heating / stop heating! Reason: " + e.getMessage());
                    }
                });
            }
        }
    }

    /**
     * Checks if the Heater/Cooler has an Error.
     * Iterate through errorList (if configured) and set the {@link #hasError} attribute.
     */
    private void hasErrorCheck() {
        if (this.useError) {
            AtomicBoolean errorFound = new AtomicBoolean(false);
            this.errorList.forEach(entry -> {
                if (errorFound.get() == false) {
                    try {
                        Channel<?> channel = this.cpm.getChannel(entry);
                        Boolean errorOccurred = TypeUtils.getAsType(OpenemsType.BOOLEAN, channel.value());
                        errorFound.set(errorOccurred != null && errorOccurred);
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.info(this.id() + ": Channel: " + entry + " Not Available!");
                    }
                }
            });
            this.hasError = errorFound.get();
            if (this.hasError) {
                this._setHeaterState(HeaterState.BLOCKED_OR_ERROR);
            }
        } else {
            this.hasError = false;
        }
    }


    /**
     * Writes the CurrentPower Applied to the {@link Heater.ChannelId#EFFECTIVE_HEATING_POWER_PERCENT}
     * and {@link Heater.ChannelId#EFFECTIVE_HEATING_POWER}
     * channel. It asks/reads the value from it's {@link #heaterComponent}.
     *
     * @param currentPowerApplied the currentPower Applied in Percent
     */
    private void updatePowerChannel(int currentPowerApplied) {
        this._setEffectiveHeatingPowerPercent(currentPowerApplied);
        double currentlyAppliedPowerInKw = (this.maxPowerKw * currentPowerApplied) / 100.0;
        this._setEffectiveHeatingPower(currentlyAppliedPowerInKw);
    }

    /**
     * Gets the correct channel and value, defined by this {@link ControlType}.
     * It Either gets a default RunPower or the setPoint, depending if the PowerValue was written into the SetPointChannel.
     * ({@link #getHeatingPowerSetpointChannel()} for ControlType KW or {@link #getHeatingPowerPercentSetpointChannel()} for ControlType Percent)
     *
     * @return the power that will be applied.
     */
    private int powerToApply() {
        WriteChannel<?> channelToGetPowerValueFrom = this.getHeatingPowerPercentSetpointChannel();
        AtomicBoolean needToCheckTime = new AtomicBoolean(true);
        switch (this.type) {
            case PERCENT:
                channelToGetPowerValueFrom = this.getHeatingPowerPercentSetpointChannel();
                break;
            case KW:
                channelToGetPowerValueFrom = this.getHeatingPowerSetpointChannel();
                break;
        }
        channelToGetPowerValueFrom.getNextWriteValueAndReset().ifPresent(value -> {
            this.overwritePower = (Integer) value;
            this.timer.resetTimer(POWER_IDENTIFIER);
            needToCheckTime.set(false);
        });

        if (needToCheckTime.get() && !this.timer.checkTimeIsUp(POWER_IDENTIFIER) == false) {
            this.overwritePower = this.defaultRunPower;
        }

        return this.overwritePower;
    }

    /**
     * Updates the Config if either: {@link Heater.ChannelId#SET_DEFAULT_ACTIVE_POWER_VALUE} or {@link Heater.ChannelId#SET_DEFAULT_MINIMUM_POWER_VALUE}
     * is updated.
     */
    private void updateConfig() {
        Map<String, Object> propertyMap = new HashMap<>();

        this.getDefaultActivePowerChannel().getNextWriteValueAndReset().ifPresent(entry -> {
            if (entry != this.defaultRunPower) {
                propertyMap.put("defaultRunPower", entry);
            }
        });
        this.getDefaultMinPowerChannel().getNextWriteValueAndReset().ifPresent(entry -> {
            if (entry != this.minPower) {
                propertyMap.put("inactiveValue", entry);
            }
        });
        if (propertyMap.size() > 0) {
            try {
                ConfigurationUpdate.updateConfig(this.ca, this.servicePid(), propertyMap);
            } catch (IOException e) {
                this.log.info(this.id() + ": Couldn't update Config.");
            }
        }
    }

    @Override
    public String debugLog() {
        String debugString = "EnableSignal: " + this.getEnableSignal() + "\n"
                + "CurrentPowerLevel: " + this.getEffectiveHeatingPower() + "\n"
                + "CurrentPowerLevelPercent: " + this.getEffectiveHeatingPowerPercent() + "\n";
        if (this.type.equals(ControlType.KW)) {
            debugString += "SetPoint Kw: ";
        } else {
            debugString += "SetPoint Percent: ";
        }
        debugString += this.overwritePower + "\n";

        return debugString;
    }
}
