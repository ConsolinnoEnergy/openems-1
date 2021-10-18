package io.openems.edge.heater.decentralized;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.exceptionalstate.api.ExceptionalState;

import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.decentralized.api.DecentralizedHeater;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;


/**
 * Decentralized heater.
 * This heater gets heat from an external Source, not directly applied within the installed heatsystem(decentralized).
 * When Enabled, the Decentralized Heater asks for Heat ({@link DecentralizedHeater.ChannelId#NEED_HEAT} and awaits a
 * {@link DecentralizedHeater.ChannelId#NEED_HEAT_ENABLE_SIGNAL}. After that it executes it's logic, monitoring a minimum Temperature.
 * If the MinimumTemperature is not Reached it asks for More Heat ({@link DecentralizedHeater.ChannelId#NEED_MORE_HEAT})
 * if the heat network is ready or not. If there shouldn't be any response, this heater will wait until a timer runs out
 * and applies it's logic anyway.
 */
@Designate(ocd = ConfigDecentralizedHeater.class, factory = true)
@Component(name = "Heater.Decentralized",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE}
)
public class DecentralizedHeaterImpl extends AbstractDecentralizedComponent implements OpenemsComponent, DecentralizedHeater, ExceptionalState, EventHandler {

    public DecentralizedHeaterImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values(),
                DecentralizedHeater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    ConfigDecentralizedHeater config;


    @Activate
    void activate(ComponentContext context, ConfigDecentralizedHeater config) {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.componentOrController(),
                config.componentOrControllerId(), config.thresholdThermometerId(), config.setPointTemperature(),
                config.forceHeating(), config.useExceptionalState(),
                config.timerNeedHeatResponse(), config.waitTimeNeedHeatResponse(),
                config.timerExceptionalState(), config.timeToWaitExceptionalState(), this.getForceHeatChannel(), this.getNeedHeatEnableSignalChannel());
        this.getNeedHeatChannel().setNextValue(false);
        this.getNeedMoreHeatChannel().setNextValue(false);
    }


    @Modified
    void modified(ComponentContext context, ConfigDecentralizedHeater config) {
        this.configurationSuccess = false;
        this.config = config;
        super.modified(context, config.id(), config.alias(), config.enabled(), config.componentOrController(), config.componentOrControllerId(),
                config.thresholdThermometerId(), config.setPointTemperature(),
                config.forceHeating(), config.useExceptionalState(),
                config.timerNeedHeatResponse(), config.waitTimeNeedHeatResponse(),
                config.timerExceptionalState(), config.timeToWaitExceptionalState(),
                this.getForceHeatChannel(), this.getNeedHeatEnableSignalChannel());
    }

    /**
     * <p>
     * The Logic of the Heater.
     * - Evaluate enable signal and exceptional state, decide if heater should be heating or not.
     * - If heater should heat: write ’true’ in channel ’NeedHeat’ to request heat network to warm up.
     * - Skip this if ’force heating’ is on or heater is active because of exceptional state. Otherwise:
     * React to signal in channel ’NeedHeatEnableSignal’ (= answer from heat network controller). Wait if ’false’,
     * continue when ’true’. If no value in the channel (= connection to heat network controller lost), wait until
     * timer runs out. Then continue.
     * To be clear: When the heater is turned on by exceptional state, it will not wait for the heat network to be
     * ready!
     * - Start heating by calling {@link AbstractDecentralizedComponent#setThresholdAndControlValve(boolean, int)} method.
     * </p>
     * <p>
     * Heating is stopped when:
     * - Heat network controller writes ’false’ in channel ’NeedHeatEnableSignal’.
     * - Enable signal ’false’
     * - If exceptional state -> Exceptional state value <= 0, or end of exceptional state.
     * When heating is stopped, method ’deactivateControlledComponents()’ is called.
     * </p>
     *
     * @param event The Event of OpenemsEdge.
     */
    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled()) {
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
                if (super.configurationSuccess) {
                    super.checkMissingComponents();
                } else {
                    try {
                        super.activationOrModifiedRoutine(this.config.componentOrController(), this.config.componentOrControllerId(),
                                this.config.thresholdThermometerId(), this.config.setPointTemperature(),
                                this.config.forceHeating(), this.config.useExceptionalState(),
                                this.config.timerNeedHeatResponse(), this.config.waitTimeNeedHeatResponse(),
                                this.config.timerExceptionalState(), this.config.timeToWaitExceptionalState(),
                                this.getForceHeatChannel(), this.getNeedHeatEnableSignalChannel());
                    } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
                        this.log.warn("Couldn't apply config, trying again later");
                        this.configurationSuccess = false;
                    }
                }
            }
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)
                    && super.isEnabled() && super.configurationSuccess) {
                if (super.currentRunEnabled()) {
                    if (super.checkAllowedToExecuteLogic()) {
                        int setPointTemperature = this.getTemperatureSetpoint().orElse(DEFAULT_SET_POINT_TEMPERATURE);
                        boolean temperatureOk = this.thresholdThermometer.thermometerAboveGivenTemperature(setPointTemperature);
                        this.getNeedMoreHeatChannel().setNextValue(!temperatureOk);
                        this.getNeedMoreHeatEnableSignalChannel().setNextValue(this.getNeedMoreHeatEnableSignalChannel()
                                .getNextWriteValueAndReset().orElse(false));
                        super.setThresholdAndControlValve(temperatureOk, setPointTemperature);
                    }
                } else {
                    this.getEnableSignalChannel().getNextWriteValueAndReset();
                    this.getEnableSignalChannel().setNextValue(false);
                    this.getNeedHeatChannel().setNextValue(false);
                    this.getNeedHeatEnableSignalChannel().getNextWriteValueAndReset();
                    this.getNeedHeatEnableSignalChannel().setNextValue(false);
                    this.getNeedMoreHeatChannel().setNextValue(false);
                    this.getNeedMoreHeatEnableSignalChannel().getNextWriteValueAndReset();
                    this.getNeedMoreHeatEnableSignalChannel().setNextValue(false);
                }
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }
}
