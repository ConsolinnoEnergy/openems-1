package io.openems.edge.utility.calculator;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.utility.api.ContainsOnlyNumbers;
import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.ValueWrapper;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This abstract Class is the Parent of Calculator implementations.
 * It stores a List of {@link ValueWrapper}.
 * Children call on activation or modification the {@link #addValues(String[], String, ComponentManager)} Method
 * to Add either Channel or static Values to the List.
 * On Events, the children get the List of the ValueWrapper and run their calculation routine.
 * After running the Routine, they call the parent to write the result to the output channel.
 * This is done in {@link #writeToOutput(double, ComponentManager)}.
 */
public abstract class AbstractCalculator extends AbstractOpenemsComponent implements OpenemsComponent {

    protected List<ValueWrapper> values = new ArrayList<>();

    protected final Logger log = LoggerFactory.getLogger(AbstractCalculator.class);

    protected ChannelAddress output;

    private InputOutputType inputOutputType;

    protected AbstractCalculator(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                                 io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    @Activate
    protected void activate(ComponentContext context, String id, String alias, boolean enabled, String outputChannel, InputOutputType inputOutputType) throws OpenemsError.OpenemsNamedException {
        super.activate(context, id, alias, enabled);
        this.output = ChannelAddress.fromString(outputChannel);
        this.inputOutputType = inputOutputType;
    }

    @Modified
    protected void modified(ComponentContext context, String id, String alias, boolean enabled, String outputChannel, InputOutputType inputOutputType) throws OpenemsError.OpenemsNamedException {
        super.modified(context, id, alias, enabled);
        this.values.clear();
        this.output = ChannelAddress.fromString(outputChannel);
        this.inputOutputType = inputOutputType;
    }

    /**
     * Add the Values to the Parent Value List.
     *
     * @param values           the values to add. Usually from the child Config.
     * @param specialCharacter the "SpecialCharacter" is unique for the children. Children know what to do when the value is unique.
     * @param cpm              the ComponentManager. Used to check if a given ChannelAddress is available.
     * @throws OpenemsError.OpenemsNamedException if the given ChannelAddress is wrong or not instantiated.
     */
    protected void addValues(String[] values, String specialCharacter, ComponentManager cpm) throws OpenemsError.OpenemsNamedException {
        List<ValueWrapper> valuesToAdd = new ArrayList<>();
        OpenemsError.OpenemsNamedException[] ex = {null};
        Arrays.stream(values).forEach(entry -> {
            if (ex[0] == null) {
                try {
                    String valueToAdd = entry;
                    boolean isSpecial = false;
                    ValueWrapper.ValueOrChannel valueOrChannel = ValueWrapper.ValueOrChannel.CHANNEL;

                    if (valueToAdd.startsWith(specialCharacter) && valueToAdd.length() > 1) {
                        valueToAdd = valueToAdd.substring(1);
                        isSpecial = true;
                    }
                    if (ContainsOnlyNumbers.containsOnlyValidNumbers(valueToAdd)) {
                        valueOrChannel = ValueWrapper.ValueOrChannel.VALUE;
                    }
                    if (valueOrChannel.equals(ValueWrapper.ValueOrChannel.CHANNEL)) {
                        ChannelAddress address = ChannelAddress.fromString(valueToAdd);
                        cpm.getChannel(address);
                        valuesToAdd.add(new ValueWrapper(address, isSpecial));
                    } else {
                        valuesToAdd.add(new ValueWrapper(valueToAdd, isSpecial));
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    ex[0] = e;
                }
            }
        });
        if (ex[0] != null) {
            throw ex[0];
        }
        this.values.addAll(valuesToAdd);
    }

    /**
     * Clears the value list.
     */
    protected void clearExistingValuesFromMap() {
        this.values.clear();
    }

    /**
     * Writes a Result to the configured {@link #output} Address.
     *
     * @param output the result of children calculation.
     * @param cpm    the ComponentManager
     * @return true on success. False when an error occurred or when the configured NEXT_WRITE_VALUE cannot be written
     * into the output because it is not an instance of a WriteChannel
     */
    protected boolean writeToOutput(double output, ComponentManager cpm) {
        try {
            Channel<?> channelToWriteInto = cpm.getChannel(this.output);
            switch (this.inputOutputType) {

                case NEXT_WRITE_VALUE:
                    if (channelToWriteInto instanceof WriteChannel<?>) {
                        ((WriteChannel<?>) channelToWriteInto).setNextWriteValueFromObject(output);
                        return true;
                    } else {
                        this.log.info(this.id() + ": OutputChannel not a Write Channel! Cannot write Output!");
                        return false;
                    }

                case VALUE:
                    channelToWriteInto.setNextValue(output);
                    channelToWriteInto.nextProcessImage();
                    return true;
                case NEXT_VALUE:
                default:
                    channelToWriteInto.setNextValue(output);
                    return true;
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("An error occurred: " + e.getError());
            return false;
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }


}
