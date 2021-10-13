package io.openems.edge.controller.heatnetwork.apartmentmodule;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.apartmentmodule.api.ApartmentModule;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.apartmentmodule.api.ControllerHeatingApartmentModule;
import io.openems.edge.controller.heatnetwork.apartmentmodule.api.ApartmentModuleControllerState;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.hydraulic.api.HeatBooster;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerThreshold;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import io.openems.edge.utility.api.MinMaxRoutine;
import io.openems.edge.utility.api.MinRoutine;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This Apartment Module Controller is a slightly Complex Controller, that allows to Monitor ApartmentCords combined with a Pump and
 * e.g. HydraulicLineHeater.
 * First you set up ApartmentCords. ApartmentCords are defined by 1 TopAM and then "n" many NONE TopAMs
 * Each ApartmentCord is represented by {@link Config#apartmentCords()} where Each String Entry is 1 Cord
 * Each Cord needs a thresholdThermometer AND a ResponseAddress. E.g. HydraulicLineHeater.
 * Furthermore you can apply a Map of ApartmentModules to Thermometer. This allows the TopAMs to monitor it's referenceTemperature.
 * The main purpose of this controller is to
 * a) Check for HeatRequest of ApartmentModules within a Cord
 * b) When a Cord has a HeatRequest enable the HydraulicComponent AND respond to the Response Cord ChannelAddress
 * e.g. if in Cord 1 ANY ApartmentModule has a Request -> tell the Responses mapped in {@link #responseToCords} to start/enable.
 * c) Apply ReferenceTemperatures to the TopAMs of the ApartmentCords.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Miscellaneous.Apartment.Module",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ControllerHeatingApartmentModuleImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller,
        ControllerHeatingApartmentModule {

    @Reference
    ComponentManager cpm;

    private static final String HEAT_PUMP = "HEAT_PUMP";
    private static final String THRESHOLD = "THRESHOLD";
    private static final String HEAT_BOOSTER = "HEAT_BOOSTER";
    private final Logger log = LoggerFactory.getLogger(ControllerHeatingApartmentModuleImpl.class);
    private final MinMaxRoutine minMaxRoutine = new MinRoutine();

    private static final String CONFIGURATION_SPLITTER = ":";


    //Behind each integer is a ApartmentCord --> One has to be a ApartmentModule with a Relay.
    private final Map<Integer, List<ApartmentModule>> apartmentCords = new HashMap<>();
    private final Map<Integer, List<ChannelAddress>> responseToCords = new HashMap<>();
    private Map<Integer, ThermometerThreshold> thresholdThermometerMap = new HashMap<>();
    private final Map<ApartmentModule, Thermometer> apartmentModuleToThermometerMap = new HashMap<>();
    private final List<Integer> cordsToHeatUp = new ArrayList<>();
    private HydraulicComponent responseHydraulicComponent;

    private boolean useHeatBooster;
    private HeatBooster optionalHeatBooster;
    private int heatBoosterThermometerActivationValue;
    private TimerHandler timer;

    private Config config;
    private boolean configSuccess;

    public ControllerHeatingApartmentModuleImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                ControllerHeatingApartmentModule.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        this.activationOrModifiedRoutine(config);
    }

    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.configSuccess = false;
        this.config = config;
        this.activationOrModifiedRoutine(config);
    }


    /**
     * Handles/Applies the basic Activation or modified Configuration.
     *
     * @param config the config of this controller.
     */
    private void activationOrModifiedRoutine(Config config) {
        try {
            if (config.apartmentCords().length != config.apartmentResponse().length || config.apartmentResponse().length != config.thresholdId().length) {
                this.log.warn("Activate of ControllerHeatingApartmentModule\n"
                        + "Expected the same Length for ApartmentCords (got:  " + config.apartmentCords().length
                        + ") and ApartmentResponse(got: " + config.apartmentResponse().length
                        + ") and ThresholdIds (got: " + config.thresholdId().length
                        + " Please Configure Again!");
                this.configSuccess = false;
                return;
            }
            this.allocateComponent(config, HEAT_PUMP);
            this.allocateComponent(config, THRESHOLD);
            this.useHeatBooster = config.useHeatBooster();
            if (this.useHeatBooster) {
                this.allocateComponent(config, HEAT_BOOSTER);
            }
            this.applyApartmentModules(config.apartmentCords());
            this.applyResponseToCords(config.apartmentResponse());
            this.applyApartmentModuleToThermometer(config.apartmentToThermometer());
            this.setSetPointPowerLevel(config.powerLevelPump());
            this.setSetPointTemperature(config.setPointTemperature());
            this.getSetPointPowerLevelChannel().nextProcessImage();
            this.getSetPointTemperatureChannel().nextProcessImage();
            this.heatBoosterThermometerActivationValue = config.heatBoostTemperature();
            this.timer = new TimerHandlerImpl(this.id(), this.cpm);
            this.timer.addOneIdentifier(CHECK_MISSING_COMPONENT_IDENTIFIER, config.timerId(), CHECK_MISSING_COMPONENTS_TIME);
            this.configSuccess = true;
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Couldn't apply Config! Trying again later");
            this.configSuccess = false;
        }
    }

    /**
     * Maps ApartmentModules to Thermometer. Usually you find configure a {@link io.openems.edge.thermometer.api.ThermometerVirtual}
     * and use the TemperatureValue of  the AM.
     *
     * @param apartmentToThermometer the Configuration that Maps An ApartmentModule to a Thermometer.
     * @throws ConfigurationException if somethings configured in a wrong way.
     */
    private void applyApartmentModuleToThermometer(String[] apartmentToThermometer) throws ConfigurationException {
        if (apartmentToThermometer.length > 0 && !apartmentToThermometer[0].equals("")) {
            ConfigurationException[] ex = {null};
            Map<String, String> apartmentIdToThermometerMap = new HashMap<>();
            Arrays.stream(apartmentToThermometer).forEach(entry -> {
                String[] entries = entry.split(CONFIGURATION_SPLITTER);
                if (entries.length == 2) {
                    apartmentIdToThermometerMap.put(entries[0], entries[1]);
                }
            });
            //key == AM ID, value == ThermometerId
            apartmentIdToThermometerMap.forEach((key, value) -> {
                if (ex[0] == null) {
                    try {
                        OpenemsComponent am = this.cpm.getComponent(key);
                        OpenemsComponent thermometer = this.cpm.getComponent(value);
                        if (am instanceof ApartmentModule && thermometer instanceof Thermometer) {
                            this.apartmentModuleToThermometerMap.put((ApartmentModule) am, (Thermometer) thermometer);
                        } else {
                            throw new ConfigurationException("ApplyApartmentModuleToThermometer", "Couldn't find Component or wrong instance");
                        }
                    } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
                        ex[0] = new ConfigurationException("ApplyApartmentModuleToThermometer", "Couldn't find Component or wrong instance");
                    }
                }
            });
            if (ex[0] != null) {
                throw ex[0];
            }
        }
    }

    /**
     * Allocates the Component to their corresponding configuration.
     *
     * @param config        the config of the component.
     * @param allocatorType static string, defined by this component.
     * @throws ConfigurationException             is thrown if the user typed in something wrong.
     * @throws OpenemsError.OpenemsNamedException if the component couldn't be found.
     */
    private void allocateComponent(Config config, String allocatorType) throws
            ConfigurationException, OpenemsError.OpenemsNamedException {

        switch (allocatorType) {
            case HEAT_PUMP:
                OpenemsComponent component;
                String deviceToGet;
                deviceToGet = config.heatPumpId();
                component = this.cpm.getComponent(deviceToGet);
                if (component instanceof HydraulicComponent) {
                    this.responseHydraulicComponent = (HydraulicComponent) component;
                } else {
                    throw new ConfigurationException("Allocate Components in Controller Heating Apartment Module",
                            "Component with id: " + component.id() + " is not from an expected Type");
                }
                break;
            case THRESHOLD:
                this.allocateAllThreshold(config);
                break;
            case HEAT_BOOSTER:
                OpenemsComponent component1;
                String deviceToGet1 = config.heatBoosterId();
                component1 = this.cpm.getComponent(deviceToGet1);
                if (component1 instanceof HeatBooster) {
                    this.optionalHeatBooster = (HeatBooster) component1;
                } else {
                    throw new ConfigurationException("ControllerHeatingApartmentModuleImpl: AllocateComponent",
                            "HeatBoosterId wrong: " + config.heatBoosterId() + " In Component: " + super.id());
                }
                break;
            default:
                throw new ConfigurationException("This shouldn't occur", "Weird...");
        }
    }

    /**
     * Allocates the ThresholdThermometer to the ApartmentCords.
     *
     * @param config the config of this Controller
     * @throws ConfigurationException             if Duplications are found or Components are not an instance of ThresholdThermometer
     * @throws OpenemsError.OpenemsNamedException if a Component with given Id cannot be found.
     */
    private void allocateAllThreshold(Config config) throws
            ConfigurationException, OpenemsError.OpenemsNamedException {
        ConfigurationException[] exConf = {null};
        OpenemsError.OpenemsNamedException[] exNamed = {null};
        List<String> thresholdIds = Arrays.asList(config.thresholdId());
        Set<String> duplicates = this.foundDuplicates(thresholdIds);
        if (duplicates.size() > 0) {
            throw new ConfigurationException("Allocate ThresholdThermometer: Duplicates",
                    "Duplicates were found in Config for Thresholds: " + duplicates.toString());
        }

        thresholdIds.forEach(threshold -> {
            if (exConf[0] == null && exNamed[0] == null) {
                try {
                    OpenemsComponent component = this.cpm.getComponent(threshold);
                    if (component instanceof ThermometerThreshold) {
                        this.thresholdThermometerMap.put(thresholdIds.indexOf(threshold), (ThermometerThreshold) component);
                    } else {
                        exConf[0] = new ConfigurationException("allocateAllThreshold",
                                "Component not an instance of ThresholdThermometer: " + component.id());
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    exNamed[0] = e;
                }
            }
        });
        if (exConf[0] != null) {
            throw exConf[0];
        }
        if (exNamed[0] != null) {
            throw exNamed[0];
        }
    }

    /**
     * Maps a Response ChannelAddress to an ApartmentCord.
     *
     * @param apartmentResponse the ChannelAddresses that will be responded to
     * @throws ConfigurationException if ChannelAddress is in a wrong format.
     */
    private void applyResponseToCords(String[] apartmentResponse) throws ConfigurationException {
        //Split entry into corresponding cords
        Map<Integer, String> keyEntryMap = new HashMap<>();
        for (int x = 0; x < apartmentResponse.length; x++) {
            keyEntryMap.put(x, apartmentResponse[x]);
        }
        ConfigurationException[] ex = {null};

        keyEntryMap.forEach((key, value) -> {
            if (ex[0] == null) {
                //Check if Entries are valid ChannelAddresses
                List<String> response = Arrays.asList(value.split(CONFIGURATION_SPLITTER));
                response.forEach(string -> {
                    if (ex[0] == null) {
                        try {
                            ChannelAddress channelAddressToPut = ChannelAddress.fromString(string);
                            if (this.responseToCords.containsKey(key)) {
                                this.responseToCords.get(key).add(channelAddressToPut);
                            } else {
                                List<ChannelAddress> channelList = new ArrayList<>();
                                channelList.add(channelAddressToPut);
                                this.responseToCords.put(key, channelList);
                            }
                        } catch (OpenemsError.OpenemsNamedException e) {
                            ex[0] = new ConfigurationException("ApplyResponseToChords", "ChannelAddress has wrong format: " + string);
                        }
                    }
                });
            }
        });

        if (ex[0] != null) {
            throw ex[0];
        }
    }

    /**
     * Maps ApartmentModules To Cord Map.
     *
     * @param apartmentCords the apartmentCords.
     * @throws ConfigurationException if something is wrong.
     */
    private void applyApartmentModules(String[] apartmentCords) throws ConfigurationException {
        //Split Map to corresponding Cord
        List<String> everyApartmentCord = new ArrayList<>();
        Map<Integer, String> keyEntryMap = new HashMap<>();
        for (int x = 0; x < apartmentCords.length; x++) {
            keyEntryMap.put(x, apartmentCords[x]);
            everyApartmentCord.addAll(Arrays.asList(apartmentCords[x].split(CONFIGURATION_SPLITTER)));
        }
        Set<String> duplicates = this.foundDuplicates(everyApartmentCord);
        if (duplicates.size() > 0) {
            throw new ConfigurationException("ApartmentModules", "Duplications in Config found of Apartmentmodules: " + duplicates.toString());
        }
        ConfigurationException[] ex = {null};
        keyEntryMap.forEach((key, value) -> {
            if (ex[0] == null) {
                //Check each entry of cord
                List<String> apartmentModuleIds = Arrays.asList(value.split(CONFIGURATION_SPLITTER));
                List<ApartmentModule> apartmentModuleList = new ArrayList<>();
                apartmentModuleIds.forEach(apartmentModule -> {
                    if (ex[0] == null) {
                        try {
                            OpenemsComponent component = this.cpm.getComponent(apartmentModule.trim());
                            if (component instanceof ApartmentModule) {
                                apartmentModuleList.add((ApartmentModule) component);
                            } else {
                                ex[0] = new ConfigurationException("Apply ApartmentModules ", "Couldn't find Component with id: " + apartmentModule);
                            }
                        } catch (OpenemsError.OpenemsNamedException e) {
                            ex[0] = new ConfigurationException("Apply ApartmentModules ", "Couldn't find Component with id: " + apartmentModule);
                        }
                    }
                });
                //Check if only 1 top-module exists
                int topModuleCounter = (int) apartmentModuleList.stream().filter(ApartmentModule::isTopAm).count();
                if (topModuleCounter != 1) {
                    ex[0] = new ConfigurationException("Apply apartmentModules", "Size of Top AM Modules: " + topModuleCounter);
                } else {
                    this.apartmentCords.put(key, apartmentModuleList);
                }
            }
        });

        if (ex[0] != null) {
            throw ex[0];
        }

    }


    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        if (this.configSuccess) {
            boolean emergencyStop = this.getEmergencyStopChannel().getNextWriteValue().isPresent()
                    && this.getEmergencyStopChannel().getNextWriteValue().get();
            if (this.checkMissingComponents() || emergencyStop) {
                this.disableAllComponents();
                //Note: EnableSignals will be set automatically to false/they getNextWriteValueAndReset --> if no enable --> they will go offline
                this.setState(ApartmentModuleControllerState.EMERGENCY_STOP);
            } else {
                this.heatRoutine();
            }
        } else {
            this.activationOrModifiedRoutine(this.config);
        }

    }

    /**
     * On Emergency disable every Cord.
     */
    private void disableAllCords() {
        this.responseToCords.forEach((key, channelAddressList) -> {
            channelAddressList.forEach(channelAddress -> {
                try {
                    Channel<?> channel = this.cpm.getChannel(channelAddress);
                    if (channel instanceof WriteChannel<?>) {
                        ((WriteChannel<?>) channel).setNextWriteValueFromObject(null);
                    } else {
                        channel.setNextValue(null);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write in Channel! " + super.id());
                }
            });
        });
    }

    private void disableAllComponents() throws OpenemsError.OpenemsNamedException {
        this.responseHydraulicComponent.setPowerLevel(HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
        if (this.useHeatBooster) {
            this.optionalHeatBooster.getHeatBoosterEnableSignalChannel().setNextWriteValueFromObject(null);
        }
        this.disableAllCords();
    }

    /**
     * Usual Routine that runs if no EmergencyStop is active.
     * Check for heatrequests and if response requirements are met --> respondchannels are set to true.
     * Also activate the heatpump on requests.
     *
     * @throws OpenemsError.OpenemsNamedException if the Corresponding respond channel couldn't be found.
     */
    private void heatRoutine() throws OpenemsError.OpenemsNamedException {

        boolean emergencyEnablePump = this.getEmergencyPumpStartChannel().getNextWriteValue().isPresent()
                && this.getEmergencyPumpStartChannel().getNextWriteValue().get();
        boolean emergencyResponse = this.getEmergencyEnableEveryResponseChannel().getNextWriteValue().isPresent()
                && this.getEmergencyEnableEveryResponseChannel().getNextWriteValue().get();

        AtomicReference<List<Integer>> keysThatHadResponse = new AtomicReference<>(new ArrayList<>());
        AtomicReference<Map<Integer, List<Thermometer>>> cordToThermometerMap = new AtomicReference<>(new HashMap<>());
        //check for Requests and put into keysThatHadResponse
        this.checkHeatRequests(emergencyResponse, keysThatHadResponse, cordToThermometerMap);
        this.applyReferenceTemperatureToAmOfCords(cordToThermometerMap);
        this.addKeysToCordsToHeatUp(keysThatHadResponse);
        //Activate pump on requests or on Emergency
        this.activatePumpOnRequestsOrEmergency(keysThatHadResponse, emergencyEnablePump || emergencyResponse);
        List<OpenemsError.OpenemsNamedException> errors = new ArrayList<>();
        //check if Temperature < setpoint or if emergency Response needs to react
        this.checkResponseRequirementAndRespond(emergencyResponse || emergencyEnablePump, emergencyResponse, errors);
        if (errors.size() > 0) {
            throw errors.get(0);
        }

    }

    /**
     * For Each cord check the thermometer, get the Min Value and set it as Reference for the TopAm to check.
     *
     * @param cordToThermometerMap the previous checked Cords and thermometer applied
     */
    private void applyReferenceTemperatureToAmOfCords(AtomicReference<Map<Integer, List<Thermometer>>> cordToThermometerMap) {
        cordToThermometerMap.get().forEach((key, value) -> {
            List<Integer> thermometerChannelList = new ArrayList<>();
            value.forEach(thermometer -> thermometerChannelList.add(thermometer.getTemperature().orElse(Integer.MAX_VALUE)));
            this.apartmentCords.get(key).stream().filter(ApartmentModule::isTopAm).findFirst().ifPresent(topAm -> {
                try {
                    topAm.getReferenceTemperatureChannel().setNextWriteValueFromObject(this.minMaxRoutine.executeRoutine(thermometerChannelList));
                    ;
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't apply min value to TopAm of cord " + key);
                }
            });
        });
    }

    /**
     * Adds the configured Keys to the CordsToHeatUp.
     *
     * @param keysThatHadResponse usually from run method. Contains all the keys to add.
     */
    private void addKeysToCordsToHeatUp(AtomicReference<List<Integer>> keysThatHadResponse) {
        keysThatHadResponse.get().forEach(key -> {
            if (this.cordsToHeatUp.contains(key) == false) {
                this.cordsToHeatUp.add(key);
            }
            this.apartmentCords.get(key).stream().filter(ApartmentModule::isTopAm).findAny().ifPresent(entry -> {
                try {
                    entry.heatRequestInApartmentCord().setNextWriteValueFromObject(true);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't set general HeatRequest");
                }
            });
        });
    }

    /**
     * Checks if the ResponseRequirements are met and then respond to the Channel of this.respondsToChords. Keys determined by keysThatHadRepsonse.
     *
     * @param emergencyOccurred checks if any Emergency occurred --> If not--> Set State to extra Heat if required
     * @param emergencyResponse Contains the emergencyResponse Boolean. Usually from own channel.
     * @param errors            the List of Errors that will be filled in case of any error --> thrown later.
     */
    private void checkResponseRequirementAndRespond(boolean emergencyOccurred, boolean emergencyResponse,
                                                    List<OpenemsError.OpenemsNamedException> errors) {


        this.thresholdThermometerMap.forEach((key, threshold) -> {
            if (this.cordsToHeatUp.contains(key)) {
                if (threshold.thermometerBelowGivenTemperature(this.getSetPointTemperature()) || emergencyResponse) {
                    this.responseToCords.get(key).forEach(channel -> {
                        Channel<?> channelToGet;
                        try {
                            channelToGet = this.cpm.getChannel(channel);
                            if (channelToGet instanceof WriteChannel<?> && channelToGet.getType().equals(OpenemsType.BOOLEAN)) {
                                ((WriteChannel<?>) channelToGet).setNextWriteValueFromObject(true);
                            } else {
                                this.log.error("Channel: " + channelToGet
                                        + " Not the correct Channel! Is either not WriteChannel or not Boolean!"
                                        + channelToGet.channelDoc().getAccessMode() + " " + channelToGet.getType());
                            }
                        } catch (OpenemsError.OpenemsNamedException e) {
                            errors.add(e);
                        }

                    });
                } else {
                    this.cordsToHeatUp.remove(key);
                }
            }
        });
        if (emergencyOccurred == false && this.cordsToHeatUp.size() > 0) {
            this.setState(ApartmentModuleControllerState.EXTRA_HEAT);
        }

    }

    /**
     * Activates the Heatpump in case of emergency or on HeatRequest.
     *
     * @param keysThatHadResponse List to check if any Requests are given. Usually filled by checkHeatRequest Method
     * @param emergencyEnablePump a Boolean to check if there is a case of emergency.
     */
    private void activatePumpOnRequestsOrEmergency(AtomicReference<List<Integer>> keysThatHadResponse,
                                                   boolean emergencyEnablePump) {
        try {
            if (keysThatHadResponse.get().size() > 0 || emergencyEnablePump || this.cordsToHeatUp.size() > 0) {
                this.responseHydraulicComponent.setPointPowerLevelChannel().setNextWriteValueFromObject(this.getSetPointPowerLevel());
                if (this.useHeatBooster) {
                    this.heatBoostEnableIfConditionsApply();
                }
                this.setState(ApartmentModuleControllerState.HEAT_PUMP_ACTIVE);
                if (emergencyEnablePump) {
                    this.setState(ApartmentModuleControllerState.EMERGENCY_ON);
                }
            } else {
                this.responseHydraulicComponent.setPointPowerLevelChannel().setNextWriteValueFromObject(HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
                this.setState(ApartmentModuleControllerState.IDLE);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't set PowerLevelPercent");
        }
    }

    /**
     * Enable HeatBooster if configured.
     */
    private void heatBoostEnableIfConditionsApply() {
        AtomicBoolean applyHeatBoost = new AtomicBoolean(false);
        this.thresholdThermometerMap.forEach((key, value) -> {
            if (applyHeatBoost.get() == false
                    && value.thermometerBelowGivenTemperature(this.heatBoosterThermometerActivationValue)) {
                applyHeatBoost.set(true);
            }
        });
        if (applyHeatBoost.get()) {
            try {
                this.optionalHeatBooster.setHeatBoosterEnableSignal(true);
                this.log.info("Applied HEATBOOST for : " + super.id());
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't activate HeatBooster of Controller: " + super.id());
            }
        }
    }

    /**
     * Checks if there are any HeatRequests available and add them to the List.
     *
     * @param emergencyResponse    In Case of an emergency this was set to true --> all responses will be set to true
     * @param keysThatHadResponse  the List that will be filled with keys of the heatRequest cords.
     * @param cordToThermometerMap the Cord To ThermometerList. Add Thermometers to check and set Reference for TopAm later.
     */
    private void checkHeatRequests(boolean emergencyResponse, AtomicReference<List<Integer>> keysThatHadResponse, AtomicReference<Map<Integer, List<Thermometer>>> cordToThermometerMap) {
        if (emergencyResponse) {
            keysThatHadResponse.set(new ArrayList<>(this.responseToCords.keySet()));
            this.setState(ApartmentModuleControllerState.EMERGENCY_ON);
        } else {
            this.apartmentCords.forEach((key, value) -> value.forEach(apartmentModule -> {
                if (apartmentModule.getLastKnownRequestStatusValue()) {
                    if (!keysThatHadResponse.get().contains(key)) {
                        keysThatHadResponse.get().add(key);
                    }
                    if (cordToThermometerMap.get().containsKey(key)) {
                        if (this.apartmentModuleToThermometerMap.containsKey(apartmentModule)) {
                            cordToThermometerMap.get().get(key).add(this.apartmentModuleToThermometerMap.get(apartmentModule));
                        }
                    } else {
                        List<Thermometer> thermometerListToAdd = new ArrayList<>();
                        if (this.apartmentModuleToThermometerMap.containsKey(apartmentModule)) {
                            thermometerListToAdd.add(this.apartmentModuleToThermometerMap.get(apartmentModule));
                        }
                        cordToThermometerMap.get().put(key, thermometerListToAdd);
                    }
                }
            }));
        }
    }

    /**
     * Checks for Missing Components --> e.g. Happens on Reactivation of subcomponents.
     *
     * @return a Boolean true if everything is ok false if component is missing or something.
     */

    private boolean checkMissingComponents() {
        if (this.timer.checkTimeIsUp(CHECK_MISSING_COMPONENT_IDENTIFIER)) {
            OpenemsComponent component;
            String id = null;
            AtomicBoolean componentNotFound = new AtomicBoolean(false);
            Map<Integer, ThermometerThreshold> copiedMap = new HashMap<>();
            try {
                this.thresholdThermometerMap.forEach((key, thresholdThermometer) -> {
                    if (componentNotFound.get() == false) {
                        String idThreshold;
                        idThreshold = thresholdThermometer.id();
                        OpenemsComponent componentOfThermometer = null;
                        try {
                            componentOfThermometer = this.cpm.getComponent(idThreshold);
                            if (thresholdThermometer.equals(componentOfThermometer) == false) {
                                if (componentOfThermometer instanceof ThermometerThreshold) {
                                    copiedMap.put(key, (ThermometerThreshold) componentOfThermometer);
                                } else {
                                    componentNotFound.set(true);
                                    copiedMap.put(key, thresholdThermometer);
                                }
                            } else {
                                copiedMap.put(key, thresholdThermometer);
                            }

                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.warn("Couldn't find Threshold!");
                            componentNotFound.set(true);
                        }
                    }
                });
                this.thresholdThermometerMap = copiedMap;

                AtomicInteger listSize = new AtomicInteger(0);
                this.apartmentCords.forEach((key, value) -> listSize.getAndAdd((int) value.stream().filter(entry -> {
                    try {
                        return !entry.isEnabled() || !entry.equals(this.cpm.getComponent(entry.id()));
                    } catch (OpenemsError.OpenemsNamedException e) {
                        componentNotFound.set(true);
                        this.log.warn("Couldn't find component");
                        return true;
                    }
                }).count()));
                if (listSize.get() > 0) {
                    this.applyApartmentModules(this.config.apartmentCords());
                    this.applyApartmentModuleToThermometer(this.config.apartmentToThermometer());
                } else {
                    this.apartmentModuleToThermometerMap.forEach((key, value) -> {
                        if (listSize.get() <= 0) {
                            try {
                                OpenemsComponent foundComponent = this.cpm.getComponent(value.id());
                                if (!value.equals(foundComponent)) {
                                    listSize.getAndIncrement();
                                }
                            } catch (OpenemsError.OpenemsNamedException e) {
                                componentNotFound.set(true);
                                this.log.warn("Couldn't find Thermometer!");
                            }
                        }
                    });
                    if (listSize.get() > 0) {
                        this.applyApartmentModuleToThermometer(this.config.apartmentToThermometer());
                    }
                }
                id = this.responseHydraulicComponent.id();
                component = this.cpm.getComponent(id);
                if (this.responseHydraulicComponent.equals(component) == false) {
                    if (component instanceof HydraulicComponent) {
                        this.responseHydraulicComponent = (HydraulicComponent) component;
                    } else {
                        componentNotFound.set(true);
                    }
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.error("Couldn't find OpenemsComponent with id: " + id);
                this.log.error("Component: " + this.id() + " will stop: EmergencyStop");
                componentNotFound.set(true);
            } catch (ConfigurationException e) {
                this.log.error("Couldn't apply ApartmentModuleChord when refreshing reference");
                componentNotFound.set(true);
            }
            if (componentNotFound.get() == false) {
                this.timer.resetTimer(CHECK_MISSING_COMPONENT_IDENTIFIER);
            }
            return componentNotFound.get();
        }
        return false;
    }

    @Override
    public String debugLog() {
        return this.getControllerState().toString();
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Checks for duplications in Configuration.
     *
     * @param checkList the List of Strings that will be checked
     * @return a Set of Strings that are duplicated
     */
    private Set<String> foundDuplicates(List<String> checkList) {
        Set<String> duplications = new HashSet<>();
        Set<String> normal = new HashSet<>();
        checkList.forEach(entry -> {
            if (normal.add(entry) == false) {
                duplications.add(entry);
            }
        });
        return duplications;
    }
}
