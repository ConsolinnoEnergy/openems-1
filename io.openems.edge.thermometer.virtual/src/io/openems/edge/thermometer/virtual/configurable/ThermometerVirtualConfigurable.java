package io.openems.edge.thermometer.virtual.configurable;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerConfigurable;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
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

import java.io.IOException;
import java.util.Dictionary;
import java.util.Optional;

/**
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Thermometer.Virtual.Configurable", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
        })

public class ThermometerVirtualConfigurable extends AbstractOpenemsComponent implements OpenemsComponent,
        ThermometerConfigurable, EventHandler {

    @Reference
    ConfigurationAdmin ca;

    @Reference
    ComponentManager cpm;

    private boolean autoRun = false;

    private boolean isActive;

    private boolean useInactiveTemperature;

    private static final String ENABLE_SIGNAL_IDENTIFIER = "ThermometerVirtualEnableSignalIdentifier";

    TimerHandler timerHandler;

    private boolean configurationSuccess;


    Config config;

    private final Logger log = LoggerFactory.getLogger(ThermometerVirtualConfigurable.class);

    public ThermometerVirtualConfigurable() {
        super(OpenemsComponent.ChannelId.values(),
                ThermometerConfigurable.ChannelId.values(),
                Thermometer.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        this.activationOrModifiedRoutine();
    }


    @Modified
    void modified(ComponentContext context, Config config) {
        this.configurationSuccess = false;
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        this.activationOrModifiedRoutine();

    }

    private void activationOrModifiedRoutine() {
        this._getActiveTemperature().setNextValue(this.config.activeTemperature());
        this._getInactiveTemperature().setNextValue(this.config.inactiveTemperature());
        this.autoRun = this.config.autoApply();
        this.useInactiveTemperature = this.config.useInactiveTemperature();
        this.createTimer(this.config.id(), this.config.timerID(), this.config.waitTime());
    }


    private void createTimer(String id, String timerId, int maxWaitTime) {
        try {
            if (this.timerHandler != null) {
                this.timerHandler.removeComponent();
            }
            this.timerHandler = new TimerHandlerImpl(id, this.cpm);
            this.timerHandler.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, timerId, maxWaitTime);
            this.configurationSuccess = true;
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.configurationSuccess = false;
            this.log.warn("Couldn't apply Config. Trying again later.");
        }
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (this.configurationSuccess) {
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {

                if (this._getDefaultActiveTemperatureChannel().getNextWriteValue().isPresent()) {
                    this.updateConfig(true);
                }
                if (this._getDefaultInactiveTemperatureChannel().getNextWriteValue().isPresent()) {
                    this.updateConfig(false);
                }

            } else if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {

                if (this.autoRun || this.shouldRun()) {
                    this.isActive = true;
                    if (this._getActiveTemperature().value().isDefined()) {
                        this.getTemperatureChannel().setNextValue(this._getActiveTemperature().value().get());
                    }

                } else {
                    this.isActive = false;
                    if (this.useInactiveTemperature) {
                        if (this._getInactiveTemperature().value().isDefined()) {
                            this.getTemperatureChannel().setNextValue(this._getInactiveTemperature().value().get());
                        }
                    }
                }
                this.getEnableSignal().setNextValue(this.isActive);
            }
        } else {
            this.createTimer(this.id(), this.config.timerID(), this.config.waitTime());
        }
    }

    private boolean shouldRun() {
        if (this.getEnableSignal().getNextWriteValue().isPresent()) {
            this.timerHandler.resetTimer(ENABLE_SIGNAL_IDENTIFIER);
            return this.getEnableSignal().getNextWriteValueAndReset().orElse(false);
        }
        return this.isActive && this.timerHandler.checkTimeIsUp(ENABLE_SIGNAL_IDENTIFIER);
    }

    private void updateConfig(boolean activeTemperatureValue) {
        Configuration c;

        try {
            Optional<Integer> channelTemp = activeTemperatureValue ? this._getDefaultActiveTemperatureChannel().getNextWriteValueAndReset() : this._getDefaultInactiveTemperatureChannel().getNextWriteValueAndReset();
            c = this.ca.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();
            String propertyName = activeTemperatureValue ? "activeTemperature" : "inactiveTemperature";
            int setPointTemperature = (int) properties.get(propertyName);

            if (channelTemp.isPresent() && setPointTemperature != channelTemp.get()) {
                properties.put(propertyName, channelTemp);
                c.update(properties);
            }
        } catch (IOException e) {
            this.log.warn("Couldn't update ChannelProperty, reason: " + e.getMessage());
        }
    }
}
