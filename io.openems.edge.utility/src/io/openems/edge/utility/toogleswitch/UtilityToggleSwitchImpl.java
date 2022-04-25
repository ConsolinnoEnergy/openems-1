package io.openems.edge.utility.toogleswitch;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.configupdate.ConfigurationUpdate;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.utility.api.ContainsOnlyNumbers;
import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.ToggleSwitch;
import io.openems.edge.utility.api.ValueType;
import io.openems.edge.utility.api.ValueWrapper;
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
@Designate(ocd = Config.class, factory = true)
@Component(name = "Utility.ToggleSwitch", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)

public class UtilityToggleSwitchImpl extends AbstractOpenemsComponent implements OpenemsComponent, ToggleSwitch, EventHandler {

    private final Logger log = LoggerFactory.getLogger(UtilityToggleSwitchImpl.class);

    Config config;

    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin cm;

    private boolean toggleActive;
    private ToggleState toggleState;
    private ValueWrapper valueWrapperA;
    private ValueWrapper valueWrapperB;
    private ValueType valueTypeA;
    private ValueType valueTypeB;

    private ChannelAddress outputChannelAddress;
    private InputOutputType inputOutputType;
    private boolean useOutput;

    public UtilityToggleSwitchImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ToggleSwitch.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }


    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);

    }

    /**
     * Apply config data and update output. Depending on the default {@link ToggleState}.
     *
     * @param config the configuration of this component.
     * @throws OpenemsError.OpenemsNamedException thrown if a channel cannot be found.
     * @throws ConfigurationException             thrown when something is configured in a wrong way.
     */
    private void activationOrModifiedRoutine(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.config = config;
        this.toggleActive = false;
        this.valueTypeA = config.valueTypeA();
        this.valueTypeB = config.valueTypeB();
        this.valueWrapperA = this.createValueWrapper(config.valueTypeA(), config.stateAValue());
        this.valueWrapperB = this.createValueWrapper(config.valueTypeB(), config.stateBValue());
        this.toggleState = config.defaultState();
        this.useOutput = config.useOutput();
        if (this.useOutput) {
            this.outputChannelAddress = ChannelAddress.fromString(config.outputChannelAddress());
            this.inputOutputType = config.outputType();
        }
        this.writeToOutput();
    }

    /**
     * Creates a Value Wrapper. Important later for toggleStateValue.
     *
     * @param valueType  is the Value a channelAddress, static number or a String.
     * @param stateValue the configured Value. Either a ChannelAddress, number or a String.
     * @return a new instance of a ValueWrapper.
     * @throws OpenemsError.OpenemsNamedException thrown if the configured ChannelAddress is not correct.
     * @throws ConfigurationException             thrown if the user configured something wrong.
     */
    private ValueWrapper createValueWrapper(ValueType valueType, String stateValue) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        switch (valueType) {
            case CHANNEL_ADDRESS:
                if (!ContainsOnlyNumbers.containsOnlyValidNumbers(stateValue)) {
                    return new ValueWrapper(ChannelAddress.fromString(stateValue), false);
                } else {
                    throw new ConfigurationException("createValueWrapper", "Value is not a valid ChannelAddress: " + stateValue);
                }
            case STATIC_NUMBER:
                if (ContainsOnlyNumbers.containsOnlyValidNumbers(stateValue)) {
                    return new ValueWrapper(stateValue, false);
                } else {
                    throw new ConfigurationException("createValueWrapper", "Value does not contain only numbers: " + stateValue);
                }
            case STRING:
                return new ValueWrapper(stateValue, false);
        }
        throw new ConfigurationException("createValueWrapper", "This error shouldn't occur");
    }

    /**
     * Writes the value of the {@link #toggleState} to the output.
     * Get the Value as a String.
     * Get the OutputChannel (if configured)
     * and write it to the output, depending on the {@link InputOutputType}.
     *
     * @throws OpenemsError.OpenemsNamedException thrown if nextWriteValue fails.
     */
    private void writeToOutput() throws OpenemsError.OpenemsNamedException {
        String value = this.getStringValueFromWrapper(this.toggleState);
        if (value == null || value.equals("")) {
            this.log.warn(this.id() + " Couldn't process value! WriteToOutput on hold!");
            return;
        }
        this.getCurrentValueChannel().setNextValue(value);
        if (this.useOutput) {
            Channel<?> outputChannel = this.cpm.getChannel(this.outputChannelAddress);
            switch (this.inputOutputType) {

                case VALUE:
                    outputChannel.setNextValue(value);
                    outputChannel.nextProcessImage();
                    break;
                case NEXT_VALUE:
                    outputChannel.setNextValue(value);
                    break;
                case NEXT_WRITE_VALUE:
                    if (outputChannel instanceof WriteChannel<?>) {
                        try {
                            ((WriteChannel<?>) outputChannel).setNextWriteValueFromObject(value);
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.warn("Couldn't write to channel. Reason: " + e.getError());
                        }
                    } else {
                        this.log.info("Channel not a Write Channel. Switching inputOutputType to nextValue");
                        try {
                            ConfigurationUpdate.updateConfig(this.cm, this.servicePid(), "outputType", InputOutputType.NEXT_VALUE);
                        } catch (IOException e) {
                            this.inputOutputType = InputOutputType.NEXT_VALUE;
                        }
                    }
                    break;
            }
        }
    }


    /**
     * Depending on the given {@link ToggleState}, get the Value from the {@link ValueWrapper},
     * by calling the {@link #getValueFromValueWrapper(ValueWrapper, ValueType)} method.
     *
     * @param toggleState the Current State of the ToggleSwitch. Either {@link ToggleState#A} or {@link ToggleState#B}.
     * @return the fetched Value as a String.
     */

    private String getStringValueFromWrapper(ToggleState toggleState) {
        switch (toggleState) {

            case A:
                return this.getValueFromValueWrapper(this.valueWrapperA, this.valueTypeA);

            case B:
                return this.getValueFromValueWrapper(this.valueWrapperB, this.valueTypeB);
        }
        return "";
    }

    /**
     * Internal method to get the value from the given {@link ValueWrapper}.
     * The received value depends on the {@link ValueType}.
     *
     * @param valueWrapper the valueWrapper (Either {@link #valueWrapperA} or {@link #valueWrapperB}.
     * @param valueType    the {@link ValueType} of the Value stored within the valueWrapper.
     * @return the value from the ValueWrapper as a String.
     */
    private String getValueFromValueWrapper(ValueWrapper valueWrapper, ValueType valueType) {
        switch (valueType) {

            case CHANNEL_ADDRESS:

                ChannelAddress address = valueWrapper.getChannelAddress();
                try {
                    Channel<?> channel = this.cpm.getChannel(address);
                    return TypeUtils.getAsType(OpenemsType.STRING, channel.value());
                } catch (OpenemsError.OpenemsNamedException e) {
                    return "";
                }
            case STRING:
            case STATIC_NUMBER:
                return valueWrapper.getValue();
        }
        return "";
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

    /**
     * Switches the current {@link ToggleState}.
     */
    private void switchState() {
        if (this.toggleState.equals(ToggleState.A)) {
            this.toggleState = ToggleState.B;
        } else {
            this.toggleState = ToggleState.A;
        }
        this.getToggleStateChannel().setNextValue(this.toggleState.equals(ToggleState.A));
    }

    @Override
    public String debugLog() {
        return this.id() + " ToggleState: " + this.toggleState;
    }
}
