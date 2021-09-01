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
import io.openems.edge.heater.Cooler;
import io.openems.edge.cooler.decentral.api.DecentralizedCooler;
import io.openems.edge.heater.HeaterState;
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
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS}
)

public class DecentralizedCoolerImpl extends AbstractOpenemsComponent implements OpenemsComponent, DecentralizedCooler,ExceptionalState, EventHandler {

    private static final int DEFAULT_MAXIMUM_VALVE = 100;
    private final Logger log = LoggerFactory.getLogger(DecentralizedCoolerImpl.class);
    @Reference
    ComponentManager cpm;

    private final static String NEED_COOL_RESPONSE_IDENTIFIER = "DECENTRAL_COOLER_NEED_COOL_RESPONSE_IDENTIFIER";
    private final static String EXCEPTIONAL_STATE_IDENTIFIER = "DECENTRAL_COOLER_EXCEPTIONAL_STATE_IDENTIFIER";

    private TimerHandler timer;
    private ExceptionalStateHandler exceptionalStateHandler;

    private HydraulicComponent hydraulicComponent;
    private HydraulicController configuredHydraulicController;
    private boolean isComponent;
    private ThermometerThreshold thresholdThermometer;
    private boolean wasNeedCoolEnableLastCycle;
    private boolean useExceptionalState;
    private boolean exceptionalStatePresentBefore = false;

    public DecentralizedCoolerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Cooler.ChannelId.values(),
                DecentralizedCooler.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.enabled() == false) {
            return;
        }

        OpenemsComponent componentFetchedByComponentManager;

        this.isComponent = config.componentOrController().equals("Component");
        componentFetchedByComponentManager = this.cpm.getComponent(config.componentOrControllerId());
        if (isComponent) {
            if (componentFetchedByComponentManager instanceof HydraulicComponent) {
                this.hydraulicComponent = (HydraulicComponent) componentFetchedByComponentManager;
            } else {
                throw new ConfigurationException("activate", "The Component with id: "
                        + config.componentOrControllerId() + " is not a HydraulicComponent");
            }
        } else if (componentFetchedByComponentManager instanceof HydraulicController) {
            this.configuredHydraulicController = (HydraulicController) componentFetchedByComponentManager;
        } else {
            throw new ConfigurationException("activate", "The Component with id "
                    + config.componentOrControllerId() + "not an instance of ValveController");
        }

        componentFetchedByComponentManager = cpm.getComponent(config.thresholdThermometerId());
        if (componentFetchedByComponentManager instanceof ThermometerThreshold) {
            this.thresholdThermometer = (ThermometerThreshold) componentFetchedByComponentManager;
            this.thresholdThermometer.setSetPointTemperature(config.setPointTemperature(), super.id());
        } else {
            throw new ConfigurationException("activate",
                    "Component with ID: " + config.thresholdThermometerId() + " not an instance of Threshold");
        }
        this.setSetPointTemperature(config.setPointTemperature());
        if (config.shouldCloseOnActivation()) {
            if (isComponent) {
                this.hydraulicComponent.forceClose();
            } else {
                this.configuredHydraulicController.setEnableSignal(false);
            }
        }
        this.getForceCoolChannel().setNextValue(config.forceCooling());
        this.setState(HeaterState.OFFLINE.name());
        this.useExceptionalState = config.enableExceptionalStateHandling();
        this.initializeTimer(config);
        this.getNeedCoolChannel().setNextValue(false);
        this.getNeedMoreCoolChannel().setNextValue(false);

    }


    @Override
    public void setOffline() throws OpenemsError.OpenemsNamedException {
        this.getEnableSignalChannel().setNextWriteValue(false);
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
            if (this.errorInCooler()) {
                //TODO DO SOMETHING (?)
            }
            checkMissingComponents();
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
                        this.setState(HeaterState.AWAIT.name());
                        this.closeComponentOrDisableComponentController();
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't enable/Disable the ValveController!");
                }
            } else {
                deactivateControlledComponents();
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
        this.thresholdThermometer.setSetPointTemperatureAndActivate(this.getSetPointTemperature(), super.id());
        //Static Valve Controller Works on it's own with given Temperature
        if (this.isComponent == false) {
            this.configuredHydraulicController.getEnableSignalChannel().setNextWriteValueFromObject(true);
            this.configuredHydraulicController.setControlType(ControlType.TEMPERATURE);
        }
        // Check if SetPointTemperature above Thermometer --> Either
        if (this.thresholdThermometer.thermometerAboveGivenTemperature(this.getSetPointTemperature())) {
            this.setState(HeaterState.RUNNING.name());
            this.getNeedMoreCoolChannel().setNextValue(false);
            if (this.isComponent) {
                this.hydraulicComponent.setPointPowerLevelChannel().setNextValue(100);
            }
        } else {
            this.getNeedMoreCoolChannel().setNextValue(true);
            if (this.isComponent) {
                this.closeComponentOrDisableComponentController();
            }
            this.setState(HeaterState.PRE_COOL.name());
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
            if (this.isComponent) {
                if (this.hydraulicComponent.isEnabled() == false) {
                    componentFetchedByCpm = cpm.getComponent(this.hydraulicComponent.id());
                    if (componentFetchedByCpm instanceof HydraulicComponent) {
                        this.hydraulicComponent = (HydraulicComponent) componentFetchedByCpm;
                    }
                }
            } else {
                if (this.configuredHydraulicController.isEnabled() == false) {
                    componentFetchedByCpm = cpm.getComponent(this.configuredHydraulicController.id());
                    if (componentFetchedByCpm instanceof HydraulicController) {
                        this.configuredHydraulicController = (HydraulicController) componentFetchedByCpm;
                    }
                }
            }
            if (this.thresholdThermometer.isEnabled() == false) {
                componentFetchedByCpm = cpm.getComponent(this.thresholdThermometer.id());
                if (componentFetchedByCpm instanceof ThermometerThreshold) {
                    this.thresholdThermometer = (ThermometerThreshold) componentFetchedByCpm;
                }
            }
        } catch (OpenemsError.OpenemsNamedException ignored) {
            //TODO (?) Was soll passieren wenn Komponente nicht gefunden werden kann/falsche instanceof nur heaterstate --> error?
            this.setState(HeaterState.ERROR.name());
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
        this.setState(HeaterState.OFFLINE.name());
    }

    /**
     * When Called close the Valve (if configured) or otherwise disable the ValveController.
     */
    private void closeComponentOrDisableComponentController() throws OpenemsError.OpenemsNamedException {
        if (this.isComponent) {
            this.hydraulicComponent.setPointPowerLevelChannel().setNextValue(0);
        } else {
            this.configuredHydraulicController.getEnableSignalChannel().setNextWriteValueFromObject(false);
        }
    }


    //_---------------------------------TODOS---------------------------------//


    //TODO IDK What to do here --> Override methods of Cooler
    @Override
    public boolean hasError() {
        return this.errorInCooler();
    }

    @Override
    public void requestMaximumPower() {

    }

    @Override
    public void setIdle() {
    }

    @Override
    public boolean setPointPowerPercentAvailable() {
        return false;
    }

    @Override
    public boolean setPointPowerAvailable() {
        return false;
    }

    @Override
    public boolean setPointTemperatureAvailable() {
        return true;
    }

    @Override
    public int calculateProvidedPower(int demand, float bufferValue) throws OpenemsError.OpenemsNamedException {
        //TODO (?)
        return 0;
    }

    @Override
    public int getMaximumThermalOutput() {
        //TODO
        return 0;
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
            this.getExceptionalStateValueChannel().setNextValue(100);
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
