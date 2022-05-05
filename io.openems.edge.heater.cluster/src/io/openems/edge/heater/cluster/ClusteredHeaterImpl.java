package io.openems.edge.heater.cluster;

import com.google.common.collect.Lists;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.heater.api.EnergyControlMode;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.api.ClusterState;
import io.openems.edge.heater.api.ClusteredHeater;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * The Heater Cluster stores "n" amount of Heater. Those should be the same HeaterType.
 * They should have the same amount of a maximum HeatingPower.
 * </p>
 * <p>
 * When the HeaterCluster is Enabled. It iterates through its heater and activates them, depending on the power demand.
 * The iteration order for activating is: Get the highest priority -> get the first -> second -> third -> etc. heater.
 * Whenever the demand is satisfied -> stop iterating and run the selected heater.
 * </p>
 * <p>
 * Furthermore, there are three Types of possible demands. The default value, that always applies when no other demand is set.
 * The "overwriteDemand" and the "recommendedDemand".
 * </p>
 * <p>
 * When both overwrite and recommended are written, check which value is the greater one.
 * When the pastPowerValue is greater than the overwriteValue. Apply the pastPowerValue.
 * However, when the recommended value is only available and it's less than the pastPowerValue, apply the recommended value.
 * This is because, the recommended value is usually set by an AI.
 * The overwriteValue works like a "fallback" when the AI miscalculates, and it gets too cold.
 * </p>
 * <p>
 * Furthermore, the Heater within the priorities can be optionally sorted. Select a ChannelId and a deltaTime, and whenever
 * the Time is up, those Heater will be sorted by the given ChannelId. If Ascending or Descending is up to the configuration.
 * </p>
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Cluster", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)

public class ClusteredHeaterImpl extends AbstractOpenemsComponent implements OpenemsComponent, ClusteredHeater, EventHandler {

    @Reference
    ComponentManager cpm;

    private final Logger log = LoggerFactory.getLogger(ClusteredHeaterImpl.class);

    Map<ClusteredHeaterPriority, Map<Integer, Heater>> priorityToHeaterMap = new HashMap<>();
    private int maxIndividualHeatingPowerKw = 150;
    private EnergyControlMode energyControlModeInput = EnergyControlMode.KW;
    private EnergyControlMode energyControlModeOutput = EnergyControlMode.KW;
    private float currentPowerSelected = -1.f;
    private float previousPowerSelected = -1.f;
    private final Map<Heater, Integer> currentRunningHeatersWithSetPoint = new HashMap<>();
    private Config config;
    private boolean configSuccess = false;
    private boolean shouldSort;
    private SortType sortType;
    private String channelIdToSortBy;
    private TimerHandler timerHandler;
    private static final String SORT_AGAIN = "CLUSTERED_HEATER_SORT_AGAIN";

    public ClusteredHeaterImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values(),
                ClusteredHeater.ChannelId.values());
        for (ClusteredHeaterPriority value : ClusteredHeaterPriority.values()) {
            this.priorityToHeaterMap.put(value, new HashMap<>());
        }
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        this.activationOrModificationRoutine(config);

    }

    private void activationOrModificationRoutine(Config config) {
        this.configSuccess = false;
        this.priorityToHeaterMap.forEach((key, value) -> {
            value.clear();
        });
        this.currentRunningHeatersWithSetPoint.clear();
        this.currentPowerSelected = config.defaultPowerLevel();
        this.energyControlModeInput = config.energyControlModeInput();
        this.energyControlModeOutput = config.energyControlModeOutput();
        this.maxIndividualHeatingPowerKw = config.maxPowerOfaSingleHeaterInThisCluster();
        this.getDefaultActivePowerChannel().setNextValue(config.defaultPowerLevel());
        try {
            if (config.highPrioHeater() != null && config.highPrioHeater().length > 0 && !config.highPrioHeater()[0].equals("")) {
                this.putConfiguredHeaterToMap(Arrays.asList(config.highPrioHeater()), this.priorityToHeaterMap.get(ClusteredHeaterPriority.HIGH));
            }
            if (config.midPrioHeater() != null && config.midPrioHeater().length > 0 && !config.midPrioHeater()[0].equals("")) {
                this.putConfiguredHeaterToMap(Arrays.asList(config.midPrioHeater()), this.priorityToHeaterMap.get(ClusteredHeaterPriority.MID));
            }
            if (config.lowPrioHeater() != null && config.lowPrioHeater().length > 0 && !config.lowPrioHeater()[0].equals("")) {
                this.putConfiguredHeaterToMap(Arrays.asList(config.lowPrioHeater()), this.priorityToHeaterMap.get(ClusteredHeaterPriority.LOW));
            }
            this.shouldSort = config.sortHeater();
            this.sortType = config.sortType();
            this.channelIdToSortBy = config.channelId();
            if (this.timerHandler != null) {
                this.timerHandler.removeComponent();
            }
            this.timerHandler = new TimerHandlerImpl(this.id(), this.cpm);
            this.timerHandler.addOneIdentifier(SORT_AGAIN, config.timerId(), config.maxTimeInterval());
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configSuccess = false;
            this.log.warn("Something went wrong with configuration, trying again later!");
            return;
        }
        this.configSuccess = true;
    }

    private void putConfiguredHeaterToMap(List<String> heaterIdList, Map<Integer, Heater> heaterMapToPut) throws ConfigurationException, OpenemsError.OpenemsNamedException {

        ConfigurationException[] ex = {null};
        OpenemsError.OpenemsNamedException[] exception = {null};


        heaterIdList.forEach(entry -> {
            if (ex[0] == null && exception[0] == null) {
                try {
                    OpenemsComponent component = this.cpm.getComponent(entry);
                    if (component instanceof Heater) {
                        heaterMapToPut.put(heaterIdList.indexOf(entry), (Heater) component);
                    } else {
                        throw new ConfigurationException("activationOrModifiedRoutine",
                                "OpenemsComponent: " + entry + " Not an instance of Heater!");
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    exception[0] = e;
                } catch (ConfigurationException e) {
                    ex[0] = e;
                }
            }
        });

        if (ex[0] != null) {
            heaterMapToPut.clear();
            throw ex[0];
        }
        if (exception[0] != null) {
            heaterMapToPut.clear();
            throw exception[0];
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        this.activationOrModificationRoutine(config);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (this.configSuccess) {
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
                //refresh default ActivePowerChannel if externally set
                if (this.getDefaultActivePowerChannel().getNextWriteValue().isPresent()) {
                    this.getDefaultActivePowerChannel().setNextValue(this.getDefaultActivePowerChannel().getNextWriteValueAndReset());
                }
                //Externally Set EnableSignal
                if (this.getEnableSignalChannel().getNextWriteValueAndReset().orElse(false)) {
                    this.getEnableSignalChannel().setNextValue(true);
                    this.sortHeater();
                    float selectedPowerLevelThisRun = -1.f;
                    //Convert Values depending on expected INPUT -> Convert to general KW input!
                    float defaultPower = this.convertPower((float)
                            this.getDefaultActivePowerChannel().value()
                                    .orElse(this.getDefaultActivePowerChannel().getNextValue().orElse(100)), this.energyControlModeInput);
                    Float recommendedPowerLevel = this.convertPower(this.getRecommendedSetPointHeatingPower().getNextWriteValue().orElse(null), this.energyControlModeInput);
                    Float overwritePowerLevel = this.convertPower(this.getOverWriteSetPointHeatingPowerChannel().getNextWriteValueAndReset().orElse(null), this.energyControlModeInput);
                    List<Heater> keySet = new ArrayList<>(this.currentRunningHeatersWithSetPoint.keySet());
                    keySet.forEach(entry -> {
                        if (entry.getHeaterState().asEnum().equals(HeaterState.BLOCKED_OR_ERROR) || entry.getHeaterState().asEnum().equals(HeaterState.UNDEFINED)) {
                            this.currentRunningHeatersWithSetPoint.remove(entry);
                        }
                    });

                    //recommended is always active if overwriteIsNot
                    if (recommendedPowerLevel != null) {
                        selectedPowerLevelThisRun = recommendedPowerLevel;
                        this.currentPowerSelected = recommendedPowerLevel;
                    }
                    if (overwritePowerLevel != null) {
                        selectedPowerLevelThisRun = Math.max(overwritePowerLevel, selectedPowerLevelThisRun);
                    }
                    if (recommendedPowerLevel == null && overwritePowerLevel == null) {
                        selectedPowerLevelThisRun = defaultPower;
                    }

                    //Now i know what percentage i need to get / what KW
                    // This is either the Maximum of the previous run or the recommended Power
                    this.currentPowerSelected = Math.max(selectedPowerLevelThisRun, this.currentPowerSelected);

                    float currentPowerPossible = 0.f;
                    //How much Power is possible atm!
                    if (this.currentRunningHeatersWithSetPoint.size() > 0) {
                        currentPowerPossible = this.currentRunningHeatersWithSetPoint.size() * this.maxIndividualHeatingPowerKw;
                    }
                    //Add Heater if demand cannot be supplied
                    if (this.currentPowerSelected > currentPowerPossible) {
                        //set HeaterSetPoint existing to Maximum
                        this.currentRunningHeatersWithSetPoint.replaceAll((key, value) -> this.maxIndividualHeatingPowerKw);
                        //add possible Heater
                        this.addRunningHeatersDependingOnPowerDemand(this.currentPowerSelected, currentPowerPossible);
                        this.previousPowerSelected = this.currentPowerSelected;
                    } else if (this.previousPowerSelected != this.currentPowerSelected) {
                        //Adapt since powerSupply can be met with current running heaters
                        this.adaptPowerSupplyRunningHeater();
                        this.previousPowerSelected = this.currentPowerSelected;
                    }

                    this.enableSelectedRunningHeatersAndConvertPowerSetPoint();


                } else {
                    this.getEnableSignalChannel().setNextValue(false);
                    this.getClusterStateChannel().setNextValue(ClusterState.OFF.getValue());
                    this.currentPowerSelected = -1.f;
                }
            }
        } else {
            this.activationOrModificationRoutine(this.config);
        }
    }

    /**
     * <p>
     * 1. Get all Heater
     * 2. For each heater in priority map -> sort them by given ChannelId
     * 3. Invert (Descending) if configured {@link SortType#DESCENDING}
     * 4. Put them in an new Hashmap
     * 5. the old {@link #priorityToHeaterMap} is replaced by the new sorted one.
     * </p>
     * <p>
     * Sorting the heater only happens, if {@link #shouldSort} is configured and the time is up.
     * </p>
     */
    private void sortHeater() {
        if (this.shouldSort && this.timerHandler.checkTimeIsUp(SORT_AGAIN)) {
            this.timerHandler.resetTimer(SORT_AGAIN);
            Map<ClusteredHeaterPriority, Map<Integer, Heater>> newSortedMap = new HashMap<>();

            this.priorityToHeaterMap.forEach((key, value) -> {
                List<Heater> heaterList = new ArrayList<>();
                value.forEach((index, heater) -> {
                    heaterList.add(heater);
                });
                heaterList.sort((heater1, heater2) -> {
                    Integer calcDouble;
                    Integer calcDouble2;
                    try {
                        calcDouble = TypeUtils.getAsType(OpenemsType.INTEGER, this.cpm.getChannel(new ChannelAddress(heater1.id(), this.channelIdToSortBy)).value());
                        if (calcDouble == null) {
                            calcDouble = 0;
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn(this.id() + "Channel not available for " + heater1.id() + " channel: " + this.channelIdToSortBy);
                        calcDouble = 0;
                    }
                    try {
                        calcDouble2 = TypeUtils.getAsType(OpenemsType.INTEGER, this.cpm.getChannel(new ChannelAddress(heater2.id(), this.channelIdToSortBy)).value());
                        if (calcDouble2 == null) {
                            calcDouble2 = 0;
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        calcDouble2 = 0;
                    }
                    return calcDouble.compareTo(calcDouble2);
                });
                List<Heater> copiedList = Lists.reverse(heaterList);
                AtomicInteger counter = new AtomicInteger(0);
                Map<Integer, Heater> newHeaterList = new HashMap<>();
                if (this.sortType.equals(SortType.DESCENDING)) {
                    copiedList.forEach(heater -> {
                        newHeaterList.put(counter.getAndIncrement(), heater);
                    });
                } else {
                    heaterList.forEach(heater -> {
                        newHeaterList.put(counter.getAndIncrement(), heater);
                    });
                }
                newSortedMap.put(key, newHeaterList);
            });
            this.priorityToHeaterMap = newSortedMap;
        }
    }

    /**
     * PowerSetPoint is calculated -> now apply them to the heater. StoredPowerSetPoint is in KW.
     */

    private void enableSelectedRunningHeatersAndConvertPowerSetPoint() {

        this.currentRunningHeatersWithSetPoint.forEach((heater, setPoint) -> {
            if (setPoint != null) {
                try {
                    heater.getEnableSignalChannel().setNextWriteValueFromObject(true);
                    switch (this.energyControlModeOutput) {
                        case PERCENT:
                            heater.setHeatingPowerPercentSetpoint((setPoint * 100.f) / this.maxIndividualHeatingPowerKw);
                            break;
                        case KW:
                        case TEMPERATURE:
                        default:
                            heater.setHeatingPowerSetpoint(setPoint);
                            break;
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't set EnableSignal or PowerSetPoint to heater: " + heater.id()
                            + " in : " + this.id() + " in Method enableSelectedRunningHeatersAndConvertPowerSetPoint");
                    this.log.warn("Reason: " + e.getCause());
                }
            }
        });

    }

    /**
     * When existing heaters are enough to provide enough power. Regulate them depending on the demand!
     */
    private void adaptPowerSupplyRunningHeater() {
        AtomicInteger powerToApply = new AtomicInteger((int) Math.ceil(this.currentPowerSelected));
        this.currentRunningHeatersWithSetPoint.forEach((heater, setPoint) -> {
            powerToApply.set(powerToApply.get() - setPoint);
        });
        boolean increasePerformance = powerToApply.get() > 0;
        //Power > 0 -> increase Performance of Heater! and reduce in the process the remaining powerToApply
        // else REDUCE
        if (increasePerformance) {
            Arrays.asList(ClusteredHeaterPriority.values()).forEach(priority -> {
                Map<Integer, Heater> priorityMap = this.priorityToHeaterMap.get(priority);
                if (priorityMap != null && !priorityMap.isEmpty() && powerToApply.get() != 0) {
                    for (int x = 0; x < priorityMap.size(); x++) {
                        Heater heaterToGet = priorityMap.get(x);
                        int powerProvided = this.currentRunningHeatersWithSetPoint.get(heaterToGet);
                        //increase Performance
                        if (powerToApply.get() > 0) {
                            if (powerProvided + powerToApply.get() > this.maxIndividualHeatingPowerKw) {
                                this.currentRunningHeatersWithSetPoint.replace(heaterToGet, this.maxIndividualHeatingPowerKw);
                                powerToApply.set(powerToApply.get() - (this.maxIndividualHeatingPowerKw - powerProvided));
                            } else {
                                this.currentRunningHeatersWithSetPoint.replace(heaterToGet, (powerProvided + powerToApply.get()));
                                powerToApply.set(0);
                            }
                        } else {
                            break;
                        }
                    }
                }
            });
        } else {
            //Reverse order of ClusteredPriority and reduce from the least priority to max priority
            List<ClusteredHeaterPriority> values = Arrays.asList(ClusteredHeaterPriority.values());
            Collections.reverse(values);
            values.forEach(priority -> {
                Map<Integer, Heater> priorityMap = this.priorityToHeaterMap.get(priority);
                if (priorityMap != null && priorityMap.size() > 0 && powerToApply.get() != 0) {
                    for (int x = priorityMap.size() - 1; x >= 0; x--) {
                        Heater heaterToGet = priorityMap.get(x);
                        int powerProvided = this.currentRunningHeatersWithSetPoint.get(heaterToGet);
                        //if power needs to be reduced
                        if (powerToApply.get() < 0) {
                            if (powerProvided + powerToApply.get() <= 0) {
                                powerToApply.set(powerToApply.get() + powerProvided);
                                this.currentRunningHeatersWithSetPoint.remove(heaterToGet);
                            } else {
                                this.currentRunningHeatersWithSetPoint.replace(heaterToGet, (powerProvided + powerToApply.get()));
                                powerToApply.set(0);
                            }
                        } else {
                            break;
                        }
                    }
                }
            });
        }

    }

    /**
     * Converts the Power.
     *
     * @param inputValue  the input PowerLevel.
     * @param controlMode the {@link EnergyControlMode}.
     * @return the converted Power in KW.
     */
    private Float convertPower(Float inputValue, EnergyControlMode controlMode) {
        if (inputValue == null) {
            return null;
        } else {
            switch (controlMode) {

                case KW:
                    return inputValue;
                case TEMPERATURE:
                    this.log.error("Temperature is not supported! Might not work as you expect to be!");
                    return inputValue;
                case PERCENT:
                default:
                    return (inputValue * this.maxIndividualHeatingPowerKw) / 100;
            }
        }

    }

    /**
     * Demand is above providing -> add new Heater to currentlyHandled Heater and adapt Power.
     *
     * @param currentPowerSelected the maximum Power needed this time
     * @param currentPowerPossible power Possible at the moment -> this allows to calculate the powerNeeded
     */
    private void addRunningHeatersDependingOnPowerDemand(float currentPowerSelected, float currentPowerPossible) {
        float powerNeeded = currentPowerSelected - currentPowerPossible;
        float neededFunctioningHeater = powerNeeded / this.maxIndividualHeatingPowerKw;
        //How many heater are needed with the given HeatingPower
        int roundedNeededHeater = (int) Math.ceil(neededFunctioningHeater);
        AtomicInteger possibleHeaterToAdd = new AtomicInteger(0);
        //Check for possible Heaters that can be added -> neither already handled nor Blocked
        this.priorityToHeaterMap.forEach((key, value) -> {
            value.forEach((position, heater) -> {
                if (!this.currentRunningHeatersWithSetPoint.containsKey(heater) && !heater.getHeaterState().asEnum().equals(HeaterState.BLOCKED_OR_ERROR) && !heater.getHeaterState().asEnum().equals(HeaterState.UNDEFINED)) {
                    possibleHeaterToAdd.getAndIncrement();
                }
            });
        });
        AtomicInteger neededHeater = new AtomicInteger(roundedNeededHeater);
        //Calculate the Power the LastHeater in line needs
        AtomicInteger powerSetPointLastHeater = new AtomicInteger((int) Math.ceil(powerNeeded));
        if (neededHeater.get() > 1) {
            powerSetPointLastHeater.set((int) Math.ceil(powerNeeded - ((neededHeater.get()) - 1) * this.maxIndividualHeatingPowerKw));
        }
        Arrays.stream(ClusteredHeaterPriority.values()).forEachOrdered(clusterKey -> {
            Map<Integer, Heater> mapToWorkWith = this.priorityToHeaterMap.get(clusterKey);
            if (mapToWorkWith != null && !mapToWorkWith.isEmpty() && possibleHeaterToAdd.get() > 0 && neededHeater.get() > 0) {
                mapToWorkWith.forEach((key, heater) -> {
                    if (!this.currentRunningHeatersWithSetPoint.containsKey(heater)) {
                        int powerToPut = neededHeater.get() > 1 ? this.maxIndividualHeatingPowerKw : powerSetPointLastHeater.get();
                        this.currentRunningHeatersWithSetPoint.put(heater, powerToPut);
                        possibleHeaterToAdd.getAndDecrement();
                        neededHeater.getAndDecrement();
                    }
                });
            }
        });
        //happens if possibleHeaterToAdd are less than needed Heater
        if (neededHeater.get() > 0) {
            this.getClusterStateChannel().setNextValue(ClusterState.NOT_ENOUGH_POWER.getValue());
            int missingPower = (neededHeater.get() - 1) * this.maxIndividualHeatingPowerKw + powerSetPointLastHeater.get();
            this.log.warn("Couldn't set enough Heater to provide Power. Missing Power: " + missingPower);
        } else {
            this.getClusterStateChannel().setNextValue(ClusterState.OK.getValue());
        }
    }

}
