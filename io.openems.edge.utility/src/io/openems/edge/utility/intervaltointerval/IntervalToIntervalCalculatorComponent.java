package io.openems.edge.utility.intervaltointerval;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.utility.api.IntervalToIntervalCalculator;
import io.openems.edge.utility.api.IntervalToIntervalCalculatorImpl;
import io.openems.edge.utility.api.IntervalToIntervalHelperCalculatorImpl;
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

/**
 * <p>
 * This helperClass provides the ability to calculate by a rule of three.
 * But not a simple RuleOfThree. Provide the Input with a certain Range. And put the output into a different
 * RuleOfThree. E.g. You have an analogue Signal of 4-20mA and want to set it into ratio of Pressure
 * with a range of -5 to 5 bar.
 * Therefore: Calculate the Percentage value of the 4-20 mA input and after that,
 * calculate based on the percentage value the pressure.
 * Additionally: 4mA could represent 50% instead of 0% but 20mA 100%
 * Same goes for the Output.
 * </p>
 * <p>Also if the In or Output is a Percentage channel the Calculation is easier.</p>
 * This Implementation uses the HelperClasses within the API folder of {@link IntervalToIntervalHelperCalculatorImpl}
 * and {@link IntervalToIntervalCalculator} to calculate those values.
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
    void activate(ComponentContext context, IntervalToIntervalCalculatorConfig config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
    }

    @Modified
    void modified(ComponentContext context, IntervalToIntervalCalculatorConfig config) throws ConfigurationException {
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

        }
    }
}
