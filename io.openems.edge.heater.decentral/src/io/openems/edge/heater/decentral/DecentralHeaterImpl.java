package io.openems.edge.heater.decentral;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.heatnetwork.valve.api.ControlType;
import io.openems.edge.controller.heatnetwork.valve.api.ValveController;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.heater.decentral.api.DecentralHeater;
import io.openems.edge.heatsystem.components.Valve;
import io.openems.edge.thermometer.api.ThermometerThreshold;
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


/**
 * Decentral heater.
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

public class DecentralHeaterImpl extends AbstractOpenemsComponent implements OpenemsComponent, DecentralHeater,
        ExceptionalState, EventHandler {

    private final Logger log = LoggerFactory.getLogger(DecentralHeaterImpl.class);
    @Reference
    ComponentManager cpm;

    private static final String NEED_HEAT_RESPONSE_IDENTIFIER = "DECENTRAL_HEATER_NEED_HEAT_RESPONSE_IDENTIFIER";
    private static final String ENABLE_SIGNAL_IDENTIFIER = "DECENTRAL_HEATER_ENABLE_SIGNAL_IDENTIFIER";
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "DECENTRAL_HEATER_EXCEPTIONAL_STATE_IDENTIFIER";

    private TimerHandler timer;
    private EnableSignalHandler enableSignalHandler;
    private ExceptionalStateHandler exceptionalStateHandler;

    private boolean componentEnabled;
    private Valve configuredValve;
    private ValveController configuredValveController;
    private boolean isValve;
    private ThermometerThreshold thresholdThermometer;
    private boolean wasNeedHeatEnableLastCycle;
    private boolean useExceptionalState;

    public DecentralHeaterImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values(),
                DecentralHeater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.enabled() == false) {
            return;
        }
        this.componentEnabled = config.enabled();

        OpenemsComponent componentFetchedByComponentManager;

        this.isValve = config.valveOrController().equals("Valve");
        componentFetchedByComponentManager = this.cpm.getComponent(config.valveOrControllerId());
        if (this.isValve) {
            if (componentFetchedByComponentManager instanceof Valve) {
                this.configuredValve = (Valve) componentFetchedByComponentManager;
            } else {
                throw new ConfigurationException("activate", "The Component with id: "
                        + config.valveOrControllerId() + " is not a Valve");
            }
        } else if (componentFetchedByComponentManager instanceof ValveController) {
            this.configuredValveController = (ValveController) componentFetchedByComponentManager;
        } else {
            throw new ConfigurationException("activate", "The Component with id "
                    + config.valveOrControllerId() + "not an instance of ValveController");
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
            if (this.isValve) {
                this.configuredValve.forceClose();
            } else {
                this.configuredValveController.setEnableSignal(false);
            }
        }
        this.getForceHeatChannel().setNextValue(config.forceHeating());
        this._setHeaterState(HeaterState.OFF.getValue());
        this.useExceptionalState = config.useExceptionalState();
        this.initializeTimer(config);
        this.getNeedHeatChannel().setNextValue(false);
        this.getNeedMoreHeatChannel().setNextValue(false);
        this._setErrorMessage("No error");
        this.getErrorMessageChannel().nextProcessImage();
    }

    /**
     * Initialize timers and handlers.
     *
     * @param config the config of this component
     * @throws OpenemsError.OpenemsNamedException if the timer couldn't be found
     * @throws ConfigurationException             if id is found but they're not instances of timer in {@link TimerHandler}
     */
    private void initializeTimer(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.timer = new TimerHandlerImpl(this.id(), this.cpm);
        String timerTypeHeatResponse;
        if (config.heatResponseTimerIsSecondsNotCycles()) {
            timerTypeHeatResponse = "TimerByTime";
        } else {
            timerTypeHeatResponse = "TimerByCycles";
        }
        this.timer.addOneIdentifier(NEED_HEAT_RESPONSE_IDENTIFIER, timerTypeHeatResponse, config.waitTimeNeedHeatResponse());

        String timerTypeEnableSignal;
        if (config.exceptionalStateTimerIsCyclesNotSeconds()) {
            timerTypeEnableSignal = "TimerByCycles";
        } else {
            timerTypeEnableSignal = "TimerByTime";
        }
        this.timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerTypeEnableSignal, config.waitTimeExceptionalState());
        this.enableSignalHandler = new EnableSignalHandlerImpl(this.timer, ENABLE_SIGNAL_IDENTIFIER);

        if (config.useExceptionalState()) {
            String timerTypeExceptionalState;
            if (config.exceptionalStateTimerIsCyclesNotSeconds()) {
                timerTypeExceptionalState = "TimerByCycles";
            } else {
                timerTypeExceptionalState = "TimerByTime";
            }
            this.timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerTypeExceptionalState, config.waitTimeExceptionalState());
            this.getExceptionalStateValueChannel().setNextValue(100);
            this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(this.timer, EXCEPTIONAL_STATE_IDENTIFIER);
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        this.deactivateControlledComponents();
    }

    /**
     * The Logic of the Heater.
     * - Evaluate enable signal and exceptional state, decide if heater should be heating or not.
     * - If heater should heat: write ’true’ in channel ’NeedHeat’ to request heat network to warm up.
     * - Skip this if ’force heating’ is on or heater is active because of exceptional state. Otherwise:
     *   React to signal in channel ’NeedHeatEnableSignal’ (= answer from heat network controller). Wait if ’false’,
     *   continue when ’true’. If no value in the channel (= connection to heat network controller lost), wait until
     *   timer runs out. Then continue.
     *   To be clear: When the heater is turned on by exceptional state, it will not wait for the heat network to be
     *   ready!
     * - Start heating by calling ’setThresholdAndControlValve()’ method.
     *
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
        if (this.componentEnabled && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            this.checkMissingComponents();
            if (this.getErrorMessage().get().equals("No error") == false) {
                //TODO DO SOMETHING (?)
            }
            if (this.getTemperatureSetpointChannel().getNextWriteValue().isPresent()) {
                int temperatureSetpointWrite = this.getTemperatureSetpointChannel().getNextWriteValue().get();
                this._setTemperatureSetpoint(temperatureSetpointWrite);
            }

            //First things first: Is Heater Enabled
            boolean currentRunHeaterEnabled = this.enableSignalHandler.deviceShouldBeHeating(this);
            boolean exceptionalStateOverride = false;
            if (this.useExceptionalState) {
                boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
                if (exceptionalStateActive) {
                    int exceptionalStateValue = this.getExceptionalStateValue();
                    currentRunHeaterEnabled = exceptionalStateValue > 0;
                    exceptionalStateOverride = exceptionalStateValue > 0;
                }
            }
            if (currentRunHeaterEnabled) {
                try {
                    this.getNeedHeatChannel().setNextValue(true);
                    // Check if heat network is ready. Ignore if ’forceHeating’ option is on or heater is activated by exceptional state.
                    boolean currentRunNeedHeatEnable = this.checkIsCurrentHeatNeedEnabled();
                    if (currentRunNeedHeatEnable || this.getIsForceHeating() || exceptionalStateOverride) {
                        this.wasNeedHeatEnableLastCycle = true;
                        // Activate thresholdThermometer and check if temperature setpoint can be met. Otherwise close
                        // valve and ask for more heat.
                        this.setThresholdAndControlValve();
                    } else {
                        this.wasNeedHeatEnableLastCycle = false;
                        this._setHeaterState(HeaterState.STANDBY.getValue());
                        this.closeValveOrDisableValveController();
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't enable/Disable the ValveController!");
                }
            } else {
                this.deactivateControlledComponents();
                this.timer.resetTimer(NEED_HEAT_RESPONSE_IDENTIFIER);
                this.wasNeedHeatEnableLastCycle = false;
            }
        }
    }

    /**
     * The method that does the actual heating.
     * Send temperature setpoint to thresholdThermometer. Ask thresholdThermometer if temperature is above setpoint.
     * If yes: open valves, write ’false’ in ’NeedMoreHeat’ channel.
     * If no: close valves, write ’true’ in ’NeedMoreHeat’ channel.
     */
    private void setThresholdAndControlValve() throws OpenemsError.OpenemsNamedException {
        int temperatureSetPoint = this.getTemperatureSetpoint().orElse(DecentralHeater.DEFAULT_SETPOINT_TEMPERATURE);
        this.thresholdThermometer.setSetPointTemperatureAndActivate(temperatureSetPoint, super.id());
        //Static Valve Controller Works on it's own with given Temperature
        if (this.isValve == false) {
            try {
                this.configuredValveController.setEnableSignal(true);
                this.configuredValveController.setControlType(ControlType.TEMPERATURE);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't apply EnableSignal (true) to the Valve Controller in " + super.id());
            }
        }
        // Check if SetPointTemperature above Thermometer --> Either
        if (this.thresholdThermometer.thermometerAboveGivenTemperature(temperatureSetPoint)) {
            this._setHeaterState(HeaterState.RUNNING.getValue());
            this.getNeedMoreHeatChannel().setNextValue(false);
            if (this.isValve) {
                this.configuredValve.setPointPowerLevelChannel().setNextValue(100);
            }
        } else {
            this.getNeedMoreHeatChannel().setNextValue(true);
            if (this.isValve) {
                this.closeValveOrDisableValveController();
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
                this.timer.checkTimeIsUp(NEED_HEAT_RESPONSE_IDENTIFIER) || this.wasNeedHeatEnableLastCycle;

        Optional<Boolean> needHeatEnableSignal = this.getNeedHeatEnableSignalChannel().getNextWriteValueAndReset();
        if (needHeatEnableSignal.isPresent()) {
            this.timer.resetTimer(NEED_HEAT_RESPONSE_IDENTIFIER);
            currentRunNeedHeatEnable = needHeatEnableSignal.get();
        }
        return currentRunNeedHeatEnable;
    }

    /**
     * Check if any component isn't enabled anymore and references needs to be set again.
     */
    private void checkMissingComponents() {
        OpenemsComponent componentFetchedByCpm;
        try {
            if (this.isValve) {
                if (this.configuredValve.isEnabled() == false) {
                    componentFetchedByCpm = cpm.getComponent(this.configuredValve.id());
                    if (componentFetchedByCpm instanceof Valve) {
                        this.configuredValve = (Valve) componentFetchedByCpm;
                    }
                }
            } else {
                if (this.configuredValveController.isEnabled() == false) {
                    componentFetchedByCpm = cpm.getComponent(this.configuredValveController.id());
                    if (componentFetchedByCpm instanceof ValveController) {
                        this.configuredValveController = (ValveController) componentFetchedByCpm;
                    }
                }
            }
            if (this.thresholdThermometer.isEnabled() == false) {
                componentFetchedByCpm = cpm.getComponent(this.thresholdThermometer.id());
                if (componentFetchedByCpm instanceof ThermometerThreshold) {
                    this.thresholdThermometer = (ThermometerThreshold) componentFetchedByCpm;
                }
            }
            this._setErrorMessage("No error");
        } catch (OpenemsError.OpenemsNamedException ignored) {
            //TODO (?) Was soll passieren wenn Komponente nicht gefunden werden kann/falsche instanceof nur heaterstate --> error?
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
        this.getNeedHeatChannel().setNextValue(false);
        this.getNeedMoreHeatChannel().setNextValue(false);
        this.thresholdThermometer.releaseSetPointTemperatureId(super.id());
        try {
            this.closeValveOrDisableValveController();
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't disable ValveController!");
        }
        this._setHeaterState(HeaterState.OFF.getValue());
    }

    /**
     * When Called close the Valve (if configured) or otherwise disable the ValveController.
     */
    private void closeValveOrDisableValveController() throws OpenemsError.OpenemsNamedException {
        if (this.isValve) {
            this.configuredValve.setPointPowerLevelChannel().setNextValue(0);
        } else {
            this.configuredValveController.getEnableSignalChannel().setNextWriteValueFromObject(false);
        }
    }

}
