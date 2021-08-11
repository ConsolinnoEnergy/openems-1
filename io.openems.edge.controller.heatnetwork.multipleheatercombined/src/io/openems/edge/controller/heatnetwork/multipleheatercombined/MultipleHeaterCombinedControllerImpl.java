package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.multipleheatercombined.api.MultipleHeaterCombinedController;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Designate(ocd = Config.class, factory = true)
@Component(name = "MultipleHeaterCombined",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class MultipleHeaterCombinedControllerImpl extends AbstractOpenemsComponent implements OpenemsComponent,
        Controller, MultipleHeaterCombinedController, ExceptionalState {

    private final Logger log = LoggerFactory.getLogger(MultipleHeaterCombinedControllerImpl.class);

    @Reference
    protected ComponentManager cpm;

    private final Map<Heater, ThermometerWrapper> heaterTemperatureWrapperMap = new HashMap<>();
    private final Map<HeaterHierarchy, List<Heater>> heaterHierarchyMap = new HashMap<>();
    private final Map<Heater, HeaterActiveWrapper> activeStateHeaterAndHeatWrapper = new HashMap<>();

    private boolean useExceptionalState;
    private boolean exceptionalStateActiveBefore;
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "MULTIPLE_HEATER_COMBINED_EXCEPTIONAL_STATE_IDENTIFIER";
    private TimerHandler timer;
    private ExceptionalStateHandler exceptionalStateHandler;
    private boolean overRideExceptionalStateActivateAllHeater;

    public MultipleHeaterCombinedControllerImpl() {

        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                MultipleHeaterCombinedController.ChannelId.values(),
                ExceptionalState.ChannelId.values());

    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {

        super.activate(context, config.id(), config.alias(), config.enabled());

        //Allocate Each Component, to Corresponding Heater etc if Enabled. Sorted by Hierarchy.
        //ATM Only One Heater per Hierarchy is possible --> Future Impl to Handle multiple Heater of each Hierarchy should be rel.easy
        //Each HeaterType is Optional.

        //----------------------ALLOCATE/ CONFIGURE HEATER/TemperatureSensor/HeatmeterMbus of Heater -----------------//
        if (config.usePrimaryHeater()) {
            this.allocateConfig(config.primaryHeaterId(), HeaterHierarchy.PRIMARY, config.primaryTemperatureSensorMin(), config.primaryHeaterMinTemperature(),
                    config.primaryTemperatureSensorMax(), config.primaryHeaterMaxTemperature());
        }
        if (config.useSecondaryHeater()) {
            this.allocateConfig(config.secondaryHeaterId(), HeaterHierarchy.SECONDARY, config.secondaryTemperatureSensorMin(), config.secondaryTemperatureMin(),
                    config.secondaryTemperatureSensorMax(), config.secondaryTemperatureMax());
        }
        if (config.useTertiaryHeater()) {
            this.allocateConfig(config.tertiaryHeaterId(), HeaterHierarchy.TERTIARY, config.tertiaryTemperatureSensorMin(), config.tertiaryTemperatureMin(),
                    config.tertiaryTemperatureSensorMax(), config.tertiaryTemperatureMax());
        }
        this.useExceptionalState = config.useExceptionalState();
        if (this.useExceptionalState) {
            this.timer = new TimerHandlerImpl(super.id(), this.cpm);
            this.timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, config.timerId(), config.waitTime());
            this.getExceptionalStateValueChannel().setNextValue(100);
            this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(this.timer, EXCEPTIONAL_STATE_IDENTIFIER);

        }

        this.setIsHeating(false);
        this.setHasError(false);
        this.setIsOk(true);
    }

    // ------------------- Config Related --------------------- //

    /**
     * Allocates the config of each Heater. They will be mapped to wrapper classes for easier handling and functions etc.
     *
     * @param heater_id            the id of the heater
     * @param hierarchy            is Primary/Secondary/Tertiary etc
     * @param temperatureSensorMin TemperatureSensor vor minimum Temperature
     * @param temperatureMin       TemperatureValue for min Temp.
     * @param temperatureSensorMax TemperatureSensor for MaximumTemp.
     * @param temperatureMax       TemperatureValue max allowed.
     * @throws OpenemsError.OpenemsNamedException if Id not found
     * @throws ConfigurationException             if instanceof is wrong.
     */
    private void allocateConfig(String heater_id, HeaterHierarchy hierarchy, String temperatureSensorMin,
                                int temperatureMin, String temperatureSensorMax,
                                int temperatureMax) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        Heater heater;
        try {
            if (this.cpm.getComponent(heater_id) instanceof Heater) {
                heater = this.cpm.getComponent(heater_id);
                this.activeStateHeaterAndHeatWrapper.put(heater, new HeaterActiveWrapper());
                if (this.heaterHierarchyMap.containsKey(hierarchy)) {
                    this.heaterHierarchyMap.get(hierarchy).add(heater);
                } else {
                    List<Heater> heaterList = new ArrayList<>();
                    heaterList.add(heater);
                    this.heaterHierarchyMap.put(hierarchy, heaterList);
                }
                this.heaterTemperatureWrapperMap.put(heater, this.createTemperatureWrapper(temperatureSensorMin, temperatureMin, temperatureSensorMax, temperatureMax));
            }
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.heaterTemperatureWrapperMap.clear();
            this.heaterHierarchyMap.clear();
            this.activeStateHeaterAndHeatWrapper.clear();
            throw e;
        }
    }


    /**
     * Creates A Thermometer Wrapper for the Corresponding Heater.
     * Thermometer wrapper Holds Information of min/max thermometer and min max Temp value as well as some helper Methods.
     *
     * @param temperatureSensorMin the min Temperature Sensor for the heater
     * @param temperatureMin       the min Temperature that needs to be reached at least
     * @param temperatureSensorMax the max Temperature Sensor for the heater
     * @param temperatureMax       the max Temperature that allowed
     * @return the Thermometer Wrapper for the Heater
     * @throws OpenemsError.OpenemsNamedException if Ids cannot be found
     * @throws ConfigurationException             if ThermometerIds not an Instance of Thermometer
     */
    private ThermometerWrapper createTemperatureWrapper(String temperatureSensorMin,
                                                        int temperatureMin, String temperatureSensorMax, int temperatureMax)
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
        wrapper = new ThermometerWrapper(min, max, temperatureMin, temperatureMax);

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
        this.checkMissingHeaterComponents();
        this.checkMissingThermometer();
        if (this.useExceptionalState) {
            boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
            if (exceptionalStateActive) {
                if (this.getExceptionalStateValue() <= 0) {
                    this.overRideExceptionalStateActivateAllHeater = false;
                    return;
                } else {
                    this.overRideExceptionalStateActivateAllHeater = true;
                }
            }
        }

        AtomicBoolean heaterError = new AtomicBoolean(false);
        AtomicBoolean isHeating = new AtomicBoolean(false);
        //Go through ordered HeaterHierarchy -> Order of declaration
        Arrays.stream(HeaterHierarchy.values()).forEachOrdered(hierarchy -> {
            if (this.heaterHierarchyMap.containsKey(hierarchy) && this.heaterHierarchyMap.get(hierarchy).size() > 0) {
                this.heaterLogic(this.heaterHierarchyMap.get(hierarchy), heaterError);
            }
        });
        this.setIsHeating(isHeating.get());
        //Sets both error and ok
        this.setHasError(heaterError.get());
    }

    private void checkMissingThermometer() {
        this.heaterTemperatureWrapperMap.forEach((heater, wrapper) -> {
            Thermometer max = wrapper.getMaxThermometer();
            Thermometer min = wrapper.getMinThermometer();
            Thermometer maxNew = null;
            Thermometer minNew = null;

            if (max.isEnabled() == false) {
                try {
                    OpenemsComponent component = this.cpm.getComponent(max.id());

                    if (component instanceof Thermometer) {
                        maxNew = (Thermometer) component;
                        wrapper.getThermometerKindThermometerMap().put(ThermometerKind.DEACTIVATE_THERMOMETER, maxNew);
                        Integer value = wrapper.getThermometerAndValue().get(max);
                        wrapper.getThermometerAndValue().remove(max);
                        wrapper.getThermometerAndValue().put(maxNew, value);
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
                        wrapper.getThermometerKindThermometerMap().put(ThermometerKind.ACTIVATE_THERMOMETER, minNew);
                        Integer value = wrapper.getThermometerAndValue().get(min);
                        wrapper.getThermometerAndValue().remove(min);
                        wrapper.getThermometerAndValue().put(minNew, value);
                    } else {
                        this.log.warn("New Instance of Thermometer : " + min.id() + " is not a Thermometer. MultiHeater won't work!");
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't find Max Thermometer : " + min.id() + " MultiHeater might not be working!");
                }

            }
        });
    }

    /**
     * For Each Heater in a Prio Order (Enum -> HeaterHierarchy), calculate the Provided Power and activate Heater;
     * If Error occurred, notify by writing into AtomicBooleam.
     * If Any Heater heats/activates -> set AtomicBoolean isHeating to true.
     * Performance Demand will be either calculated by Heater MBus OR the calculated power given by the heater.
     *
     * @param allHeater   all Heater of this Prioriy.
     * @param heaterError does a Heater has an error usually from run
     */
    private void heaterLogic(List<Heater> allHeater, AtomicBoolean heaterError) {
        allHeater.forEach(heater -> {
            if (heater.getErrorMessage().get().equals("No error") == false) {
                heaterError.set(true);
            }
            //what can be provided
            //ThermometerWrapper holding min and max values as well as Thermometer
            ThermometerWrapper thermometerWrapper = this.heaterTemperatureWrapperMap.get(heater);
            //HeatWrapper holding activeState and alwaysActive
            HeaterActiveWrapper heaterActiveWrapper = this.activeStateHeaterAndHeatWrapper.get(heater);
            //get the Wrapperclass and check if Heater should be turned of, as well as Checking performance demand
            //HeatControl                                           PerformanceDemand + Time Control
            if (thermometerWrapper.offTemperatureAboveMaxValue()
                    && (this.useExceptionalState && this.overRideExceptionalStateActivateAllHeater) == false) {
                heaterActiveWrapper.setActive(false);
                //Check wrapper if thermometer below min temp
            } else if (thermometerWrapper.onTemperatureBelowMinValue() || (this.useExceptionalState && this.overRideExceptionalStateActivateAllHeater)) {
                heaterActiveWrapper.setActive(true);
            }
            //Enable
            try {
                if (heaterActiveWrapper.isActive()) {
                    heater.getEnableSignalChannel().setNextWriteValue(heaterActiveWrapper.isActive());
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't set the enableSignal: " + super.id() + " Reason: " + e.getMessage());
            }


        });
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
                        ThermometerWrapper wrapperOfMap = this.heaterTemperatureWrapperMap.get(oldHeater.get());
                        this.heaterTemperatureWrapperMap.remove(oldHeater.get());
                        this.heaterTemperatureWrapperMap.put(newHeater, wrapperOfMap);
                        //replace in old HeaterActiveWrapperMap
                        HeaterActiveWrapper wrapper = this.activeStateHeaterAndHeatWrapper.get(oldHeater.get());
                        this.activeStateHeaterAndHeatWrapper.remove(oldHeater.get());
                        this.activeStateHeaterAndHeatWrapper.put(newHeater, wrapper);
                        //replace old heater in Hierarchy
                        heaterFound.set(false);
                        AtomicReference<HeaterHierarchy> hierarchyName = new AtomicReference<>();
                        AtomicInteger oldHeaterIndexInHierarchy = new AtomicInteger(0);
                        this.heaterHierarchyMap.forEach(((heaterHierarchy, heaters) -> {
                            if (heaterFound.get() == false) {
                                heaters.forEach(entry -> {

                                    if (heaterFound.get() == false && entry.id().equals(oldHeater.get().id())) {
                                        heaterFound.set(true);
                                        hierarchyName.set(heaterHierarchy);
                                        oldHeaterIndexInHierarchy.set(heaters.indexOf(entry));
                                    }
                                });
                            }
                        }));
                        if (heaterFound.get() && hierarchyName.get() != null) {
                            this.heaterHierarchyMap.get(hierarchyName.get()).remove(oldHeaterIndexInHierarchy.get());
                            this.heaterHierarchyMap.get(hierarchyName.get()).add(newHeater);
                        }
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Heater: " + heaterOfMissingHeaterList.id() + " is Missing! It won't run/heat anymore!");
                }
            });
        }
    }

    @Deactivate
    protected void deactivate() {
        this.timer.removeComponent();
        super.deactivate();
    }
}
