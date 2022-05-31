package io.openems.edge.utility.toogleswitch;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.configupdate.ConfigurationUpdate;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import io.openems.edge.utility.api.ToggleSwitch;
import io.openems.edge.utility.api.ValueType;
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

import java.io.IOException;

/**
 * <p>
 * The Implementation of the {@link ToggleSwitch}.
 * This switches states between State A and B after a set amount of delta Time.
 * The {@link ToggleState} (A == true/active; B == false/inactive) will be stored in {@link ToggleSwitch.ChannelId#TOGGLE_STATE}.
 * The current value (what is written to an output) is stored as a string in the {@link ToggleSwitch.ChannelId#CURRENT_VALUE}.
 * </p>
 */
@Designate(ocd = AutomatedToggleConfig.class, factory = true)
@Component(name = "Utility.ToggleAutomated", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)

public class AutomatedToggleSwitchImpl extends AbstractToggle implements OpenemsComponent, ToggleSwitch, EventHandler {


    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin cm;

    private TimerHandler timerHandler;
    private static final String SWITCH_STATE_IDENTIFIER = "AUTOMATED_TOGGLE_SWITCH";
    private AutomatedToggleConfig config;
    private boolean toggleEachCycle;

    public AutomatedToggleSwitchImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ToggleSwitch.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, AutomatedToggleConfig config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.valueTypeA(), config.valueTypeB(),
                config.stateAValue(), config.stateBValue(), config.defaultState(), config.useOutput(),
                config.outputChannelAddress(), config.outputType(), this.cpm, this.cm);
        this.createTimerHandler();
    }


    @Modified
    void modified(ComponentContext context, AutomatedToggleConfig config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.config = config;
        super.modified(context, config.id(), config.alias(), config.enabled(), config.valueTypeA(), config.valueTypeB(),
                config.stateAValue(), config.stateBValue(), config.defaultState(), config.useOutput(),
                config.outputChannelAddress(), config.outputType(), this.cpm, this.cm);
        this.toggleEachCycle = config.toggleEachCycle();
        this.createTimerHandler();
    }

    /**
     * Creates a TimerHandler for automated ToggleSwitch / tells if the time is up to change states.
     */
    private void createTimerHandler() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (this.timerHandler != null) {
            this.timerHandler.removeComponent();
        }
        if (!this.toggleEachCycle) {
            this.timerHandler = new TimerHandlerImpl(this.id(), super.cpm);
            this.timerHandler.addOneIdentifier(SWITCH_STATE_IDENTIFIER, this.config.timerId(), this.config.deltaTime());
        }

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }


    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            if (this.toggleEachCycle) {
                this.switchState();
            } else {
                boolean currentlyActive = this.timerHandler.checkTimeIsUp(SWITCH_STATE_IDENTIFIER);
                if (currentlyActive) {
                    this.switchState();
                    this.timerHandler.resetTimer(SWITCH_STATE_IDENTIFIER);
                }
            }
            try {
                this.writeToOutput();
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn(this.id() + " Couldn't write to output. Reason: " + e.getMessage());
            }
        }
    }
}

