package io.openems.edge.utility.comparator;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.utility.api.ComparatorType;
import io.openems.edge.utility.api.ContainsOnlyNumbers;
import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.StaticComparator;
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

/**
 * This Class receives 2 Inputs and Compares them, depending on the {@link io.openems.edge.utility.api.ComparatorType}
 * The Result is written to the configured output.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Utility.Comparator", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)

public class UtilityComparator extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

    private final Logger log = LoggerFactory.getLogger(UtilityComparator.class);
    private static final Double MISSING_VALUE = Double.MIN_VALUE;

    public UtilityComparator() {
        super(ChannelId.values());
    }

    private ValueWrapper inputAValueWrapper;
    private ValueWrapper inputBValueWrapper;
    private ChannelAddress outputChannel;
    private InputOutputType outputType;
    private InputOutputType inputAType;
    private InputOutputType inputBType;
    private ComparatorType comparatorType;

    @Reference
    ComponentManager cpm;

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);

    }

    /**
     * Applies the Configuration.
     *
     * @param config the config of this Component
     */
    void activationOrModifiedRoutine(Config config) throws OpenemsError.OpenemsNamedException {
        if (ContainsOnlyNumbers.containsOnlyValidNumbers(config.compareOne())) {
            this.inputAValueWrapper = new ValueWrapper(config.compareOne(), false);
        } else {
            this.inputAValueWrapper = new ValueWrapper(ChannelAddress.fromString(config.compareOne()), false);
        }
        if (ContainsOnlyNumbers.containsOnlyValidNumbers(config.compareTwo())) {
            this.inputBValueWrapper = new ValueWrapper(config.compareTwo(), false);
        } else {
            this.inputBValueWrapper = new ValueWrapper(ChannelAddress.fromString(config.compareTwo()), false);
        }

        this.outputChannel = ChannelAddress.fromString(config.output());
        this.inputAType = config.compareOneInputOutputType();
        this.inputBType = config.compareTwoInputOutputType();
        this.comparatorType = config.comparatorType();
        this.outputType = config.compareResultInputOutputType();

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * When the input values are defined and not null.
     * Compare them via {@link StaticComparator} and write the output to
     * the OutputChannel.
     *
     * @param event usually {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_CONTROLLERS}.
     */
    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            try {
                Double valueA = this.getValueWrapperValue(this.inputAValueWrapper, this.inputAType);
                Double valueB = this.getValueWrapperValue(this.inputBValueWrapper, this.inputBType);
                if (!valueA.equals(MISSING_VALUE) && !valueB.equals(MISSING_VALUE)) {
                    this.writeToOutput(StaticComparator.compare(this.comparatorType, (Double) TypeUtils.getAsType(OpenemsType.DOUBLE, valueA),
                            (Double) TypeUtils.getAsType(OpenemsType.DOUBLE, valueB)));
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.error(this.id() + " Couldn't find Channel! Reason: " + e.getMessage());
            }
        }
    }


    /**
     * Writes the Comparison Result to the Output.
     *
     * @param compare compareResult
     * @throws OpenemsError.OpenemsNamedException when the output channel cannot be found.
     */
    private void writeToOutput(Boolean compare) throws OpenemsError.OpenemsNamedException {
        Channel<?> outputChannel = this.cpm.getChannel(this.outputChannel);
        switch (this.outputType) {

            case VALUE:
                outputChannel.setNextValue(compare);
                outputChannel.nextProcessImage();
                break;
            case NEXT_VALUE:
                outputChannel.setNextValue(compare);
                break;
            case NEXT_WRITE_VALUE:
                if (outputChannel instanceof WriteChannel<?>) {
                    ((WriteChannel<?>) outputChannel).setNextWriteValueFromObject(compare);
                } else {
                    this.outputType = InputOutputType.NEXT_VALUE;
                    this.writeToOutput(compare);
                }
                break;
        }
    }

    /**
     * Get the Value of the Wrapper.
     *
     * @param input_valueWrapper the value Wrapper holding the value
     * @param input_type         inputOutputType only relevant when ValueWrapper stores a ChannelAddress.
     * @return the ValueWrapper Value or ChannelValue
     * @throws OpenemsError.OpenemsNamedException when Channel cannot be found.
     */
    private Double getValueWrapperValue(ValueWrapper input_valueWrapper, InputOutputType input_type) throws OpenemsError.OpenemsNamedException {

        if (input_valueWrapper.getType().equals(ValueWrapper.ValueOrChannel.VALUE)) {
            return TypeUtils.getAsType(OpenemsType.DOUBLE, input_valueWrapper.getValue());
        } else {
            Double value = MISSING_VALUE;
            Channel<?> channel = this.cpm.getChannel(input_valueWrapper.getChannelAddress());
            switch (input_type) {

                case VALUE:
                    value = TypeUtils.getAsType(OpenemsType.DOUBLE, channel.value());
                    break;
                case NEXT_VALUE:
                    value = TypeUtils.getAsType(OpenemsType.DOUBLE, channel.getNextValue());
                    break;
                case NEXT_WRITE_VALUE:
                    if (channel instanceof WriteChannel<?>) {
                        value = TypeUtils.getAsType(OpenemsType.DOUBLE, ((WriteChannel<?>) channel).getNextWriteValue());
                    } else {
                        input_type = InputOutputType.NEXT_VALUE;
                        return this.getValueWrapperValue(input_valueWrapper, input_type);
                    }
                    break;
            }
            return value;
        }
    }
}
