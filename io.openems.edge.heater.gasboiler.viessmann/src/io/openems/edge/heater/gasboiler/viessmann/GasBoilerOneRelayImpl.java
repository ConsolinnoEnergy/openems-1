package io.openems.edge.heater.gasboiler.viessmann;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import io.openems.edge.relay.api.Relay;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Designate(ocd = ConfigOneRelay.class, factory = true)
@Component(name = "Heater.Viessmann.GasBoilerOneRelay",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class GasBoilerOneRelayImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler,
        ExceptionalState, Heater {

    private final Logger log = LoggerFactory.getLogger(GasBoilerOneRelayImpl.class);

    @Reference
    ComponentManager cpm;

    private boolean componentEnabled;
    private Relay relay;

    private EnableSignalHandler enableSignalHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "GASBOILER_VIESSMANN_RELAY_ENABLE_SIGNAL_IDENTIFIER";
    private boolean useExceptionalState;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "GASBOILER_VIESSMANN_RELAY_EXCEPTIONAL_STATE_IDENTIFIER";

    ConfigOneRelay config;


    public GasBoilerOneRelayImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, ConfigOneRelay config) throws OpenemsError.OpenemsNamedException,
            ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.componentEnabled = config.enabled();
        this.config = config;

        if (this.cpm.getComponent(config.relayId()) instanceof Relay) {
            this.relay = this.cpm.getComponent(config.relayId());
        } else {
            throw new ConfigurationException("activate", "The Component with id: "
                    + config.relayId() + " is not a relay module");
        }

        TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
        String timerTypeEnableSignal;
        if (config.enableSignalTimerIsCyclesNotSeconds()) {
            timerTypeEnableSignal = "TimerByCycles";
        } else {
            timerTypeEnableSignal = "TimerByTime";
        }
        timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, timerTypeEnableSignal, config.waitTimeEnableSignal());
        this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
        this.useExceptionalState = config.useExceptionalState();
        if (this.useExceptionalState) {
            String timerTypeExceptionalState;
            if (config.exceptionalStateTimerIsCyclesNotSeconds()) {
                timerTypeExceptionalState = "TimerByCycles";
            } else {
                timerTypeExceptionalState = "TimerByTime";
            }
            timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerTypeExceptionalState, config.waitTimeExceptionalState());
            this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
        }

        if (this.componentEnabled == false) {
            this._setHeaterState(HeaterState.OFF.getValue());
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        try {
            if (this.relay != null) {
                this.relay.getRelaysWriteChannel().setNextWriteValue(false);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String debugLog() {

        if (this.relay.getRelaysWriteChannel().value().isDefined()) {
            String active = this.relay.getRelaysWriteChannel().value().get() ? "active" : "not Active";
            return this.id() + " Status: " + active;
        }

        return "No Value available yet";

    }

    @Override
    public void handleEvent(Event event) {
        if (this.componentEnabled && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            this.channelmapping();
        }
    }

    protected void channelmapping() {

        // Handle EnableSignal.
        boolean turnOnHeater = this.enableSignalHandler.deviceShouldBeHeating(this);

        // Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
        int exceptionalStateValue = 0;
        boolean exceptionalStateActive = false;
        if (this.useExceptionalState) {
            exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
            if (exceptionalStateActive) {
                exceptionalStateValue = this.getExceptionalStateValue();
                if (exceptionalStateValue <= 0) {
                    // Turn off heater when ExceptionalStateValue = 0.
                    turnOnHeater = false;
                } else {
                    turnOnHeater = true;
                }
            }
        }

        // Check missing components. The relay turns the heater on or off.
        boolean errorDetected = false;
        if (this.relay != null && this.relay.isEnabled()) {
            try {
                this.relay.getRelaysWriteChannel().setNextWriteValue(turnOnHeater);
            } catch (OpenemsError.OpenemsNamedException e) {
                this._setErrorMessage("OpenEMS error: Could not write in relay module command channel.");
                this.log.warn("Couldn't write in Channel " + e.getMessage());
                errorDetected = true;
            }
        } else {
            try {
                if (this.cpm.getComponent(this.config.relayId()) instanceof Relay) {
                    this.relay = this.cpm.getComponent(this.config.relayId());
                    this.relay.getRelaysWriteChannel().setNextWriteValue(turnOnHeater);
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this._setErrorMessage("OpenEMS error: Could not find configured relay module.");
                this.log.warn("Could not find configured relay module!");
                errorDetected = true;
            }
        }

        // Set heater state.
        if (errorDetected == false) {
            this._setErrorMessage("No error");
            if (turnOnHeater) {
                this._setHeaterState(HeaterState.HEATING.getValue());
            } else {
                this._setHeaterState(HeaterState.STANDBY.getValue());
            }
        } else {
            this._setHeaterState(HeaterState.OFF.getValue());
        }
    }
}
