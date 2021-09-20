package io.openems.edge.heater.decentral;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.decentral.api.DecentralizedCooler;
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
 * The Decentralized Cooler. It provides an equivalent functionality as the decentralized Heater, but it is used for cooling
 * purposes. And therefore the "needMoreHeat" (which is now NeedMoreCool) condition is swapped.
 * The "NeedHeat" Channel of the DecentralizedHeater is now {@link DecentralizedCooler.ChannelId#NEED_COOL} and the
 * {@link DecentralizedCooler.ChannelId#NEED_COOL_ENABLE_SIGNAL}.
 * It gets an HydraulicComponent, and looks up, if the "EnableSignal" was set.
 * If the Signal is received, it starts an HydraulicComponent and starts the Cooling process, when the Temperature is OK.
 */
@Designate(ocd = ConfigDecentralizedCooler.class, factory = true)
@Component(name = "Cooler.Decentralized",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE})
public class DecentralizedCoolerImpl extends AbstractDecentralizedComponent implements OpenemsComponent, EventHandler, Heater, DecentralizedCooler {

    public DecentralizedCoolerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values(),
                DecentralizedCooler.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, ConfigDecentralizedCooler config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.componentOrController(),
                config.componentOrControllerId(), config.thresholdThermometerId(), config.setPointTemperature(),
                config.forceCooling(), config.enableExceptionalStateHandling(),
                config.timerNeedCoolResponse(), config.timeNeedCoolResponse(),
                config.timerExceptionalState(), config.timeToWaitExceptionalState(),
                this.getForceCoolChannel(), this.getNeedCoolEnableSignalChannel());

        this.getNeedCoolChannel().setNextValue(false);
        this.getNeedMoreCoolChannel().setNextValue(false);
    }


    @Modified
    void modified(ComponentContext context, ConfigDecentralizedHeater config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled(), config.componentOrController(), config.componentOrControllerId(),
                config.thresholdThermometerId(), config.setPointTemperature(),
                config.forceHeating(), config.useExceptionalState(),
                config.timerNeedHeatResponse(), config.waitTimeNeedHeatResponse(),
                config.timerExceptionalState(), config.timeToWaitExceptionalState(),
                this.getForceCoolChannel(), this.getNeedCoolEnableSignalChannel());
    }

    /**
     * <p>
     * The Logic of the Cooler.
     * - Evaluate enable signal and exceptional state, decide if cooler should be cooling or not.
     * - If cooler should cool: write ’true’ in channel ’NeedCool’ to request cool network to cool down.
     * - Skip this if ’force cooling’ is true or cooler is active because of exceptional state. Otherwise:
     * React to signal in channel ’NeedCoolEnableSignal’ (= answer from heat network controller).
     * Wait if ’false’ and continue when ’true’.
     * If no value in the channel (= connection to heat network controller lost), wait until
     * timer runs out. Then continue.
     * To be clear: When the cooler is turned on by exceptional state, it will not wait for the cool network to be
     * ready!
     * - Start heating by calling {@link AbstractDecentralizedComponent#setThresholdAndControlValve(boolean, int)} method.
     * </p>
     * <p>
     * Heating is stopped when:
     * - cool network controller writes ’false’ in channel ’NeedCoolEnableSignal’.
     * - {@link Heater.ChannelId#ENABLE_SIGNAL} is empty.
     * - If exceptional state -> Exceptional state value <= 0, or end of exceptional state.
     * When cooling is stopped, method ’deactivateControlledComponents()’ is called.
     * </p>
     *
     * @param event The Event of OpenEmsEdge.
     */
    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled()) {
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
                super.checkMissingComponents();
            }
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS) && super.isEnabled()) {
                if (super.currentRunEnabled()) {
                    if (super.checkAllowedToExecuteLogic()) {
                        int setPointTemperature = this.getTemperatureSetpoint().orElse(DEFAULT_SETPOINT_TEMPERATURE);
                        boolean temperatureOk = this.thresholdThermometer.thermometerBelowGivenTemperature(setPointTemperature);
                        this.getNeedMoreCoolChannel().setNextValue(!temperatureOk);
                        super.setThresholdAndControlValve(temperatureOk, setPointTemperature);
                    }
                } else {
                    this.getNeedCoolChannel().setNextValue(false);
                    this.getNeedCoolEnableSignalChannel().getNextWriteValueAndReset();
                    this.getNeedCoolEnableSignalChannel().setNextValue(false);
                    this.getNeedMoreCoolChannel().setNextValue(false);
                    this.getNeedMoreCoolEnableSignalChannel().getNextWriteValueAndReset();
                    this.getNeedMoreCoolEnableSignalChannel().setNextValue(false);
                }
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }
}
