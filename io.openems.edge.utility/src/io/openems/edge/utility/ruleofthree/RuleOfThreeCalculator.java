package io.openems.edge.utility.ruleofthree;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.EventConstants;
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
 * </p>
 * <p>Also if the In or Output is a Percentage channel the Calculation is easier.</p>
 */

@Designate(ocd = RuleOfThreeCalculatorConfig.class, factory = true)
@Component(name = "Utility.RuleOfThree", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class RuleOfThreeCalculator extends AbstractOpenemsComponent implements OpenemsComponent {

    private final Logger log = LoggerFactory.getLogger(RuleOfThreeCalculator.class);

    public RuleOfThreeCalculator() {
        super(ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, RuleOfThreeCalculatorConfig config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Modified
    void modified(ComponentContext context, RuleOfThreeCalculatorConfig config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

}
