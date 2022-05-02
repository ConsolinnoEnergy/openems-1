package io.openems.edge.utility.api;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * <p>The OutputWriter receives allows an easier use to receive an Input from a {@link ChannelAddress} via the {@link ComponentManager}
 * depending on the {@link InputOutputType}.
 * </p>
 * <p>
 * Additionally it also allows to write to an output, depending on the {@link InputOutputType}, writing into the given
 * {@link ChannelAddress} via the {@link ComponentManager}.
 * </p>
 */
public interface OutputWriter {

    Logger log = LoggerFactory.getLogger(OutputWriter.class);

    /**
     * Get a Value from the given ChannelAddress, depending on the InputOutputType.
     * @param inputOutputType the InputOutputType.
     * @param channelAddress the ChannelAddress.
     * @param cpm the ComponentManager
     * @return an Optional Value.
     */
    static Optional<?> getValueFromInputOutputType(InputOutputType inputOutputType, ChannelAddress channelAddress, ComponentManager cpm) {
        try {
            Channel<?> channel = cpm.getChannel(channelAddress);
            switch (inputOutputType) {

                case NEXT_VALUE:
                    return Optional.of(channel.getNextValue().get());
                case NEXT_WRITE_VALUE:
                    if (channel instanceof WriteChannel<?>) {
                        return Optional.of(((WriteChannel<?>) channel).getNextWriteValue());
                    } else {
                        return getValueFromInputOutputType(InputOutputType.NEXT_VALUE, channelAddress, cpm);
                    }
                case VALUE:
                default:
                    return Optional.of(channel.value().get());
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            return Optional.empty();
        }
    }

    /**
     * This method writes to a Channel depending on the given Value and InputOutputType.
     * @param value the Value, written/set to the channel.
     * @param outputType the InputOutputType for this channel.
     * @param output the ChannelAddress receiving the Value.
     * @param cpm the ComponentManager.
     */
    static void writeToOutput(Object value, InputOutputType outputType, ChannelAddress output, ComponentManager cpm) {
        Channel<?> channel = null;
        try {
            channel = cpm.getChannel(output);
        } catch (OpenemsError.OpenemsNamedException e) {
            log.warn("Couldn't write to output : " + output + " Channel does not exist.");
            return;
        }
        switch (outputType) {
            case VALUE:
                channel.setNextValue(value);
                channel.nextProcessImage();
                break;
            case NEXT_WRITE_VALUE:
                if (channel instanceof WriteChannel<?>) {
                    try {
                        ((WriteChannel<?>) channel).setNextWriteValueFromObject(value);
                    } catch (OpenemsError.OpenemsNamedException e) {
                        log.warn("Couldn't write to output of Channel: " + output);
                    }
                } else {
                    writeToOutput(value, InputOutputType.NEXT_VALUE, output, cpm);
                }
                break;
            case NEXT_VALUE:
            default:
                channel.setNextValue(value);
                break;
        }
    }
}
