package io.openems.edge.controller.heatnetwork.watchdog;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * This Controller watches over Temperatures and checks if they have significantly changed or not. If not,
 * there could be an issue with a corresponding heater, hydraulicComponent or the HeatNetwork is too cold.
 * If an error occurred -> write to the target (ExceptionalState) to disable the component.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Heatnetwork.ErrorWatchDog")
public class ControllerHeatnetworkWatchdogImpl extends AbstractOpenemsComponent implements Controller, OpenemsComponent {


    private final Logger log = LoggerFactory.getLogger(ControllerHeatnetworkWatchdogImpl.class);
    private static final String CHECK_THERMOMETER_DIFFERENCE_IDENTIFIER = "ThermometerDifference";
    private static final String CHECK_ERROR_HANDLING_TIME_IDENTIFIER = "ErrorTime";
    private static final String CHECK_MISSING_COMPONENTS = "MissingComponents";
    //Check for missing components after this time
    private static final int DELTA_TIME_MISSING_COMPONENTS = 30;
    private boolean currentlyHeatingOrCooling;
    private boolean invert;
    private int errorDelta;
    protected Thermometer sourceThermometer;
    protected Thermometer targetThermometer;
    private TimerHandler timeHandler;
    private ChannelAddress activateChannelAddress;
    private String activateValue;
    private boolean configurationDone;
    private ExceptionalState target;
    private boolean errorState;
    private boolean configSuccess;


    Config config;

    public ControllerHeatnetworkWatchdogImpl() {
        super(OpenemsComponent.ChannelId.values(), Controller.ChannelId.values());
    }

    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin cm;

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    /**
     * This method is called on activation or modification to set up base Configuration.
     *
     * @param config the config of this component.
     */
    private void activationOrModifiedRoutine(Config config) {
        try {
            this.config = config;
            this.activateChannelAddress = ChannelAddress.fromString(config.enableChannel());
            this.activateValue = config.expectedValue();
            this.invert = config.invert();
            this.errorDelta = config.errorDelta();
            this.timeHandler = new TimerHandlerImpl(config.timerId(), this.cpm);
            this.timeHandler.addOneIdentifier(CHECK_THERMOMETER_DIFFERENCE_IDENTIFIER, config.timerId(), config.testPeriod());
            this.timeHandler.addOneIdentifier(CHECK_ERROR_HANDLING_TIME_IDENTIFIER, config.timerId(), config.errorPeriod());
            this.timeHandler.addOneIdentifier(CHECK_MISSING_COMPONENTS, config.timerId(), DELTA_TIME_MISSING_COMPONENTS);
            this.configurationDone = config.configurationDone();
            this.allocateComponents(config.sourceThermometer(), config.targetThermometer(), config.targetComponentId());
            if (config.componentId() != null && !config.componentId().trim().equals("")) {
                this.update(this.cm.getConfiguration(this.servicePid(), "?"), "channelIdList", new ArrayList<>(this.cpm.getComponent(config.componentId()).channels()), config.channelIdList().length);
            }
            this.configSuccess = true;
        } catch (IOException | ConfigurationException | OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't apply Config. Trying again later!" + e.getMessage());
            this.configSuccess = false;
        }
    }


    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();

    }

    /**
     * Allocates external Components.
     *
     * @param sourceThermometer      the Source Thermometer.
     * @param targetThermometer      exceptionalStateTarget Thermometer
     * @param exceptionalStateTarget the OpenEMS Component implementing the {@link ExceptionalState}
     * @throws OpenemsError.OpenemsNamedException thrown if component cannot be found
     * @throws ConfigurationException             if the component is the wrong instance of
     */
    private void allocateComponents(String sourceThermometer, String targetThermometer, String exceptionalStateTarget) throws OpenemsError.OpenemsNamedException, ConfigurationException {

        if (this.cpm.getComponent(sourceThermometer) instanceof Thermometer) {
            this.sourceThermometer = this.cpm.getComponent(sourceThermometer);
        } else {
            throw new ConfigurationException("AllocateComponents",
                    "Not a Thermometer. Check if name is correct and try again, thermometerId: " + sourceThermometer);
        }

        if (this.cpm.getComponent(targetThermometer) instanceof Thermometer) {
            this.targetThermometer = this.cpm.getComponent(targetThermometer);
        } else {
            throw new ConfigurationException("AllocateComponents",
                    "Not a Thermometer. Check if name is correct and try again, thermometerId: " + targetThermometer);
        }
        if (this.cpm.getComponent(exceptionalStateTarget) instanceof ExceptionalState) {
            this.target = this.cpm.getComponent(exceptionalStateTarget);
        } else {
            throw new ConfigurationException("AllocateComponents",
                    "Not an ExceptionalState Component; Check if Name is correct and try again. Id: " + exceptionalStateTarget);
        }

    }


    /**
     * <p>
     * Checks if there is an Error in the Valve or Pump. And overwrite them with the ExceptionalState.
     * At first, check if the SourceComponent has the expected Value.
     * After that check if the time is up to calculate the difference between the two thermometer.
     * Should the difference between those Thermometer fall below the configured errorDelta (or exceeds if inverse)
     * Everything is fine and working as expected, the timer resets and tested again after the configured TestTime is up.
     * </p>
     * <p>
     * Should the errorDelta be exceeded or in case of inverse: Fall Below the errorDelta -> Error Handling is enabled
     * This means the target value get the {@link ExceptionalState#DEFAULT_MIN_EXCEPTIONAL_VALUE}.
     * </p>
     */

    @Override
    public void run() {
        if (this.configurationDone) {
            if (this.configSuccess) {
                if (this.timeHandler.checkTimeIsUp(CHECK_MISSING_COMPONENTS)) {
                    this.checkMissingComponents();
                }
                boolean done = false;
                boolean error = false;
                if (!this.errorState) {
                    if (!this.currentlyHeatingOrCooling && this.checkActivateValue()) {
                        this.timeHandler.resetTimer(CHECK_THERMOMETER_DIFFERENCE_IDENTIFIER);
                        this.currentlyHeatingOrCooling = true;
                    } else if (this.currentlyHeatingOrCooling && this.timeHandler.checkTimeIsUp(CHECK_THERMOMETER_DIFFERENCE_IDENTIFIER)) {
                        if (this.invert) {
                            error = (Math.abs(this.sourceThermometer.getTemperatureValue() - this.targetThermometer.getTemperatureValue()) <= this.errorDelta);
                        } else {
                            error = (Math.abs(this.sourceThermometer.getTemperatureValue() - this.targetThermometer.getTemperatureValue()) >= this.errorDelta);
                        }
                        done = true;
                    }
                    if (done) {
                        this.timeHandler.resetTimer(CHECK_THERMOMETER_DIFFERENCE_IDENTIFIER);
                        this.currentlyHeatingOrCooling = false;
                    }
                }
                try {
                    this.handleError(error);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't write to the ExceptionalState Component. Reason: " + e.getMessage());
                }
            } else {
                this.activationOrModifiedRoutine(this.config);
            }
        }
    }


    /**
     * Sometimes Components need to refresh References. This will set new references if needed.
     */
    private void checkMissingComponents() {
        try {
            OpenemsComponent component = this.cpm.getComponent(this.sourceThermometer.id());
            if (this.sourceThermometer.equals(component) == false) {
                if (component instanceof Thermometer) {
                    this.sourceThermometer = (Thermometer) component;
                }
            }
            component = this.cpm.getComponent(this.targetThermometer.id());
            if (this.targetThermometer.equals(component) == false) {
                if (component instanceof Thermometer) {
                    this.targetThermometer = (Thermometer) component;
                }
            }
            component = this.cpm.getComponent(this.target.id());
            if (this.target.equals(component) == false) {
                this.target = (ExceptionalState) component;
            }
            this.timeHandler.resetTimer(CHECK_MISSING_COMPONENTS);
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't refresh old references! Trying again next Cycle");

        }
    }

    /**
     * Check if an error Occurred.
     *
     * @param error is an error active.
     * @throws OpenemsError.OpenemsNamedException thrown if setNextWriteValue fails.
     */
    private void handleError(boolean error) throws OpenemsError.OpenemsNamedException {
        if (error) {
            this.timeHandler.resetTimer(CHECK_ERROR_HANDLING_TIME_IDENTIFIER);
        }
        if (error || this.errorState) {
            this.errorState = true;
            this.target.getExceptionalStateEnableSignalChannel().setNextWriteValue(true);
            this.target.getExceptionalStateValueChannel().setNextWriteValue(ExceptionalState.DEFAULT_MIN_EXCEPTIONAL_VALUE);
            this.log.warn("ERROR STATE ACTIVE, TARGET: " + this.target.id() + " SET TO EXCEPTIONAL STATE with: " + ExceptionalState.DEFAULT_MIN_EXCEPTIONAL_VALUE);
            if (this.timeHandler.checkTimeIsUp(CHECK_ERROR_HANDLING_TIME_IDENTIFIER)) {
                this.errorState = false;
            }
        }
    }

    /**
     * Check if the active Value of the targetComponent matches the expected value.
     *
     * @return a boolean.
     */
    private boolean checkActivateValue() {
        boolean active = false;
        try {
            Channel<?> channel = this.cpm.getChannel(this.activateChannelAddress);
            if (channel.value().isDefined()) {
                active = channel.value().get().toString().equals(this.activateValue);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't find Component with ChannelAddress: " + this.activateChannelAddress);
        }
        return active;

    }

    /**
     * Update method available for Components.
     *
     * @param config        config of the Component, will be updated automatically.
     * @param configTarget  target, where to put ChannelIds. Usually something like "ChannelIds".
     * @param channelsGiven Channels of the Component, collected by this.channels, filtered by "_Property"
     * @param length        length of the configTarget entries. If Length doesn't match ChannelSize --> Update.
     */
    public void update(Configuration config, String configTarget, List<Channel<?>> channelsGiven, int length) throws IOException {
        List<Channel<?>> channels =
                channelsGiven.stream().filter(entry ->
                        !entry.channelId().id().startsWith("_Property")
                ).collect(Collectors.toList());
        if (length != channels.size()) {
            this.updateConfig(config, configTarget, channels);
        }
    }


    /**
     * Update Config.
     *
     * @param config       Configuration of the OpenemsComponent
     * @param configTarget usually from Parent-->Config.
     * @param channels     usually from Parent --> Channels.
     */

    private void updateConfig(Configuration config, String configTarget, List<Channel<?>> channels) throws IOException {
        AtomicInteger counter = new AtomicInteger(0);
        String[] channelIdArray = new String[channels.size()];
        channels.forEach(channel -> channelIdArray[counter.getAndIncrement()] = channel.channelId().id());
        Dictionary<String, Object> properties = config.getProperties();
        properties.put(configTarget, this.propertyInput(Arrays.toString(channelIdArray)));
        config.update(properties);
    }

    /**
     * Workaround for OSGi Arrays to String --> Otherwise it won't be correct.
     *
     * @param types OpenemsTypes etc
     * @return String Array which will be put to new Config
     */
    private String[] propertyInput(String types) {
        types = types.replaceAll("\\[", "");
        types = types.replaceAll("]", "");
        types = types.replace(" ", "");
        return types.split(",");
    }


}
