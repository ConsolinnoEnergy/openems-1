package io.openems.edge.utility.calculator;

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
import io.openems.edge.utility.api.IntervalToIntervalCalculator;
import io.openems.edge.utility.api.IntervalToIntervalCalculatorImpl;
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

import java.util.Optional;

/**
 * <p>
 * This Component provides the ability to Calculate a function Value from one Interval A [a;b] to another Interval B [c;d].
 * E.g. You have an analogue Signal of 4-20mA and want to set it into ratio of Pressure
 * with a range of -5 to 5 bar.
 * Set up Interval A with [4;20] and Interval B with [-5;5]
 * </p>
 * {@link IntervalToIntervalCalculator} us used to calculate the output.
 * You can use the IntervalToIntervalCalculator to calculate the output within your own class.
 */

@Designate(ocd = IntervalToIntervalCalculatorConfig.class, factory = true)
@Component(name = "Utility.Calculator.IntervalToInterval", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

public class IntervalToIntervalCalculatorComponent extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

    private final Logger log = LoggerFactory.getLogger(IntervalToIntervalCalculatorComponent.class);

    private final IntervalToIntervalCalculator intervalToIntervalCalculator = new IntervalToIntervalCalculatorImpl();

    @Reference
    ComponentManager cpm;

    private IntervalToIntervalCalculatorConfig config;

    public IntervalToIntervalCalculatorComponent() {
        super(ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, IntervalToIntervalCalculatorConfig config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
    }

    @Modified
    void modified(ComponentContext context, IntervalToIntervalCalculatorConfig config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.config = config;
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            try {
                Channel<?> inputChannel = this.cpm.getChannel(ChannelAddress.fromString(this.config.inputChannelAddress()));
                Channel<?> outputChannel = this.cpm.getChannel(ChannelAddress.fromString(this.config.outputChannelAddress()));
                Optional<?> inputValue;
                double output;
                Double input;
                switch (this.config.inputType()) {


                    case NEXT_VALUE:
                        inputValue = inputChannel.getNextValue().asOptional();
                        break;
                    case NEXT_WRITE_VALUE:
                        if (inputChannel instanceof WriteChannel<?>) {
                            inputValue = ((WriteChannel<?>) inputChannel).getNextWriteValue();
                        } else {
                            this.log.warn("InputChannel Not A Write Channel! Please Reconfigure!");
                            return;
                        }
                        break;
                    case VALUE:
                    default:
                        inputValue = inputChannel.value().asOptional();
                        break;
                }
                if (inputValue.isPresent()) {
                    input = TypeUtils.getAsType(OpenemsType.DOUBLE, inputValue);
                    if (input != null) {
                        output = this.intervalToIntervalCalculator.calculateDoubleByCalculationType(this.config.representationType(),
                                this.config.inputMinIntervalA(), this.config.inputMaxIntervalA(), this.config.inputMinIntervalB(),
                                this.config.inputMaxIntervalB(), input);
                        switch (this.config.outputType()) {

                            case VALUE:
                            case NEXT_VALUE:
                                outputChannel.setNextValue(output);
                                break;
                            case NEXT_WRITE_VALUE:
                                if (outputChannel instanceof WriteChannel<?>) {
                                    ((WriteChannel<?>) outputChannel).setNextWriteValueFromObject(output);
                                } else {
                                    this.log.info(this.id() + " : Given Channel is not a Write Channel. Using nextValue instead");
                                    outputChannel.setNextValue(output);
                                }
                                break;
                        }
                    }
                }

            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.error("Wrong ChannelAddress: " + e.getMessage());
            }
        }
    }
}
