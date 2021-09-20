package io.openems.edge.cooler.decentral;

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
import io.openems.edge.heater.api.Cooler;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.cooler.decentral.api.DecentralizedCooler;
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
 * The Decentralized Cooler. It provides an equivalent functionality as the decentralized Heater, but it is used for cooling
 * purposes. And therefore the "needMoreHeat" condition is swapped.
 * It gets an HydraulicComponent, and asks, after the "EnableSignal" was set, for a startSignal from an external source.
 * (Central Cooler).
 * If the Signal is received, it starts an HydraulicComponent and starts the Cooling process.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Cooler.Decentralized",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})
public class DecentralizedCoolerImpl extends AbstractOpenemsComponent implements OpenemsComponent, DecentralizedCooler, Heater, ExceptionalState, EventHandler {

    private final Logger log = LoggerFactory.getLogger(DecentralizedCoolerImpl.class);
    @Reference
    ComponentManager cpm;

    private static final String NEED_COOL_RESPONSE_IDENTIFIER = "DECENTRAL_COOLER_NEED_COOL_RESPONSE_IDENTIFIER";
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "DECENTRAL_COOLER_EXCEPTIONAL_STATE_IDENTIFIER";

    private TimerHandler timer;
    private ExceptionalStateHandler exceptionalStateHandler;

    private HydraulicComponent hydraulicComponent;
    private HydraulicController configuredHydraulicController;
    private ComponentType componentType;
    private ThermometerThreshold thresholdThermometer;
    private boolean useExceptionalState;
    private boolean wasNeedCoolEnableLastCycle;

    public DecentralizedCoolerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values(),
                DecentralizedCooler.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.enabled() == false) {
            return;
        }

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
        this.setTemperatureSetpoint(config.setPointTemperature());
        if (config.shouldCloseOnActivation()) {
            switch (this.componentType) {

                case COMPONENT:
                    this.hydraulicComponent.forceClose();
                    break;
                case CONTROLLER:
                    this.configuredHydraulicController.setEnableSignal(false);
                    break;
            }
        }

        this.getForceCoolChannel().setNextValue(config.forceCooling());
        this._setHeaterState(HeaterState.OFF);
        this.useExceptionalState = config.enableExceptionalStateHandling();
        this.initializeTimer(config);
        this.getNeedCoolChannel().setNextValue(false);
        this.getNeedMoreCoolChannel().setNextValue(false);
    }

    /**
     * The Logic of the Cooler.
     * --> Should heat? --> enable Signal of Cooler
     * --> Request NeedCool
     * --> Wait till response (OR ForceCool @Paul da bin ich mir nicht sicher...)
     * --> check if heat is ok --> else request more heat
     * --> check if valve or valveController
     * --> if valve-->open 100% if  heat ok
     * --> else request in valveController --> position by temperature value
     * --> if shouldn't heat --> call deactivateControlledComponents (requests to false, threshold release Id, etc)
     *
     * @param event The Event of OpenemsEdge.
     */
    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS) && super.isEnabled()) {
            this.checkMissingComponents();
            AtomicBoolean currentRunCoolerEnabled = new AtomicBoolean();
            AtomicBoolean exceptionalStateOverride = new AtomicBoolean(false);
            currentRunCoolerEnabled.set(checkIsCurrentRunCoolerEnabled());
            if (this.useExceptionalState) {
                boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
                if (exceptionalStateActive) {
                    this.reactToExceptionalState(this.getExceptionalStateValue(), currentRunCoolerEnabled);
                    this.reactToExceptionalState(this.getExceptionalStateValue(), exceptionalStateOverride);
                }
            }
            if (currentRunCoolerEnabled.get()) {
                try {
                    this.getNeedCoolChannel().setNextValue(true);
                    //Is Cooler allowed to Cool
                    boolean currentRunNeedCoolEnable = checkIsCurrentCoolNeedEnabled();
                    if (currentRunNeedCoolEnable || this.getIsForceCooling() || exceptionalStateOverride.get()) {
                        this.wasNeedCoolEnableLastCycle = true;
                        //activateThermometerThreshold and check if setPointTemperature can be met otherwise shut valve
                        // and ask for more heat
                        this.setThresholdAndControlValve();
                    } else {
                        this.wasNeedCoolEnableLastCycle = false;
                        this.setState(HeaterState.STARTING_UP_OR_PREHEAT);
                        this.closeComponentOrDisableComponentController();
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't enable/Disable the ValveController!");
                }
            } else {
                this.deactivateControlledComponents();
                this.timer.resetTimer(NEED_COOL_RESPONSE_IDENTIFIER);
            }
        }
    }


    private void reactToExceptionalState(int exceptionalStateValue, AtomicBoolean currentRunCoolerEnabled) {
        currentRunCoolerEnabled.set(exceptionalStateValue > 0);
    }

    private boolean checkTimeIsUpAndResetIfTrue(String identifier) {
        boolean timeIsUp = this.timer.checkTimeIsUp(identifier);
        if (timeIsUp) {
            this.timer.resetTimer(identifier);
        }
        return timeIsUp;
    }

    /**
     * If Controller is Enabled AND permission to heat is set.
     * Check if ThermometerThreshold is ok --> if yes activate Valve/ValveController --> Else Close Valve and say "I need more Cool".
     */
    private void setThresholdAndControlValve() throws OpenemsError.OpenemsNamedException {
        this.thresholdThermometer.setSetPointTemperatureAndActivate(this.getTemperatureSetpoint().get(), super.id());
        //Static Valve Controller Works on it's own with given Temperature
        if (this.componentType.equals(ComponentType.CONTROLLER)) {
            this.configuredHydraulicController.getEnableSignalChannel().setNextWriteValueFromObject(true);
            this.configuredHydraulicController.setControlType(ControlType.TEMPERATURE);
        }
        // Check if SetPointTemperature above Thermometer --> Either
        if (this.thresholdThermometer.thermometerBelowGivenTemperature(this.getSetPointTemperature())) {
            this.setState(HeaterState.RUNNING);
            this.getNeedMoreCoolChannel().setNextValue(false);
            if (this.componentType.equals(ComponentType.COMPONENT)) {
                this.hydraulicComponent.setPointPowerLevelChannel().setNextValue(HydraulicComponent.DEFAULT_MAX_POWER_VALUE);
            }
        } else {
            this.getNeedMoreCoolChannel().setNextValue(true);
            if (this.componentType.equals(ComponentType.CONTROLLER)) {
                this.closeComponentOrDisableComponentController();
            }
            this.setState(HeaterState.STARTING_UP_OR_PREHEAT);
        }
    }

    /**
     * This methods checks if the enabled Signal for need Cool was set OR if the Signal isn't Present -->
     * check if last Cycle was enabled and currentWaitCycles >= Max Wait. If in doubt --> COOL
     *
     * @return enabled;
     */
    private boolean checkIsCurrentCoolNeedEnabled() {
        boolean currentRunNeedCoolEnable =
                this.timer.checkTimeIsUp(NEED_COOL_RESPONSE_IDENTIFIER) == false
                        || wasNeedCoolEnableLastCycle;

        Optional<Boolean> needCoolEnableSignal = this.getNeedCoolEnableSignalChannel().getNextWriteValueAndReset();
        if (needCoolEnableSignal.isPresent()) {
            this.timer.resetTimer(NEED_COOL_RESPONSE_IDENTIFIER);
            currentRunNeedCoolEnable = needCoolEnableSignal.get();
        }
        this.getNeedCoolEnableSignalChannel().setNextValue(currentRunNeedCoolEnable);
        return currentRunNeedCoolEnable;
    }

    /**
     * This methods checks if the enabled Signal was set OR if the enableSignal isn't Present -->
     * check if last Cycle was enabled and currentWaitCycles > Max Wait.
     * if in doubt --> COOL!
     *
     * @return enabled;
     */
    private boolean checkIsCurrentRunCoolerEnabled() {
        return this.getEnableSignalChannel().getNextWriteValueAndReset().orElse(false);
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
                        componentFetchedByCpm = cpm.getComponent(this.hydraulicComponent.id());
                        if (componentFetchedByCpm instanceof HydraulicComponent) {
                            this.hydraulicComponent = (HydraulicComponent) componentFetchedByCpm;
                        }
                    }
                    break;
                case CONTROLLER:
                    if (this.configuredHydraulicController.isEnabled() == false) {
                        componentFetchedByCpm = cpm.getComponent(this.configuredHydraulicController.id());
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
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.setState(HeaterState.BLOCKED_OR_ERROR);
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
        this.getNeedCoolChannel().setNextValue(false);
        this.getNeedMoreCoolChannel().setNextValue(false);
        this.thresholdThermometer.releaseSetPointTemperatureId(super.id());
        try {
            this.closeComponentOrDisableComponentController();
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't disable ValveController!");
        }
        this.setState(HeaterState.OFF);
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
     * Init the Timer to the identifier
     *
     * @param config the config of this component
     * @throws OpenemsError.OpenemsNamedException if the timer couldn't be found
     * @throws ConfigurationException             if id is found but they're not instances of timer in {@link TimerHandler}
     */
    private void initializeTimer(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.timer = new TimerHandlerImpl(this.id(), this.cpm);
        this.timer.addOneIdentifier(NEED_COOL_RESPONSE_IDENTIFIER, config.timerNeedCoolResponse(), config.timeNeedCoolResponse());

        if (config.enableExceptionalStateHandling()) {
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
