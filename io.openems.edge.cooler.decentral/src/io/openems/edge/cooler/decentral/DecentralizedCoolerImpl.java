package io.openems.edge.cooler.decentral;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.heatnetwork.valve.api.ControlType;
import io.openems.edge.controller.heatnetwork.valve.api.ValveController;
import io.openems.edge.heater.Cooler;
import io.openems.edge.heater.HeaterState;
import io.openems.edge.cooler.decentral.api.DecentralizeCooler;
import io.openems.edge.heatsystem.components.Valve;
import io.openems.edge.thermometer.api.ThermometerThreshold;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

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

public class DecentralizedCoolerImpl extends AbstractOpenemsComponent implements OpenemsComponent, DecentralizeCooler, EventHandler {

    private static final int DEFAULT_MAXIMUM_VALVE = 100;
    private final Logger log = LoggerFactory.getLogger(DecentralizedCoolerImpl.class);
    @Reference
    ComponentManager cpm;

    private Valve configuredValve;
    private ValveController configuredValveController;
    private boolean isValve;
    private ThermometerThreshold thermometerThreshold;
    private final AtomicInteger currentWaitCycleNeedCoolEnable = new AtomicInteger(0);
    private int maxWaitCyclesNeedCoolEnable;
    private boolean wasNeedCoolEnableLastCycle;

    public DecentralizedCoolerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Cooler.ChannelId.values(),
                DecentralizeCooler.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());

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
            this.thermometerThreshold = (ThermometerThreshold) componentFetchedByComponentManager;
            this.thermometerThreshold.setSetPointTemperature(config.setPointTemperature(), super.id());
        } else {
            throw new ConfigurationException("activate",
                    "Component with ID: " + config.thresholdThermometerId() + " not an instance of Threshold");
        }
        this.setSetPointTemperature(config.setPointTemperature());
        if (config.shouldCloseOnActivation()) {
            if (this.isValve) {
                this.configuredValve.forceClose();
            } else {
                this.configuredValveController.setEnableSignal(false);
            }
        }
        this.getForceCoolChannel().setNextValue(config.forceCooling());
        this.maxWaitCyclesNeedCoolEnable = config.waitCyclesNeedCoolResponse();
        this.setState(HeaterState.OFFLINE.name());
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
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            if (this.errorInCooler()) {
                //TODO DO SOMETHING (?)
            }
            this.checkMissingComponents();
            //First things first: Is Cooler Enabled
            boolean currentRunCoolerEnabled = this.checkIsCurrentRunCoolerEnabled();
            if (currentRunCoolerEnabled) {
                this.getNeedCoolChannel().setNextValue(true);
                //Is Cooler allowed to Cool
                boolean currentRunNeedCoolEnable = this.checkIsCurrentCoolNeedEnabled();
                if (currentRunNeedCoolEnable || this.getIsForceCooling()) {
                    this.currentWaitCycleNeedCoolEnable.getAndSet(0);
                    this.wasNeedCoolEnableLastCycle = true;
                    //activateThresholdThermometer and check if setPointTemperature can be met otherwise shut valve
                    // and ask for more heat
                    try {
                        this.setThresholdAndControlValve();
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Couldn't apply Start Signal to Valve, reason: " + e.getMessage());
                    }
                } else {
                    this.wasNeedCoolEnableLastCycle = false;
                    this.setState(HeaterState.AWAIT.name());
                    this.closeValveOrDisableValveController();
                }
            } else {
                this.deactivateControlledComponents();
            }
        }
    }

    /**
     * If Controller is Enabled AND permission to heat is set.
     * Check if ThresholdThermometer is ok --> if yes activate Valve/ValveController --> Else Close Valve and say "I need more Cool".
     */
    private void setThresholdAndControlValve() throws OpenemsError.OpenemsNamedException {
        this.thermometerThreshold.setSetPointTemperatureAndActivate(this.getSetPointTemperature(), super.id());
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
        if (this.thermometerThreshold.thermometerBelowGivenTemperature(this.getSetPointTemperature())) {
            this.setState(HeaterState.RUNNING.name());
            this.getNeedMoreCoolChannel().setNextValue(false);
            if (this.isValve) {
                this.configuredValve.setPointPowerLevelChannel().setNextValue(DEFAULT_MAXIMUM_VALVE);
            }
        } else {
            this.getNeedMoreCoolChannel().setNextValue(true);
            if (this.isValve) {
                this.closeValveOrDisableValveController();
            }
            this.setState(HeaterState.PRE_COOL.name());
        }
    }

    /**
     * This methods checks if the enabled Signal for need Cool was set OR if the Signal isn't Present -->
     * check if last Cycle was enabled and currentWaitCycles >= Max Wait. If in doubt --> HEAT
     *
     * @return enabled;
     */
    private boolean checkIsCurrentCoolNeedEnabled() {
        boolean currentRunNeedCoolEnable = this.currentWaitCycleNeedCoolEnable.get() >= this.maxWaitCyclesNeedCoolEnable
                || this.wasNeedCoolEnableLastCycle;

        Optional<Boolean> needCoolEnableSignal = this.getNeedCoolEnableSignalChannel().getNextWriteValueAndReset();
        if (needCoolEnableSignal.isPresent()) {
            this.currentWaitCycleNeedCoolEnable.set(0);
            currentRunNeedCoolEnable = needCoolEnableSignal.get();
        } else if (this.currentWaitCycleNeedCoolEnable.get() < this.maxWaitCyclesNeedCoolEnable) {
            this.currentWaitCycleNeedCoolEnable.getAndIncrement();
        }
        return currentRunNeedCoolEnable;
    }

    /**
     * This methods checks if the enabled Signal was set OR if the enableSignal isn't Present -->
     * check if last Cycle was enabled and currentWaitCycles > Max Wait.
     * if in doubt --> HEAT!
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
            if (this.isValve) {
                if (this.configuredValve.isEnabled() == false) {
                    componentFetchedByCpm = this.cpm.getComponent(this.configuredValve.id());
                    if (componentFetchedByCpm instanceof Valve) {
                        this.configuredValve = (Valve) componentFetchedByCpm;
                    }
                }
            } else {
                if (this.configuredValveController.isEnabled() == false) {
                    componentFetchedByCpm = this.cpm.getComponent(this.configuredValveController.id());
                    if (componentFetchedByCpm instanceof ValveController) {
                        this.configuredValveController = (ValveController) componentFetchedByCpm;
                    }
                }
            }
            if (this.thermometerThreshold.isEnabled() == false) {
                componentFetchedByCpm = this.cpm.getComponent(this.thermometerThreshold.id());
                if (componentFetchedByCpm instanceof ThermometerThreshold) {
                    this.thermometerThreshold = (ThermometerThreshold) componentFetchedByCpm;
                }
            }
        } catch (OpenemsError.OpenemsNamedException ignored) {
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
        this.thermometerThreshold.releaseSetPointTemperatureId(super.id());
        this.closeValveOrDisableValveController();
        this.setState(HeaterState.OFFLINE.name());
    }

    /**
     * When Called close the Valve (if configured) or otherwise disable the ValveController.
     */
    private void closeValveOrDisableValveController() {
        if (this.isValve) {
            this.configuredValve.setPointPowerLevelChannel().setNextValue(0);
        } else {
            try {
                this.configuredValveController.setEnableSignal(false);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't apply Enable Signal (false) in ValveController of " + super.id());
            }
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

}
