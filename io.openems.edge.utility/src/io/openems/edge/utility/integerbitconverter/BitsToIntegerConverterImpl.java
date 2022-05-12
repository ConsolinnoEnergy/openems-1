package io.openems.edge.utility.integerbitconverter;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.utility.api.IntegerBitConverter;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Component receives n amounts of bit inputs via channel and converts them to an Integer.
 * Those bits are written internally to its own {@link IntegerBitConverter.ChannelId#INTEGER_VALUE}.
 * And also to its own {@link IntegerBitConverter.ChannelId#LONG_VALUE} (unsigned int value).
 */

@Designate(ocd = BitsToIntegerConverterConfig.class, factory = true)
@Component(name = "Utility.Converter.BitsToInteger", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class BitsToIntegerConverterImpl extends AbstractOpenemsComponent implements OpenemsComponent, IntegerBitConverter, EventHandler {

    private final Logger log = LoggerFactory.getLogger(BitsToIntegerConverterImpl.class);

    public BitsToIntegerConverterImpl() {
        super(OpenemsComponent.ChannelId.values(),
                IntegerBitConverter.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, BitsToIntegerConverterConfig config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Modified
    void modified(ComponentContext context, BitsToIntegerConverterConfig config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            Integer result = this._generateIntegerFromChannel();
            this.integerValueChannel().setNextValue(result);
            this.longValueChannel().setNextValue(Integer.toUnsignedLong(result));
        }
    }

}
