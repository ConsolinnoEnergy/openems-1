package io.openems.edge.hydraulic.minmax;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "MinMaxToChannelImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})

public class MinMaxToChannel extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

    private final Logger logger = LoggerFactory.getLogger(MinMaxToChannel.class);

    @Reference
    ComponentManager cpm;

    private MinMaxRoutine minMax;
    private List<ChannelAddress> channelAddresses;
    private List<ChannelAddress> answers;

    public MinMaxToChannel() {
        super(ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.channelAddresses = this.channelStringsToAddress(Arrays.asList(config.channel()));
        this.answers = this.channelStringsToAddress(Arrays.asList(config.answerChannel()));

        switch (config.minOrMax()) {
            case MIN:
                this.minMax = new MinRoutine();
                break;
            case MAX:
                this.minMax = new MaxRoutine();
                break;
        }

    }

    private List<ChannelAddress> channelStringsToAddress(List<String> channelStrings) throws OpenemsError.OpenemsNamedException {
        List<ChannelAddress> addresses = new ArrayList<>();
        OpenemsError.OpenemsNamedException[] ex = {null};
        channelStrings.forEach(entry -> {
            if (ex[0] == null) {
                try {
                    ChannelAddress channelAddress = ChannelAddress.fromString(entry);
                    if (addresses.contains(channelAddress) == false) {
                        addresses.add(channelAddress);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    ex[0] = e;
                }
            }
        });
        if (ex[0] != null) {
            throw ex[0];
        }
        return addresses;
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        List<String> channelStrings = Arrays.asList(config.channel());
        this.channelAddresses.clear();
        this.answers.clear();
        this.channelAddresses = this.channelStringsToAddress(Arrays.asList(config.channel()));
        this.answers = this.channelStringsToAddress(Arrays.asList(config.answerChannel()));

        switch (config.minOrMax()) {
            case MIN:
                this.minMax = new MinRoutine();
                break;
            case MAX:
                this.minMax = new MaxRoutine();
                break;
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            try {
                List<Integer> values = getIntegerValuesFromAddresses();
                int minMaxToWrite = this.minMax.executeRoutine(values);
                this.answerChannel(minMaxToWrite);
            } catch (OpenemsError.OpenemsNamedException e) {
                this.logger.warn("Couldn't access Channel in " + super.id());
            }
        }
    }

    private void answerChannel(int minMaxToWrite) throws OpenemsError.OpenemsNamedException {
        OpenemsError.OpenemsNamedException[] ex = {null};
        this.answers.forEach(entry -> {
            if (ex[0] == null) {
                Channel<?> answerChannel;
                try {
                    answerChannel = this.getChannelFromAddress(entry);

                    if (answerChannel instanceof WriteChannel<?>) {
                        ((WriteChannel<?>) answerChannel).setNextWriteValueFromObject(minMaxToWrite);
                    } else {
                        answerChannel.setNextValue(minMaxToWrite);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    ex[0] = e;
                }
            }
        });
        if (ex[0] != null) {
            throw ex[0];
        }

    }

    private Channel<?> getChannelFromAddress(ChannelAddress channelAddress) throws OpenemsError.OpenemsNamedException {
        return this.cpm.getChannel(channelAddress);
    }

    private List<Integer> getIntegerValuesFromAddresses() throws OpenemsError.OpenemsNamedException {
        OpenemsError.OpenemsNamedException[] ex = {null};
        List<Integer> values = new ArrayList<>();
        this.channelAddresses.forEach(entry -> {
            if (ex[0] == null) {
                try {
                    Channel<?> channel = this.getChannelFromAddress(entry);
                    if (channel.value().isDefined()) {
                        values.add((Integer) channel.value().get());
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    ex[0] = e;
                }
            }

        });
        if (ex[0] != null) {
            throw ex[0];
        }
        return values;
    }
}
