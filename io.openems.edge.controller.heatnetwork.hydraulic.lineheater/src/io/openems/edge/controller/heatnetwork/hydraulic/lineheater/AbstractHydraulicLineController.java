package io.openems.edge.controller.heatnetwork.hydraulic.lineheater;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api.HydraulicLineController;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass.ChannelLineController;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass.LineController;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass.OneChannelLineController;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass.ValveLineController;
import io.openems.edge.heater.decentral.api.DecentralizedCooler;
import io.openems.edge.heater.decentral.api.DecentralizedHeater;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerValueWrapper;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * The Hydraulic Line Heater Implementation. It allows a Line to heat up fast and can regulate a MinMax Value,
 * when: either Decentralized Heater has a request
 * OR if externally an EnableSignal was set.
 * After that, it either Activates One Channel with a true/false value, a Valve, Sets the MinMax amount, or you can set up
 * 4 Different channel for reading and writing.
 * Depends on the Configuration of the {@link LineHeaterType}.
 */

abstract class AbstractHydraulicLineController extends AbstractOpenemsComponent implements OpenemsComponent, Controller, HydraulicLineController {

    private final Logger log = LoggerFactory.getLogger(AbstractHydraulicLineController.class);

    @Reference
    protected ComponentManager cpm;

    private static final int DEFAULT_TEMPERATURE = -127;
    private static final int CONFIG_CHANNEL_LIST_LENGTH_NO_MIN_MAX_OR_ONLY_MIN_MAX = 2;
    private static final int CONFIG_CHANNEL_LIST_WITH_READ_WRITE_AND_MIN_MAX_LENGTH = 4;
    private static final int READ_ADDRESS_OR_MAX_ADDRESS_ON_ONLY_MIN_MAX_INDEX = 0;
    private static final int WRITE_ADDRESS_OR_MIN_ADDRESS_ON_ONLY_MIN_MAX_INDEX = 1;
    private static final int MAX_ADDRESS_INDEX = 2;
    private static final int MIN_ADDRESS_INDEX = 3;
    private static final int FULL_MINUTE = 60;
    private static final int FALLBACK_TEMPERATURE = 500;
    private static final int CHECK_MISSING_COMPONENTS = 60;
    private TimerHandler timerHandler;
    //
    private static final String IDENTIFIER_FALLBACK = "HYDRAULIC_HEATER_FALLBACK_IDENTIFIER";
    private static final String IDENTIFIER_CYCLE_RESTART = "HYDRAULIC_LINE_CYCLE_RESTART_IDENTIFIER";
    private static final String IDENTIFIER_CHECK_MISSING_COMPONENTS = "HYDRAULIC_LINE_CHECK_MISSING_IDENTIFIER";
    private Thermometer thermometerLine;
    private boolean useDecentralizedComponent;
    private DecentralizedHeater decentralizedHeaterOptional;
    private DecentralizedCooler decentralizedCoolerOptional;

    private ThermometerValueWrapper referenceTemperature;
    private boolean shouldFallback = false;
    private boolean isFallback = false;
    private int minuteFallbackStart;
    private int minuteFallbackStop;
    private LineController lineController;
    private Boolean onlyMinMax;
    private boolean useMinMax;
    private boolean wasActiveBefore;
    private DecentralizedReactionType reactionTypeHeaterOrCooler;
    protected boolean configSuccess;
    //NOTE: If more Variation comes --> create extra "LineHeater"Classes in this controller etc

    private HeaterType heaterType = HeaterType.HEATER;

    enum HeaterType {
        COOLER, HEATER
    }

    public AbstractHydraulicLineController(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                                           io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }


    protected void activate(ComponentContext context, String id, String alias, boolean enabled,
                            String referenceThermometer, boolean useMinMax, boolean useDecentralizedHeater,
                            String decentralizedHeaterId, DecentralizedReactionType reactionType,
                            String defaultTemperature, LineHeaterType lineHeaterType, boolean valueIsBoolean,
                            String channelAddress, String[] channels, String bypassValve, String timerId,
                            int deltaTimeFallback, int deltaTimeCycleRestart, boolean shouldFallback,
                            int minuteFallbackStart, int minuteFallbackStop,
                            double maxValveValue, double minValveValue, boolean minMaxOnly, HeaterType heaterType) {
        super.activate(context, id, alias, enabled);
        this.activateOrModifiedRoutine(referenceThermometer, useMinMax, useDecentralizedHeater, decentralizedHeaterId, reactionType,
                defaultTemperature, lineHeaterType, valueIsBoolean, channelAddress, channels, bypassValve, timerId,
                deltaTimeFallback, deltaTimeCycleRestart, shouldFallback, minuteFallbackStart, minuteFallbackStop,
                maxValveValue, minValveValue, minMaxOnly, heaterType);
    }

    void modified(ComponentContext context, String id, String alias, boolean enabled, String referenceThermometer, boolean useMinMax, boolean useDecentralizedHeater,
                  String decentralizedHeaterId, DecentralizedReactionType reactionType,
                  String defaultTemperature, LineHeaterType lineHeaterType, boolean valueIsBoolean,
                  String channelAddress, String[] channels, String bypassValve, String timerId,
                  int deltaTimeFallback, int deltaTimeCycleRestart, boolean shouldFallback,
                  int minuteFallbackStart, int minuteFallbackStop,
                  double maxValveValue, double minValveValue, boolean minMaxOnly, HeaterType heaterType) {
        super.modified(context, id, alias, enabled);
        this.configSuccess = false;
        this.activateOrModifiedRoutine(referenceThermometer, useMinMax, useDecentralizedHeater, decentralizedHeaterId, reactionType,
                defaultTemperature, lineHeaterType, valueIsBoolean, channelAddress, channels, bypassValve, timerId,
                deltaTimeFallback, deltaTimeCycleRestart, shouldFallback, minuteFallbackStart, minuteFallbackStop,
                maxValveValue, minValveValue, minMaxOnly, heaterType);
    }

    /**
     * Applies the Configuration of the LineController.
     *
     * @param referenceThermometer               the referenceThermometer this component has to determine it's temperature each run
     * @param useMinMax                          use MinMax of a HydraulicComponent (usually only necessary for a Valve. Pump does not support MinMax).
     * @param useDecentralizedComponent          does the LineController uses a {@link DecentralizedCooler} or {@link DecentralizedHeater}.
     * @param decentralizedComponentId           the ID of the decentralized Component if useDecentralizedComponent is true
     * @param reactionTypeDecentralizedComponent When to react to a decentralizedComponent
     * @param defaultTemperature                 the default Temperature, the LineHeater orients itself see {@link #startConditionApplies(int, int)}.
     * @param lineHeaterType                     the LineHeaterType e.g. Control a Valve, one or MultipleChannel.
     * @param valueIsBoolean                     if the Value you want to write is Boolean.
     * @param channelAddress                     the ChannelAddress if only one channel needs to be controlled.
     * @param channels                           the Channel field for multiChannel
     * @param bypassValveId                      the HydraulicComponent, usually a Valve
     * @param timerId                            the timer to use -> check for: CycleRestart, Time To Fallback, Check for missing Components
     * @param deltaTimeFallback                  -> delta Time for Fallback use.
     * @param deltaTimeCycleRestart              -> Delta Time CycleRestart
     * @param shouldFallback                     should the LineHeater use FallbackLogic
     * @param minuteFallbackStart                TimeFrame for FallbackWindow
     * @param minuteFallbackStop                 TimeFrame for FallbackWindow
     * @param maxValveValue                      maximum valve value per default
     * @param minValveValue                      minimum valve value per default
     * @param minMaxOnly                         only set min and max value
     * @param heaterType                         HeaterType -> Cooler or Heater
     */
    protected void activateOrModifiedRoutine(String referenceThermometer, boolean useMinMax, boolean useDecentralizedComponent,
                                             String decentralizedComponentId, DecentralizedReactionType reactionTypeDecentralizedComponent,
                                             String defaultTemperature, LineHeaterType lineHeaterType, boolean valueIsBoolean,
                                             String channelAddress, String[] channels, String bypassValveId, String timerId,
                                             int deltaTimeFallback, int deltaTimeCycleRestart, boolean shouldFallback,
                                             int minuteFallbackStart, int minuteFallbackStop,
                                             double maxValveValue, double minValveValue, boolean minMaxOnly, HeaterType heaterType) {
        try {
            this.heaterType = heaterType;
            this.thermometerLine = (Thermometer) this.allocateComponent(referenceThermometer, ComponentType.THERMOMETER);
            this.useMinMax = useMinMax;
            this.onlyMinMax = minMaxOnly;
            this.createSpecificLineHeater(lineHeaterType, valueIsBoolean, bypassValveId, channelAddress, useMinMax, channels);

            this.useDecentralizedComponent = useDecentralizedComponent;

            if (this.useDecentralizedComponent) {
                this.reactionTypeHeaterOrCooler = reactionTypeDecentralizedComponent;
                switch (this.heaterType) {
                    case COOLER:
                        this.decentralizedCoolerOptional = (DecentralizedCooler) this.allocateComponent(decentralizedComponentId,
                                ComponentType.DECENTRALIZED_COOLER);
                        break;
                    case HEATER:
                    default:
                        this.decentralizedHeaterOptional = (DecentralizedHeater) this.allocateComponent(decentralizedComponentId,
                                ComponentType.DECENTRALIZED_HEATER);
                        break;
                }
            }
            this.referenceTemperature = new ThermometerValueWrapper(defaultTemperature);
            this.shouldFallback = shouldFallback;
            this.minuteFallbackStart = minuteFallbackStart % FULL_MINUTE;
            this.minuteFallbackStop = minuteFallbackStop % FULL_MINUTE;
            this.maxValueChannel().setNextWriteValueFromObject(maxValveValue);
            this.minValueChannel().setNextWriteValueFromObject(minValveValue);

            if (this.timerHandler != null) {
                this.timerHandler.removeComponent();
            }
            this.timerHandler = new TimerHandlerImpl(super.id(), this.cpm);
            this.timerHandler.addOneIdentifier(IDENTIFIER_FALLBACK, timerId, deltaTimeFallback);
            this.timerHandler.addOneIdentifier(IDENTIFIER_CYCLE_RESTART, timerId, deltaTimeCycleRestart);
            this.timerHandler.addOneIdentifier(IDENTIFIER_CHECK_MISSING_COMPONENTS, timerId, CHECK_MISSING_COMPONENTS);
            this.configSuccess = true;
        } catch (Exception e) {
            this.log.warn("Couldn't apply config, trying again later!");
            this.configSuccess = false;
        }
    }

    /**
     * Create the specific LineHeater, usually determined by Config.
     *
     * @param type                  the LineHeaterType -> do you control a HydraulicComponent Directly or oneChannel or use MultipleChannel
     * @param valueToWriteIsBoolean is the Value you want to write a Boolean.
     * @param bypassValve           the Id of the Component if LineHeaterType is Directly Controlling a HydraulicComponent
     *                              Note: Most of the Time it is a Valve instead of a general HydraulicComponent -> Reason for naming
     * @param channelAddress        the ChannelAddress for oneChannel Only
     * @param useMinMax             is minMax configured
     * @param channels              Channels for MultipleChannel config and MultiChannelLineHeater
     * @throws Exception thrown if channelAddresses are wrong, Config is wrong or OpenEmsNameException.
     */
    private void createSpecificLineHeater(LineHeaterType type, boolean valueToWriteIsBoolean,
                                          String bypassValve, String channelAddress, boolean useMinMax,
                                          String[] channels
    ) throws Exception {

        switch (type) {
            case VALVE:
                this.createValveLineHeater(valueToWriteIsBoolean, bypassValve);
                break;
            case ONE_CHANNEL:
                this.createLineHeater(valueToWriteIsBoolean, channelAddress);
                break;
            case MULTIPLE_CHANNEL:
                int compareLength = useMinMax ? CONFIG_CHANNEL_LIST_WITH_READ_WRITE_AND_MIN_MAX_LENGTH : CONFIG_CHANNEL_LIST_LENGTH_NO_MIN_MAX_OR_ONLY_MIN_MAX;
                if (channels.length != compareLength) {
                    throw new ConfigurationException("ChannelSize", "ChannelSize should be" + compareLength + " but is : " + channels.length);
                } else {
                    this.createChannelLineHeater(valueToWriteIsBoolean, channels, useMinMax);
                }
                break;
        }
    }

    /**
     * Creates the Multiple Channel Line Heater if configured correctly.
     *
     * @param booleanControlled write true/false (0/1) on start/stop Heating.
     * @param channels          the configured channels for the Lineheater.
     * @param useMinMax         use the MinMax reducing/increasing the min max value of the channel
     * @throws OpenemsError.OpenemsNamedException if ChannelAddresses couldn't be found.
     */
    private void createChannelLineHeater(boolean booleanControlled, String[] channels, boolean useMinMax) throws OpenemsError.OpenemsNamedException {
        ChannelAddress readAddress = ChannelAddress.fromString(channels[READ_ADDRESS_OR_MAX_ADDRESS_ON_ONLY_MIN_MAX_INDEX]);
        ChannelAddress writeAddress = ChannelAddress.fromString(channels[WRITE_ADDRESS_OR_MIN_ADDRESS_ON_ONLY_MIN_MAX_INDEX]);
        ChannelAddress maxAddress = null;
        ChannelAddress minAddress = null;
        if (useMinMax && !this.onlyMinMax) {
            maxAddress = ChannelAddress.fromString(channels[MAX_ADDRESS_INDEX]);
            minAddress = ChannelAddress.fromString(channels[MIN_ADDRESS_INDEX]);
        }
        if (this.onlyMinMax) {
            maxAddress = readAddress;
            minAddress = writeAddress;
            this.lineController = new ChannelLineController(booleanControlled, null, null, maxAddress, minAddress, this.cpm, this.useMinMax);
        } else {
            this.lineController = new ChannelLineController(booleanControlled, readAddress, writeAddress, maxAddress, minAddress, this.cpm, this.useMinMax);
        }
    }

    /**
     * Creates the Valve Line Heater.
     *
     * @param booleanControlled is the Valve booleanControlled -> min max or 0 100
     * @param valveId           the ValveId usually from config.
     * @throws Exception thrown if valveId couldn't be found or is not an instance of valve
     */
    private void createValveLineHeater(boolean booleanControlled, String valveId) throws Exception {
        HydraulicComponent valve = (HydraulicComponent) this.allocateComponent(valveId, ComponentType.HYDRAULIC_COMPONENT);
        this.lineController = new ValveLineController(booleanControlled, valve, this.useMinMax);
    }

    /**
     * Create the OneChannel Line Heater.
     *
     * @param useBooleanValue use 1/0 on StartHeating/StopHeating
     * @param channelAddress  channelAddress of the OneChannel to Control
     * @throws OpenemsError.OpenemsNamedException if ChannelAddress is wrong
     */
    private void createLineHeater(boolean useBooleanValue, String channelAddress) throws OpenemsError.OpenemsNamedException {
        this.lineController = new OneChannelLineController(useBooleanValue, ChannelAddress.fromString(channelAddress), this.cpm);
    }

    /**
     * Allocates the Component depending on the type.
     *
     * @param deviceId the Id of the device
     * @param type     the ComponentType of given Id, determined by internal methods
     * @return the OpenemsComponent if not correctly configured
     * @throws Exception if either OpenemsComponent is not available or not an instance of given type
     */
    private OpenemsComponent allocateComponent(String deviceId, ComponentType type) throws Exception {
        switch (type) {
            case HYDRAULIC_COMPONENT:
                return this.allocateHydraulicComponent(deviceId);
            case DECENTRALIZED_COOLER:
                return this.allocateDecentralizedCooler(deviceId);
            case THERMOMETER:
                return this.allocateThermometer(deviceId);
            case DECENTRALIZED_HEATER:
                return this.allocateDecentralizedHeater(deviceId);
        }
        throw new Exception("Internal Error, this shouldn't occur");

    }

    /**
     * Allocates a DecentralizedCooler.
     *
     * @param deviceId the deviceId
     * @return the component if available and is correct Instance
     * @throws ConfigurationException             if component is not an instance of DecentralizedCooler
     * @throws OpenemsError.OpenemsNamedException if Component cannot be found.
     */

    private OpenemsComponent allocateDecentralizedCooler(String deviceId) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        if (this.cpm.getComponent(deviceId) instanceof DecentralizedCooler) {
            return this.cpm.getComponent(deviceId);
        }
        throw new ConfigurationException("allocateDecentralizedCooler", "OpenEmsComponent not an Instance of DecentralizedCooler!");
    }

    /**
     * Allocates a DecentralizedHeater.
     *
     * @param deviceId the deviceId
     * @return the component if available and is correct Instance
     * @throws ConfigurationException             if component is not an instance of DecentralizedHeater
     * @throws OpenemsError.OpenemsNamedException if Component cannot be found.
     */

    private OpenemsComponent allocateDecentralizedHeater(String deviceId) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        if (this.cpm.getComponent(deviceId) instanceof DecentralizedHeater) {
            return this.cpm.getComponent(deviceId);
        }
        throw new ConfigurationException("allocateDecentralizedHeater", "OpenEmsComponent not an Instance of DecentralizedHeater!");
    }

    /**
     * Allocates the Valve if LineHeater is a ValveLineHeater.
     *
     * @param device the deviceId
     * @return the Component
     * @throws Exception if config is wrong.
     */
    private OpenemsComponent allocateHydraulicComponent(String device) throws Exception {
        if (this.cpm.getComponent(device) instanceof HydraulicComponent) {
            return this.cpm.getComponent(device);
        }
        throw new Exception("Internal Error, this shouldn't occur");

    }

    /**
     * Allocates a Thermometer for Reference.
     *
     * @param device the deviceId
     * @return the component
     * @throws OpenemsError.OpenemsNamedException if id could not be found
     * @throws ConfigurationException             if component not an instance of valve
     */
    private OpenemsComponent allocateThermometer(String device) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (this.cpm.getComponent(device) instanceof Thermometer) {
            return this.cpm.getComponent(device);
        } else {
            throw new ConfigurationException("The Device " + device
                    + " is not a TemperatureSensor", "Configuration is wrong of TemperatureSensor");
        }
    }

    /**
     * The Logic of the abstract Line Controller.
     * 1. Check if Requests are Present
     * 2. If no Requests are Present -> check if the Controller should use a Fallback
     * 3. If Requests are Present AND they say to execute Logic -> check Temperatures and Cycle
     * 4. If start conditions apply -> execute Logic e.g. Open up a Valve
     */
    protected void abstractRun() {
        if (this.configSuccess) {
            if (this.timerHandler.checkTimeIsUp(IDENTIFIER_CHECK_MISSING_COMPONENTS)) {
                this.checkIsComponentOld();
            }
            int temperatureCurrent = this.thermometerLine.getTemperature().orElse(DEFAULT_TEMPERATURE);
            Value<Boolean> decentralizedRequestValue = this.getDecentralizedRequestIsDefinedAndValue();
            //check if Request present -> need to Fallback or not
            boolean decentralizedRequestPresent = decentralizedRequestValue != null && decentralizedRequestValue.isDefined();
            //check for value -> need to heat up/cool down or not
            boolean decentralizedRequest = decentralizedRequestPresent && decentralizedRequestValue.orElse(false);

            Optional<Boolean> signal = this.enableSignalChannel().getNextWriteValueAndReset();
            this.enableSignalChannel().setNextValue(signal.orElse(false));

            if (signal.isPresent() == false && decentralizedRequestPresent == false) {
                if (this.shouldFallback && this.timerHandler.checkTimeIsUp(IDENTIFIER_FALLBACK)) {
                    this.isFallback = true;
                    this.isFallbackChannel().setNextValue(true);
                }
            } else {
                this.timerHandler.resetTimer(IDENTIFIER_FALLBACK);
                this.isFallback = false;
                this.isFallbackChannel().setNextValue(false);
            }
            if (this.isFallback) {
                if (this.isMinuteFallbackStart()) {
                    this.startProcess();
                } else {
                    this.stopProcess();
                }
            } else if (decentralizedRequestPresent || signal.isPresent()) {
                if (decentralizedRequest || signal.orElse(false)) {
                    int temperatureReference;
                    try {
                        temperatureReference = this.referenceTemperature.validateChannelAndGetValue(this.cpm);
                    } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
                        temperatureReference = FALLBACK_TEMPERATURE;
                    }
                    if (this.startConditionApplies(temperatureCurrent, temperatureReference)) {
                        if ((this.timerHandler.checkTimeIsUp(IDENTIFIER_CYCLE_RESTART))
                                || this.wasActiveBefore) {
                            this.startProcess();
                            this.timerHandler.resetTimer(IDENTIFIER_CYCLE_RESTART);
                        }
                    } else {
                        //temperatureReference Reached
                        this.stopProcess();
                    }
                } else {
                    //decentralized request is False or not Enabled
                    this.stopProcess();
                }
            } else {
                // no fallback no Request Present
                this.stopProcess();
            }
        }

    }

    /**
     * Check for old References and reset those.
     * Reset Reference for Thermometer, and decentralizedComponent if configured.
     */
    private void checkIsComponentOld() {
        try {
            OpenemsComponent component = this.cpm.getComponent(this.thermometerLine.id());
            if (component instanceof Thermometer && !this.thermometerLine.equals(component)) {
                this.thermometerLine = (Thermometer) component;
            }
            if (this.useDecentralizedComponent) {
                switch (this.heaterType) {
                    case COOLER:
                        if (this.decentralizedCoolerOptional != null) {
                            component = this.cpm.getComponent(this.decentralizedCoolerOptional.id());
                            if (component instanceof DecentralizedCooler && !this.decentralizedCoolerOptional.equals(component)) {
                                this.decentralizedCoolerOptional = (DecentralizedCooler) component;
                            }
                        }
                        break;
                    case HEATER:
                        if (this.decentralizedHeaterOptional != null) {
                            component = this.cpm.getComponent(this.decentralizedHeaterOptional.id());
                            if (component instanceof DecentralizedCooler && !this.decentralizedHeaterOptional.equals(component)) {
                                this.decentralizedHeaterOptional = (DecentralizedHeater) component;
                            }
                        }
                        break;
                }
            }
            this.timerHandler.resetTimer(IDENTIFIER_CHECK_MISSING_COMPONENTS);
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't allocate missing/deprecated Components. Trying again next Cycle!");
        }
    }

    /**
     * Checks if the StartCondition applies, determined by the HeaterType.
     *
     * @param tempCurrent          the current Temperature measured this run.
     * @param temperatureReference the reference Temperature -> needs to reach this at leas
     * @return the comparison between the two Temperatures.
     */

    private boolean startConditionApplies(int tempCurrent, int temperatureReference) {
        switch (this.heaterType) {
            case COOLER:
                return tempCurrent > temperatureReference;
            case HEATER:
            default:
                return tempCurrent < temperatureReference;
        }
    }

    /**
     * Check if the DecentralizedComponent has a Request. The detailed request is determined by configuration.
     *
     * @return if request is Defined
     */
    private Value<Boolean> getDecentralizedRequestIsDefinedAndValue() {
        if (this.useDecentralizedComponent) {
            switch (this.heaterType) {
                case COOLER:
                    if (this.decentralizedCoolerOptional != null) {
                        switch (this.reactionTypeHeaterOrCooler) {
                            case NEED_COOL:
                                return this.decentralizedCoolerOptional.getNeedCoolChannel().value();
                            case NEED_MORE_COOL:
                                return this.decentralizedCoolerOptional.getNeedMoreCoolChannel().value();
                        }
                    }
                    break;
                case HEATER:
                    if (this.decentralizedHeaterOptional != null) {
                        switch (this.reactionTypeHeaterOrCooler) {
                            case NEED_MORE_HEAT:
                                return this.decentralizedHeaterOptional.getNeedMoreHeatChannel().value();
                            case NEED_HEAT:
                                return this.decentralizedHeaterOptional.getNeedHeatChannel().value();
                        }
                    }
                    break;
            }
        }
        return null;
    }

    /**
     * Starts the Heating process -> if min Max should be used -> set the min max value
     * Start heating afterwards by calling the LineHeater.
     */
    private void startProcess() {
        try {
            if (this.useMinMax) {
                this.lineController.setMaxAndMin(maxValueChannel().value().orElse((double) HydraulicComponent.DEFAULT_MAX_POWER_VALUE),
                        minValueChannel().value().orElse((double) HydraulicComponent.DEFAULT_MIN_POWER_VALUE));
            }
            if (this.onlyMinMax) {
                this.lineController.onlySetMaxMin();
            } else {
                this.lineController.startProcess();
            }
            this.wasActiveBefore = true;
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Error while trying to heat!");
        }
    }

    /**
     * Stops the Heating Process.
     */
    private void stopProcess() {
        try {
            if (onlyMinMax == false) {
                this.lineController.stopProcess();
            }
            this.wasActiveBefore = false;
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Error while trying to stop Heating");
        }
    }


    /**
     * <p>
     * On Fallback, Check the TimeFrame -> execute Fallback Process (e.g. open up Valve) when within the configured
     * Timeframe.
     * </p>
     *
     * @return true if HydraulicLineHeater should start fallback heating.
     */
    private boolean isMinuteFallbackStart() {
        int minuteOfHour = DateTime.now().getMinuteOfHour();
        boolean isStartAfterStop = this.minuteFallbackStart > this.minuteFallbackStop;
        if (isStartAfterStop) {
            //if start = 45 and stop 15
            //logic switches start from 00-15 or from 45-60
            return minuteOfHour >= this.minuteFallbackStart || minuteOfHour < this.minuteFallbackStop;
        } else {
            return minuteOfHour >= this.minuteFallbackStart && minuteOfHour < this.minuteFallbackStop;
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Get the DecentralizedReactionType by DecentralizedHeaterType.
     *
     * @param reactionType the DecentralizedHeaterReactionType
     * @return the DecentralizedReactionType
     */
    protected DecentralizedReactionType getDecentralizedReactionType(DecentralizedHeaterReactionType reactionType) {
        switch (reactionType) {

            case NEED_MORE_HEAT:
                return DecentralizedReactionType.NEED_MORE_HEAT;
            case NEED_HEAT:
            default:
                return DecentralizedReactionType.NEED_HEAT;
        }
    }

    /**
     * Get the DecentralizedReactionType by DecentralizedCoolerType.
     *
     * @param reactionType the DecentralizedCoolerReactionType
     * @return the DecentralizedReactionType
     */
    protected DecentralizedReactionType getDecentralizedReactionType(DecentralizedCoolerReactionType reactionType) {
        switch (reactionType) {

            case NEED_MORE_COOL:
                return DecentralizedReactionType.NEED_MORE_COOL;
            case NEED_COOL:
            default:
                return DecentralizedReactionType.NEED_COOL;
        }
    }

}
