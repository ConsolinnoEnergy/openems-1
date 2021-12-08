package io.openems.edge.controller.heatnetwork.surveillance.temperature;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.hydrauliccomponent.api.ControlType;
import io.openems.edge.controller.hydrauliccomponent.api.HydraulicController;
import io.openems.edge.heater.api.Cooler;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerThreshold;
import io.openems.edge.thermometer.api.ThermometerType;
import io.openems.edge.thermometer.api.ThermometerWrapper;
import io.openems.edge.thermometer.api.ThermometerWrapperForCoolingImpl;
import io.openems.edge.thermometer.api.ThermometerWrapperForHeatingImpl;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The TemperatureSurveillanceController is a Controller that monitors 3 different Temperatures and enables/disables it's
 * Components, depending on the Configuration.
 * There are 3 "valid" SurveillanceTypes.
 * HEATER_ONLY, VALVE_CONTROLLER_ONLY, HEATER_AND_VALVE_CONTROLLER
 * HeaterOnly enables the Heater if the activationConditions apply.
 * Equivalent thing happen for the valveControllerOnly setting, enabling a ValveController.
 * If HeaterAndValveController setting is enabled, the Heater will be enabled at first, and after a configured WaitTime
 * The ValveController will be additionally enabled.
 */

abstract class AbstractTemperatureSurveillanceController extends AbstractOpenemsComponent implements OpenemsComponent, Controller {
    private final Logger log = LoggerFactory.getLogger(TemperatureSurveillanceControllerHeatingImpl.class);


    ComponentManager cpm;

    protected boolean configSuccess;
    //Different ThresholdThermometer
    private Thermometer referenceThermometer;
    private Thermometer activationThermometer;
    private Thermometer deactivationThermometer;
    private HydraulicController optionalHydraulicController;
    private Heater optionalHeater;
    private SurveillanceType surveillanceType;
    private boolean isRunning;
    private static final String HYDRAULIC_CONTROLLER_IDENTIFIER = "TEMP_SURVEILLANCE_CONTROLLER_IDENTIFIER";
    private TimerHandler timer;
    private ThermometerWrapper thermometerWrapper;
    private SurveillanceHeatingType heatingType = SurveillanceHeatingType.HEATING;
    private static final String CHECK_COMPONENTS_IDENTIFIER = "TEMP_SURVEILLANCE_CHECK_COMPONENTS";
    private static final int CHECK_MISSING_COMPONENTS_DELTA_TIME = 60;
    private boolean disableLogicOnOffline = false;

    protected enum SurveillanceHeatingType {
        HEATING, COOLING
    }


    protected AbstractTemperatureSurveillanceController(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                                                        io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    protected void activate(ComponentContext context, String id, String alias, boolean enabled,
                            String thermometerActivateId, String thermometerDeactivateId,
                            String referenceThermometerId, SurveillanceHeatingType heatingType, SurveillanceType surveillanceType,
                            int activationOffset, int deactivationOffset, String heaterId, String hydraulicControllerId,
                            String timerId, int deltaTime, ComponentManager cpm, boolean useDisableLogicOnOffline) throws ConfigurationException {
        super.activate(context, id, alias, enabled);
        this.cpm = cpm;
        this.heatingType = heatingType;
        this.surveillanceType = surveillanceType;
        this.activationOrModifiedRoutine(thermometerActivateId, thermometerDeactivateId, referenceThermometerId,
                activationOffset, deactivationOffset, heaterId, hydraulicControllerId, timerId, deltaTime, useDisableLogicOnOffline
        );
    }

    /**
     * Checks the configuration.
     * If the ThermometerId's have duplications -> throw an exception (Because config will never work).
     * Otherwise check if the Configuration might succeed. If not -> Wait until the run method is called.
     * Some Components might be enabled later.
     *
     * @param thermometerActivateId   thermometerActivateId the activation Thermometer id.
     * @param thermometerDeactivateId thermometerDeactivateId the deactivation Thermometer id.
     * @param referenceThermometerId  referenceThermometerId the reference Thermometer id.
     * @param activationOffset        activationOffset offset for Activation e.g. if reference is 750dC but you want to activate on 800 -> offset would be 50
     * @param deactivationOffset      deactivationOffset offset for deactivation e.g. if reference is 750 but you want to deactivate at 650 -> offset would be -100
     * @param heaterId                heaterOrCoolerId the heater or cooler id depending on the {@link SurveillanceHeatingType}
     * @param hydraulicControllerId   hydraulicControllerId the hydraulicController id.
     * @param timerId                 timerId the timer to stop the time.
     * @param deltaTime               the delta Time -> wait cycles or Time.
     * @param useDisableLogicOnOffline should an "OFFLINE" state be handled in the Same way as the HeaterState "BlockedOrError"
     * @throws ConfigurationException if the Thermometer id's are duplicated.
     */
    protected void activationOrModifiedRoutine(String thermometerActivateId, String thermometerDeactivateId,
                                               String referenceThermometerId,
                                               int activationOffset, int deactivationOffset, String heaterId,
                                               String hydraulicControllerId, String timerId, int deltaTime, boolean useDisableLogicOnOffline) throws ConfigurationException {
        if (this.configContainsSameThermometerIds(thermometerActivateId, thermometerDeactivateId, referenceThermometerId)) {
            this.configSuccess = false;
            throw new ConfigurationException("Activate - TemperatureSurveillanceController",
                    "Activate and Deactivate and Reference Thermometer are not allowed to be the same! "
                            + thermometerActivateId + thermometerDeactivateId + referenceThermometerId);
        }
        try {
            this.allocateComponents(thermometerActivateId, thermometerDeactivateId, referenceThermometerId,
                    activationOffset, deactivationOffset, heaterId, hydraulicControllerId, timerId, deltaTime);
            this.disableLogicOnOffline = useDisableLogicOnOffline;
            this.configSuccess = true;
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Couldn't allocate Components, this Controller will try again later " + super.id());
        }
    }

    /**
     * Init the Timer to the identifier.
     *
     * @param timerId   the id of the timer.
     * @param deltaTime the waitTime.
     * @throws OpenemsError.OpenemsNamedException if the timer couldn't be found
     * @throws ConfigurationException             if id is found but they're not instances of a Timer
     */

    private void initializeTimer(String timerId, int deltaTime) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.timer = new TimerHandlerImpl(super.id(), this.cpm);
        this.timer.addOneIdentifier(CHECK_COMPONENTS_IDENTIFIER, timerId, CHECK_MISSING_COMPONENTS_DELTA_TIME);
        if (this.surveillanceType.equals(SurveillanceType.HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER)) {
            this.timer.addOneIdentifier(HYDRAULIC_CONTROLLER_IDENTIFIER, timerId, deltaTime);
        }
    }

    void modified(ComponentContext context, String id, String alias, boolean enabled,
                  String thermometerActivateId, String thermometerDeactivateId,
                  String referenceThermometerId, SurveillanceHeatingType heatingType, SurveillanceType surveillanceType,
                  int activationOffset, int deactivationOffset, String heaterId, String hydraulicControllerId,
                  String timerId, int deltaTime, ComponentManager cpm, boolean useDisableLogicOnOffline) throws ConfigurationException {
        super.modified(context, id, alias, enabled);
        this.cpm = cpm;
        this.configSuccess = false;
        this.heatingType = heatingType;
        this.surveillanceType = surveillanceType;
        this.activationOrModifiedRoutine(thermometerActivateId, thermometerDeactivateId, referenceThermometerId,
                activationOffset, deactivationOffset, heaterId, hydraulicControllerId, timerId, deltaTime,
                useDisableLogicOnOffline);
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
     * @param thermometerActivateId   the activation Thermometer id.
     * @param thermometerDeactivateId the deactivation Thermometer id.
     * @param referenceThermometerId  the reference Thermometer id.
     * @param activationOffset        offset for Activation e.g. if reference is 750dC but you want to activate on 800 -> offset would be 50
     * @param deactivationOffset      offset for deactivation e.g. if reference is 750 but you want to deactivate at 650 -> offset would be -100
     * @param heaterOrCoolerId        the heater or cooler id depending on the {@link SurveillanceHeatingType}
     * @param hydraulicControllerId   the hydraulicController id.
     * @param timerId                 the timer to stop the time.
     * @param deltaTime               the delta Time -> wait cycles or Time.
     * @throws ConfigurationException             if the componentIds are available in the OpenEMS Edge but not an instance of the correct Class
     * @throws OpenemsError.OpenemsNamedException if the id couldn't be found at all.
     */
    private void allocateComponents(String thermometerActivateId, String thermometerDeactivateId,
                                    String referenceThermometerId, int activationOffset,
                                    int deactivationOffset, String heaterOrCoolerId,
                                    String hydraulicControllerId,
                                    String timerId, int deltaTime) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        OpenemsComponent openemsComponentToAllocate;
        openemsComponentToAllocate = this.cpm.getComponent(referenceThermometerId);
        if (openemsComponentToAllocate instanceof Thermometer) {
            this.referenceThermometer = (Thermometer) openemsComponentToAllocate;
        } else {
            throw new ConfigurationException("AllocateComponents", "ThermometerId: "
                    + referenceThermometerId + " Not an Instance of Thermometer");
        }


        openemsComponentToAllocate = this.cpm.getComponent(thermometerActivateId);
        if (openemsComponentToAllocate instanceof Thermometer) {
            this.activationThermometer = (Thermometer) openemsComponentToAllocate;
        } else {
            throw new ConfigurationException("AllocateComponents", "ThresholdThermometerId: "
                    + thermometerActivateId + " Not an Instance of ThresholdThermometer");
        }
        openemsComponentToAllocate = this.cpm.getComponent(thermometerDeactivateId);
        if (openemsComponentToAllocate instanceof Thermometer) {
            this.deactivationThermometer = (Thermometer) openemsComponentToAllocate;
        } else {
            throw new ConfigurationException("AllocateComponents", "ThresholdThermometerId: "
                    + thermometerDeactivateId + " Not an Instance of ThresholdThermometer");
        }

        ChannelAddress referenceAddress = this.referenceThermometer.getTemperatureChannel().address();
        switch (this.heatingType) {

            case COOLING:
                this.thermometerWrapper = new ThermometerWrapperForCoolingImpl(this.activationThermometer, this.deactivationThermometer,
                        referenceAddress.toString(), referenceAddress.toString(), this.cpm, activationOffset, deactivationOffset);
                break;
            case HEATING:
            default:
                this.thermometerWrapper = new ThermometerWrapperForHeatingImpl(this.activationThermometer, this.deactivationThermometer,
                        referenceAddress.toString(), referenceAddress.toString(), this.cpm, activationOffset, deactivationOffset);
                break;
        }


        if (this.surveillanceType.equals(SurveillanceType.HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER)
                || this.surveillanceType.equals(SurveillanceType.HEATER_OR_COOLER_ONLY)) {
            openemsComponentToAllocate = this.cpm.getComponent(heaterOrCoolerId);
            if (openemsComponentToAllocate instanceof Heater) {
                if ((this.heatingType.equals(SurveillanceHeatingType.HEATING) && !(openemsComponentToAllocate instanceof Cooler))
                        || this.heatingType.equals(SurveillanceHeatingType.COOLING) && openemsComponentToAllocate instanceof Cooler) {
                    this.optionalHeater = (Heater) openemsComponentToAllocate;
                } else {
                    throw new ConfigurationException("AllocateComponents", "Allocated Component is wrong instance! " + heaterOrCoolerId);
                }
            } else {
                throw new ConfigurationException("AllocateComponents", "HeaterId: "
                        + heaterOrCoolerId + " Not an Instance of Heater");
            }
        }
        if (this.surveillanceType.equals(SurveillanceType.HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER)
                || this.surveillanceType.equals(SurveillanceType.HYDRAULIC_CONTROLLER_ONLY)) {
            openemsComponentToAllocate = this.cpm.getComponent(hydraulicControllerId);
            if (openemsComponentToAllocate instanceof HydraulicController) {
                this.optionalHydraulicController = (HydraulicController) openemsComponentToAllocate;
            } else {
                throw new ConfigurationException("AllocateComponents", "ValveControllerId: "
                        + hydraulicControllerId + " Not an Instance of ValveController");
            }
        }
        this.initializeTimer(timerId, deltaTime);
    }

    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Depending on SurveillanceType Different "Controlling" applies
     * If ActivationConditions apply (Same for every SurveillanceType).
     * <p>
     * Either: Enable HEATER/Cooler (on Heater ONLY mode)
     * OR
     * Enable HydraulicController (on HydraulicController ONLY Mode
     * OR
     * First Enable Heater and then after certain WaitTime Enable ValveController (HEATER_AND_HYDRAULIC_CONTROLLER).
     * on deactivation: Disable corresponding Components
     * </p>
     */
    protected void abstractRun() {
        try {
            if (this.configSuccess) {
                this.checkForMissingComponents();
                if (this.thermometerWrapper.shouldDeactivate() || this.optionalHeaterHasError()) {
                    this.isRunning = false;
                    this.disableComponents();
                    if (this.surveillanceType.equals(SurveillanceType.HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER)) {
                        this.timer.resetTimer(HYDRAULIC_CONTROLLER_IDENTIFIER);
                    }
                } else if (this.thermometerWrapper.shouldActivate() || this.isRunning) {
                    this.isRunning = true;
                    switch (this.surveillanceType) {
                        case HEATER_OR_COOLER_ONLY:
                            this.optionalHeater.getEnableSignalChannel().setNextWriteValueFromObject(true);
                            break;
                        case HYDRAULIC_CONTROLLER_ONLY:
                            this.optionalHydraulicController.setEnableSignal(true);
                            this.optionalHydraulicController.setControlType(ControlType.TEMPERATURE);
                            break;
                        case HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER:
                            this.optionalHeater.getEnableSignalChannel().setNextWriteValueFromObject(true);
                            if (this.timer.checkTimeIsUp(HYDRAULIC_CONTROLLER_IDENTIFIER)) {
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
            }
        } catch (ConfigurationException | OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't set EnableSignal, Write to Channel, or ReadFromChannel Address: " + e.getMessage());
        }
    }

    private boolean optionalHeaterHasError() {
        switch (this.surveillanceType) {
            case HEATER_OR_COOLER_ONLY:
            case HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER:
                HeaterState state = this.optionalHeater.getHeaterState().asEnum();
                if (this.disableLogicOnOffline) {
                    return state.equals(HeaterState.BLOCKED_OR_ERROR) || state.equals(HeaterState.OFF);
                }
                return state.equals(HeaterState.BLOCKED_OR_ERROR);
            case HYDRAULIC_CONTROLLER_ONLY:
            case NOTHING:
            default:
                return false;
        }
    }

    /**
     * Disables the Component if the {@link SurveillanceType} matches.
     *
     * @throws OpenemsError.OpenemsNamedException if the write fails.
     */
    private void disableComponents() throws OpenemsError.OpenemsNamedException {
        switch (this.surveillanceType) {
            case HYDRAULIC_CONTROLLER_ONLY:
            case HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER:
                if (this.optionalHydraulicController != null) {
                    this.optionalHydraulicController.setEnableSignal(false);
                }
        }
    }

    /**
     * Reallocates the Component if one Component was reactivated. Or deleted and activated later again.
     */
    private void checkForMissingComponents() {
        if (this.timer.checkTimeIsUp(CHECK_COMPONENTS_IDENTIFIER)) {
            try {
                OpenemsComponent allocatedOpenemsComponent;
                allocatedOpenemsComponent = this.cpm.getComponent(this.activationThermometer.id());
                if (!this.activationThermometer.equals(allocatedOpenemsComponent) && allocatedOpenemsComponent instanceof ThermometerThreshold) {
                    this.activationThermometer = (ThermometerThreshold) allocatedOpenemsComponent;
                    this.thermometerWrapper.renewThermometer(ThermometerType.ACTIVATE_THERMOMETER, this.activationThermometer);
                }
                allocatedOpenemsComponent = this.cpm.getComponent(this.deactivationThermometer.id());
                if (!this.deactivationThermometer.equals(allocatedOpenemsComponent) && allocatedOpenemsComponent instanceof ThermometerThreshold) {
                    this.deactivationThermometer = (ThermometerThreshold) allocatedOpenemsComponent;
                    this.thermometerWrapper.renewThermometer(ThermometerType.DEACTIVATE_THERMOMETER, this.deactivationThermometer);
                }

                if (!this.referenceThermometer.equals(this.cpm.getComponent(this.referenceThermometer.id()))) {
                    allocatedOpenemsComponent = this.cpm.getComponent(this.referenceThermometer.id());
                    if (allocatedOpenemsComponent instanceof ThermometerThreshold) {
                        this.referenceThermometer = (ThermometerThreshold) allocatedOpenemsComponent;
                    }
                }

                if (this.surveillanceType.equals(SurveillanceType.NOTHING)) {
                    return;
                }
                if (this.surveillanceType.equals(SurveillanceType.HEATER_OR_COOLER_ONLY)
                        || this.surveillanceType.equals(SurveillanceType.HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER)) {
                    allocatedOpenemsComponent = this.cpm.getComponent(this.optionalHeater.id());
                    if (!this.optionalHeater.equals(allocatedOpenemsComponent) && allocatedOpenemsComponent instanceof Heater) {
                        this.optionalHeater = (Heater) allocatedOpenemsComponent;
                    }
                }
                if (this.surveillanceType.equals(SurveillanceType.HYDRAULIC_CONTROLLER_ONLY)
                        || this.surveillanceType.equals(SurveillanceType.HEATER_OR_COOLER_AND_HYDRAULIC_CONTROLLER)) {
                    if (this.optionalHydraulicController.isEnabled() == false) {
                        allocatedOpenemsComponent = this.cpm.getComponent(this.optionalHydraulicController.id());
                        if (allocatedOpenemsComponent instanceof HydraulicController) {
                            this.optionalHydraulicController = (HydraulicController) allocatedOpenemsComponent;
                        }
                    }
                }
                this.timer.resetTimer(CHECK_COMPONENTS_IDENTIFIER);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.info("Couldn't check for missing Component! " + e.getMessage());
            }
        }
    }


}
