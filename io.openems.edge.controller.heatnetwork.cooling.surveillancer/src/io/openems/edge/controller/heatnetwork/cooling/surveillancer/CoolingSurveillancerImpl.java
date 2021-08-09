package io.openems.edge.controller.heatnetwork.cooling.surveillancer;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This Controller Checks if the incoming Cooling request can be handled.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "CoolingSurveillancerImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class CoolingSurveillancerImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller {

    @Reference
    ComponentManager cpm;

    private final Logger log = LoggerFactory.getLogger(CoolingSurveillancerImpl.class);
    private List<Channel<Boolean>> inputRequests;
    private List<Channel<Boolean>> inputWatchdogs;
    private List<WriteChannel<Boolean>> outputs;

    public CoolingSurveillancerImpl() {
        super(OpenemsComponent.ChannelId.values(), Controller.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        try {
            this.inputRequests = this.checkInputChannels(config.inputRequest());
            this.inputWatchdogs = this.checkInputChannels(config.inputWatchdogs());
            this.outputs = this.checkOutputChannels(config.output());
            super.activate(context, config.id(), config.alias(), config.enabled());
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.error("Given Channels are not Existent or incompatible (not Boolean).");
        }
    }

    /**
     * Checks if the Configured Channels exists and are Boolean Channel.
     *
     * @param channelAddresses Array of ChannelAddresses as String.
     * @return List of Channels
     * @throws OpenemsError.OpenemsNamedException if Channel doesn't exist
     * @throws ConfigurationException             if Channel is not a Boolean Channel
     */
    @SuppressWarnings("unchecked")
    private List<Channel<Boolean>> checkInputChannels(String[] channelAddresses) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        List<Channel<Boolean>> returnChannel = new ArrayList<>();
        for (int n = 0; n < channelAddresses.length; n++) {
            Channel<?> test = this.cpm.getChannel(ChannelAddress.fromString(channelAddresses[n]));
            if (test.getType().equals(OpenemsType.BOOLEAN)) {
                returnChannel.add(n, (Channel<Boolean>) test);
            } else {
                throw new ConfigurationException("Not Boolean Channel", "Check Config");
            }
        }
        return returnChannel;
    }
    /**
     * Checks if the Configured Channels exists and are Boolean Channel.
     *
     * @param channelAddresses Array of ChannelAddresses as String.
     * @return List of Channels
     * @throws OpenemsError.OpenemsNamedException if Channel doesn't exist
     * @throws ConfigurationException             if Channel is not a Boolean Channel
     */
    @SuppressWarnings("unchecked")
    private List<WriteChannel<Boolean>> checkOutputChannels(String[] channelAddresses) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        List<WriteChannel<Boolean>> returnChannel = new ArrayList<>();
        for (int n = 0; n < channelAddresses.length; n++) {
            WriteChannel<?> test = this.cpm.getChannel(ChannelAddress.fromString(channelAddresses[n]));
            if (test.getType().equals(OpenemsType.BOOLEAN)) {
                returnChannel.add(n, (WriteChannel<Boolean>) test);
            } else {
                throw new ConfigurationException("Not Boolean Channel", "Check Config");
            }
        }
        return returnChannel;
    }
    @Modified
    void modified(ComponentContext context, Config config) {
        try {
            this.inputRequests = this.checkInputChannels(config.inputRequest());
            this.inputWatchdogs = this.checkInputChannels(config.inputWatchdogs());
            this.outputs = this.checkOutputChannels(config.output());
            super.modified(context, config.id(), config.alias(), config.enabled());
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.error("Given Channels are not Existent or incompatible (not Boolean).");
        }

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void run() {

        if (this.checkInput(this.inputRequests) && !this.checkInput(this.inputWatchdogs)) {
            this.setOutputs();
        }
    }

    /**
     * Sets all values of the outputs to the Specified Value.
     */
    private void setOutputs() {
        this.outputs.forEach(channel -> {
            try {
                channel.setNextWriteValue(true);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.error("Could not set Write Value!");
            }
        });
    }

    /**
     * Checks an inputChannel List for a true value.
     * @param input the input Channel List.
     * @return true if at least one member is true
     */
    private boolean checkInput(List<Channel<Boolean>> input) {
        AtomicBoolean returnValue = new AtomicBoolean(false);
        input.stream().filter(channel -> channel.value().isDefined() && channel.value().get()).findFirst().ifPresent(channel -> returnValue.set(true));
        return returnValue.get();
    }
}
