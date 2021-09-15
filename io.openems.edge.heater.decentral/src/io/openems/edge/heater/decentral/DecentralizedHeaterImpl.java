package io.openems.edge.heater.decentral;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.hydrauliccomponent.api.ControlType;
import io.openems.edge.controller.hydrauliccomponent.api.HydraulicController;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;

import io.openems.edge.heater.api.ComponentType;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.decentral.api.DecentralizedHeater;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.thermometer.api.ThermometerThreshold;

import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
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


import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * Decentralized heater.
 * This heater gets heat from a heat network that is monitored by a heat network controller (HNC). A heat network
 * constantly looses energy when it is hot, so it is more energy efficient to let it cool down when it is not needed.
 * The job of the HNC is then to turn it on only when it is needed.
 * If this heater wants to heat (= get heat from the heat network), it tells that to the HNC via the ’NeedHeat’
 * channel. The HNC then prepares the heat network and writes in the ’NeedHeatEnableSignal’ channel of this heater
 * if the heat network is ready or not. If the HNC is not responding, this heater will wait until a timer runs out
 * and then open the valves to the heat network anyway.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Decentral",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS}
)
public class DecentralizedHeaterImpl extends AbstractOpenemsComponent implements OpenemsComponent, DecentralizedHeater, ExceptionalState, EventHandler {

    private final Logger log = LoggerFactory.getLogger(DecentralizedHeaterImpl.class);

    @Reference
    ComponentManager cpm;

    private static final String NEED_HEAT_RESPONSE_IDENTIFIER = "DECENTRAL_HEATER_NEED_HEAT_RESPONSE_IDENTIFIER";
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "DECENTRAL_HEATER_EXCEPTIONAL_STATE_IDENTIFIER";
    private TimerHandler timer;
    private ExceptionalStateHandler exceptionalStateHandler;
    private HydraulicComponent hydraulicComponent;
    private HydraulicController configuredHydraulicController;
    private ComponentType componentType;
    private ThermometerThreshold thresholdThermometer;
    private boolean wasNeedHeatEnableLastCycle;
    private boolean useExceptionalState;

    public DecentralizedHeaterImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values(),
                DecentralizedHeater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.enabled() == false) {
            return;
        }
        this.activationOrModifiedRoutine(config);
    }

    private void activationOrModifiedRoutine(Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        OpenemsComponent componentFetchedByComponentManager;
        this.componentType = config.componentOrController();
        componentFetchedByComponentManager = this.cpm.getComponent(config.componentOrControllerId());
        switch (this.componentType) {
            case COMPONENT:
                if (componentFetchedByComponentManager instanceof HydraulicComponent) {
                    this.hydraulicComponent = (HydraulicComponent) componentFetchedByComponentManager;
                } else {
                    throw new ConfigurationException("activate", "The Component with id: "
                            + config.componentOrControllerId() + " is not a HydraulicComponent");
                }
                break;
            case CONTROLLER:
                if (componentFetchedByComponentManager instanceof HydraulicController) {
                    this.configuredHydraulicController = (HydraulicController) componentFetchedByComponentManager;
                } else {
                    throw new ConfigurationException("activate", "The Component with id "
                            + config.componentOrControllerId() + "not an instance of ValveController");
                }
                break;
        }
        componentFetchedByComponentManager = this.cpm.getComponent(config.thresholdThermometerId());
        if (componentFetchedByComponentManager instanceof ThermometerThreshold) {
            this.thresholdThermometer = (ThermometerThreshold) componentFetchedByComponentManager;
            this.thresholdThermometer.setSetPointTemperature(config.setPointTemperature(), super.id());
        } else {
            throw new ConfigurationException("activate",
                    "Component with ID: " + config.thresholdThermometerId() + " not an instance of Threshold");
        }
        this._setTemperatureSetpoint(config.setPointTemperature());
        this.getTemperatureSetpointChannel().nextProcessImage();
        if (config.shouldCloseOnActivation()) {
            switch (this.componentType) {
                case COMPONENT:
                    this.hydraulicComponent.forceClose();
                    break;
                case CONTROLLER:
                    this.configuredHydraulicController.setEnableSignal(false);
                    break;
            }
            this.getForceHeatChannel().setNextValue(config.forceHeating());
            this._setHeaterState(HeaterState.OFF.getValue());
            this.useExceptionalState = config.useExceptionalState();
            this.initializeTimer(config);
            this.getNeedHeatChannel().setNextValue(false);
            this.getNeedMoreHeatChannel().setNextValue(false);

        }
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        if (config.enabled() == false) {
            return;
        }
        this.activationOrModifiedRoutine(config);

    }

    /**
     * The Logic of the Heater.
     * - Evaluate enable signal and exceptional state, decide if heater should be heating or not.
     * - If heater should heat: write ’true’ in channel ’NeedHeat’ to request heat network to warm up.
     * - Skip this if ’force heating’ is on or heater is active because of exceptional state. Otherwise:
     * React to signal in channel ’NeedHeatEnableSignal’ (= answer from heat network controller). Wait if ’false’,
     * continue when ’true’. If no value in the channel (= connection to heat network controller lost), wait until
     * timer runs out. Then continue.
     * To be clear: When the heater is turned on by exceptional state, it will not wait for the heat network to be
     * ready!
     * - Start heating by calling ’setThresholdAndControlValve()’ method.
     * <p>
     * Heating is stopped when:
     * - Heat network controller writes ’false’ in channel ’NeedHeatEnableSignal’.
     * - Enable signal ’false’
     * - If exceptional state -> Exceptional state value <= 0, or end of exceptional state.
     * When heating is stopped, method ’deactivateControlledComponents()’ is called.
     *
     * @param event The Event of OpenemsEdge.
     */
    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS) && super.isEnabled()) {
            this.checkMissingComponents();
            AtomicBoolean currentRunHeaterEnabled = new AtomicBoolean();
            AtomicBoolean exceptionalStateOverride = new AtomicBoolean(false);
            currentRunHeaterEnabled.set(this.getEnableSignalChannel().getNextWriteValueAndReset().orElse(false));
            if (this.useExceptionalState) {
                if (this.exceptionalStateHandler.exceptionalStateActive(this)) {
                    this.reactToExceptionalState(this.getExceptionalStateValue(), currentRunHeaterEnabled);
                    this.reactToExceptionalState(this.getExceptionalStateValue(), exceptionalStateOverride);
                }
            }
            if (currentRunHeaterEnabled.get()) {
                try {
                    this.getNeedHeatChannel().setNextValue(true);
                    boolean currentRunNeedHeatEnable = this.checkIsCurrentHeatNeedEnabled();
                    if (currentRunNeedHeatEnable || this.getIsForceHeating() || exceptionalStateOverride.get()) {
                        this.wasNeedHeatEnableLastCycle = true;
                        this.setThresholdAndControlValve();
                    } else {
                        this.wasNeedHeatEnableLastCycle = false;
                        this._setHeaterState(HeaterState.STARTING_UP_OR_PREHEAT);
                        this.closeComponentOrDisableComponentController();
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't enable/Disable the ValveController!");
                }
            } else {
                this.deactivateControlledComponents();
                this.timer.resetTimer(NEED_HEAT_RESPONSE_IDENTIFIER);
            }
        }
    }


    private void reactToExceptionalState(int exceptionalStateValue, AtomicBoolean currentRunHeaterEnabled) {
        currentRunHeaterEnabled.set(exceptionalStateValue > ExceptionalState.DEFAULT_MIN_EXCEPTIONAL_VALUE);
    }

    /**
     * If Controller is Enabled AND permission to heat is set.
     * Check if ThermometerThreshold is ok --> if yes activate Valve/ValveController --> Else Close Valve and say "I need more Heat".
     */
    private void setThresholdAndControlValve() throws OpenemsError.OpenemsNamedException {
        int setPointTemperature = this.getTemperatureSetpoint().orElse(DEFAULT_SETPOINT_TEMPERATURE);
        this.thresholdThermometer.setSetPointTemperatureAndActivate(setPointTemperature, super.id());
        //Static Valve Controller Works on it's own with given Temperature
        if (this.componentType.equals(ComponentType.CONTROLLER)) {
            this.configuredHydraulicController.getEnableSignalChannel().setNextWriteValueFromObject(true);
            this.configuredHydraulicController.setControlType(ControlType.TEMPERATURE);
        }
        // Check if SetPointTemperature above Thermometer --> Either
        if (this.thresholdThermometer.thermometerAboveGivenTemperature(setPointTemperature)) {
            this._setHeaterState(HeaterState.RUNNING);
            this.getNeedMoreHeatChannel().setNextValue(false);
            if (this.componentType.equals(ComponentType.COMPONENT)) {
                this.hydraulicComponent.setPointPowerLevelChannel().setNextValue(HydraulicComponent.DEFAULT_MAX_POWER_VALUE);
            }
        } else {
            this.getNeedMoreHeatChannel().setNextValue(true);
            if (this.componentType.equals(ComponentType.COMPONENT)) {
                this.closeComponentOrDisableComponentController();
            }
            this._setHeaterState(HeaterState.STARTING_UP_OR_PREHEAT.getValue());
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
    private boolean checkIsCurrentHeatNeedEnabled() {
        boolean currentRunNeedHeatEnable =
                this.timer.checkTimeIsUp(NEED_HEAT_RESPONSE_IDENTIFIER) == false
                        || this.wasNeedHeatEnableLastCycle;

        Optional<Boolean> needHeatEnableSignal = this.getNeedHeatEnableSignalChannel().getNextWriteValueAndReset();
        if (needHeatEnableSignal.isPresent()) {
            this.timer.resetTimer(NEED_HEAT_RESPONSE_IDENTIFIER);
            currentRunNeedHeatEnable = needHeatEnableSignal.get();
        }
        this.getNeedHeatEnableSignalChannel().setNextValue(currentRunNeedHeatEnable);
        return currentRunNeedHeatEnable;
    }

    /**
     * Check if any component isn't enabled anymore and references needs to be set again.
     */
    private void checkMissingComponents() {
        OpenemsComponent componentFetchedByCpm;
        try {
            switch (this.componentType) {

                case COMPONENT:
                    if (this.hydraulicComponent.isEnabled() == false) {
                        componentFetchedByCpm = this.cpm.getComponent(this.hydraulicComponent.id());
                        if (componentFetchedByCpm instanceof HydraulicComponent) {
                            this.hydraulicComponent = (HydraulicComponent) componentFetchedByCpm;
                        }
                    }
                    break;
                case CONTROLLER:
                    if (this.configuredHydraulicController.isEnabled() == false) {
                        componentFetchedByCpm = this.cpm.getComponent(this.configuredHydraulicController.id());
                        if (componentFetchedByCpm instanceof HydraulicController) {
                            this.configuredHydraulicController = (HydraulicController) componentFetchedByCpm;
                        }
                    }
                    break;
            }
            if (this.thresholdThermometer.isEnabled() == false) {
                componentFetchedByCpm = this.cpm.getComponent(this.thresholdThermometer.id());
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
     * "deactivate" logic e.g. if heat is not needed anymore.
     * Channel Request -> false;
     * Release thresholdThermometer
     * if valve --> close (OR force close? @Pauli)
     * if ValveController --> force close or close?
     */
    void deactivateControlledComponents() {
        this.getEnableSignalChannel().getNextWriteValueAndReset();
        this.getEnableSignalChannel().setNextValue(false);
        this.getNeedHeatChannel().setNextValue(false);
        this.getNeedHeatEnableSignalChannel().getNextWriteValueAndReset();
        this.getNeedHeatEnableSignalChannel().setNextValue(false);
        this.getNeedMoreHeatChannel().setNextValue(false);
        this.getNeedMoreHeatEnableSignalChannel().getNextWriteValueAndReset();
        this.getNeedMoreHeatEnableSignalChannel().setNextValue(false);
        this._setHeaterState(HeaterState.OFF);
        this.thresholdThermometer.releaseSetPointTemperatureId(super.id());
        try {
            this.closeComponentOrDisableComponentController();
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't disable HydraulicController!");
        }
        this._setHeaterState(HeaterState.OFF);
    }

    /**
     * When Called close the Valve (if configured) or otherwise disable the ValveController.
     */
    private void closeComponentOrDisableComponentController() throws OpenemsError.OpenemsNamedException {
        switch (this.componentType) {
            case COMPONENT:
                this.hydraulicComponent.setPointPowerLevelChannel().setNextValue(HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
                break;
            case CONTROLLER:
                this.configuredHydraulicController.getEnableSignalChannel().setNextWriteValueFromObject(false);
                break;
        }
    }


    /**
     * Init the Timer to the identifier.
     *
     * @param config the config of this component
     * @throws OpenemsError.OpenemsNamedException if the timer couldn't be found
     * @throws ConfigurationException             if id is found but they're not instances of timer in {@link TimerHandler}
     */
    private void initializeTimer(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.timer = new TimerHandlerImpl(this.id(), this.cpm);
        this.timer.addOneIdentifier(NEED_HEAT_RESPONSE_IDENTIFIER, config.timerNeedHeatResponse(), config.waitTimeNeedHeatResponse());
        if (config.useExceptionalState()) {
            this.timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, config.timerExceptionalState(), config.timeToWaitExceptionalState());
            this.getExceptionalStateValueChannel().setNextValue(ExceptionalState.DEFAULT_MAX_EXCEPTIONAL_VALUE);
            this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(this.timer, EXCEPTIONAL_STATE_IDENTIFIER);
        }
    }

    @Deactivate
    protected void deactivate() {
        if (this.timer != null) {
            this.timer.removeComponent();
        }
        super.deactivate();
    }

}
