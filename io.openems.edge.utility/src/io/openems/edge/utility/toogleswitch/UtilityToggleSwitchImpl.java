package io.openems.edge.utility.toogleswitch;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.configupdate.ConfigurationUpdate;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.utility.api.ToggleSwitch;
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
 * The Implementation of the {@link io.openems.edge.utility.api.ToggleSwitch}.
 * The {@link ToggleSwitch} receives a signal
 * (== Button is pressed and held down --> ChannelValue of {@link ToggleSwitch.ChannelId#SIGNAL_ACTIVE} == true)
 * On release -> toggle to another state.
 * E.g. the current state == State A.
 * On {@link ToggleSwitch.ChannelId#SIGNAL_ACTIVE} == true and followed by false; switch state to State B
 * When this procedure is repeated it toggles the state back from State B to A.
 * The {@link ToggleState} (A == true/active; B == false/inactive) will be stored in {@link ToggleSwitch.ChannelId#TOGGLE_STATE}.
 * The current value (what is written to an output) is stored as a string in the {@link ToggleSwitch.ChannelId#CURRENT_VALUE}.
 * </p>
 * <p>
 * When the state has changed; the Configuration with the Default State will be updated. To avoid a different default state,
 * after an unexpected restart/crash of the Software.
 * </p>
 */
@Designate(ocd = ToggleSwitchConfig.class, factory = true)
@Component(name = "Utility.ToggleSwitch", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)

public class UtilityToggleSwitchImpl extends AbstractToggle implements OpenemsComponent, ToggleSwitch, EventHandler {


    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin cm;

    private ToggleSwitchConfig config;


    public UtilityToggleSwitchImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ToggleSwitch.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, ToggleSwitchConfig config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.valueTypeA(), config.valueTypeB(),
                config.stateAValue(), config.stateBValue(), config.defaultState(), config.useOutput(),
                config.outputChannelAddress(), config.outputType(), this.cpm, this.cm);
    }


    @Modified
    void modified(ComponentContext context, ToggleSwitchConfig config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.config = config;
        super.modified(context, config.id(), config.alias(), config.enabled(), config.valueTypeA(), config.valueTypeB(),
                config.stateAValue(), config.stateBValue(), config.defaultState(), config.useOutput(),
                config.outputChannelAddress(), config.outputType(), this.cpm, this.cm);
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }


    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            Boolean currentlyActive = this.getSignalActiveChannel().getNextWriteValue().orElse(false);
            if (currentlyActive && !this.toggleActive) {
                this.toggleActive = true;
            } else if (!currentlyActive && this.toggleActive) {
                this.toggleActive = false;
                this.switchState();
                try {
                    //When OpenEMS Crashes, the current state is saved in the config.
                    ConfigurationUpdate.updateConfig(this.cm, this.servicePid(),
                            "defaultState", this.toggleState.name());
                } catch (IOException e) {
                    this.log.warn(this.id() + " Couldn't update Config. Reason: " + e.getMessage());
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