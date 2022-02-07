package io.openems.edge.utility.minmax;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.utility.api.MinMax;
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
 * This component gets a list of Channel and sorts the Values depending if this {@link MinMax}
 * is Min or Max. The Min/Max Value will be written into a responseChannel e.g. you can get a List of Thermometer Values
 * and write the Max Value into a response Channel.
 * This will be used in e.g. the TemperatureSurveillanceController ->
 * A Temp surveillance controller can have different ActivationTemperatures.
 * or in other words 2 Temperature Values will be defined, and if Reference < Activation Temperature then activate the temp surveillance.
 * However this activationTemperature may change.
 * If a Heatnetwork needs HeatRadiator usage -> use Temp X
 * If a Heatnetwork needs to heat up HeatStorages -> use Temp Y
 * Both active? Which one to use -> MAX
 * Where to write the value ? -> VirtualThermometer -> Apply VirtualTemp to current Temperature
 * This can be used anywhere, however, this works only with int values atm.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "MinMaxToChannelImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})

public class MinMaxToChannel extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

    private final Logger logger = LoggerFactory.getLogger(MinMaxToChannel.class);

    @Reference
    ComponentManager cpm;

    private List<ChannelAddress> channelAddresses;
    private List<ChannelAddress> response;
    private MinMax minMax;
    private int minMaxToWrite;

    public MinMaxToChannel() {
        super(ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.channelAddresses = this.channelStringsToAddress(Arrays.asList(config.inputChannel()));
        this.response = this.channelStringsToAddress(Arrays.asList(config.responseChannel()));
        this.minMax = config.minOrMax();

    }

    /**
     * Get the ChannelAddress entries from the Config and add them to the channelAddresses list.
     *
     * @param channelStrings List of Strings containing ChannelAddresses, usually from config.
     * @return the ChannelAddresses
     * @throws OpenemsError.OpenemsNamedException if ChannelAddress is wrong.
     */
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
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.channelAddresses.clear();
        this.response.clear();
        this.channelAddresses = this.channelStringsToAddress(Arrays.asList(config.inputChannel()));
        this.response = this.channelStringsToAddress(Arrays.asList(config.responseChannel()));
        this.minMax = config.minOrMax();

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Get all Values from the saved ChannelAddresses and get the Value that will be written in the response channel.
     *
     * @param event the event, usually after Controllers.
     */
    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled()) {
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
                try {
                    List<Integer> values = this.getIntegerValuesFromAddresses();
                    Integer[] arrayValues = (Integer[]) this.getIntegerValuesFromAddresses().toArray();
                    int minMaxToWrite = 0;
                    switch (this.minMax) {
                        case MIN:
                            minMaxToWrite = TypeUtils.max(arrayValues);
                            break;
                        default:
                        case MAX:
                            minMaxToWrite = TypeUtils.min(arrayValues);
                            break;
                    }
                    this.writeValueToResponseChannel(minMaxToWrite);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.logger.warn("Couldn't access Channel in " + super.id());
                }
            }
        }
    }

    /**
     * This will be called in the handleEvent method and writes the Min/Max Value into the responseChannel.
     *
     * @param minMaxToWrite the value determined by the {@link TypeUtils#min(Integer...)} or {@link TypeUtils#max(Integer...)}.
     * @throws OpenemsError.OpenemsNamedException if write fails
     */
    private void writeValueToResponseChannel(int minMaxToWrite) throws OpenemsError.OpenemsNamedException {
        this.minMaxToWrite = minMaxToWrite;
        OpenemsError.OpenemsNamedException[] ex = {null};
        this.response.forEach(entry -> {
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

    /**
     * Gets a channel from a channelAddress.
     *
     * @param channelAddress the channelAddress.
     * @return the Channel from the Address.
     * @throws OpenemsError.OpenemsNamedException if Channel is not available.
     */
    private Channel<?> getChannelFromAddress(ChannelAddress channelAddress) throws OpenemsError.OpenemsNamedException {
        return this.cpm.getChannel(channelAddress);
    }

    /**
     * Gets the Integer values from the Channel of this {@link #channelAddresses}.
     *
     * @return the Integer Value list from the channel
     * @throws OpenemsError.OpenemsNamedException if channel cannot be found.
     */
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
