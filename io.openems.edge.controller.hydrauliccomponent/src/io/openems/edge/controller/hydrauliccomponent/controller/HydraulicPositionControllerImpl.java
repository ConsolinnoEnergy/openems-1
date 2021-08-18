package io.openems.edge.controller.hydrauliccomponent.controller;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.hydrauliccomponent.api.ControlType;
import io.openems.edge.controller.hydrauliccomponent.api.HydraulicController;
import io.openems.edge.controller.hydrauliccomponent.api.HydraulicPosition;
import io.openems.edge.controller.hydrauliccomponent.api.HydraulicPositionController;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.thermometer.api.Thermometer;

import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Hydraulic.Position", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class HydraulicPositionControllerImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, HydraulicPositionController {

    private final Logger log = LoggerFactory.getLogger(HydraulicPositionControllerImpl.class);

    private static final int ENTRY_LENGTH = 2;
    @Reference
    ComponentManager cpm;

    private HydraulicComponent controlledComponent;
    private final List<HydraulicPosition> hydraulicPositionList = new ArrayList<>();
    private ControlType controlType;
    private boolean closeWhenNeitherAutoRunNorEnableSignal;
    //Can be ANY Thermometer! (VirutalThermometer would be the best)
    private Thermometer referenceThermometer;
    private boolean useFallback;
    //Wait this amount of cycles if no EnabledSignal is present!
    private static final String MAX_WAIT_CYCLE_IDENTIFIER = "VALVE_CONTROLLER_STATIC_MAX_WAIT_CYCLE_IDENTIFIER";
    //if enabled signal stays null this component runs for this amount of time:
    private static final String MIN_RUN_TIME_AFTER_FALLBACK_IDENTIFIER = "VALVE_CONTROLLER_STATIC_MIN_RUN_TIME_IDENTIFIER";
    private TimerHandler timer;
    private boolean isRunning = false;


    private DateTime initialTimeStamp;
    private boolean hadToFallbackBefore;

    public HydraulicPositionControllerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                HydraulicController.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {

        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.enabled() == false) {
            return;
        }
        if (this.cpm.getComponent(config.componentToControl()) instanceof HydraulicComponent) {

            this.controlledComponent = this.cpm.getComponent(config.componentToControl());
        } else {
            throw new ConfigurationException("ActivateMethod HydraulicPositionController", config.componentToControl() + " Not an instance of HydraulicComponent");
        }
        ControlType controlTypeOfThisRun = this.getControlType();

        this.controlType = controlTypeOfThisRun != null ? controlTypeOfThisRun : ControlType.TEMPERATURE;


        OpenemsComponent componentToFetch = this.cpm.getComponent(config.thermometerId());
        if (componentToFetch instanceof Thermometer) {
            this.referenceThermometer = (Thermometer) componentToFetch;
        } else {
            throw new ConfigurationException("Activate of HydraulicPositionController", "Instance of "
                    + config.thermometerId() + " is not a Thermometer");
        }
        ConfigurationException[] exceptions = {null};
        //Split entry: temperature:ValueOfValve
        Arrays.asList(config.temperaturePositionMap()).forEach(entry -> {
            if (exceptions[0] == null && entry.contains(":") && entry.equals("") == false) {
                try {
                    String[] entries = entry.split(":");
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
        if (ControlType.contains(config.controlType().toUpperCase().trim())) {
            this.controlType = ControlType.valueOf(config.controlType().toUpperCase().trim());
        } else {
            throw new ConfigurationException("ControlTypeConfig", config.controlType() + " does not exist");
        }
        this.setAutoRun(config.autorun());
        this.closeWhenNeitherAutoRunNorEnableSignal = config.shouldCloseWhenNoSignal();
        this.forceAllowedChannel().setNextValue(config.allowForcing());
        this.useFallback = config.useFallback();
        this.setTimer(config);
    }

    private void setTimer(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.timer = new TimerHandlerImpl(super.id(), this.cpm);
        this.timer.addOneIdentifier(MAX_WAIT_CYCLE_IDENTIFIER, config.timerForRunning(), config.waitForSignalAfterActivation());
        if (config.useFallback()) {
            this.timer.addOneIdentifier(MIN_RUN_TIME_AFTER_FALLBACK_IDENTIFIER, config.timerForFallback(), config.fallbackRunTime());
        }
    }

    /**
     * Set the Valve Position by the given Temperature. Check available list and get the valvePosition by Temperature closest to given temperature.
     *
     * @param temperature the temperature the valveController orientates it's position.
     */
    private void setPositionByTemperature(int temperature) {
        if ((temperature == Integer.MIN_VALUE) == false) {
            AtomicReference<HydraulicPosition> selectedPosition = new AtomicReference<>();
            selectedPosition.set(this.hydraulicPositionList.get(0));
            this.hydraulicPositionList.forEach(hydraulicPosition -> {
                //As long as position Temperature < current Temp && position temperature greater than current Position temp.
                // e.g. Temperature is 50; current position in iteration is 45 and selected position temp was 42
                if (hydraulicPosition.getTemperature() <= temperature && hydraulicPosition.getTemperature() > selectedPosition.get().getTemperature()) {
                    selectedPosition.set(hydraulicPosition);
                    //if current Position is greater Than temp -> check for either : selected Pos beneath temp -> select current position
                    // OR if current pos has lower temp but selected is greater than current --> select current
                    //Example: Temperature 50; selected position 45; new has 55; take 55
                    //new iteration temperature 50; selected 55; current is 52; take 52 position
                } else if (hydraulicPosition.getTemperature() >= temperature) {
                    if (hydraulicPosition.getTemperature() <= selectedPosition.get().getTemperature()
                            && (selectedPosition.get().getTemperature() < temperature || hydraulicPosition.getTemperature() < selectedPosition.get().getTemperature())) {
                        selectedPosition.set(hydraulicPosition);
                    }
                }
            });
            double setPosition = selectedPosition.get().getHydraulicPosition();
            try {
                this.controlledComponent.setPointPowerLevelChannel().setNextWriteValueFromObject(setPosition);
            } catch (OpenemsError.OpenemsNamedException e) {
                e.printStackTrace();
            }
            this.log.info("Setting: " + this.controlledComponent.id() + " to : " + setPosition);
            this.setSetPointPosition((int) setPosition);
        }

    }

    /**
     * Sets Position by concrete Percentage Value, written into the requested PositionChannel.
     *
     * @param percent the percent of requested Position.
     */
    private void setPositionByPercent(int percent) throws OpenemsError.OpenemsNamedException {
        if (this.controlledComponent.readyToChange() && percent != Integer.MIN_VALUE) {
            this.controlledComponent.setPointPowerLevelChannel().setNextValue(percent);
            this.setSetPointPosition(percent);
            if (this.controlledComponent.powerLevelReached()) {
                try {
                    this.getRequestedPositionChannel().setNextWriteValue(null);
                } catch (OpenemsError.OpenemsNamedException ignored) {
                }
            }

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
        checkComponentsStillEnabled();
        //TODO Do getNextWriteValueAndReset!
        if (this.isEnabledOrAutoRun()) {
            this.isRunning = true;
            //check for forceOpen/Close
            if (forceAllowed()) {
                if (isForcedOpen()) {
                    this.forceOpenValve();
                } else if (isForcedClose()) {
                    this.forceCloseValve();
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
                if (this.controlledComponent.getPowerLevelValue() != 0) {
                    this.controlledComponent.setPointPowerLevelChannel().setNextValue(0);
                }
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
                this.timer.resetTimer(MIN_RUN_TIME_AFTER_FALLBACK_IDENTIFIER);
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
     * .
     * If Fallback is enabled ->check if WaitCycles are at max is up and if that's the case check if Time is up
     * if: currentCycle > Max --> Check if Time is up (Controller did run for a min) --> if Time up --> Return false and reset
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
                if (this.timer
                        .checkTimeIsUp(MIN_RUN_TIME_AFTER_FALLBACK_IDENTIFIER) == false) {
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

    private void forceCloseValve() throws OpenemsError.OpenemsNamedException {
        if (this.controlledComponent.readyToChange()) {
            this.controlledComponent.forceClose();
            this.isForcedCloseChannel().setNextValue(false);
        }
    }

    private void forceOpenValve() throws OpenemsError.OpenemsNamedException {
        if (this.controlledComponent.readyToChange()) {
            this.controlledComponent.forceOpen();
            this.isForcedOpenChannel().setNextValue(false);
        }
    }

    private void checkComponentsStillEnabled() {
        try {
            if (this.controlledComponent.isEnabled() == false) {
                if (this.cpm.getComponent(this.controlledComponent.id()) instanceof HydraulicComponent) {
                    this.controlledComponent = this.cpm.getComponent(this.controlledComponent.id());
                }
            }
            if (this.referenceThermometer.isEnabled() == false) {
                if (this.cpm.getComponent(this.referenceThermometer.id()) instanceof Thermometer) {
                    this.referenceThermometer = this.cpm.getComponent(this.referenceThermometer.id());
                }
            }
        } catch (OpenemsError.OpenemsNamedException ignored) {
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        this.timer.removeComponent();
    }

    @Override
    public double getPositionByTemperature(int temperature) {
        //TODO not important atm
        return 0;
    }

    @Override
    public int getTemperatureByPosition(double position) {
        //TODO not important atm
        return 0;
    }

    @Override
    public void addPositionByTemperatureAndPosition(int temperature, int hydraulicPosition) {
        Optional<HydraulicPosition> containingPosition = this.hydraulicPositionList.stream().filter(position -> position.getTemperature() == temperature).findFirst();
        if (containingPosition.isPresent()) {
            containingPosition.get().setHydraulicPosition(hydraulicPosition);
        } else {
            this.hydraulicPositionList.add(new HydraulicPosition(temperature, hydraulicPosition));
        }
    }

    @Override
    public double getCurrentPositionOfComponent() {
        return this.controlledComponent.getPowerLevelValue();
    }
}
