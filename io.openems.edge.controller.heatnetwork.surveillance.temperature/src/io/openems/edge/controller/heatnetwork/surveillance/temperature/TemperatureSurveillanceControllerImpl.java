package io.openems.edge.controller.heatnetwork.surveillance.temperature;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.surveillance.temperature.api.TemperatureSurveillanceController;
import io.openems.edge.controller.hydrauliccomponent.api.ControlType;
import io.openems.edge.controller.hydrauliccomponent.api.HydraulicController;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerThreshold;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The TemperatureSurveillanceController is a Controller that monitores 3 different Temperatures and enables/disables it's
 * Components, depending on the Configuration.
 * There are 3 "valid" SurveillanceTypes.
 * HEATER_ONLY, VALVE_CONTROLLER_ONLY, HEATER_AND_VALVE_CONTROLLER
 * HeaterOnly enables the Heater if the activationConditions apply.
 * Equivalent thing happen for the valveControllerOnly setting, enabling a ValveController.
 * If HeaterAndValveController setting is enabled, the Heater will be enabled at first, and after a configured WaitTime
 * The ValveController will be additionally enabled.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "controller.temperature.surveillance",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TemperatureSurveillanceControllerImpl extends AbstractOpenemsComponent implements OpenemsComponent,
        Controller, TemperatureSurveillanceController {

    private final Logger log = LoggerFactory.getLogger(TemperatureSurveillanceControllerImpl.class);

    @Reference
    ComponentManager cpm;

    private Config config;
    private boolean configSuccess;
    private final AtomicInteger cycleCount = new AtomicInteger(0);
    private static final int MAX_CYCLE_FOR_CONFIG = 10;
    //Different ThresholdThermometer
    private Thermometer referenceThermometer;
    private Thermometer activationThermometer;
    private Thermometer deactivationThermometer;
    private int activationOffset;
    private int deactivationOffset;
    private HydraulicController optionalHydraulicController;
    private Heater optionalHeater;
    private TemperatureSurveillanceType surveillanceType;
    private DateTime initialWaitTimeStamp;
    private boolean isRunning;
    private static final String VALVE_IDENTIFIER = "TEMP_SURVEILLANCE_VALVE_IDENTIFIER";
    private TimerHandler timer;


    public TemperatureSurveillanceControllerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    /**
     * Checks the configuration.
     * If the ThermometerId's have duplications -> throw an exception (Because config will never work).
     * Otherwise check if the Configuration might succeed. If not -> Wait until the run method is called.
     * Some Components might be enabled later.
     *
     * @param config the config of this Component.
     * @throws ConfigurationException if the Thermometer id's are duplicated.
     */
    private void activationOrModifiedRoutine(Config config) throws ConfigurationException {
        this.config = config;
        if (this.configContainsSameThermometerIds(config.thermometerActivateId(), config.thermometerDeactivateId(), config.referenceThermometerId())) {
            this.configSuccess = false;
            throw new ConfigurationException("Activate - TemperatureSurveillanceController",
                    "Activate and Deactivate and Reference Thermometer are not allowed to be the same! "
                            + config.thermometerActivateId() + config.thermometerDeactivateId() + config.referenceThermometerId());
        }
        try {
            this.surveillanceType = config.surveillanceType();
            this.activationOffset = config.offsetActivate();
            this.deactivationOffset = config.offsetDeactivate();
            this.allocateComponents(config);

            this.configSuccess = true;
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Couldn't allocate Components, this Controller will try again later " + super.id());
        }
    }

    /**
     * Init the Timer to the identifier.
     *
     * @param config the config of this component
     * @throws OpenemsError.OpenemsNamedException if the timer couldn't be found
     * @throws ConfigurationException             if id is found but they're not instances of a Timer
     */

    private void initializeTimer(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (this.surveillanceType.equals(TemperatureSurveillanceType.HEATER_AND_VALVE_CONTROLLER)) {
            this.timer = new TimerHandlerImpl(super.id(), this.cpm);
            this.timer.addOneIdentifier(VALVE_IDENTIFIER, config.timerId(), config.timeToWaitValveOpen());
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    /**
     * Check for duplicated Thermometer ids.
     *
     * @param ids the Thermometer ids.
     * @return a boolean.
     */

    private boolean configContainsSameThermometerIds(String... ids) {
        List<String> idList = new ArrayList<>();
        List<String> doubleIds = new ArrayList<>();
        Arrays.stream(ids).forEach(entry -> {
            if (idList.contains(entry)) {
                doubleIds.add(entry);
            } else {
                idList.add(entry);
            }
        });
        if (doubleIds.size() > 0) {
            this.log.error("Duplicated Thermometer ids found: " + doubleIds.toString());
            return true;
        } else {
            return false;
        }
    }

    /**
     * Allocates the Components corresponding to the Config.
     *
     * @param config the config of the Component
     * @throws ConfigurationException             if the componentIds are available in the OpenEMS Edge but not an instance of the correct Class
     * @throws OpenemsError.OpenemsNamedException if the id couldn't be found at all.
     */
    private void allocateComponents(Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        OpenemsComponent openemsComponentToAllocate;
        openemsComponentToAllocate = this.cpm.getComponent(config.referenceThermometerId());
        if (openemsComponentToAllocate instanceof Thermometer) {
            this.referenceThermometer = (Thermometer) openemsComponentToAllocate;
        } else {
            throw new ConfigurationException("AllocateComponents", "ThermometerId: "
                    + config.referenceThermometerId() + " Not an Instance of Thermometer");
        }


        openemsComponentToAllocate = this.cpm.getComponent(config.thermometerActivateId());
        if (openemsComponentToAllocate instanceof Thermometer) {
            this.activationThermometer = (Thermometer) openemsComponentToAllocate;
        } else {
            throw new ConfigurationException("AllocateComponents", "ThresholdThermometerId: "
                    + config.thermometerActivateId() + " Not an Instance of ThresholdThermometer");
        }
        openemsComponentToAllocate = this.cpm.getComponent(config.thermometerDeactivateId());
        if (openemsComponentToAllocate instanceof Thermometer) {
            this.deactivationThermometer = (Thermometer) openemsComponentToAllocate;
        } else {
            throw new ConfigurationException("AllocateComponents", "ThresholdThermometerId: "
                    + config.thermometerDeactivateId() + " Not an Instance of ThresholdThermometer");
        }
        if (this.surveillanceType.equals(TemperatureSurveillanceType.HEATER_AND_VALVE_CONTROLLER)
                || this.surveillanceType.equals(TemperatureSurveillanceType.HEATER_ONLY)) {
            openemsComponentToAllocate = this.cpm.getComponent(config.heaterId());
            if (openemsComponentToAllocate instanceof Heater) {
                this.optionalHeater = (Heater) openemsComponentToAllocate;
            } else {
                throw new ConfigurationException("AllocateComponents", "HeaterId: "
                        + config.heaterId() + " Not an Instance of Heater");
            }
        }
        if (this.surveillanceType.equals(TemperatureSurveillanceType.HEATER_AND_VALVE_CONTROLLER)
                || this.surveillanceType.equals(TemperatureSurveillanceType.VALVE_CONTROLLER_ONLY)) {
            openemsComponentToAllocate = this.cpm.getComponent(config.valveControllerId());
            if (openemsComponentToAllocate instanceof HydraulicController) {
                this.optionalHydraulicController = (HydraulicController) openemsComponentToAllocate;
            } else {
                throw new ConfigurationException("AllocateComponents", "ValveControllerId: "
                        + config.valveControllerId() + " Not an Instance of ValveController");
            }
        }
        this.initializeTimer(config);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Depending on SurveillanceType Different "Controlling" applies
     * If ActivationConditions apply (Same for every SurveillanceType).
     * <p>
     * Either: Enable HEATER (on Heater ONLY mode)
     * OR
     * Enable ValveController (on ValveController ONLY Mode
     * OR
     * First Enable Heater and then after certain WaitTime Enable ValveController (HEATER_AND_VALVE_CONTROLLER).
     * on deactivation: Disable corresponding Components
     * </p>
     */
    @Override
    public void run() {
        try {
            if (this.configSuccess) {
                this.checkForMissingComponents();
                if (this.deactivationConditionsApply()) {
                    this.isRunning = false;
                    this.disableComponents();
                }

                if (this.activationConditionsApply() || this.isRunning) {
                    this.isRunning = true;
                    switch (this.surveillanceType) {
                        case HEATER_ONLY:
                            this.optionalHeater.getEnableSignalChannel().setNextWriteValueFromObject(true);
                            break;
                        case VALVE_CONTROLLER_ONLY:
                            this.optionalHydraulicController.setEnableSignal(true);
                            this.optionalHydraulicController.setControlType(ControlType.TEMPERATURE);
                            break;
                        case HEATER_AND_VALVE_CONTROLLER:
                            this.optionalHeater.getEnableSignalChannel().setNextWriteValueFromObject(true);
                            if (this.timer.checkTimeIsUp(VALVE_IDENTIFIER)) {
                                this.optionalHydraulicController.setEnableSignal(true);
                                this.optionalHydraulicController.setControlType(ControlType.TEMPERATURE);
                            } else {
                                this.optionalHydraulicController.setEnableSignal(false);
                            }
                            break;
                        case NOTHING:
                            break;
                    }
                }
            } else {
                this.allocateComponents(this.config);
                this.configSuccess = true;
            }
        } catch (ConfigurationException | OpenemsError.OpenemsNamedException e) {
            if (this.cycleCount.get() > MAX_CYCLE_FOR_CONFIG) {
                this.log.warn("Couldn't allocate Configuration for component: " + super.id());
            } else {
                this.cycleCount.getAndIncrement();
            }
            this.configSuccess = false;
        }

    }

    /**
     * Disables the Component if the {@link TemperatureSurveillanceType} matches.
     *
     * @throws OpenemsError.OpenemsNamedException if the write fails.
     */
    private void disableComponents() throws OpenemsError.OpenemsNamedException {
        switch (this.surveillanceType) {
            case VALVE_CONTROLLER_ONLY:
            case HEATER_AND_VALVE_CONTROLLER:
                if (this.optionalHydraulicController != null) {
                    this.optionalHydraulicController.setEnableSignal(false);
                }
        }
    }

    /**
     * Reallocates the Component if one Component was reactivated.
     *
     * @throws OpenemsError.OpenemsNamedException if Component is Missing.
     */
    private void checkForMissingComponents() throws OpenemsError.OpenemsNamedException {
        OpenemsComponent allocatedOpenemsComponent;
        if (this.activationThermometer.isEnabled() == false) {
            allocatedOpenemsComponent = this.cpm.getComponent(this.activationThermometer.id());
            if (allocatedOpenemsComponent instanceof ThermometerThreshold) {
                this.activationThermometer = (ThermometerThreshold) allocatedOpenemsComponent;
            }
        }
        if (this.deactivationThermometer.isEnabled() == false) {
            allocatedOpenemsComponent = this.cpm.getComponent(this.deactivationThermometer.id());
            if (allocatedOpenemsComponent instanceof ThermometerThreshold) {
                this.deactivationThermometer = (ThermometerThreshold) allocatedOpenemsComponent;
            }
        }
        if (this.surveillanceType.equals(TemperatureSurveillanceType.NOTHING)) {
            return;
        }
        if (this.surveillanceType.equals(TemperatureSurveillanceType.HEATER_ONLY)
                || this.surveillanceType.equals(TemperatureSurveillanceType.HEATER_AND_VALVE_CONTROLLER)) {
            if (this.optionalHeater.isEnabled() == false) {
                allocatedOpenemsComponent = this.cpm.getComponent(this.optionalHeater.id());
                if (allocatedOpenemsComponent instanceof Heater) {
                    this.optionalHeater = (Heater) allocatedOpenemsComponent;
                }

            }
        }
        if (this.surveillanceType.equals(TemperatureSurveillanceType.VALVE_CONTROLLER_ONLY)
                || this.surveillanceType.equals(TemperatureSurveillanceType.HEATER_AND_VALVE_CONTROLLER)) {
            if (this.optionalHydraulicController.isEnabled() == false) {
                allocatedOpenemsComponent = this.cpm.getComponent(this.optionalHydraulicController.id());
                if (allocatedOpenemsComponent instanceof HydraulicController) {
                    this.optionalHydraulicController = (HydraulicController) allocatedOpenemsComponent;
                }
            }
        }
    }

    /**
     * The Deactivation Condition is: DeactivationThermometer > SetPoint.
     *
     * @return the comparison boolean.
     */
    private boolean deactivationConditionsApply() {
        return this.referenceThermometer.getTemperatureValue() > (this.deactivationThermometer.getTemperatureValue() + this.deactivationOffset);
    }

    /**
     * The ActivationCondition is ActivationThermometer < setPoint+Offset.
     *
     * @return the comparison boolean.
     */
    private boolean activationConditionsApply() {
        return this.referenceThermometer.getTemperatureValue() < this.activationThermometer.getTemperatureValue() + this.activationOffset;
    }
}
