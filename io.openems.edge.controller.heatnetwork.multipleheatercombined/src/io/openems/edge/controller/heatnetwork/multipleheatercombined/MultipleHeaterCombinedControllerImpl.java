package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.multipleheatercombined.api.MultipleHeaterCombinedController;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerType;
import io.openems.edge.thermometer.api.ThermometerWrapperImpl;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The MultipleHeaterCombined controller allows the monitoring and enabling of any {@link Heater}.
 * Each Heater gets an activation and deactivation Thermometer as well as temperature.
 * For Example: Heater A has an (activation) Thermometer B and a (deactivation) Thermometer C with an activation Temperature
 * of 400dC and a deactivation Temperature of 600dC.
 * The thermometer C will be checked if it's Temperature is > than the deactivation Temp. of 600 dC.
 * If so -> disable the Heater (Don't write in the enable Signal) -> set the {@link HeaterActiveWrapper#setActive(boolean)}}
 * to false therefore don't write in the corresponding heater EnableSignal Channel.
 * Else if the Activation Thermometer B is beneath the activation Temperature of 400dC set the {@link HeaterActiveWrapper#setActive(boolean)}
 * to true and therefore write into the heater EnableSignal Channel.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "MultipleHeaterCombined",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class MultipleHeaterCombinedControllerImpl extends AbstractOpenemsComponent implements OpenemsComponent,
        Controller, MultipleHeaterCombinedController {

    private final Logger log = LoggerFactory.getLogger(MultipleHeaterCombinedControllerImpl.class);

    @Reference
    protected ComponentManager cpm;

    private final Map<Heater, ThermometerWrapperImpl> heaterTemperatureWrapperMap = new HashMap<>();
    private final List<Heater> configuredHeater = new ArrayList<>();
    private final Map<Heater, HeaterActiveWrapper> activeStateHeaterAndHeatWrapper = new HashMap<>();
    private boolean configurationSuccess;
    private final AtomicInteger configurationCounter = new AtomicInteger(0);
    private static final int MAX_WAIT_COUNT = 10;

    private boolean useTimer;
    private TimerHandler timer;
    private final String timerId = "overWatch";
    private String timerType;


    private Config config;

    public MultipleHeaterCombinedControllerImpl() {

        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                MultipleHeaterCombinedController.ChannelId.values());

    }

    @Activate
    void activate(ComponentContext context, Config config) {

        super.activate(context, config.id(), config.alias(), config.enabled());
        this.setIsHeating(false);
        this.setHasError(false);
        this.setIsOk(true);
        this.config = config;
        this.useTimer = config.useTimer();

        //----------------------ALLOCATE/ CONFIGURE HEATER/TemperatureSensor -----------------//
        try {
            this.allocateConfig(config.heaterIds(), config.activationThermometers(), config.activationTemperatures(),
                    config.deactivationThermometers(), config.deactivationTemperatures());
            this.configurationSuccess = true;
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configurationSuccess = false;
        }

    }

    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        this.useTimer = config.useTimer();
        try {
            this.allocateConfig(config.heaterIds(), config.activationThermometers(), config.activationTemperatures(), config.deactivationThermometers(),
                    config.deactivationTemperatures());
            this.configurationSuccess = true;
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configurationSuccess = false;
        }
    }

    // ------------------- Config Related --------------------- //

    /**
     * Allocates the config of each Heater. They will be mapped to wrapper classes for easier handling and functions etc.
     *
     * @param heater_id            the id of the heater
     * @param temperatureSensorMin TemperatureSensor for minimum Temperature
     * @param temperatureMin       TemperatureValue for min Temp.
     * @param temperatureSensorMax TemperatureSensor for MaximumTemp.
     * @param temperatureMax       TemperatureValue max allowed.
     * @throws OpenemsError.OpenemsNamedException if Id not found
     * @throws ConfigurationException             if instanceof is wrong.
     */
    private void allocateConfig(String[] heater_id, String[] temperatureSensorMin,
                                String[] temperatureMin, String[] temperatureSensorMax,
                                String[] temperatureMax) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (this.configEntriesDifferentSize(heater_id.length, temperatureSensorMin.length, temperatureMin.length,
                temperatureSensorMax.length, temperatureMax.length)) {
            throw new ConfigurationException("allocate Config of MultipleHeaterCombined: " + super.id(), "Check Config Size Entries!");
        }
        if (this.useTimer) {
            this.timer = new TimerHandlerImpl(this.config.id(), this.cpm);
            this.timerType = this.config.timerId();
            this.timer.addOneIdentifier(this.timerId, this.timerType, this.config.timeDelta());
        }

        List<String> heaterIds = Arrays.asList(heater_id);
        OpenemsError.OpenemsNamedException[] ex = {null};
        ConfigurationException[] exC = {null};
        Arrays.stream(heater_id).forEach(entry -> {
            try {
                if ((ex[0] == null && exC[0] == null)) {
                    OpenemsComponent component = this.cpm.getComponent(entry);
                    int index = heaterIds.indexOf(entry);
                    if (component instanceof Heater) {
                        Heater heater = ((Heater) component);

                        if (this.configuredHeater.stream().anyMatch(existingHeater -> existingHeater.id().equals(heater.id()))) {
                            this.allocateConfigHadError();
                            throw new ConfigurationException("Allocate Config " + super.id(), "Heater already configured with id: " + heater.id() + " in : " + super.id());
                        }
                        if (Arrays.toString(temperatureSensorMin).contains(Arrays.toString(temperatureSensorMax))) {
                            this.allocateConfigHadError();
                            throw new ConfigurationException("Allocate Config " + super.id(), "One or more TemperatureSensors where used as Activation and Deactivation Thermometer.");
                        }
                        this.configuredHeater.add(heater);
                        this.activeStateHeaterAndHeatWrapper.put(heater, new HeaterActiveWrapper());
                        this.heaterTemperatureWrapperMap.put(heater, this.createTemperatureWrapper(temperatureSensorMin[index], temperatureMin[index], temperatureSensorMax[index], temperatureMax[index]));
                    } else {
                        this.allocateConfigHadError();
                        throw new ConfigurationException("MultipleHeaterCombined: AllocateConfig " + super.id(), "HeaterId not an instance of Heater");
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
    private ThermometerWrapperImpl createTemperatureWrapper(String temperatureSensorMin,
                                                            String temperatureMin, String temperatureSensorMax, String temperatureMax)
            throws OpenemsError.OpenemsNamedException, ConfigurationException {
        Thermometer min;
        Thermometer max;
        ThermometerWrapperImpl wrapper;
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
        wrapper = new ThermometerWrapperImpl(min, max, temperatureMin, temperatureMax, this.cpm);

        return wrapper;
    }


    /**
     * MultipleHeaterCombined logic.
     * <p>
     * Each Heater got a Temperature where they should activate and deactivate.
     * When the activationThermometer reaches the activationTemperature, the Heater activates / {@link Heater#getEnableSignalChannel()}
     * will receive a nextWriteValue of true.
     * The Heater stays active, till the deactivationThermometer reaches the deactivationTemperature.
     * When the Deactivation Temperature is reached, the {@link Heater#getEnableSignalChannel()} won't be set in any way.
     * This way the Heater stays deactivated, until the activationTemperature is reached again.
     * </p>
     */
    @Override
    public void run() {
        if (this.configurationSuccess) {
            this.checkMissingThermometer();
            this.checkMissingHeaterComponents();
            AtomicBoolean heaterError = new AtomicBoolean(false);
            AtomicBoolean isHeating = new AtomicBoolean(false);

            this.configuredHeater.forEach(heater -> {
                if (heater.getHeaterState().isDefined() && heater.getHeaterState().get().equals(HeaterState.BLOCKED_OR_ERROR.getValue())) {
                    heaterError.set(true);
                }

                //ThermometerWrapper holding min and max values as well as Thermometer corresponding to the heater
                ThermometerWrapperImpl thermometerWrapper = this.heaterTemperatureWrapperMap.get(heater);
                //HeatWrapper holding activeState and alwaysActive
                HeaterActiveWrapper heaterActiveWrapper = this.activeStateHeaterAndHeatWrapper.get(heater);
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
                        if (!this.useTimer || this.timer.checkTimeIsUp(this.timerId)) {
                            heater.getEnableSignalChannel().setNextWriteValue(heaterActiveWrapper.isActive());
                            isHeating.set(true);
                            if (this.useTimer) {
                                this.timer.resetTimer(this.timerId);
                            }
                        }
                    } else if (this.useTimer) {
                        this.timer.resetTimer(this.timerId);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    heaterError.set(true);
                    this.log.warn("Couldn't set the enableSignal: " + super.id() + " Reason: " + e.getMessage());
                } catch (ConfigurationException e) {
                    heaterError.set(true);
                    this.log.warn("Couldn't read from Configured TemperatureChannel of Heater: " + heater.id());
                }
            });
            //Sets both error and ok
            this.setIsHeating(isHeating.get());
            this.setHasError(heaterError.get());
        } else {
            try {
                this.allocateConfig(this.config.heaterIds(), this.config.activationThermometers(), this.config.activationTemperatures(),
                        this.config.deactivationThermometers(), this.config.deactivationTemperatures());
                this.configurationSuccess = true;
            } catch (ConfigurationException | OpenemsError.OpenemsNamedException e) {
                //In the first few Cycles some Components May not be activated, only warn user when the Max Wait Count is reached.
                //since this should only happen on restart -> no reset is necessary
                if (this.configurationCounter.get() >= MAX_WAIT_COUNT) {
                    this.log.warn("Couldn't set Configuration for Controller : " + super.id() + " Check the Configuration of this Controller!");
                    this.getHasErrorChannel().setNextValue(true);
                } else {
                    this.configurationCounter.getAndIncrement();
                }
            }
        }
    }


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
                        ThermometerWrapperImpl wrapperOfMap = this.heaterTemperatureWrapperMap.get(oldHeater.get());
                        this.heaterTemperatureWrapperMap.remove(oldHeater.get());
                        this.heaterTemperatureWrapperMap.put(newHeater, wrapperOfMap);
                        //replace in old HeaterActiveWrapperMap
                        HeaterActiveWrapper wrapper = this.activeStateHeaterAndHeatWrapper.get(oldHeater.get());
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

    private void checkMissingThermometer() {
        this.heaterTemperatureWrapperMap.forEach((heater, wrapper) -> {
            Thermometer max = wrapper.getDeactivationThermometer();
            Thermometer min = wrapper.getActivationThermometer();
            Thermometer maxNew = null;
            Thermometer minNew = null;

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

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }
}
