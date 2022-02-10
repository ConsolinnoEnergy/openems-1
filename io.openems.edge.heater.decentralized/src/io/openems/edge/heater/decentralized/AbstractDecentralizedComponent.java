package io.openems.edge.heater.decentralized;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.hydrauliccomponent.api.ControlType;
import io.openems.edge.controller.hydrauliccomponent.api.HydraulicController;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.ComponentType;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.thermometer.api.ThermometerThreshold;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Abstract Decentralized Component.
 * This Class provides the basic Logic for e.g. a Decentralized Cooler or Heater to run.
 * It checks if the current Run is enabled, if the Center of the Heat or CoolNetwork allows this component to Heat/Cool.
 * And reacts to ExceptionalStates.
 */
public abstract class AbstractDecentralizedComponent extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler, Heater, ExceptionalState {

    protected final Logger log = LoggerFactory.getLogger(AbstractDecentralizedComponent.class);

    protected ComponentManager cpm;

    private static final String NEED_HEAT_RESPONSE_IDENTIFIER = "DECENTRALIZED_COMPONENT_COOL_OR_HEAT_RESPONSE_IDENTIFIER";
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "DECENTRALIZED_HEATER_EXCEPTIONAL_STATE_IDENTIFIER";
    protected TimerHandler timer;
    private ExceptionalStateHandler exceptionalStateHandler;
    private HydraulicComponent hydraulicComponent;
    private HydraulicController configuredHydraulicController;
    private ComponentType componentType;
    protected ThermometerThreshold thresholdThermometer;
    private boolean wasNeedHeatEnableLastCycle;
    private boolean useExceptionalState;
    WriteChannel<Boolean> response;
    WriteChannel<Boolean> forced;
    protected boolean configurationSuccess;


    public AbstractDecentralizedComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                                          io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    void activate(ComponentContext context, String id, String alias, boolean enabled, ComponentType componentType,
                  String componentId, String thresholdId, int tempSetPoint, boolean forceHeatingOrCooling,
                  boolean useExceptionalState, String timerIdResponse, int waitTimeResponse,
                  String timerIdExceptionalState, int timeToWaitExceptionalState,
                  WriteChannel<Boolean> forced, WriteChannel<Boolean> response, ComponentManager cpm) {
        super.activate(context, id, alias, enabled);
        this.cpm = cpm;
        if (enabled == false) {
            return;
        }
        try {
            this.activationOrModifiedRoutine(componentType, componentId, thresholdId, tempSetPoint,
                    forceHeatingOrCooling, useExceptionalState,
                    timerIdResponse, waitTimeResponse,
                    timerIdExceptionalState, timeToWaitExceptionalState, forced, response);
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Couldn't apply Config. Try again later");
            this.configurationSuccess = false;
        }
    }

    /**
     * This Method is called on activation or modification. It applies the basic configuration of the decentralized heater or cooler.
     *
     * @param componentType              the Component Type the DecentralizedComponent controls.
     * @param componentId                the ComponentId corresponding to the ComponentType.
     * @param thresholdId                the ThresholdThermometerId.
     * @param tempSetPoint               the SetPoint for the DecentralizedComponent. Reach this at least/most depending if it's a cooler or heater.
     * @param forceHeatingOrCooling      should this heater be allowed to use force heating.
     * @param useExceptionalState        should this heater use an exceptionalState.
     * @param timerIdResponse            Timer Id for the EnableSignalResponse
     * @param waitTimeResponse           waitTime for EnableSignalResponse. (How long to wait for Center to answer)
     * @param timerIdExceptionalState    Timer Id for the ExceptionalState
     * @param timeToWaitExceptionalState waitTime for ExceptionalState.
     * @param forced                     the Channel where the forcedConfiguration will be stored in.
     * @param response                   the Channel where the EnableSignal from the Center is expected.
     * @throws ConfigurationException             if something is wrong configured
     * @throws OpenemsError.OpenemsNamedException if Component cannot be found.
     */
    protected void activationOrModifiedRoutine(ComponentType componentType, String componentId, String thresholdId, int tempSetPoint,
                                               boolean forceHeatingOrCooling, boolean useExceptionalState,
                                               String timerIdResponse, int waitTimeResponse,
                                               String timerIdExceptionalState, int timeToWaitExceptionalState,
                                               WriteChannel<Boolean> forced, WriteChannel<Boolean> response)
            throws ConfigurationException, OpenemsError.OpenemsNamedException {
        OpenemsComponent componentFetchedByComponentManager;
        this.componentType = componentType;
        componentFetchedByComponentManager = this.cpm.getComponent(componentId);
        switch (this.componentType) {
            case COMPONENT:
                if (componentFetchedByComponentManager instanceof HydraulicComponent) {
                    this.hydraulicComponent = (HydraulicComponent) componentFetchedByComponentManager;
                } else {
                    throw new ConfigurationException("activate", "The Component with id: "
                            + componentId + " is not a HydraulicComponent");
                }
                break;
            case CONTROLLER:
                if (componentFetchedByComponentManager instanceof HydraulicController) {
                    this.configuredHydraulicController = (HydraulicController) componentFetchedByComponentManager;
                } else {
                    throw new ConfigurationException("activate", "The Component with id "
                            + componentId + "not an instance of ValveController");
                }
                break;
        }
        componentFetchedByComponentManager = this.cpm.getComponent(thresholdId);
        if (componentFetchedByComponentManager instanceof ThermometerThreshold) {
            this.thresholdThermometer = (ThermometerThreshold) componentFetchedByComponentManager;
            this.thresholdThermometer.setSetPointTemperature(tempSetPoint, super.id());
        } else {
            throw new ConfigurationException("activate",
                    "Component with ID: " + thresholdId + " not an instance of Threshold");
        }
        this._setTemperatureSetpoint(tempSetPoint);
        this.getTemperatureSetpointChannel().nextProcessImage();
        this.forced = forced;
        this.response = response;
        this.forced.setNextValue(forceHeatingOrCooling);
        this.forced.nextProcessImage();
        this._setHeaterState(HeaterState.OFF.getValue());
        this.useExceptionalState = useExceptionalState;
        this.initializeTimer(timerIdResponse, waitTimeResponse, timerIdExceptionalState, timeToWaitExceptionalState);
        this.configurationSuccess = true;
    }


    @Modified
    void modified(ComponentContext context, String id, String alias, boolean enabled, ComponentType componentType, String componentId, String thresholdId, int tempSetPoint,
                  boolean forceHeating, boolean useExceptionalState,
                  String timerIdResponse, int waitTimeResponse,
                  String timerIdExceptionalState, int timeToWaitExceptionalState,
                  WriteChannel<Boolean> forced, WriteChannel<Boolean> needResponse, ComponentManager cpm) {
        super.modified(context, id, alias, enabled);
        this.cpm = cpm;
        this.timer.removeComponent();
        try {
            this.activationOrModifiedRoutine(componentType, componentId, thresholdId, tempSetPoint,
                    forceHeating, useExceptionalState,
                    timerIdResponse, waitTimeResponse,
                    timerIdExceptionalState, timeToWaitExceptionalState,
                    forced, needResponse);
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Couldn't apply Config. Try again later");
            this.configurationSuccess = false;
        }
    }

    /**
     * Checks if the Heater is Enabled this Run (EnableSignal is set Or if exceptionalState should be used ->
     * check for the ExceptionalState).
     * <p>
     * If not -> deactivate the Controlled Components, Reset the Timer and write false to the EnableSignalChannel.
     * </p>
     *
     * @return true if the decentralizedComponentLogic should be executed.
     */
    protected boolean currentRunEnabled() {
        AtomicBoolean currentRunHeaterEnabled = new AtomicBoolean();
        currentRunHeaterEnabled.set(this.getEnableSignalChannel().getNextWriteValueAndReset().orElse(false));
        if (this.useExceptionalState) {
            if (this.exceptionalStateHandler.exceptionalStateActive(this)) {
                this.applyExceptionalState(this.getExceptionalStateValue(), currentRunHeaterEnabled);
            }
        }
        if (currentRunHeaterEnabled.get() == false) {
            this.deactivateControlledComponents();
            this.timer.resetTimer(NEED_HEAT_RESPONSE_IDENTIFIER);
            this.getEnableSignalChannel().getNextWriteValueAndReset();
            this.getEnableSignalChannel().setNextValue(false);
        }

        return currentRunHeaterEnabled.get();
    }

    /**
     * Applies the ExceptionalState if the ExceptionalState is active.
     *
     * @param exceptionalStateValue   the current exceptionalStateValue.
     * @param currentRunHeaterEnabled the AtomicBoolean, defining if the component is enabled this cycle.
     */

    private void applyExceptionalState(int exceptionalStateValue, AtomicBoolean currentRunHeaterEnabled) {
        currentRunHeaterEnabled.set(exceptionalStateValue > ExceptionalState.DEFAULT_MIN_EXCEPTIONAL_VALUE);
    }

    /**
     * If Controller is Enabled AND permission to heat/cool is set.
     * In Any case enable the Controller Component -> it should know what to do.
     * But the native Component does not -> set the hydraulicComponent to a minimum when setPointTemperature is not active.
     * Children should check if the thresholdTemp is true or false. OnFalse -> ask for More Heat/Cooling.
     *
     * @param thresholdTempMet    Children tell parent if the thresholdTempConditions were met.
     * @param setPointTemperature the SetPoint of this DecentralizedComponent.
     */

    protected void setThresholdAndControlValve(boolean thresholdTempMet, int setPointTemperature) {
        this.thresholdThermometer.setSetPointTemperatureAndActivate(setPointTemperature, super.id());
        if (this.componentType.equals(ComponentType.CONTROLLER)) {
            try {
                this.configuredHydraulicController.getEnableSignalChannel().setNextWriteValueFromObject(true);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't set EnableSignal for HydraulicController. Reason: " + e.getMessage());
            }
            this.configuredHydraulicController.setControlType(ControlType.TEMPERATURE);
        }
        if (thresholdTempMet) {
            if (this.componentType.equals(ComponentType.COMPONENT)) {
                this.hydraulicComponent.setPowerLevel(HydraulicComponent.DEFAULT_MAX_POWER_VALUE);
            }
        } else {
            if (this.componentType.equals(ComponentType.COMPONENT)) {
                this.closeComponentOrDisableComponentController();
            }
            this._setHeaterState(HeaterState.STARTING_UP_OR_PREHEAT);
        }
    }

    /**
     * Takes the ’nextWrite’ of 'NeedHeatEnableSignal' with get and reset, then returns it. If no value is present, the
     * following fallback rules apply:
     * - If return value was ’true’ last cycle, keep returning ’true’.
     * - If return value was ’false’ last cycle, keep returning ’false’ until timer is up. If timer is up, return ’true’.
     *
     * @return the ’nextWrite’ of 'NeedHeatEnableSignal' channel, or the fallback value;
     */
    protected boolean checkAllowedToExecuteLogic() {
        boolean currentRunNeedHeatEnable =
                this.timer.checkTimeIsUp(NEED_HEAT_RESPONSE_IDENTIFIER) || this.wasNeedHeatEnableLastCycle;

        Optional<Boolean> needHeatEnableSignal = this.response.getNextWriteValueAndReset();
        if (needHeatEnableSignal.isPresent()) {
            this.timer.resetTimer(NEED_HEAT_RESPONSE_IDENTIFIER);
            currentRunNeedHeatEnable = needHeatEnableSignal.get();
        }
        this.response.setNextValue(currentRunNeedHeatEnable);
        this.wasNeedHeatEnableLastCycle = currentRunNeedHeatEnable || this.forced.value().orElse(false);
        if (this.wasNeedHeatEnableLastCycle == false) {
            this._setHeaterState(HeaterState.STANDBY);
            this.closeComponentOrDisableComponentController();
        }
        return this.wasNeedHeatEnableLastCycle;
    }

    /**
     * Check if any component isn't enabled anymore and references needs to be set again.
     */
    protected void checkMissingComponents() {
        OpenemsComponent componentFetchedByCpm;
        try {
            switch (this.componentType) {

                case COMPONENT:
                    componentFetchedByCpm = this.cpm.getComponent(this.hydraulicComponent.id());

                    if (this.hydraulicComponent.equals(componentFetchedByCpm) == false) {
                        if (componentFetchedByCpm instanceof HydraulicComponent) {
                            this.hydraulicComponent = (HydraulicComponent) componentFetchedByCpm;
                        }
                    }
                    break;
                case CONTROLLER:
                    componentFetchedByCpm = this.cpm.getComponent(this.configuredHydraulicController.id());
                    if (this.configuredHydraulicController.equals(componentFetchedByCpm) == false) {
                        if (componentFetchedByCpm instanceof HydraulicController) {
                            this.configuredHydraulicController = (HydraulicController) componentFetchedByCpm;
                        }
                    }
                    break;
            }
            componentFetchedByCpm = this.cpm.getComponent(this.thresholdThermometer.id());
            if (this.thresholdThermometer.equals(componentFetchedByCpm) == false) {
                if (componentFetchedByCpm instanceof ThermometerThreshold) {
                    this.thresholdThermometer = (ThermometerThreshold) componentFetchedByCpm;
                }
            }
            this._setErrorMessage("No error");
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this._setErrorMessage("Could not find all components");
        }
    }

    /**
     * Deactivate Controlled Components if no EnableSignal/force open was set.
     * Channel Request -> false;
     * Release thresholdThermometer
     * and disable Components.
     */
    void deactivateControlledComponents() {
        this._setHeaterState(HeaterState.OFF);
        this.thresholdThermometer.releaseSetPointTemperatureId(super.id());
        this.closeComponentOrDisableComponentController();
        this._setHeaterState(HeaterState.OFF);
    }

    /**
     * When Called close the Valve (if configured) or otherwise disable the ValveController.
     */
    private void closeComponentOrDisableComponentController() {
        try {
            switch (this.componentType) {
                case COMPONENT:
                    this.hydraulicComponent.setPowerLevel(HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
                    break;
                case CONTROLLER:
                    this.configuredHydraulicController.getEnableSignalChannel().setNextWriteValueFromObject(false);
                    break;
            }
        } catch (OpenemsError.OpenemsNamedException | IllegalArgumentException e) {
            this.log.warn("Couldn't disable Components. Reason: " + e.getMessage());
        }
    }


    /**
     * Initialize the Timer to the identifier.
     *
     * @param timerIdResponse            Id of the Timer for the response channel.
     * @param waitTimeResponse           waitTime for the Response Channel if no Signal is incoming.
     * @param timerIdExceptionalState    Timer Id to use for the ExceptionalState (if enabled)
     * @param timeToWaitExceptionalState WaitTime for the Exceptional State to become invalid, if it was present before.
     * @throws OpenemsError.OpenemsNamedException if the timer couldn't be found
     * @throws ConfigurationException             if id is found but they're not instances of timer in {@link TimerHandler}
     */
    private void initializeTimer(String timerIdResponse, int waitTimeResponse, String timerIdExceptionalState, int timeToWaitExceptionalState) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.timer = new TimerHandlerImpl(this.id(), this.cpm);
        this.timer.addOneIdentifier(NEED_HEAT_RESPONSE_IDENTIFIER, timerIdResponse, waitTimeResponse);
        if (this.useExceptionalState) {
            this.timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerIdExceptionalState, timeToWaitExceptionalState);
            this.getExceptionalStateValueChannel().setNextValue(ExceptionalState.DEFAULT_MAX_EXCEPTIONAL_VALUE);
            this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(this.timer, EXCEPTIONAL_STATE_IDENTIFIER);
        }
    }

    protected void deactivate() {
        if (this.timer != null) {
            this.timer.removeComponent();
        }
        super.deactivate();
    }

}
