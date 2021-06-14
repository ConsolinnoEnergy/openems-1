package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.multipleheatercombined.api.MultipleHeaterCombinedController;
import io.openems.edge.heater.Heater;
import io.openems.edge.thermometer.api.Thermometer;
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

/**
 * The MultipleHeaterCombined controller allows the monitoring and enabling of any {@link Heater}.
 * They are categorized in primary, secondary and tertiary heater.
 * Each Heater gets an activation and deactivation Thermometer as well as temperature.
 * Lets say Heater A has a Thermometer Aa and a Thermometer Ab with an activation Temperature of 400dC and a deactivation Temperature of 600dC
 * The thermometer Ab will be checked if it's Temperature is > than the deactivation Temp. of 600 dC
 * If so -> disable the Heater (Don't write in the enable Signal) -> set the {@link HeaterActiveWrapper#setActive(boolean)}}
 * to false therefore don't write in the heater Channel
 * Else if the Activation Thermometer Aa is beneath the activation Temperature of 400dC set the {@link HeaterActiveWrapper#setActive(boolean)}
 * to true and therefore write into the heater Channel.
 * Remember you can have n Heater in each Primary/Secondary/Tertiary Category.
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

    private final Map<Heater, ThermometerWrapper> heaterTemperatureWrapperMap = new HashMap<>();
    private final List<Heater> configuredHeater = new ArrayList<>();
    private final Map<Heater, HeaterActiveWrapper> activeStateHeaterAndHeatWrapper = new HashMap<>();
    private boolean configurationSuccess;
    private final AtomicInteger configurationCounter = new AtomicInteger(0);
    private static final int MAX_WAIT_COUNT = 10;

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
     * @param temperatureSensorMin TemperatureSensor vor minimum Temperature
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
        wrapper = new ThermometerWrapper(min, max, temperatureMin, temperatureMax, this.cpm);

        return wrapper;
    }


    /**
     * MultipleHeaterCombined logic to activate Primary-->secondary--->Fallback Heater depending on demand.
     * <p>
     * Via AverageConsumption from the HeatMeter a performance demand is calculated.
     * Each Heater got a Temperature where they should activate and deactivate (Saving energy).
     * For HeatOnly:
     * If the temperature threshold is met either deactivate (above max temperature) or activate (below min Temperature)
     * the Heater. In Order -> Primary --> secondary --> fallback.
     * Else check ConsumptionMeter and calculate Heatdemand.
     * update the Heatdemand by either Heatmeter value OR the returned performance value of the Heater
     * (Heatmeter is more accurate due to the fact, that Heater are slow in Heating)
     * In Addition a BufferValue is needed and calculated; depending on the Average Temperature in the System.
     * Meaning either More or Less Performance than needed is calculated/provided.
     * The Buffer Values are set via Config. As Well as all the TemperatureSensors Heaters etc etc.
     * </p>
     */
    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        if (this.configurationSuccess) {
            AtomicBoolean heaterError = new AtomicBoolean(false);
            AtomicBoolean isHeating = new AtomicBoolean(false);

            this.configuredHeater.forEach(heater -> {
                if (heater.hasError()) {
                    heaterError.set(true);
                }
                //ThermometerWrapper holding min and max values as well as Thermometer corresponding to the heater
                ThermometerWrapper thermometerWrapper = this.heaterTemperatureWrapperMap.get(heater);
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
                        heater.getEnableSignalChannel().setNextWriteValue(heaterActiveWrapper.isActive());
                        isHeating.set(true);
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
            } catch (ConfigurationException e) {
                //In the first few Cycles some Components May not be activated, only warn user when the Max Wait Count is reached.
                //since this should only happen on restart -> no reset is necessary
                if (this.configurationCounter.get() >= MAX_WAIT_COUNT) {
                    this.log.warn("Couldn't set Configuration for Controller : " + super.id() + " Check the Configuration of this Controller!");
                } else {
                    this.configurationCounter.getAndIncrement();
                }
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }
}
