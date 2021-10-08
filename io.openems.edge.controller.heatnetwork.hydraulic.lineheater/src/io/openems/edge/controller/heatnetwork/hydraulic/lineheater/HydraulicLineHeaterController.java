package io.openems.edge.controller.heatnetwork.hydraulic.lineheater;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api.HydraulicLineController;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

/**
 * The Hydraulic Line Heater Implementation. It allows a Line to heat up fast and can regulate a MinMax Value,
 * when: either Decentralized Heater has a request
 * OR if externally an EnableSignal was set.
 * After that, it either Activates One Channel with a true/false value, a Valve/HydraulicComponent, Sets the MinMax amount, or you can set up
 * 4 Different channel for reading and writing.
 * Depends on the Configuration of the {@link LineHeaterType}.
 */
@Designate(ocd = ConfigLineHeater.class, factory = true)
@Component(name = "Controller.Heatnetwork.LineHeater",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class HydraulicLineHeaterController extends AbstractHydraulicLineController implements OpenemsComponent, Controller, HydraulicLineController {


    ConfigLineHeater config;
    //NOTE: If more Variation comes --> create extra "LineHeater"Classes in this controller etc


    public HydraulicLineHeaterController() {
        super(OpenemsComponent.ChannelId.values(),
                HydraulicLineController.ChannelId.values(),
                Controller.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, ConfigLineHeater config) {
        this.config = config;
        DecentralizedReactionType reactionType = super.getDecentralizedReactionType(config.reactionType());
        super.activate(context, config.id(), config.alias(), config.enabled(),
                config.tempSensorReference(), config.useMinMax(), config.useDecentralizedHeater(),
                config.decentralizedHeaterReference(), reactionType,
                config.temperatureDefault(), config.lineHeaterType(), config.valueToWriteIsBoolean(),
                config.channelAddress(), config.channels(), config.valveBypass(), config.timerId(),
                config.timeoutMaxRemote(), config.timeoutRestartCycle(), config.shouldFallback(),
                config.minuteFallbackStart(), config.minuteFallbackStop(),
                config.maxValveValue(), config.minValveValue(), config.maxMinOnly(), HeaterType.HEATER);
    }

    @Modified
    void modified(ComponentContext context, ConfigLineHeater config) {
        this.configSuccess = false;
        this.config = config;
        DecentralizedReactionType reactionType = super.getDecentralizedReactionType(config.reactionType());
        super.modified(context, config.id(), config.alias(), config.enabled(),
                config.tempSensorReference(), config.useMinMax(), config.useDecentralizedHeater(),
                config.decentralizedHeaterReference(), reactionType,
                config.temperatureDefault(), config.lineHeaterType(), config.valueToWriteIsBoolean(),
                config.channelAddress(), config.channels(), config.valveBypass(), config.timerId(),
                config.timeoutMaxRemote(), config.timeoutRestartCycle(), config.shouldFallback(),
                config.minuteFallbackStart(), config.minuteFallbackStop(),
                config.maxValveValue(), config.minValveValue(), config.maxMinOnly(), HeaterType.HEATER);
    }


    @Override
    public void run() {
        if (this.configSuccess) {
            super.abstractRun();
        } else {
            DecentralizedReactionType reactionType = super.getDecentralizedReactionType(this.config.reactionType());
            super.activateOrModifiedRoutine(this.config.tempSensorReference(), this.config.useMinMax(), this.config.useDecentralizedHeater(),
                    this.config.decentralizedHeaterReference(), reactionType,
                    this.config.temperatureDefault(), this.config.lineHeaterType(), this.config.valueToWriteIsBoolean(),
                    this.config.channelAddress(), this.config.channels(), this.config.valveBypass(), this.config.timerId(),
                    this.config.timeoutMaxRemote(), this.config.timeoutRestartCycle(), this.config.shouldFallback(),
                    this.config.minuteFallbackStart(), this.config.minuteFallbackStop(),
                    this.config.maxValveValue(), this.config.minValveValue(), this.config.maxMinOnly(), HeaterType.HEATER);
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }
}
