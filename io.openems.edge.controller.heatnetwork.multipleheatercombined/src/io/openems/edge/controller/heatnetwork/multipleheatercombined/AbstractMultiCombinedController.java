package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heater.api.Cooler;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerType;
import io.openems.edge.thermometer.api.ThermometerWrapper;
import io.openems.edge.thermometer.api.ThermometerWrapperForCoolingImpl;
import io.openems.edge.thermometer.api.ThermometerWrapperForHeatingImpl;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The Abstract Class of a MultiHeater/Cooler Controller. It Provides the basic ability to enable/disable Heater/Cooler.
 * Depends on activation/Deactivation Temperatures and thermometers.
 * This class gets n ActivationThermometer, n DeactivationThermometer, n Heater XOR Cooler and n Deactivation Temperatures as well as
 * n ActivationThermometer.
 * Check for each Heater if the Deactivation or Activation Conditions are met.
 */
public abstract class AbstractMultiCombinedController extends AbstractOpenemsComponent implements OpenemsComponent {

    private final Logger log = LoggerFactory.getLogger(MultipleHeaterCombinedControllerImpl.class);

    @Reference
    protected ComponentManager cpm;

    private final Map<Heater, ThermometerWrapper> heaterTemperatureWrapperMap = new HashMap<>();
    private final List<Heater> configuredHeater = new ArrayList<>();
    private final Map<Heater, ActiveWrapper> activeStateHeaterAndHeatWrapper = new HashMap<>();
    protected boolean configurationSuccess;

    private boolean useTimer;
    private TimerHandler timer;
    private final String identifier = "MULTI_COMBINED_IDENTIFIER";
    protected ControlType controlType = ControlType.HEATER;
    protected AtomicBoolean heaterError = new AtomicBoolean(false);
    protected AtomicBoolean isHeatingOrCooling = new AtomicBoolean(false);

    public AbstractMultiCombinedController(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                                           io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    void activate(ComponentContext context, String id, String alias, boolean enabled, boolean useTimer, String timerId,
                  int deltaTime, ControlType controlType, String[] heaterIds,
                  String[] activationThermometers, String[] activationTemperatures,
                  String[] deactivationThermometers, String[] deactivationTemperatures) {

        super.activate(context, id, alias, enabled);
        this.useTimer = useTimer;
        //----------------------ALLOCATE/ CONFIGURE HEATER/TemperatureSensor -----------------//
        try {
            this.allocateConfig(controlType, timerId, deltaTime, heaterIds, activationThermometers, activationTemperatures,
                    deactivationThermometers, deactivationTemperatures);
            this.configurationSuccess = true;
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configurationSuccess = false;
        }

    }

    void modified(ComponentContext context, String id, String alias, boolean enabled, boolean useTimer, String timerId,
                  int deltaTime, ControlType controlType, String[] heaterIds,
                  String[] activationThermometers, String[] activationTemperatures,
                  String[] deactivationThermometers, String[] deactivationTemperatures) {
        super.modified(context, id, alias, enabled);
        this.configuredHeater.clear();
        this.activeStateHeaterAndHeatWrapper.clear();
        this.heaterTemperatureWrapperMap.clear();
        this.useTimer = useTimer;
        if (this.timer != null) {
            this.timer.removeComponent();
        }
        try {
            this.allocateConfig(controlType, timerId, deltaTime, heaterIds, activationThermometers, activationTemperatures, deactivationThermometers,
                    deactivationTemperatures);
            this.configurationSuccess = true;
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configurationSuccess = false;
        }
    }

    // ------------------- Config Related --------------------- //

    /**
     * Allocates the config of each Heater. They will be mapped to wrapper classes for easier handling and functions etc.
     *
     * @param controlType              is the Controller for Heating or Cooling
     * @param timerId                  the Id of the Timer that is configured.
     * @param deltaTime                the deltaTime used by the Controller.
     * @param heater_id                the id of the heater
     * @param activationThermometer    TemperatureSensor for minimum Temperature
     * @param activationTemperatures   TemperatureValue for min Temp.
     * @param deactivationThermometer  TemperatureSensor for MaximumTemp.
     * @param deactivationTemperatures TemperatureValue max allowed.
     * @throws OpenemsError.OpenemsNamedException if Id not found
     * @throws ConfigurationException             if instanceof is wrong.
     */
    protected void allocateConfig(ControlType controlType, String timerId, int deltaTime, String[] heater_id, String[] activationThermometer,
                                  String[] activationTemperatures, String[] deactivationThermometer,
                                  String[] deactivationTemperatures) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (this.configEntriesDifferentSize(heater_id.length, activationThermometer.length, activationTemperatures.length,
                deactivationThermometer.length, deactivationTemperatures.length)) {
            throw new ConfigurationException("allocate Config of MultipleHeaterCombined: " + super.id(), "Check Config Size Entries!");
        }
        this.controlType = controlType;
        if (this.useTimer) {
            this.timer = new TimerHandlerImpl(this.id(), this.cpm);
            this.timer.addOneIdentifier(this.identifier, timerId, deltaTime);
        }

        List<String> heaterIds = Arrays.asList(heater_id);
        OpenemsError.OpenemsNamedException[] ex = {null};
        ConfigurationException[] exC = {null};
        Arrays.stream(heater_id).forEach(entry -> {
            try {
                if ((ex[0] == null && exC[0] == null)) {
                    OpenemsComponent component = this.cpm.getComponent(entry);
                    int index = heaterIds.indexOf(entry);
                    if (component instanceof Heater
                            && (this.controlType.equals(ControlType.HEATER)
                            || (this.controlType.equals(ControlType.COOLER) && component instanceof Cooler))) {
                        Heater heater = ((Heater) component);
                        if (this.configuredHeater.stream().anyMatch(existingHeater -> existingHeater.id().equals(heater.id()))) {
                            this.allocateConfigHadError();
                            throw new ConfigurationException("Allocate Config " + super.id(), "Heater already configured with id: " + heater.id() + " in : " + super.id());
                        }
                        if (Arrays.toString(activationThermometer).contains(Arrays.toString(deactivationThermometer))) {
                            this.allocateConfigHadError();
                            throw new ConfigurationException("Allocate Config " + super.id(), "One or more TemperatureSensors where used as Activation and Deactivation Thermometer.");
                        }
                        this.configuredHeater.add(heater);
                        this.activeStateHeaterAndHeatWrapper.put(heater, new ActiveWrapper());
                        this.heaterTemperatureWrapperMap.put(heater, this.createTemperatureWrapper(activationThermometer[index], activationTemperatures[index], deactivationThermometer[index], deactivationTemperatures[index]));
                    } else {
                        this.allocateConfigHadError();
                        String errorMessage = "HeaterId not an instance of Heater";
                        if (this.controlType.equals(ControlType.COOLER)) {
                            errorMessage += " or Not an Instance of Cooler";
                        }
                        throw new ConfigurationException("MultipleHeaterCombined: AllocateConfig " + super.id(), errorMessage);
                    }
                }

            } catch (OpenemsError.OpenemsNamedException e) {
                this.allocateConfigHadError();
                ex[0] = e;
            } catch (ConfigurationException e) {
                this.allocateConfigHadError();
                exC[0] = e;
            }

        });
        if (ex[0] != null) {
            throw ex[0];
        }
        if (exC[0] != null) {
            throw exC[0];
        }

    }

    /**
     * This will be called if an Error Occurred while applying the Config.
     * It clears Lists and Maps that might be initialized to prevent lingering Heater from an invalid Config.
     */
    private void allocateConfigHadError() {
        this.configuredHeater.clear();
        this.heaterTemperatureWrapperMap.clear();
        this.activeStateHeaterAndHeatWrapper.clear();
    }

    /**
     * Get the Size from config Arrays such as HeaterIds, Temperatures, and Thermometers.
     *
     * @param entrySize the length of each config Array that's needed.
     * @return true if a size difference occurred.
     */

    private boolean configEntriesDifferentSize(Integer... entrySize) {
        List<Integer> comparison = Arrays.asList(entrySize);
        AtomicInteger comparedLength = new AtomicInteger(entrySize.length > 0 ? entrySize[0] : 0);
        AtomicBoolean different = new AtomicBoolean(false);
        comparison.forEach(entry -> {
            if (!different.get() && comparedLength.get() != entry) {
                different.set(true);
            }
        });
        return different.get();
    }


    /**
     * Creates A Thermometer Wrapper for the Corresponding Heater.
     * Thermometer wrapper Holds Information of min/max thermometer and min max Temp value as well as some helper Methods.
     *
     * @param temperatureSensorMin the min Temperature Sensor for the heater <- Activation Thermometer
     * @param temperatureMin       the min Temperature that needs to be reached at least <- Activation Threshold
     * @param temperatureSensorMax the max Temperature Sensor for the heater <- Deactivation Thermometer
     * @param temperatureMax       the max Temperature that allowed <- Deactivation Threshold
     * @return the Thermometer Wrapper for the Heater
     * @throws OpenemsError.OpenemsNamedException if Ids cannot be found
     * @throws ConfigurationException             if ThermometerIds not an Instance of Thermometer
     */
    private ThermometerWrapper createTemperatureWrapper(String temperatureSensorMin,
                                                        String temperatureMin, String temperatureSensorMax, String temperatureMax)
            throws OpenemsError.OpenemsNamedException, ConfigurationException {
        Thermometer min;
        Thermometer max;
        ThermometerWrapper wrapper;
        if (this.cpm.getComponent(temperatureSensorMin) instanceof Thermometer) {
            min = this.cpm.getComponent(temperatureSensorMin);
        } else {
            throw new ConfigurationException("createTemperatureWrapper", temperatureSensorMin + " is not an Instance of Thermometer");
        }
        if (this.cpm.getComponent(temperatureSensorMax) instanceof Thermometer) {
            max = this.cpm.getComponent(temperatureSensorMax);
        } else {
            throw new ConfigurationException("createTemperatureWrapper", temperatureSensorMax + " is not an Instance of Thermometer");
        }
        switch (this.controlType) {

            case HEATER:
                wrapper = new ThermometerWrapperForHeatingImpl(min, max, temperatureMin, temperatureMax, this.cpm);
                break;
            case COOLER:
                wrapper = new ThermometerWrapperForCoolingImpl(min, max, temperatureMin, temperatureMax, this.cpm);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + this.controlType);
        }
        return wrapper;
    }


    /**
     * MultipleHeaterCombined logic.
     * <p>
     * Each Heater got a Temperature where they should activate and deactivate.
     * When the activationThermometer reaches the activationTemperature, the Heater XOR Cooler activates / {@link Heater#getEnableSignalChannel()}
     * will receive a nextWriteValue of true.
     * The Heater XOR Cooler stays active, until the deactivationThermometer reaches the deactivationTemperature.
     * When the Deactivation Temperature is reached, the {@link Heater#getEnableSignalChannel()} won't be set in any way.
     * This way the Heater stays deactivated, until the activationTemperature is reached again (and the Deactivation Conditions are not met)
     * </p>
     */

    protected void abstractRun() {
        this.checkMissingThermometer();
        this.checkMissingHeaterComponents();
        this.heaterError.set(false);
        this.isHeatingOrCooling.set(false);

        this.configuredHeater.forEach(heater -> {
            if (heater.getHeaterState().isDefined() && heater.getHeaterState().get().equals(HeaterState.BLOCKED_OR_ERROR.getValue())) {
                this.heaterError.set(true);
            }

            //ThermometerWrapper holding min and max values as well as Thermometer corresponding to the heater
            ThermometerWrapper thermometerWrapper = this.heaterTemperatureWrapperMap.get(heater);
            //HeatWrapper holding activeState and alwaysActive
            ActiveWrapper heaterActiveWrapper = this.activeStateHeaterAndHeatWrapper.get(heater);
            //Get the WrapperClass and check if Heater should be turned of, as well as Checking performance demand
            //HeatControl                                           PerformanceDemand + Time Control
            //Enable
            try {
                if (thermometerWrapper.shouldDeactivate()) {
                    heaterActiveWrapper.setActive(false);
                    //Check wrapper if thermometer below min temp
                } else if (thermometerWrapper.shouldActivate()) {
                    heaterActiveWrapper.setActive(true);
                }

                if (heaterActiveWrapper.isActive()) {
                    if (!this.useTimer || this.timer.checkTimeIsUp(this.identifier)) {
                        heater.getEnableSignalChannel().setNextWriteValue(heaterActiveWrapper.isActive());
                        this.isHeatingOrCooling.set(true);
                        if (this.useTimer) {
                            this.timer.resetTimer(this.identifier);
                        }
                    }
                } else if (this.useTimer) {
                    this.timer.resetTimer(this.identifier);
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.heaterError.set(true);
                this.log.warn("Couldn't set the enableSignal: " + super.id() + " Reason: " + e.getMessage());
            } catch (ConfigurationException e) {
                this.heaterError.set(true);
                this.log.warn("Couldn't read from Configured TemperatureChannel of Heater: " + heater.id());
            }
        });
    }

    /**
     * Checks for missing Components (Heater).
     * Refresh the references.
     */
    private void checkMissingHeaterComponents() {
        List<Heater> missingHeater = new ArrayList<>();
        this.heaterTemperatureWrapperMap.forEach((heater, wrapper) -> {
            if (heater.isEnabled() == false) {
                missingHeater.add(heater);
            }
        });
        if (missingHeater.size() > 0) {
            missingHeater.forEach(heaterOfMissingHeaterList -> {
                try {
                    //get new heater
                    OpenemsComponent component = this.cpm.getComponent(heaterOfMissingHeaterList.id());
                    if (component instanceof Heater) {
                        Heater newHeater = (Heater) component;
                        AtomicReference<Heater> oldHeater = new AtomicReference<>();
                        AtomicBoolean heaterFound = new AtomicBoolean(false);
                        //get old heater
                        this.heaterTemperatureWrapperMap.keySet().forEach(heaterKey -> {
                            if (heaterFound.get() == false && heaterKey.id().equals(newHeater.id())) {
                                heaterFound.set(true);
                                oldHeater.set(heaterKey);
                            }
                        });
                        //replace old heater in temperatureWrapperMap
                        ThermometerWrapper wrapperOfMap = this.heaterTemperatureWrapperMap.get(oldHeater.get());
                        this.heaterTemperatureWrapperMap.remove(oldHeater.get());
                        this.heaterTemperatureWrapperMap.put(newHeater, wrapperOfMap);
                        //replace in old HeaterActiveWrapperMap
                        ActiveWrapper wrapper = this.activeStateHeaterAndHeatWrapper.get(oldHeater.get());
                        this.activeStateHeaterAndHeatWrapper.remove(oldHeater.get());
                        this.activeStateHeaterAndHeatWrapper.put(newHeater, wrapper);
                        //replace old Heater
                        AtomicInteger index = new AtomicInteger(-1);
                        this.configuredHeater.stream().filter(entry ->
                                entry.id().equals(newHeater.id())).findFirst().ifPresent(entry -> index.set(this.configuredHeater.indexOf(entry)));
                        if (index.get() >= 0) {
                            this.configuredHeater.remove(index.get());
                            this.configuredHeater.add(newHeater);
                        }

                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Heater: " + heaterOfMissingHeaterList.id() + " is Missing! It won't run/heat anymore!");
                }
            });
        }
    }

    /**
     * Check for missing Thermometer. Swap old and New References within ThermometerWrapper.
     */

    private void checkMissingThermometer() {
        this.heaterTemperatureWrapperMap.forEach((heater, wrapper) -> {
            Thermometer max = wrapper.getDeactivationThermometer();
            Thermometer min = wrapper.getActivationThermometer();
            Thermometer maxNew;
            Thermometer minNew;

            if (max.isEnabled() == false) {
                try {
                    OpenemsComponent component = this.cpm.getComponent(max.id());

                    if (component instanceof Thermometer) {
                        maxNew = (Thermometer) component;
                        wrapper.renewThermometer(ThermometerType.DEACTIVATE_THERMOMETER, maxNew);
                    } else {
                        this.log.warn("New Instance of Thermometer : " + max.id() + " is not a Thermometer. MultiHeater won't work!");
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't find Max Thermometer : " + max.id() + " MultiHeater might not be working!");
                }
            }
            if (min.isEnabled() == false) {
                try {
                    OpenemsComponent component = this.cpm.getComponent(min.id());

                    if (component instanceof Thermometer) {
                        minNew = (Thermometer) component;
                        wrapper.renewThermometer(ThermometerType.ACTIVATE_THERMOMETER, minNew);
                    } else {
                        this.log.warn("New Instance of Thermometer : " + min.id() + " is not a Thermometer. MultiHeater won't work!");
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't find Max Thermometer : " + min.id() + " MultiHeater might not be working!");
                }

            }
        });
    }
}
