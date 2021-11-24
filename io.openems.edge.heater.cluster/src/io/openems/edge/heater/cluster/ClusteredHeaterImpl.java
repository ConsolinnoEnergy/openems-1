package io.openems.edge.heater.cluster;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.heater.api.EnergyControlMode;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.cluster.api.ClusterState;
import io.openems.edge.heater.cluster.api.ClusteredHeater;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;


@Designate(ocd = Config.class, factory = true)
@Component(name = "ClusterHeater", immediate = true,
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
    private Map<Heater, Integer> currentRunningHeatersWithSetPoint = new HashMap<>();
    private Config config;
    private boolean configSuccess = false;

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
            this.putConfiguredHeaterToMap(Arrays.asList(config.highPrioHeater()), this.priorityToHeaterMap.get(ClusteredHeaterPriority.HIGH));
            this.putConfiguredHeaterToMap(Arrays.asList(config.midPrioHeater()), this.priorityToHeaterMap.get(ClusteredHeaterPriority.MID));
            this.putConfiguredHeaterToMap(Arrays.asList(config.lowPrioHeater()), this.priorityToHeaterMap.get(ClusteredHeaterPriority.LOW));
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configSuccess = false;
            this.log.warn("Something went wrong with configuration, trying again later!");
        }

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

            if (this.getEnableSignalChannel().getNextWriteValueAndReset().orElse(false)) {
                this.getEnableSignalChannel().setNextValue(true);

                float selectedPowerLevelThisRun = -1.f;
                //Convert Values depending on expected INPUT -> Convert to general KW input!
                float defaultPower = this.convertPowerInput((float)
                        this.getDefaultActivePowerChannel().value()
                                .orElse(this.getDefaultActivePowerChannel().getNextValue().orElse(100)));
                Float recommendedPowerLevel = this.convertPowerInput(this.getRecommendedSetPointHeatingPower().getNextWriteValue().orElse(null));
                Float overwritePowerLevel = this.convertPowerInput(this.getOverWriteSetPointHeatingPowerChannel().getNextWriteValueAndReset().orElse(null));
                List<Heater> keySet = new ArrayList<>(this.currentRunningHeatersWithSetPoint.keySet());
                keySet.forEach(entry -> {
                    if (entry.getHeaterState().asEnum().equals(HeaterState.BLOCKED_OR_ERROR)) {
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
                } else {
                    //Adapt since powerSupply can be met with current running heaters
                    this.adaptPowerSupplyRunningHeater();
                }

                this.enableSelectedRunningHeatersAndConvertPowerSetPoint();


            } else {
                this.getEnableSignalChannel().setNextValue(false);
                this.getClusterStateChannel().setNextValue(ClusterState.OFF.getValue());
                this.currentPowerSelected = -1.f;
            }

        } else {
            this.activationOrModificationRoutine(this.config);
        }
    }

    private void enableSelectedRunningHeatersAndConvertPowerSetPoint() {

    }

    private void adaptPowerSupplyRunningHeater() {

    }

    private Float convertPowerInput(Float inputValue) {
        if (inputValue == null) {
            return null;
        } else {
            switch (this.energyControlModeInput) {

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

    private void checkForRemovalOfNotNeededHeater() {

    }

    /**
     * Demand is above providing -> add new Heater to currentlyHandled Heater and adapt Power.
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
                if (!this.currentRunningHeatersWithSetPoint.containsKey(heater) && !heater.getHeaterState().asEnum().equals(HeaterState.BLOCKED_OR_ERROR)) {
                    possibleHeaterToAdd.getAndIncrement();
                }
            });
        });
        AtomicInteger neededHeater = new AtomicInteger(roundedNeededHeater);
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

        if (neededHeater.get() > 0) {
            this.getClusterStateChannel().setNextValue(ClusterState.NOT_ENOUGH_POWER.getValue());
        } else {
            this.getClusterStateChannel().setNextValue(ClusterState.OK.getValue());
        }
    }

}
