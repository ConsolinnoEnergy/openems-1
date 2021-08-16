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
import io.openems.edge.heater.decentral.api.DecentralHeater;
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
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


@Designate(ocd = Config.class, factory = true)
@Component(name = "HydraulicLineHeaterController",
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
    private DecentralHeater decentralHeaterOptional;

    private int temperatureDefault = 600;
    private boolean shouldFallback = false;
    private boolean isFallback = false;
    private int minuteFallbackStart;
    private int minuteFallbackStop;
    private LineHeater lineHeater;
    private Boolean onlyMaxMin;
    private boolean useMinMax;
    //NOTE: If more Variation comes --> create extra "LineHeater"Classes in this controller etc


    public HydraulicLineHeaterController() {

        super(OpenemsComponent.ChannelId.values(),
                HydraulicLineHeater.ChannelId.values(),
                Controller.ChannelId.values());

    }

    @Activate
    void activate(ComponentContext context, Config config) throws Exception {

        super.activate(context, config.id(), config.alias(), config.enabled());
        this.tempSensorReference = (Thermometer) this.allocateComponent(config.tempSensorReference(), ComponentType.THERMOMETER);
        this.useMinMax = config.useMinMax();
        this.createSpecificLineHeater(config);

        if (config.useDecentralHeater()) {
            this.decentralHeaterOptional = (DecentralHeater) this.allocateComponent(config.decentralheaterReference(),
                    ComponentType.DECENTRAL_HEATER);
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
     * Create the specific LineHeater, usually determined by Config.
     *
     * @param config the config of this component
     * @throws Exception thrown if channelAddresses are wrong, Config is wrong or OpenEmsNameException.
     */
    private void createSpecificLineHeater(Config config) throws Exception {

        switch (config.valveOrChannel()) {
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
        if (this.cpm.getComponent(deviceId) instanceof DecentralHeater) {
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
        boolean decentralHeatRequestPresent = this.decentralHeaterOptional != null && this.decentralHeaterOptional.getNeedHeatChannel().value().orElse(false);
        boolean decentralHeatRequest = this.getHeatRequestHeater();
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
                this.startHeating();
            } else {
                this.stopHeating(now);
            }
        } else if (decentralHeatRequestPresent || missingEnableSignal == false) {
            if (decentralHeatRequest || enableSignal) {
                if (tempReference < this.temperatureDefault) {
                    if (this.lineHeater.getLifeCycle() == null || this.timerHandler.checkTimeIsUp(IDENTIFIER_CYCLE_RESTART)) {
                        this.startHeating();
                    }
                } else {
                    //temperature Reached
                    this.stopHeating(now);
                }
            } else {
                //heat enabled is False
                this.stopHeating(now);
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
                this.lineHeater.setMaxAndMin(maxValue().value().orElse(100.d), minValue().value().orElse(0.d));
            }
            if (onlyMaxMin == false) {
                if (this.lineHeater.startHeating()) {
                    this.isRunning().setNextValue(true);
                }
            } else {
                this.lineHeater.onlySetMaxMin();
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Error while trying to heat!");
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
                    this.isRunning().setNextValue(false);
                    this.timerHandler.resetTimer(IDENTIFIER_CYCLE_RESTART);
                }
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Error while trying to stop Heating");
        }
    }

    private boolean getHeatRequestHeater() {
        if (this.decentralHeaterOptional != null) {
            return this.decentralHeaterOptional.getNeedHeat();
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
