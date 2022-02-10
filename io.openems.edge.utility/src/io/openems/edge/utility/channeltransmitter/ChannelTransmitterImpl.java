package io.openems.edge.utility.channeltransmitter;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.utility.api.ContainsOnlyNumbers;
import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.ValueWrapper;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A Utility class to write one Channel value to another.
 * {@link InputOutputType} allows to define to get a WriteValue, nextValue or Value and either call setNextWrite or setNextValue
 * of the outputChannel
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "ChannelTransmitterImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})

public class ChannelTransmitterImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

    private final Logger log = LoggerFactory.getLogger(ChannelTransmitterImpl.class);

    @Reference
    ComponentManager cpm;

    private ChannelAddress input;

    private ChannelAddress output;

    private InputOutputType inputType;
    private InputOutputType outputType;

    private boolean useAlternativeValue;
    private final List<String> forbiddenValues = new ArrayList<>();
    private ValueWrapper valueWrapper;


    public ChannelTransmitterImpl() {
        super(ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);

    }

    private void activationOrModifiedRoutine(Config config) throws OpenemsError.OpenemsNamedException {
        this.applyChannelAddresses(config.inputChannelAddress(), config.outputChannelAddress());
        this.inputType = config.inputType();
        this.outputType = config.outputType();
        this.useAlternativeValue = config.useAlternativeValue();
        if (this.useAlternativeValue) {
            if (ContainsOnlyNumbers.containsOnlyValidNumbers(config.alternativeValue()) || config.alternativeValueIsString()) {
                this.valueWrapper = new ValueWrapper(config.alternativeValue(), false);
            } else {
                ChannelAddress channelAddress = ChannelAddress.fromString(config.alternativeValue());
                cpm.getChannel(channelAddress);
                this.valueWrapper = new ValueWrapper(channelAddress, false);
            }
            if (config.forbiddenValues().length > 0 && config.forbiddenValues()[0] != null) {
                Collections.addAll(this.forbiddenValues, config.forbiddenValues());
            }
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    private void applyChannelAddresses(String input, String output) throws OpenemsError.OpenemsNamedException {
        this.input = ChannelAddress.fromString(input);
        this.output = ChannelAddress.fromString(output);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            Channel<?> inputChannel;
            Channel<?> outputChannel;
            Optional<?> inputValue;

            try {
                inputChannel = this.cpm.getChannel(this.input);
                outputChannel = this.cpm.getChannel(this.output);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Channel: " + this.input + " or: " + this.output + " not available for: " + this.id());
                return;
            }

            switch (this.inputType) {
                case NEXT_VALUE:
                    inputValue = inputChannel.getNextValue().asOptional();
                    break;
                case NEXT_WRITE_VALUE:
                    if (inputChannel instanceof WriteChannel<?>) {
                        inputValue = ((WriteChannel<?>) inputChannel).getNextWriteValue();
                    } else {
                        this.inputType = InputOutputType.NEXT_VALUE;
                        inputValue = inputChannel.getNextValue().asOptional();
                    }
                    break;
                case VALUE:
                default:
                    inputValue = inputChannel.value().asOptional();
                    break;
            }

            if (this.useAlternativeValue) {
                inputValue = this.getAlternativeValueIfNecessary(inputValue);
            }
            if (inputValue.isPresent()) {

                switch (this.outputType) {
                    case VALUE:
                        this.log.info("Value is invalid. Doing NextValue Instead: " + this.id());
                        this.outputType = InputOutputType.NEXT_VALUE;
                        outputChannel.setNextValue(inputValue.get());
                        break;
                    case NEXT_WRITE_VALUE:
                        if (outputChannel instanceof WriteChannel<?>) {
                            try {
                                ((WriteChannel<?>) outputChannel).setNextWriteValueFromObject(inputValue.get());
                            } catch (OpenemsError.OpenemsNamedException e) {
                                this.log.warn("Couldn't set NextWriteValue. Reason: " + e.getMessage());
                            }
                        } else {
                            this.log.warn("OutputChannel not a WriteChannel. Using nextValue instead");
                            this.outputType = InputOutputType.NEXT_VALUE;
                            outputChannel.setNextValue(inputValue.get());
                        }
                        break;
                    case NEXT_VALUE:
                    default:
                        outputChannel.setNextValue(inputValue.get());
                        break;
                }
            }

        }
    }

    /**
     * If the alternative Value is used -> check if the value input equals to any forbidden value.
     * If so -> return the alternative value.
     *
     * @param inputValue the inputValue
     * @return the originalValue or input.
     */
    private Optional<?> getAlternativeValueIfNecessary(Object inputValue) {
        AtomicBoolean found = new AtomicBoolean(false);
        String value = TypeUtils.getAsType(OpenemsType.STRING, inputValue);
        if (value != null) {
            if (this.forbiddenValues.size() > 0) {
                String finalValue = value;
                found.set(this.forbiddenValues.stream().anyMatch(entry -> entry.equals(finalValue)));
            }
        } else {
            found.set(this.forbiddenValues.contains("null"));

        }
        if (found.get()) {
            switch (this.valueWrapper.getType()) {

                case CHANNEL:
                    Value<?> channelValue = null;
                    try {
                        channelValue = this.cpm.getChannel(this.valueWrapper.getChannelAddress()).value();
                    } catch (OpenemsError.OpenemsNamedException e) {
                        value = null;
                        break;
                    }
                    value = TypeUtils.getAsType(OpenemsType.STRING, channelValue);
                    break;
                case VALUE:
                default:
                    value = this.valueWrapper.getValue();
                    break;
            }
        }
        return Optional.ofNullable(value);
    }
}
