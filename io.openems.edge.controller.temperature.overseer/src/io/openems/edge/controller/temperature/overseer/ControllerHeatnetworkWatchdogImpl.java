package io.openems.edge.controller.temperature.overseer;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
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
    private static final String TIMER_ID = "TestTimer";
    private static final String ERROR_ID = "ErrorTimer";
    private String componentId;
    private boolean currentlyHeating;
    private boolean invert;
    private int errorDelta;
    protected Thermometer thermometerOne;
    protected Thermometer thermometerTwo;
    private TimerHandler timeHandler;
    private Channel<?> activateChannel;
    private String activateValue;
    private boolean configurationDone;
    private ExceptionalState target;
    private boolean errorState;

    public ControllerHeatnetworkWatchdogImpl() {
        super(OpenemsComponent.ChannelId.values(), Controller.ChannelId.values());
    }

    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin cm;

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException, IOException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.componentId = config.componentId();
        this.activateChannel = this.cpm.getChannel(ChannelAddress.fromString(config.enableChannel()));
        this.activateValue = config.expectedValue();
        this.invert = config.invert();
        this.errorDelta = config.errorDelta();
        this.timeHandler = new TimerHandlerImpl(config.timerId(), this.cpm);
        this.timeHandler.addOneIdentifier(TIMER_ID, config.timerId(), config.testPeriod());
        this.timeHandler.addOneIdentifier(ERROR_ID, config.timerId(), config.errorPeriod());
        this.configurationDone = config.configurationDone();
        this.allocateComponents(config.sourceThermometer(), config.targetThermometer(), config.targetComponentId());
        if (this.componentId != null) {
            this.update(this.cm.getConfiguration(this.servicePid(), "?"), "channelIdList", new ArrayList<>(this.cpm.getComponent(this.componentId).channels()), config.channelIdList().length);
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();

    }

    private void allocateComponents(String thermometerOne, String thermometerTwo, String target) throws OpenemsError.OpenemsNamedException, ConfigurationException {

        if (this.cpm.getComponent(thermometerOne) instanceof Thermometer) {
            this.thermometerOne = this.cpm.getComponent(thermometerOne);
        } else {
            throw new ConfigurationException("AllocateComponents",
                    "Not a Thermometer. Check if name is correct and try again, thermometerId: " + thermometerOne);
        }

        if (this.cpm.getComponent(thermometerTwo) instanceof Thermometer) {
            this.thermometerTwo = this.cpm.getComponent(thermometerTwo);
        } else {
            throw new ConfigurationException("AllocateComponents",
                    "Not a Thermometer. Check if name is correct and try again, thermometerId: " + thermometerTwo);
        }
        if (this.cpm.getComponent(target) instanceof ExceptionalState) {
            this.target = this.cpm.getComponent(target);
        } else {
            throw new ConfigurationException("AllocateComponents",
                    "Not an ExceptionalState Component; Check if Name is correct and try again. Id: " + target);
        }

    }


    /**
     * Checks if there is an Error in the Valve or Pump. And overwrite them with the ExcpetionalState.
     */

    @Override
    public void run() {
        if (this.configurationDone) {
            boolean done = false;
            boolean error = false;
            if (!this.errorState) {
                if (!this.currentlyHeating && this.checkActivateValue()) {
                    this.timeHandler.resetTimer(TIMER_ID);
                    this.currentlyHeating = true;
                } else if (this.currentlyHeating && this.timeHandler.checkTimeIsUp(TIMER_ID)) {
                    if (this.invert) {
                        error = (Math.abs(this.thermometerOne.getTemperatureValue() - this.thermometerTwo.getTemperatureValue()) <= this.errorDelta);
                    } else {
                        error = (Math.abs(this.thermometerOne.getTemperatureValue() - this.thermometerTwo.getTemperatureValue()) >= this.errorDelta);
                    }
                    done = true;
                }
                if (done) {
                    this.timeHandler.resetTimer(TIMER_ID);
                    this.currentlyHeating = false;
                }
            }
            try {
                this.handleError(error);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't write to the ExceptionalState Component. Reason: " + e.getMessage());
            }
        }
    }

    private void handleError(boolean error) throws OpenemsError.OpenemsNamedException {
        if (error) {
            this.timeHandler.resetTimer(ERROR_ID);
        }
        if (error || this.errorState) {
            this.errorState = true;
            this.target.getExceptionalStateEnableChannel().setNextWriteValue(true);
            this.target.getExceptionalStateValueChannel().setNextWriteValue(0);
            if (this.timeHandler.checkTimeIsUp(ERROR_ID)) {
                this.errorState = false;
            }
        }
    }

    private boolean checkActivateValue() {
        boolean active = false;
        if (this.activateChannel.value().isDefined()) {
            active = this.activateChannel.value().get().toString().equals(this.activateValue);
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
    public void update(Configuration config, String configTarget, List<Channel<?>> channelsGiven, int length) {
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

    private void updateConfig(Configuration config, String configTarget, List<Channel<?>> channels) {
        AtomicInteger counter = new AtomicInteger(0);
        String[] channelIdArray = new String[channels.size()];
        channels.forEach(channel -> channelIdArray[counter.getAndIncrement()] = channel.channelId().id());

        try {
            Dictionary<String, Object> properties = config.getProperties();
            properties.put(configTarget, this.propertyInput(Arrays.toString(channelIdArray)));
            config.update(properties);

        } catch (IOException e) {
            e.printStackTrace();
        }
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
