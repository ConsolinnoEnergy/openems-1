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
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.utility.api.ContainsOnlyNumbers;
import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.ToggleSwitch;
import io.openems.edge.utility.api.ValueType;
import io.openems.edge.utility.api.ValueWrapper;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * The Abstract Class of the Toggle. Can either be used by the toggleSwitch or the automated toggle.
 * This class provides basic methods for the toggle to work.
 */
public abstract class AbstractToggle extends AbstractOpenemsComponent implements OpenemsComponent, ToggleSwitch {


    protected final Logger log = LoggerFactory.getLogger(AbstractToggle.class);

    protected boolean toggleActive;
    protected ToggleState toggleState;
    protected ValueWrapper valueWrapperA;
    protected ValueWrapper valueWrapperB;
    protected ValueType valueTypeA;
    protected ValueType valueTypeB;


    protected ChannelAddress outputChannelAddress;
    protected InputOutputType inputOutputType;
    protected boolean useOutput;

    protected ConfigurationAdmin cm;
    protected ComponentManager cpm;

    public AbstractToggle(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                          io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }


    void activate(ComponentContext context, String id, String alias, boolean enabled, ValueType valueTypeA, ValueType valueTypeB, String stateAValue,
                  String stateBValue, ToggleState defaultState, boolean useOutput,
                  String outputChannelAddress, InputOutputType outputType, ComponentManager cpm, ConfigurationAdmin cm) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, id, alias, enabled);
        this.activationOrModifiedRoutine(cm, cpm, valueTypeA, valueTypeB, stateAValue, stateBValue, defaultState, useOutput,
                outputChannelAddress, outputType);
    }

    void modified(ComponentContext context, String id, String alias, boolean enabled, ValueType valueTypeA, ValueType valueTypeB, String stateAValue,
                  String stateBValue, ToggleState defaultState, boolean useOutput,
                  String outputChannelAddress, InputOutputType outputType, ComponentManager cpm, ConfigurationAdmin cm) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.modified(context, id, alias, enabled);
        this.activationOrModifiedRoutine(cm, cpm, valueTypeA, valueTypeB, stateAValue, stateBValue, defaultState, useOutput,
                outputChannelAddress, outputType);
    }


    /**
     * Apply config data and update output. Depending on the default {@link ToggleState}.
     *
     * @param cm                   the Configuration Admin
     * @param cpm                  the Component Manager
     * @param valueTypeA           the {@link ValueType} of A
     * @param valueTypeB           the {@link ValueType} of B
     * @param stateAValue          the actual value of A
     * @param stateBValue          the actual value of B
     * @param defaultState         the default State.
     * @param useOutput            write state to an output?
     * @param outputChannelAddress output ChannelAddress
     * @param outputType           the {@link InputOutputType}.
     * @throws OpenemsError.OpenemsNamedException thrown if a channel cannot be found.
     * @throws ConfigurationException             thrown when something is configured in a wrong way.
     */
    protected void activationOrModifiedRoutine(ConfigurationAdmin cm, ComponentManager cpm, ValueType valueTypeA, ValueType valueTypeB, String stateAValue,
                                               String stateBValue, ToggleState defaultState, boolean useOutput,
                                               String outputChannelAddress, InputOutputType outputType) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.cm = cm;
        this.cpm = cpm;
        this.valueTypeA = valueTypeA;
        this.valueTypeB = valueTypeB;
        this.valueWrapperA = this.createValueWrapper(valueTypeA, stateAValue);
        this.valueWrapperB = this.createValueWrapper(valueTypeB, stateBValue);
        this.toggleState = defaultState;
        this.useOutput = useOutput;
        if (this.useOutput) {
            this.outputChannelAddress = ChannelAddress.fromString(outputChannelAddress);
            this.inputOutputType = outputType;

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
    protected void writeToOutput() throws OpenemsError.OpenemsNamedException {
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

    /**
     * Switches the current {@link ToggleState}.
     */
    protected void switchState() {
        if (this.toggleState.equals(ToggleState.A)) {
            this.toggleState = ToggleState.B;
        } else {
            this.toggleState = ToggleState.A;
        }
        this.getToggleStateChannel().setNextValue(this.toggleState.equals(ToggleState.A));
    }

    @Override
    public String debugLog() {
        return this.id() + " ToggleState: " + this.toggleState + " ToggleValue: " + this.getCurrentValueChannel().value();
    }

}
