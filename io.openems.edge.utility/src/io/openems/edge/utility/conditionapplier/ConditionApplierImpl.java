package io.openems.edge.utility.conditionapplier;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
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
 * This is a Prototype of a Condition Applier.
 * In Future it should be a Programmable Component that can Do a:
 * if(INPUT apply to CONDITION){
 * APPLY ActiveValue(s) to FOO(s)
 * } else {
 * APPLY PassiveValue to BAR(s)
 * }
 * AT THE MOMENT you can have an input of a Boolean Channel and set the active/passive value and a response Channel that
 * receives the active/passive value.
 * This will be used for e.g. a virtual Thermometer.
 * A Component that Activates can be monitored, and if it's active -> set a virtual temperature.
 * Why do some may want/need this? -> the Temperature Surveillance Controller needs an ActivationThermometerReference
 * The ActivationTemperature can change -> e.g. if a HeatProgram a min Temperature of X is needed.
 * But if HeatStorages need to be filled a min Temperature of Y is needed.
 * To determine if X or Y should be applied the MinMaxer can be used. However, this will not be discussed here.
 * In Future -> this could be done here. The ConditionApplier will be a greater Project and needs Time to implement etc.
 */


@Designate(ocd = Config.class, factory = true)
@Component(name = "ConditionApplier", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)

public class ConditionApplierImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

    private final Logger log = LoggerFactory.getLogger(ConditionApplierImpl.class);

    @Reference
    ComponentManager cpm;

    private ChannelAddress informationToGet;
    private ChannelAddress answerChannel;
    private boolean compareValue;
    private int trueConditionAnswer;
    private int falseConditionAnswer;

    public ConditionApplierImpl() {
        super(ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activateOrModified(config);

    }

    /**
     * This method will be called if the component either activates or is modified.
     * It splits the config in : Input Channel, if expected value should be true or false.
     * Then the Active/Passive Value and where to put the response.
     *
     * @param config the config of the component.
     * @throws OpenemsError.OpenemsNamedException if the channel cannot be found.
     */
    private void activateOrModified(Config config) throws OpenemsError.OpenemsNamedException {
        String configuration = config.answer();
        String[] entries = configuration.split(":");
        //NOTE: Magic numbers because this is a prototype
        this.informationToGet = ChannelAddress.fromString(entries[0]);
        this.compareValue = entries[1].toLowerCase().equals("true");
        this.trueConditionAnswer = Integer.parseInt(entries[2]);
        this.falseConditionAnswer = Integer.parseInt(entries[3]);
        this.answerChannel = ChannelAddress.fromString(entries[4]);
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activateOrModified(config);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Get the Input/Response Channel.
     * check if the Condition Applies from the input channel -> Apply Active Value
     * else apply passive Value
     * @param event the Event -> This components reacts usually to the TopicCycleAfterControllers
     */
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
