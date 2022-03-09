package io.openems.edge.controller.hydrauliccomponent.controller;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.hydrauliccomponent.api.ControlType;
import io.openems.edge.controller.hydrauliccomponent.api.HydraulicController;
import io.openems.edge.controller.hydrauliccomponent.api.HydraulicPosition;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.thermometer.api.Thermometer;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This Controller gets a {@link HydraulicComponent} and controls it by a configured static position set.
 * Depending on the temperature it looks for a set Position. The PowerValue will be set into the HydraulicComponent.
 * You can either configure the controller to run in autoMode or to await an enableSignal.
 */
@Designate(ocd = ConfigHydraulicStaticPosition.class, factory = true)
@Component(name = "Controller.Hydraulic.Position", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class HydraulicPositionControllerImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, HydraulicController {

    private final Logger log = LoggerFactory.getLogger(HydraulicPositionControllerImpl.class);

    private static final int ENTRY_LENGTH = 2;
    @Reference
    ComponentManager cpm;

    private HydraulicComponent controlledComponent;
    private final List<HydraulicPosition> hydraulicPositionList = new ArrayList<>();
    private ControlType controlType;
    private boolean closeWhenNeitherAutoRunNorEnableSignal;
    private Thermometer referenceThermometer;
    private boolean useFallback;
    //Wait this amount of cycles if no EnabledSignal is present!
    private static final String MAX_WAIT_CYCLE_IDENTIFIER = "HYDRAULIC_CONTROLLER_STATIC_MAX_WAIT_CYCLE_IDENTIFIER";
    //if enabled signal stays null this component runs for this amount of time:
    private static final String MIN_RUN_TIME_AFTER_FALLBACK_IDENTIFIER = "HYDRAULIC_CONTROLLER_STATIC_MIN_RUN_TIME_IDENTIFIER";

    private static final String CHECK_FOR_COMPONENTS = "HYDRAULIC_CONTROLLER_CHECK_COMPONENTS";
    private static final int DELTA_TIME_CHECK_COMPONENTS = 90;

    private static final String CONFIGURATION_SPLITTER = ":";


    private TimerHandler timer;
    private boolean isRunning = false;
    private String componentId;
    private String thermometerId;
    protected double tolerance;
    private boolean shouldCool;
    private boolean configSuccess;
    private boolean hadToFallbackBefore;

    public HydraulicPositionControllerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                HydraulicController.ChannelId.values());
    }

    ConfigHydraulicStaticPosition config;

    @Activate
    void activate(ComponentContext context, ConfigHydraulicStaticPosition config) {
        this.config = config;

        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.enabled() == false) {
            return;
        }
        try {
            this.activateOrModificationRoutine(config);
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configSuccess = false;
            this.log.warn("Configuration failed: " + e.getMessage());
        }
    }

    @Modified
    void modified(ComponentContext context, ConfigHydraulicStaticPosition config) {
        this.configSuccess = false;
        this.hydraulicPositionList.clear();
        this.config = config;
        super.modified(context, config.id(), config.alias(), config.enabled());
        try {
            this.activateOrModificationRoutine(config);
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configSuccess = false;
            this.log.warn("Configuration failed: " + e.getMessage());
        }
    }

    /**
     * Simple Routine to apply the Config to this controller. Sets Dependencies and List Entries for SetPoints.
     *
     * @param config The Configuration of this component.
     * @throws ConfigurationException             if the configuration is wrong or some Components aren't the correct instance of.
     * @throws OpenemsError.OpenemsNamedException if a component with a certain id cannot be found.
     */
    private void activateOrModificationRoutine(ConfigHydraulicStaticPosition config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.componentId = config.componentToControl();
        this.thermometerId = config.thermometerId();
        this.shouldCool = config.shouldCool();
        this.tolerance = config.tolerance();
        OpenemsComponent component = this.cpm.getComponent(config.componentToControl());
        if (component instanceof HydraulicComponent) {
            this.controlledComponent = this.cpm.getComponent(this.componentId);
        } else {
            throw new ConfigurationException("ActivationOrModifiedRoutine", config.componentToControl() + " Not an instance of HydraulicComponent");
        }
        ControlType controlTypeOfThisRun = this.getControlType();

        this.controlType = controlTypeOfThisRun != null ? controlTypeOfThisRun : ControlType.TEMPERATURE;

        component = this.cpm.getComponent(config.thermometerId());
        if (component instanceof Thermometer) {
            this.referenceThermometer = (Thermometer) component;
        } else {
            throw new ConfigurationException("Activate of HydraulicPositionController", "Instance of "
                    + config.thermometerId() + " is not a Thermometer");
        }

        ConfigurationException[] exceptions = {null};
        //Split entry: temperature:ValueOfValve
        Arrays.asList(config.temperaturePositionMap()).forEach(entry -> {
            if (exceptions[0] == null && entry.contains(CONFIGURATION_SPLITTER) && entry.equals("") == false) {
                try {
                    String[] entries = entry.split(CONFIGURATION_SPLITTER);
                    if (entries.length != ENTRY_LENGTH) {
                        throw new ConfigurationException("activate StaticValveController", "Entries: " + entries.length + " expected : " + ENTRY_LENGTH);
                    }
                    int temperature = Integer.parseInt(entries[0]);
                    double valvePosition = Double.parseDouble(entries[1]);
                    this.hydraulicPositionList.add(new HydraulicPosition(temperature, valvePosition));
                } catch (ConfigurationException e) {
                    exceptions[0] = e;
                }
            }
        });
        this.hydraulicPositionList.add(new HydraulicPosition(1000, config.defaultPosition()));
        if (exceptions[0] != null) {
            throw exceptions[0];
        }
        this.controlType = config.controlType();

        this.setAutoRun(config.autorun());
        this.closeWhenNeitherAutoRunNorEnableSignal = config.shouldCloseWhenNoSignal();
        this.forceAllowedChannel().setNextValue(config.allowForcing());
        this.useFallback = config.useFallback();
        this.setTimer(config);
        this.configSuccess = true;
    }

    /**
     * Sets the Timer for this component.
     *
     * @param config the configuration of this component.
     * @throws OpenemsError.OpenemsNamedException if the timer cannot be found.
     * @throws ConfigurationException             if the component can be found but not an instance of a Timer.
     */
    private void setTimer(ConfigHydraulicStaticPosition config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (this.timer != null) {
            this.timer.removeComponent();
        }
        this.timer = new TimerHandlerImpl(super.id(), this.cpm);
        this.timer.addOneIdentifier(MAX_WAIT_CYCLE_IDENTIFIER, config.timerForRunning(), config.waitForSignalAfterActivation());
        this.timer.addOneIdentifier(CHECK_FOR_COMPONENTS, config.timerForRunning(), DELTA_TIME_CHECK_COMPONENTS);
        if (config.useFallback()) {
            this.timer.addOneIdentifier(MIN_RUN_TIME_AFTER_FALLBACK_IDENTIFIER, config.timerForFallback(), config.fallbackRunTime());
        }
    }

    /**
     * Set the Valve Position by the given Temperature. Check available list and get the valvePosition by Temperature closest to given temperature.
     * <p>
     * For Heating:
     * Get the first entry of your position list. Iterate through configuration.
     * If the Reference temperature is higher than the Temperature of the Position, continue iteration and set a new SetPoint Position
     * when the Temperature is higher than the prev. SetPoint. Example:
     * Reference Temperature is 500dC; current position in iteration is 450 dC and selected position temp was 420dC -> select the new position with 450 dC.
     * However if current Position has a greater temperature Than reference temp -> 2 cases occur.
     * either : current selected Pos beneath temp -> take the new Position
     * OR if current pos has lower temp than the selected but is still above the ReferenceTemp--> select new
     * Example: Temperature 50; selected position 45; new has 55; take 55
     * new iteration temperature 50; selected 55; current is 52; take 52 position
     * </p>
     *
     * <p>
     * For Cooling:
     * This is equivalent to the heating process but inverse.
     * E.g.:
     * Reference is 350 dC the selectedPosition ist at 450dC and the new found position is at 420 dC -> take the 420dC
     * If the next Position found is at 300dC and the selected Position is 420dC take the 300dC
     * Next: if the new found position is 340dC and the selected position is still 300 dC while having a reference T of 350dC
     * take the 340 dC.
     * <p>
     * To avoid constant change of valve e.g.SetPoint is 30% but the exceeded the % value by 1.42314%...leave it.
     * that's why the HydraulicPosition has a {@link #tolerance}
     * </p>
     *
     * @param temperature the temperature the valveController orientates it's position.
     */
    private void setPositionByTemperature(int temperature) {
        if ((temperature != Integer.MIN_VALUE)) {
            AtomicReference<HydraulicPosition> selectedPosition = new AtomicReference<>();
            selectedPosition.set(this.hydraulicPositionList.get(0));
            if (!this.shouldCool) {
                this.hydraulicPositionList.forEach(hydraulicPosition -> {

                    if (hydraulicPosition.getTemperature() >= selectedPosition.get().getTemperature()
                            && selectedPosition.get().getTemperature() < temperature) {
                        selectedPosition.set(hydraulicPosition);
                    } else if (hydraulicPosition.getTemperature() >= temperature) {
                        if (hydraulicPosition.getTemperature() < selectedPosition.get().getTemperature()) {
                            selectedPosition.set(hydraulicPosition);
                        }
                    }
                });
            } else {
                this.hydraulicPositionList.forEach(hydraulicPosition -> {
                    if (hydraulicPosition.getTemperature() <= selectedPosition.get().getTemperature()
                            && selectedPosition.get().getTemperature() > temperature) {
                        selectedPosition.set(hydraulicPosition);
                    } else if (hydraulicPosition.getTemperature() <= temperature) {
                        if (hydraulicPosition.getTemperature() > selectedPosition.get().getTemperature()) {
                            selectedPosition.set(hydraulicPosition);
                        }
                    }
                });
            }
            double setPosition = selectedPosition.get().getHydraulicPosition();
            double currentPowerLevelValue = this.controlledComponent.getPowerLevelValue();
            if (Math.abs(currentPowerLevelValue - setPosition) > this.tolerance) {
                try {
                    this.controlledComponent.setPointPowerLevelChannel().setNextWriteValueFromObject(setPosition);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't set SetPoint for HydraulicComponent " + this.componentId);
                }
                this.setSetPointPosition((int) setPosition);
            }
        }
    }

    /**
     * Sets Position by concrete Percentage Value, written into the requested PositionChannel.
     *
     * @param percent the percent of requested Position.
     */
    private void setPositionByPercent(int percent) {
        if (this.controlledComponent.readyToChange() && percent != Integer.MIN_VALUE) {
            this.controlledComponent.setPointPowerLevelChannel().setNextValue(percent);
            this.setSetPointPosition(percent);
        }
    }

    /**
     * IF either autoRun or enable signal --> Check current Temperature (set by other component).
     * check for Forcing and if forceClose or ForceOpen is force the Valve
     * Otherwise: check for ControlType; Get the needed variable (percent or temperature)
     * and control Valve by variables (See setPositionBy Percent or setPositionByTemperature for more info)
     */
    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        //Check Requested Position
        if (this.configSuccess) {
            ControlType controlTypeOfThisRun = this.getControlType();

            this.controlType = controlTypeOfThisRun != null ? controlTypeOfThisRun : ControlType.TEMPERATURE;

            if (this.controlledComponent == null || this.referenceThermometer == null) {
                try {
                    OpenemsComponent componentToFetch;
                    componentToFetch = this.cpm.getComponent(this.config.componentToControl());
                    if (componentToFetch instanceof HydraulicComponent) {
                        this.controlledComponent = (HydraulicComponent) componentToFetch;
                    }
                    componentToFetch = this.cpm.getComponent(this.thermometerId);
                    if (componentToFetch instanceof Thermometer) {
                        this.referenceThermometer = (Thermometer) componentToFetch;
                    }
                } catch (Exception e) {
                    this.log.warn("Couldn't set Controlled Components of " + this.id());
                }
            } else {
                this.checkComponentsStillOk();
                if (this.isEnabledOrAutoRun()) {
                    this.isRunning = true;
                    //check for forceOpen/Close
                    if (forceAllowed()) {
                        if (isForcedOpen()) {
                            this.forceFullPower();
                        } else if (isForcedClose()) {
                            this.forceMinPower();
                        }
                    } else {
                        switch (this.controlType) {
                            case POSITION:
                                int percent = this.getRequestedComponentPosition();
                                this.setPositionByPercent(percent);
                                break;
                            case TEMPERATURE:
                                int temperature = this.referenceThermometer.getTemperatureValue();
                                this.setPositionByTemperature(temperature);
                                break;
                        }
                    }
                } else {
                    this.isRunning = false;
                    if (this.closeWhenNeitherAutoRunNorEnableSignal) {
                        if (this.controlledComponent.getPowerLevelValue() != HydraulicComponent.DEFAULT_MIN_POWER_VALUE) {
                            this.controlledComponent.setPointPowerLevelChannel().setNextValue(HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
                        }
                    }
                }
            }
        } else {
            try {
                this.activateOrModificationRoutine(this.config);
            } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
                this.configSuccess = false;
                this.log.warn("Configuration failed: " + e.getMessage());
            }
        }
    }

    /**
     * Checks if the Controller is allowed to Run (Either if it's autorun, the EnabledSignal is
     * Present AND true OR EnabledSignal is Missing AND fallback is active).
     *
     * @return enabled;
     */
    private boolean isEnabledOrAutoRun() {
        if (this.isAutorun()) {
            return true;
        } else {
            Optional<Boolean> enableSignal = this.getEnableSignalChannel().getNextWriteValueAndReset();
            if (enableSignal.isPresent()) {
                this.hadToFallbackBefore = false;
                if(this.useFallback) {
                    this.timer.resetTimer(MIN_RUN_TIME_AFTER_FALLBACK_IDENTIFIER);
                }
                this.timer.resetTimer(MAX_WAIT_CYCLE_IDENTIFIER);
                return enableSignal.get();
            } else {
                // run if no fallback & is running (means -> Signal was Present and Running -> but no new Value is present w.o. fallbackState)
                if (this.hadToFallbackBefore == false && this.isRunning) {
                    return this.timer.checkTimeIsUp(MAX_WAIT_CYCLE_IDENTIFIER) == false;
                }
                //Fallback -> MAxWaitCycle is up AND Duration is not reached
                return this.fallbackActivation();
            }
        }
    }

    /**
     * If Fallback is enabled. ->check if WaitCycles are at max is up and if that's the case check if Time is up.
     * If: currentCycle > Max --> Check if Time is up (Controller did run for a min) --> if Time up --> Return false and reset
     * else: return true
     *
     * @return boolean: fallbackResult.
     */

    private boolean fallbackActivation() {
        //First check Cycles
        if (this.useFallback) {
            if (this.hadToFallbackBefore == false) {
                this.hadToFallbackBefore = true;
                this.timer.resetTimer(MAX_WAIT_CYCLE_IDENTIFIER);
                return false;
            }
            //check if WaitTime is up and then run till min Run Time for Fallback is over
            if (this.timer.checkTimeIsUp(MAX_WAIT_CYCLE_IDENTIFIER)) {
                //tun till fallback is up
                if (this.timer.checkTimeIsUp(MIN_RUN_TIME_AFTER_FALLBACK_IDENTIFIER) == false) {
                    return true;
                } else {
                    //reset fallback
                    this.hadToFallbackBefore = false;
                    this.timer.resetTimer(MIN_RUN_TIME_AFTER_FALLBACK_IDENTIFIER);
                    this.timer.resetTimer(MAX_WAIT_CYCLE_IDENTIFIER);
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Set the HydraulicComponent with force to {@link HydraulicComponent#DEFAULT_MIN_POWER_VALUE}.
     */
    private void forceMinPower() {
        if (this.controlledComponent.readyToChange()) {
            this.controlledComponent.forceClose();
            this.isForcedCloseChannel().setNextValue(false);
        }
    }

    /**
     * Set the HydraulicComponent with force to {@link HydraulicComponent#DEFAULT_MAX_POWER_VALUE}.
     */
    private void forceFullPower() {
        if (this.controlledComponent.readyToChange()) {
            this.controlledComponent.forceOpen();
            this.isForcedOpenChannel().setNextValue(false);
        }
    }

    /**
     * Every {@link #DELTA_TIME_CHECK_COMPONENTS} check if the references of the Components are still ok.
     * An refresh references.
     */
    private void checkComponentsStillOk() {
        if (this.timer.checkTimeIsUp(MAX_WAIT_CYCLE_IDENTIFIER)) {
            this.timer.resetTimer(MAX_WAIT_CYCLE_IDENTIFIER);
            OpenemsComponent component;
            try {
                component = this.cpm.getComponent(this.controlledComponent.id());
                if (component instanceof HydraulicComponent && (component.equals(this.controlledComponent) == false)) {
                    this.controlledComponent = (HydraulicComponent) component;
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't find HydraulicComponent with id: " + this.controlledComponent.id());
            }
            try {
                component = this.cpm.getComponent(this.referenceThermometer.id());
                if (component instanceof Thermometer && (component.equals(this.referenceThermometer) == false)) {
                    this.referenceThermometer = (Thermometer) component;
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't find Thermometer with id: " + this.controlledComponent.id());
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        this.timer.removeComponent();
        super.deactivate();
    }

    @Override
    public double getCurrentPositionOfComponent() {
        return this.controlledComponent.getPowerLevelValue();
    }
}
