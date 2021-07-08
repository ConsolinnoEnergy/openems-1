package io.openems.edge.condition.applier;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import org.osgi.service.cm.ConfigurationAdmin;
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

import java.util.Arrays;
import java.util.List;

/**
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "ConditionApplier", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)

public class ConditionApplier extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

    private final Logger log = LoggerFactory.getLogger(ConditionApplier.class);

    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin ca;

    private SupportedDataType currentDataType;
    private SupportedOperation currentOperation;
    private ChannelAddress informationToGet;
    private ChannelAddress answerChannel;
    private boolean compareValue;
    private int trueConditionAnswer;
    private int falseConditionAnswer;

    public ConditionApplier() {
        super(OpenemsComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activateOrModified(config);

    }

    private void activateOrModified(Config config) throws OpenemsError.OpenemsNamedException {
        String configuration = config.answer();
        String[] entries = configuration.split(":");
        this.informationToGet = ChannelAddress.fromString(entries[0]);
        this.compareValue = entries[1].toLowerCase().equals("true");
        this.trueConditionAnswer = Integer.parseInt(entries[2]);
        this.falseConditionAnswer = Integer.parseInt(entries[3]);
        this.answerChannel = ChannelAddress.fromString(entries[4]);
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activateOrModified(config);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            try {
                Channel<?> informationChannel = this.cpm.getChannel(this.informationToGet);
                Channel<?> answerChannel = this.cpm.getChannel(this.answerChannel);
                int answerValue = this.falseConditionAnswer;
                if (informationChannel.value().isDefined() && informationChannel.channelDoc().getType().equals(OpenemsType.BOOLEAN)) {
                    if ((Boolean) informationChannel.value().get() == this.compareValue) {
                        answerValue = this.trueConditionAnswer;
                    }
                    if (answerChannel instanceof WriteChannel<?>) {
                        ((WriteChannel<?>) answerChannel).setNextWriteValueFromObject(answerValue);
                    } else {
                        answerChannel.setNextValue(answerValue);
                    }
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't apply Value for " + super.id() + " Reason: " + e.getMessage());
            }
        }
    }
}
