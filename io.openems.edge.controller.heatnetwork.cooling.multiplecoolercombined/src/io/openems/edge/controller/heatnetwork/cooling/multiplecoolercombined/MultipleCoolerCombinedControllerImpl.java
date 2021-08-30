package io.openems.edge.controller.heatnetwork.cooling.multiplecoolercombined;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.cooling.multiplecoolercombined.api.MultipleCoolerCombinedController;
import io.openems.edge.heater.Cooler;
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
 * The MultipleCoolerCombined controller allows the monitoring and enabling of any {@link Cooler}.
 * Each Cooler gets an activation and deactivation Thermometer as well as temperature.
 * For Example: Cooler A has an (activation) Thermometer B and a (deactivation) Thermometer C with an activation Temperature
 * of 600 dC and a deactivation Temperature of 400dC.
 * The thermometer C will be checked if it's Temperature is > than the deactivation Temp. of 600 dC.
 * If so -> disable the Cooler (Don't write in the enable Signal) -> set the {@link CoolerActiveWrapper#setActive(boolean)}}
 * to false therefore don't write in the corresponding cooler EnableSignal Channel.
 * Else if the Activation Thermometer B is beneath the activation Temperature of 600dC set the {@link CoolerActiveWrapper#setActive(boolean)}
 * to true and therefore write into the cooler EnableSignal Channel.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "MultipleCoolerCombined",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class MultipleCoolerCombinedControllerImpl extends AbstractOpenemsComponent implements OpenemsComponent,
        Controller, MultipleCoolerCombinedController {

    private final Logger log = LoggerFactory.getLogger(MultipleCoolerCombinedControllerImpl.class);

    @Reference
    protected ComponentManager cpm;

    private final Map<Cooler, ThermometerWrapper> coolerTemperatureWrapperMap = new HashMap<>();
    private final List<Cooler> configuredCooler = new ArrayList<>();
    private final Map<Cooler, CoolerActiveWrapper> activeStateCoolerAndHeatWrapper = new HashMap<>();
    private boolean configurationSuccess;
    private final AtomicInteger configurationCounter = new AtomicInteger(0);
    private static final int MAX_WAIT_COUNT = 10;

    private Config config;

    public MultipleCoolerCombinedControllerImpl() {

        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                MultipleCoolerCombinedController.ChannelId.values());

    }

    @Activate
    void activate(ComponentContext context, Config config) {

        super.activate(context, config.id(), config.alias(), config.enabled());
        this.setIsCooling(false);
        this.setHasError(false);
        this.setIsOk(true);
        this.config = config;

        //----------------------ALLOCATE/ CONFIGURE COOLER/TemperatureSensor -----------------//
        try {
            this.allocateConfig(config.coolerIds(), config.activationThermometers(), config.activationTemperatures(),
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
            this.allocateConfig(config.coolerIds(), config.activationThermometers(), config.activationTemperatures(), config.deactivationThermometers(),
                    config.deactivationTemperatures());
            this.configurationSuccess = true;
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configurationSuccess = false;
        }
    }

    // ------------------- Config Related --------------------- //

    /**
     * Allocates the config of each Cooler. They will be mapped to wrapper classes for easier handling and functions etc.
     *
     * @param cooler_id            the id of the cooler
     * @param temperatureSensorMin TemperatureSensor vor minimum Temperature
     * @param temperatureMin       TemperatureValue for min Temp.
     * @param temperatureSensorMax TemperatureSensor for MaximumTemp.
     * @param temperatureMax       TemperatureValue max allowed.
     * @throws OpenemsError.OpenemsNamedException if Id not found
     * @throws ConfigurationException             if instanceof is wrong.
     */
    private void allocateConfig(String[] cooler_id, String[] temperatureSensorMin,
                                String[] temperatureMin, String[] temperatureSensorMax,
                                String[] temperatureMax) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (this.configEntriesDifferentSize(cooler_id.length, temperatureSensorMin.length, temperatureMin.length,
                temperatureSensorMax.length, temperatureMax.length)) {
            throw new ConfigurationException("allocate Config of MultipleCoolerCombined: " + super.id(), "Check Config Size Entries!");
        }
        List<String> coolerIds = Arrays.asList(cooler_id);
        OpenemsError.OpenemsNamedException[] ex = {null};
        ConfigurationException[] exC = {null};
        Arrays.stream(cooler_id).forEach(entry -> {
            try {
                if ((ex[0] == null && exC[0] == null)) {
                    OpenemsComponent component = this.cpm.getComponent(entry);
                    int index = coolerIds.indexOf(entry);
                    if (component instanceof Cooler) {
                        Cooler cooler = ((Cooler) component);

                        if (this.configuredCooler.stream().anyMatch(existingCooler -> existingCooler.id().equals(cooler.id()))) {
                            this.allocateConfigHadError();
                            throw new ConfigurationException("Allocate Config " + super.id(), "Cooler already configured with id: " + cooler.id() + " in : " + super.id());
                        }
                        this.configuredCooler.add(cooler);
                        this.activeStateCoolerAndHeatWrapper.put(cooler, new CoolerActiveWrapper());
                        this.coolerTemperatureWrapperMap.put(cooler, this.createTemperatureWrapper(temperatureSensorMin[index], temperatureMin[index], temperatureSensorMax[index], temperatureMax[index]));
                    } else {
                        this.allocateConfigHadError();
                        throw new ConfigurationException("MultipleCoolerCombined: AllocateConfig " + super.id(), "CoolerId not an instance of Cooler");
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
     * It clears Lists and Maps that might be initialized to prevent lingering Cooler from an invalid Config.
     */
    private void allocateConfigHadError() {
        this.configuredCooler.clear();
        this.coolerTemperatureWrapperMap.clear();
        this.activeStateCoolerAndHeatWrapper.clear();
    }

    /**
     * Get the Size from config Arrays such as CoolerIds, Temperatures, and Thermometers.
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
     * Creates A Thermometer Wrapper for the Corresponding Cooler.
     * Thermometer wrapper Holds Information of min/max thermometer and min max Temp value as well as some helper Methods.
     *
     * @param temperatureSensorMin the min Temperature Sensor for the cooler <- Activation Thermometer
     * @param temperatureMin       the min Temperature that needs to be reached at least <- Activation Threshold
     * @param temperatureSensorMax the max Temperature Sensor for the cooler <- Deactivation Thermometer
     * @param temperatureMax       the max Temperature that allowed <- Deactivation Threshold
     * @return the Thermometer Wrapper for the Cooler
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
     * MultipleCoolerCombined logic.
     * <p>
     * Each Cooler got a Temperature where they should activate and deactivate.
     * When the activationThermometer reaches the activationTemperature, the Cooler activates / {@link Cooler#getEnableSignalChannel()}
     * will receive a nextWriteValue of true.
     * The Cooler stays active, till the deactivationThermometer reaches the deactivationTemperature.
     * When the Deactivation Temperature is reached, the {@link Cooler#getEnableSignalChannel()} won't be set in any way.
     * This way the Cooler stays deactivated, until the activationTemperature is reached again.
     * </p>
     */
    @Override
    public void run() {
        if (this.configurationSuccess) {
            AtomicBoolean coolerError = new AtomicBoolean(false);
            AtomicBoolean isCooling = new AtomicBoolean(false);

            this.configuredCooler.forEach(cooler -> {
                if (cooler.hasError()) {
                    coolerError.set(true);
                }
                //ThermometerWrapper holding min and max values as well as Thermometer corresponding to the cooler
                ThermometerWrapper thermometerWrapper = this.coolerTemperatureWrapperMap.get(cooler);
                //HeatWrapper holding activeState and alwaysActive
                CoolerActiveWrapper coolerActiveWrapper = this.activeStateCoolerAndHeatWrapper.get(cooler);
                //Get the WrapperClass and check if Cooler should be turned of, as well as Checking performance demand
                //HeatControl                                           PerformanceDemand + Time Control
                //Enable
                try {
                    if (thermometerWrapper.shouldDeactivate()) {
                        coolerActiveWrapper.setActive(false);
                        //Check wrapper if thermometer below min temp
                    } else if (thermometerWrapper.shouldActivate()) {
                        coolerActiveWrapper.setActive(true);
                    }

                    if (coolerActiveWrapper.isActive()) {
                        cooler.getEnableSignalChannel().setNextWriteValue(coolerActiveWrapper.isActive());
                        isCooling.set(true);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    coolerError.set(true);
                    this.log.warn("Couldn't set the enableSignal: " + super.id() + " Reason: " + e.getMessage());
                } catch (ConfigurationException e) {
                    coolerError.set(true);
                    this.log.warn("Couldn't read from Configured TemperatureChannel of Cooler: " + cooler.id());
                }


            });
            //Sets both error and ok
            this.setIsCooling(isCooling.get());
            this.setHasError(coolerError.get());
        } else {
            try {
                this.allocateConfig(this.config.coolerIds(), this.config.activationThermometers(), this.config.activationTemperatures(),
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

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }
}
