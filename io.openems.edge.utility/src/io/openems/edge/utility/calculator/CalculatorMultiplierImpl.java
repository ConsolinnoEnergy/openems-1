package io.openems.edge.utility.calculator;

import com.google.common.util.concurrent.AtomicDouble;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
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

import java.util.ArrayList;
import java.util.List;

/**
 * The CalculatorMultiplierImpl is an implementation of the {@link AbstractCalculator} Class.
 * This Child receives values to multiply them up and write the result to an output by calling parent methods.
 * Those values can either be a ChannelAddress (value) or a static value.
 * When you want to divide a value from the current product, simply add the {@link #SPECIAL_CHARACTER} in front of a config entry.
 */
@Designate(ocd = MultiplierConfig.class, factory = true)
@Component(name = "Utility.Calculator.Multiplier", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

public class CalculatorMultiplierImpl extends AbstractCalculator implements OpenemsComponent, EventHandler {
    // A Special Character signifies, that the previous Calculation should be divided by the next incoming value.
    private static final String SPECIAL_CHARACTER = "/";

    private static final double FORBIDDEN_DIVISOR = 0.d;

    private static final Double INIT_VALUE = 1.d;


    @Reference
    ComponentManager cpm;


    public CalculatorMultiplierImpl() {
        super(ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, MultiplierConfig config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled(), config.outputChannel(), config.inputOutputType());
        this.activationOrModifiedRoutine(config);
    }

    @Modified
    void modified(ComponentContext context, MultiplierConfig config) throws OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled(), config.outputChannel(), config.inputOutputType());
        this.activationOrModifiedRoutine(config);

    }

    private void activationOrModifiedRoutine(MultiplierConfig config) throws OpenemsError.OpenemsNamedException {
        super.clearExistingValuesFromMap();
        super.addValues(config.values(), SPECIAL_CHARACTER, this.cpm);
    }


    @Deactivate
    protected void deactivate() {
        super.clearExistingValuesFromMap();
        super.deactivate();
    }

    /**
     * After the OpenEMS Process image -> get all existing values from the parent.
     * If the value is a channel -> get the value.
     * If the value is a special value -> divide it by 1.
     * Multiply all values up and call the parent {@link AbstractCalculator#writeToOutput(double, ComponentManager)} method.
     *
     * @param event the OpenEMS Event. Usually {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_PROCESS_IMAGE}
     */
    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            List<Double> values = new ArrayList<>();
            super.values.forEach(entry -> {
                Double value = INIT_VALUE;
                switch (entry.getType()) {
                    case VALUE:
                        value = TypeUtils.getAsType(OpenemsType.DOUBLE, entry.getValue());
                        break;
                    case CHANNEL:
                        try {
                            Double valueAsType = TypeUtils.getAsType(OpenemsType.DOUBLE,
                                    this.cpm.getChannel(entry.getChannelAddress()).value());
                            value = valueAsType != null ? valueAsType : value;
                        } catch (OpenemsError.OpenemsNamedException e) {
                            super.log.info("Error occurred while fetching ChannelValue of Channel: "
                                    + entry.getChannelAddress() + "\nReason: " + e.getError());
                        }
                        break;
                }
                if (entry.isSpecialValue() && value != null && value != FORBIDDEN_DIVISOR) {
                    value = 1 / value;
                }
                values.add(value);
            });
            AtomicDouble atomicDouble = new AtomicDouble(INIT_VALUE);
            values.forEach(entry -> {
                if (entry != null) {
                    atomicDouble.set(atomicDouble.get() * entry);
                }
            });
            super.writeToOutput(atomicDouble.get(), this.cpm);
        }
    }
}