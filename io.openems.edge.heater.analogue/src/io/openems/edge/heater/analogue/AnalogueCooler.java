package io.openems.edge.heater.analogue;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.heater.analogue.component.AbstractAnalogueHeaterOrCoolerComponent;
import io.openems.edge.heater.api.Cooler;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.io.api.Relay;
import io.openems.edge.lucidcontrol.api.LucidControlDeviceOutput;
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

/**
 * The Analogue Cooler. It is a Cooler, that controls a Analogue Component, connected to a Cooler, such as a CHP or other
 * PowerPlant.
 * ATM Possible Analogue Heaters can be controlled by:
 * <p>
 *     <ul>
 * <li>{@link io.openems.edge.io.api.AnalogInputOutput}</li>
 * <li>{@link LucidControlDeviceOutput}</li>
 * <li>{@link io.openems.edge.io.api.Pwm} </li>
 * <li>{@link Relay}</li>
 * </ul>
 * </p>
 * <p>
 *     Important note: The subClasses / extending classes of {@link AbstractAnalogueHeaterOrCoolerComponent}
 *     will always calculate a percent value. Depending on the config if the Heater is set by a definite KW value or Percent, it will always
 *     transform the Value to a PERCENT value. Since it's easier to manage and calculate PercentValues from/for analogue Devices.
 * </p>
 */

@Designate(ocd = ConfigCoolerAnalogue.class, factory = true)
@Component(name = "Cooler.Analogue", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})

public class AnalogueCooler extends AbstractAnalogueHeaterOrCooler implements OpenemsComponent, Cooler, Heater, EventHandler {


    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin cm;


    private ConfigCoolerAnalogue config;

    public AnalogueCooler() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, ConfigCoolerAnalogue config) {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(),
                config.useErrorSignals(), config.errorChannelAddress(),
                config.analogueId(), config.analogueType(),
                config.controlType(), config.defaultMinPower(), config.timerId(), config.maxTimeEnableSignal(),
                config.maxTimePowerSignal(), config.defaultRunPower(), config.maxPower(), this.cpm, this.cm);
    }

    @Modified
    void modified(ComponentContext context, ConfigCoolerAnalogue config) {
        this.config = config;
        super.modified(context, config.id(), config.alias(), config.enabled(),
                config.useErrorSignals(), config.errorChannelAddress(),
                config.analogueId(), config.analogueType(),
                config.controlType(), config.defaultMinPower(), config.timerId(), config.maxTimeEnableSignal(),
                config.maxTimePowerSignal(), config.defaultRunPower(), config.maxPower(), this.cpm, this.cm);

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (this.configurationSuccess) {
            super.handleEvent(event);
        } else {
            try {
                super.activationOrModifiedRoutine(this.config.analogueId(), this.config.analogueType(),
                        this.config.controlType(), this.config.defaultMinPower(), this.config.timerId(), this.config.maxTimeEnableSignal(),
                        this.config.maxTimePowerSignal(), this.config.defaultRunPower(), this.config.maxPower(),
                        this.config.useErrorSignals(), this.config.errorChannelAddress());
            } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
                super.log.warn("Couldn't apply Config yet. Reason: " + e.getMessage());
            }
        }
    }

}