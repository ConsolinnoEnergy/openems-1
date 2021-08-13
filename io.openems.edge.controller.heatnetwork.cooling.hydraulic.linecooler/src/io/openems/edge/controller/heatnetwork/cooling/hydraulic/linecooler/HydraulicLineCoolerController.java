package io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.api.HydraulicLineCooler;
import io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.helperclass.ChannelLineCooler;
import io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.helperclass.LineCooler;
import io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.helperclass.OneChannelLineCooler;
import io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.helperclass.ValveLineCooler;
import io.openems.edge.cooler.decentral.api.DecentralCooler;
import io.openems.edge.heatsystem.components.Valve;
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
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


@Designate(ocd = Config.class, factory = true)
@Component(name = "HydraulicLineCoolerController",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class HydraulicLineCoolerController extends AbstractOpenemsComponent implements OpenemsComponent, Controller, HydraulicLineCooler {

    private final Logger log = LoggerFactory.getLogger(HydraulicLineCoolerController.class);

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
    private static final String IDENTIFIER_FALLBACK = "HYDRAULIC_LINE_COOLER_FALLBACK_IDENTIFIER";
    private static final String IDENTIFIER_CYCLE_RESTART = "HYDRAULIC_LINE_COOLER_CYCLE_RESTART_IDENTIFIER";
    private Thermometer tempSensorReference;
    private DecentralCooler decentralCoolerOptional;

    private int temperatureDefault = 600;
    private boolean shouldFallback = false;
    private boolean isFallback = false;
    private int minuteFallbackStart;
    private int minuteFallbackStop;
    private LineCooler lineCooler;
    private Boolean onlyMaxMin;
    private boolean useMinMax;
    //NOTE: If more Variation comes --> create extra "LineCooler"Classes in this controller etc


    public HydraulicLineCoolerController() {

        super(OpenemsComponent.ChannelId.values(),
                HydraulicLineCooler.ChannelId.values(),
                Controller.ChannelId.values());

    }

    @Activate
    void activate(ComponentContext context, Config config) throws Exception {

        super.activate(context, config.id(), config.alias(), config.enabled());
        this.tempSensorReference = (Thermometer) this.allocateComponent(config.tempSensorReference(), ComponentType.THERMOMETER);
        this.useMinMax = config.useMinMax();
        this.createSpecificLineCooler(config);

        if (config.useDecentralCooler()) {
            this.decentralCoolerOptional = (DecentralCooler) this.allocateComponent(config.decentralcoolerReference(),
                    ComponentType.DECENTRAL_COOLER);
        }
        this.temperatureDefault = config.temperatureDefault();
        this.shouldFallback = config.shouldFallback();
        this.minuteFallbackStart = config.minuteFallbackStart() % FULL_MINUTE;
        this.minuteFallbackStop = config.minuteFallbackStop() % FULL_MINUTE;
        this.maxValue().setNextWriteValueFromObject(config.maxValveValue());
        this.minValue().setNextWriteValueFromObject(config.minValveValue());
        this.onlyMaxMin = config.maxMinOnly();
        this.timerHandler = new TimerHandlerImpl(super.id(), this.cpm);
        this.timerHandler.addOneIdentifier(IDENTIFIER_FALLBACK, config.timerIdFallback(), config.timeoutMaxRemote());
        this.timerHandler.addOneIdentifier(IDENTIFIER_CYCLE_RESTART, config.timerIdRestartCycle(), config.timeoutRestartCycle());
    }

    /**
     * Create the specific LineCooler, usually determined by Config.
     *
     * @param config the config of this component
     * @throws Exception thrown if channelAddresses are wrong, Config is wrong or OpenEmsNameException.
     */
    private void createSpecificLineCooler(Config config) throws Exception {

        switch (config.valveOrChannel()) {
            case VALVE:
                this.createValveLineCooler(config.valueToWriteIsBoolean(), config.valveBypass());
                break;
            case ONE_CHANNEL:
                this.createLineCooler(config.valueToWriteIsBoolean(), config.channelAddress());
                break;
            case MULTIPLE_CHANNEL:
                int compareLength = config.useMinMax() ? CONFIG_CHANNEL_LIST_WITH_MIN_MAX_LENGTH : CONFIG_CHANNEL_LIST_LENGTH;
                if (config.channels().length != compareLength) {
                    throw new ConfigurationException("ChannelSize", "ChannelSize should be" + compareLength + " but is : " + config.channels().length);
                } else {
                    this.createChannelLineCooler(config.valueToWriteIsBoolean(), config.channels(), config.useMinMax());
                }
                break;
        }


    }

    /**
     * Creates the Multiple Channel Line Cooler if configured correctly.
     *
     * @param booleanControlled write true/false (0/1) on start/stop Cooling.
     * @param channels          the configured channels for the Linecooler.
     * @param useMinMax         use the MinMax reducing/increasing the min max value of the channel
     * @throws OpenemsError.OpenemsNamedException if ChannelAddresses couldn't be found.
     */
    private void createChannelLineCooler(boolean booleanControlled, String[] channels, boolean useMinMax) throws OpenemsError.OpenemsNamedException {
        ChannelAddress readAddress = ChannelAddress.fromString(channels[READ_ADDRESS_INDEX]);
        ChannelAddress writeAddress = ChannelAddress.fromString(channels[WRITE_ADDRESS_INDEX]);
        ChannelAddress maxAddress = null;
        ChannelAddress minAddress = null;
        if (useMinMax) {
            maxAddress = ChannelAddress.fromString(channels[MAX_ADDRESS_INDEX]);
            minAddress = ChannelAddress.fromString(channels[MIN_ADDRESS_INDEX]);
        }
        this.lineCooler = new ChannelLineCooler(booleanControlled, readAddress, writeAddress, maxAddress, minAddress, this.cpm, this.useMinMax);
    }

    /**
     * Creates the Valve Line Cooler.
     *
     * @param booleanControlled is the Valve booleanControlled -> min max or 0 100
     * @param valveId           the ValveId usually from config.
     * @throws Exception thrown if valveId couldn't be found or is not an instance of valve
     */
    private void createValveLineCooler(boolean booleanControlled, String valveId) throws Exception {
        Valve valve = (Valve) this.allocateComponent(valveId, ComponentType.VALVE);
        this.lineCooler = new ValveLineCooler(booleanControlled, valve, this.useMinMax);
    }

    /**
     * Create the OneChannel Line Cooler.
     *
     * @param useBooleanValue use 1/0 on StartCooling/StopCooling
     * @param channelAddress  channelAddress of the OneChannel to Control
     * @throws OpenemsError.OpenemsNamedException if ChannelAddress is wrong
     */
    private void createLineCooler(boolean useBooleanValue, String channelAddress) throws OpenemsError.OpenemsNamedException {
        this.lineCooler = new OneChannelLineCooler(useBooleanValue, ChannelAddress.fromString(channelAddress), this.cpm);
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
            case DECENTRAL_COOLER:
                return this.allocateDecentralCooler(deviceId);
        }
        throw new Exception("Internal Error, this shouldn't occur");

    }

    /**
     * Allocates a Cooler Component.
     *
     * @param deviceId the deviceId
     * @return the component if available and is correct Instance
     * @throws Exception if configuration is wrong.
     */

    private OpenemsComponent allocateDecentralCooler(String deviceId) throws Exception {
        if (this.cpm.getComponent(deviceId) instanceof DecentralCooler) {
            return this.cpm.getComponent(deviceId);
        }
        throw new Exception("Internal Error, this shouldn't occur");
    }

    /**
     * Allocates the Valve if LineCooler is a ValveLineCooler.
     *
     * @param device the deviceId
     * @return the Component
     * @throws Exception if config is wrong.
     */
    private OpenemsComponent allocateValve(String device) throws Exception {
        if (this.cpm.getComponent(device) instanceof Valve) {
            return this.cpm.getComponent(device);
        }
        throw new Exception("Internal Error, this shouldn't occur");

    }

    /**
     * Allocates a Thermometer for Reference
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
    public void run() throws OpenemsError.OpenemsNamedException {
        int tempReference = this.tempSensorReference.getTemperature().orElse(DEFAULT_TEMPERATURE);
        DateTime now = new DateTime();
        boolean decentralCoolRequestPresent = this.decentralCoolerOptional != null && this.decentralCoolerOptional.getNeedCoolChannel().value().orElse(false);
        boolean decentralCoolRequest = this.getCoolRequestCooler();
        Optional<Boolean> signal = this.enableSignal().getNextWriteValueAndReset();
        boolean missingEnableSignal = signal.isPresent() == false;
        boolean enableSignal = signal.orElse(false);

        if (missingEnableSignal) {
            if (this.shouldFallback && this.timerHandler.checkTimeIsUp(IDENTIFIER_FALLBACK)) {
                this.isFallback = true;
                this.isFallback().setNextValue(true);
            }
        } else {
            this.timerHandler.resetTimer(IDENTIFIER_FALLBACK);
            this.isFallback = false;
            this.isFallback().setNextValue(false);
        }

        if (this.isFallback) {
            if (this.isMinuteFallbackStart(now.getMinuteOfHour())) {
                this.startCooling();
            } else {
                this.stopCooling(now);
            }
        } else if (decentralCoolRequestPresent || missingEnableSignal == false) {
            if (decentralCoolRequest || enableSignal) {
                if (tempReference < this.temperatureDefault) {
                    if (this.lineCooler.getLifeCycle() == null || this.timerHandler.checkTimeIsUp(IDENTIFIER_CYCLE_RESTART)) {
                        this.startCooling();
                    }
                } else {
                    //temperature Reached
                    this.stopCooling(now);
                }
            } else {
                //cool enabled is False
                this.stopCooling(now);
            }
        }
    }

    /**
     * Starts the Cooling process -> if min Max should be used -> set the min max value
     * Start cooling afterwards by calling the LineCooler.
     */
    private void startCooling() {
        try {
            if (this.useMinMax) {
                this.lineCooler.setMaxAndMin(maxValue().value().orElse(100.d), minValue().value().orElse(0.d));
            }
            if (onlyMaxMin == false) {
                if (this.lineCooler.startCooling()) {
                    this.isRunning().setNextValue(true);
                }
            } else {
                this.lineCooler.onlySetMaxMin();
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Error while trying to cool!");
        }
    }

    /**
     * Stops the Cooling Process.
     *
     * @param lifecycle the current LifeCycle
     */
    private void stopCooling(DateTime lifecycle) {
        try {
            if (onlyMaxMin == false) {
                if (this.lineCooler.stopCooling(lifecycle)) {
                    this.isRunning().setNextValue(false);
                    this.timerHandler.resetTimer(IDENTIFIER_CYCLE_RESTART);
                }
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Error while trying to stop Cooling");
        }
    }

    private boolean getCoolRequestCooler() {
        if (this.decentralCoolerOptional != null) {
            return this.decentralCoolerOptional.getNeedCool();
        } else {
            return false;
        }
    }


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
