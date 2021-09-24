package io.openems.edge.controller.heatnetwork.hydraulic.lineheater;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass.LineHeater;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api.HydraulicLineHeater;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass.ChannelLineHeater;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass.OneChannelLineHeater;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass.ValveLineHeater;
import io.openems.edge.heater.decentral.api.DecentralizedHeater;
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
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
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
@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Heatnetwork.LineHeater",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class HydraulicLineHeaterController extends AbstractOpenemsComponent implements OpenemsComponent, Controller, HydraulicLineHeater {

    private final Logger log = LoggerFactory.getLogger(HydraulicLineHeaterController.class);

    @Reference
    protected ComponentManager cpm;
    private static final int DEFAULT_TEMPERATURE = -127;
    private static final int CONFIG_CHANNEL_LIST_LENGTH = 2;
    private static final int CONFIG_CHANNEL_LIST_WITH_MIN_MAX_LENGTH = 4;
    private static final int READ_ADDRESS_INDEX = 0;
    private static final int WRITE_ADDRESS_INDEX = 1;
    private static final int MAX_ADDRESS_INDEX = 2;
    private static final int MIN_ADDRESS_INDEX = 3;
    private static final int FULL_MINUTE = 60;
    private TimerHandler timerHandler;
    //
    private static final String IDENTIFIER_FALLBACK = "HYDRAULIC_LINE_HEATER_FALLBACK_IDENTIFIER";
    private static final String IDENTIFIER_CYCLE_RESTART = "HYDRAULIC_LINE_HEATER_CYCLE_RESTART_IDENTIFIER";
    private Thermometer tempSensorReference;
    private DecentralizedHeater decentralHeaterOptional;

    private int temperatureDefault = 600;
    private boolean shouldFallback = false;
    private boolean isFallback = false;
    private int minuteFallbackStart;
    private int minuteFallbackStop;
    private LineHeater lineHeater;
    private Boolean onlyMaxMin;
    private boolean useMinMax;
    private boolean wasActiveBefore;
    private DecentralizedReactionType reactionType;
    private boolean configSuccess;
    Config config;
    //NOTE: If more Variation comes --> create extra "LineHeater"Classes in this controller etc


    public HydraulicLineHeaterController() {

        super(OpenemsComponent.ChannelId.values(),
                HydraulicLineHeater.ChannelId.values(),
                Controller.ChannelId.values());

    }

    @Activate
    void activate(ComponentContext context, Config config) {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled());
        try {
            this.activateOrModifiedRoutine(config);
        } catch (Exception e) {
            this.log.warn("Couldn't apply config. Try again later.");
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.configSuccess = false;
        try {
            this.activateOrModifiedRoutine(config);
        } catch (Exception e) {
            this.log.warn("Couldn't apply config. Try again later");
        }
    }

    /**
     * Sets up the configuration for this controller. Called on activation and modification.
     *
     * @param config the config of this component
     * @throws Exception if either the Components cannot be found or the Configuration is wrong.
     */
    private void activateOrModifiedRoutine(Config config) throws Exception {
        this.tempSensorReference = (Thermometer) this.allocateComponent(config.tempSensorReference(), ComponentType.THERMOMETER);
        this.useMinMax = config.useMinMax();
        this.createSpecificLineHeater(config);

        if (config.useDecentralHeater()) {
            this.decentralHeaterOptional = (DecentralizedHeater) this.allocateComponent(config.decentralheaterReference(),
                    ComponentType.DECENTRAL_HEATER);
            this.reactionType = config.reactionType();
        }
        this.temperatureDefault = config.temperatureDefault();
        this.shouldFallback = config.shouldFallback();
        this.minuteFallbackStart = config.minuteFallbackStart() % FULL_MINUTE;
        this.minuteFallbackStop = config.minuteFallbackStop() % FULL_MINUTE;
        this.maxValueChannel().setNextWriteValueFromObject(config.maxValveValue());
        this.minValueChannel().setNextWriteValueFromObject(config.minValveValue());
        this.onlyMaxMin = config.maxMinOnly();
        if (this.timerHandler != null) {
            this.timerHandler.removeComponent();
        }
        this.timerHandler = new TimerHandlerImpl(super.id(), this.cpm);
        this.timerHandler.addOneIdentifier(IDENTIFIER_FALLBACK, config.timerIdFallback(), config.timeoutMaxRemote());
        this.timerHandler.addOneIdentifier(IDENTIFIER_CYCLE_RESTART, config.timerIdRestartCycle(), config.timeoutRestartCycle());
        this.configSuccess = true;
    }

    /**
     * Create the specific LineHeater, usually determined by Config.
     *
     * @param config the config of this component
     * @throws Exception thrown if channelAddresses are wrong, Config is wrong or OpenEmsNameException.
     */
    private void createSpecificLineHeater(Config config) throws Exception {

        switch (config.lineHeaterType()) {
            case VALVE:
                this.createValveLineHeater(config.valueToWriteIsBoolean(), config.valveBypass());
                break;
            case ONE_CHANNEL:
                this.createLineHeater(config.valueToWriteIsBoolean(), config.channelAddress());
                break;
            case MULTIPLE_CHANNEL:
                int compareLength = config.useMinMax() ? CONFIG_CHANNEL_LIST_WITH_MIN_MAX_LENGTH : CONFIG_CHANNEL_LIST_LENGTH;
                if (config.channels().length != compareLength) {
                    throw new ConfigurationException("ChannelSize", "ChannelSize should be" + compareLength + " but is : " + config.channels().length);
                } else {
                    this.createChannelLineHeater(config.valueToWriteIsBoolean(), config.channels(), config.useMinMax());
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
        ChannelAddress readAddress = ChannelAddress.fromString(channels[READ_ADDRESS_INDEX]);
        ChannelAddress writeAddress = ChannelAddress.fromString(channels[WRITE_ADDRESS_INDEX]);
        ChannelAddress maxAddress = null;
        ChannelAddress minAddress = null;
        if (useMinMax) {
            maxAddress = ChannelAddress.fromString(channels[MAX_ADDRESS_INDEX]);
            minAddress = ChannelAddress.fromString(channels[MIN_ADDRESS_INDEX]);
        }
        this.lineHeater = new ChannelLineHeater(booleanControlled, readAddress, writeAddress, maxAddress, minAddress, this.cpm, this.useMinMax);
    }

    /**
     * Creates the Valve Line Heater.
     *
     * @param booleanControlled is the Valve booleanControlled -> min max or 0 100
     * @param valveId           the ValveId usually from config.
     * @throws Exception thrown if valveId couldn't be found or is not an instance of valve
     */
    private void createValveLineHeater(boolean booleanControlled, String valveId) throws Exception {
        HydraulicComponent valve = (HydraulicComponent) this.allocateComponent(valveId, ComponentType.VALVE);
        this.lineHeater = new ValveLineHeater(booleanControlled, valve, this.useMinMax);
    }

    /**
     * Create the OneChannel Line Heater.
     *
     * @param useBooleanValue use 1/0 on StartHeating/StopHeating
     * @param channelAddress  channelAddress of the OneChannel to Control
     * @throws OpenemsError.OpenemsNamedException if ChannelAddress is wrong
     */
    private void createLineHeater(boolean useBooleanValue, String channelAddress) throws OpenemsError.OpenemsNamedException {
        this.lineHeater = new OneChannelLineHeater(useBooleanValue, ChannelAddress.fromString(channelAddress), this.cpm);
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
            case VALVE:
                return this.allocateValve(deviceId);
            case THERMOMETER:
                return this.allocateThermometer(deviceId);
            case DECENTRAL_HEATER:
                return this.allocateDecentralHeater(deviceId);
        }
        throw new Exception("Internal Error, this shouldn't occur");

    }

    /**
     * Allocates a Heater Component.
     *
     * @param deviceId the deviceId
     * @return the component if available and is correct Instance
     * @throws Exception if configuration is wrong.
     */

    private OpenemsComponent allocateDecentralHeater(String deviceId) throws Exception {
        if (this.cpm.getComponent(deviceId) instanceof DecentralizedHeater) {
            return this.cpm.getComponent(deviceId);
        }
        throw new Exception("Internal Error, this shouldn't occur");
    }

    /**
     * Allocates the Valve if LineHeater is a ValveLineHeater.
     *
     * @param device the deviceId
     * @return the Component
     * @throws Exception if config is wrong.
     */
    private OpenemsComponent allocateValve(String device) throws Exception {
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


    @Override
    public void run() {
        if (this.configSuccess) {
            int tempReference = this.tempSensorReference.getTemperature().orElse(DEFAULT_TEMPERATURE);
            DateTime now = new DateTime();
            boolean decentralHeatRequestPresent = false;
            if (this.decentralHeaterOptional != null) {
                switch (this.reactionType) {
                    case NEED_HEAT:
                        decentralHeatRequestPresent = this.decentralHeaterOptional.getNeedHeatChannel().value().isDefined();
                        break;
                    case NEED_MORE_HEAT:
                        decentralHeatRequestPresent = this.decentralHeaterOptional.getNeedMoreHeatChannel().value().isDefined();
                        break;
                }
            }
            boolean decentralizedHeatRequest = this.getRequestDecentralizedHeater();
            Optional<Boolean> signal = this.enableSignalChannel().getNextWriteValueAndReset();
            boolean missingEnableSignal = signal.isPresent() == false;
            boolean enableSignal = signal.orElse(false);
            this.enableSignalChannel().setNextValue(enableSignal);

            if (missingEnableSignal) {
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
                if (this.isMinuteFallbackStart(now.getMinuteOfHour())) {
                    this.startHeating();
                } else {
                    this.stopHeating(now);
                }
            } else if (decentralHeatRequestPresent || missingEnableSignal == false) {
                if (decentralizedHeatRequest || enableSignal) {
                    if (tempReference < this.temperatureDefault) {
                        if (this.lineHeater.getLifeCycle() == null || (this.timerHandler.checkTimeIsUp(IDENTIFIER_CYCLE_RESTART))
                                || this.wasActiveBefore) {
                            this.startHeating();
                            this.timerHandler.resetTimer(IDENTIFIER_CYCLE_RESTART);
                        }
                    } else {
                        //temperature Reached
                        this.stopHeating(now);
                    }
                } else {
                    //heat enabled is False
                    this.stopHeating(now);
                }
            } else {
                this.stopHeating(now);
            }
        } else {
            try {
                this.activateOrModifiedRoutine(this.config);
            } catch (Exception e) {
                this.log.warn("Couldn't apply Config. Try again later or check your configuration!");
            }
        }
    }

    /**
     * Starts the Heating process -> if min Max should be used -> set the min max value
     * Start heating afterwards by calling the LineHeater.
     */
    private void startHeating() {
        try {
            if (this.useMinMax) {
                this.lineHeater.setMaxAndMin(maxValueChannel().value().orElse(100.d), minValueChannel().value().orElse(0.d));
            }
            if (this.onlyMaxMin == false) {
                if (this.lineHeater.startHeating()) {
                    this.isRunningChannel().setNextValue(true);
                }
            } else {
                this.lineHeater.onlySetMaxMin();
            }
            this.wasActiveBefore = true;
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Error while trying to heat!");
            this.wasActiveBefore = false;
        }
    }

    /**
     * Stops the Heating Process.
     *
     * @param lifecycle the current LifeCycle
     */
    private void stopHeating(DateTime lifecycle) {
        try {
            if (onlyMaxMin == false) {
                if (this.lineHeater.stopHeating(lifecycle)) {
                    this.isRunningChannel().setNextValue(false);
                }
            }
            this.wasActiveBefore = false;
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Error while trying to stop Heating");
        }
    }

    /**
     * Returns a request of the decentralized Heater. If the optional decentralized Heater is configured.
     *
     * @return the request or else false.
     */
    private boolean getRequestDecentralizedHeater() {
        if (this.decentralHeaterOptional != null) {
            switch (this.reactionType) {

                case NEED_HEAT:
                    return this.decentralHeaterOptional.getNeedHeat();
                case NEED_MORE_HEAT:
                    return this.decentralHeaterOptional.getNeedMoreHeat();
                default:
                    return false;
            }
        } else {
            return false;
        }
    }

    /**
     * <p>
     * Check if {@link Config#shouldFallback} should be set to true.
     * If this lineHeater activates it's fallback lineHeating.
     * -> e.g. Heat up Line for 15 min -> start at 00 and stop at 15.
     * </p>
     *
     * @param minuteOfHour current Minute of the hour.
     * @return true if HydraulicLineHeater should start fallback heating.
     */
    private boolean isMinuteFallbackStart(int minuteOfHour) {
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
}
