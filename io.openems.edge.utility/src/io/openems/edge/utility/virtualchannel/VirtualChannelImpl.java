package io.openems.edge.utility.virtualchannel;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.utility.api.ContainsOnlyNumbers;
import io.openems.edge.utility.api.VirtualChannel;
import io.openems.edge.utility.api.VirtualChannelType;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A VirtualChannel Impl. Create on the Fly Virtual channel to write and read from.
 * This allows other Utility Classes to write their input into those Channel or Read the Values from.
 * (E.g. the Condition Applier gets the Conditions. Writes a Value into a Virtual Channel and a MinMaxer / another
 * Condition applier uses the output to react again and writes it's value into another Channel.)
 * </p>
 * <p>
 * One Instance of a VirtualChannel can only be of a certain Type. Meaning One Instance can be a BooleanChannel but not a
 * IntegerChannel at the same time.
 * </p>
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Channel.Virtual", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)

public class VirtualChannelImpl extends AbstractOpenemsComponent implements OpenemsComponent, VirtualChannel, EventHandler {

    private final Logger log = LoggerFactory.getLogger(VirtualChannelImpl.class);

    private VirtualChannelType type;


    public VirtualChannelImpl() {
        super(OpenemsComponent.ChannelId.values(),
                VirtualChannel.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.type = config.channelType();
        this.applyValue(config.defaultValue());
    }


    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.type = config.channelType();
        this.applyValue(config.defaultValue());
    }

    private void applyValue(String defaultValue) {
        try {
            switch (this.type) {
                case STRING:
                    this.getStringChannel().setNextWriteValueFromObject(defaultValue);
                    break;
                case BOOLEAN:
                    if(!(defaultValue.equalsIgnoreCase("false") || defaultValue.equalsIgnoreCase("true"))) {
                        //Either 0 or negative Value interpreted as false, else true
                        if (defaultValue.equals("0") || defaultValue.contains("-")) {
                            defaultValue = "false";
                        } else {
                            defaultValue = "true";
                        }
                    }
                    this.getBooleanChannel().setNextWriteValueFromObject(defaultValue);
                    break;
                case DOUBLE:
                    if(ContainsOnlyNumbers.containsOnlyValidNumbers(defaultValue)) {
                        this.getDoubleChannel().setNextWriteValueFromObject(defaultValue);
                    }
                    break;
                case LONG:
                    if(ContainsOnlyNumbers.containsOnlyValidNumbers(defaultValue)) {
                    this.getLongChannel().setNextWriteValueFromObject(defaultValue);
                }
                    break;
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't apply default Value!");
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public String debugLog() {
        String debugString = "Virtual Channel Type: " + this.type + " Active Value: ";
        switch (this.type) {
            case STRING:
                debugString += this.getStringChannel().value();
                break;
            case BOOLEAN:
                debugString += this.getBooleanChannel().value();
                break;
            case DOUBLE:
                debugString += this.getDoubleChannel().value();
                break;
            case LONG:
                debugString += this.getLongChannel().value();
                break;
        }
        return debugString;
    }

    @Override
    public void handleEvent(Event event) {

        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            switch (this.type) {
                case STRING:
                    this.getStringChannel().getNextWriteValue().ifPresent(entry -> this.getStringChannel().setNextValue(entry));
                    break;
                case BOOLEAN:
                    this.getBooleanChannel().getNextWriteValue().ifPresent(entry -> this.getBooleanChannel().setNextValue(entry));
                    break;
                case DOUBLE:
                    this.getDoubleChannel().getNextWriteValue().ifPresent(entry -> this.getDoubleChannel().setNextValue(entry));
                    break;
                case LONG:
                    this.getLongChannel().getNextWriteValue().ifPresent(entry -> this.getLongChannel().setNextValue(entry));
                    break;
            }

        }
    }
}
