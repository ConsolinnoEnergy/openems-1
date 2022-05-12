package io.openems.edge.utility.integerbitconverter;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.configupdate.ConfigurationUpdate;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.IntegerBitConverter;
import org.osgi.service.cm.ConfigurationAdmin;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This component receives an Integer input and converts this to bits.
 * Those bits are written internally and also to an optional output.
 */

@Designate(ocd = IntegerToBitConverterConfig.class, factory = true)
@Component(name = "Utility.Converter.IntegerToBit", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class IntegerToBitConverterImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler, IntegerBitConverter {

    private final Logger log = LoggerFactory.getLogger(IntegerToBitConverterImpl.class);
    private static final String SPLIT_STRING = ":";
    private static final int SPLIT_LENGTH = 2;
    private static final int SPLIT_BIT_POSITION = 0;
    private static final int SPLIT_CHANNEL_POSITION = 1;

    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin cm;

    private ChannelAddress input;
    private String otherComponent;
    private boolean useOtherComponent = false;
    private boolean writeToOutput = false;
    private final Map<Integer, ChannelAddress> bitToChannelMap = new HashMap<>();
    private InputOutputType inputType = InputOutputType.VALUE;
    private IntegerToBitConverterConfig config;
    private boolean configSuccess;

    public IntegerToBitConverterImpl() {
        super(OpenemsComponent.ChannelId.values(),
                IntegerBitConverter.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, IntegerToBitConverterConfig config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    @Modified
    void modified(ComponentContext context, IntegerToBitConverterConfig config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    private void activationOrModifiedRoutine(IntegerToBitConverterConfig config) {
        this.config = config;
        try {
            this.input = this.integerValueChannel().address();
            this.useOtherComponent = config.useOtherComponent();
            if (this.useOtherComponent) {
                this.displayComponent(config);
                this.otherComponent = config.otherComponentId();
                this.input = new ChannelAddress(this.otherComponent, config.inputChannel());
                this.inputType = config.inputType();
            }
            this.writeToOutput = config.writeToOutput();
            this.bitToChannelMap.clear();
            if (this.writeToOutput && config.configurationDone()) {
                this.createBitToChannelMap(config.mapBitToChannel());
            }
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException | IOException e) {
            this.log.warn(this.id() + " The Configuration went wrong: " + e.getMessage());
            this.configSuccess = false;
            return;
        }
        this.configSuccess = true;
    }

    /**
     * Creates a Bit to channelMap.
     * Each Bit position receives an output channel. Only called, when {@link #writeToOutput} is set to true.
     *
     * @param mapBitToChannel usually from config, the array where a bit is mapped to a channel(address)
     * @throws ConfigurationException             thrown when either the config entry itself is wrong or a channelAddress is missing.
     * @throws OpenemsError.OpenemsNamedException when the channelAddress cannot be created.
     */
    private void createBitToChannelMap(String[] mapBitToChannel) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        for (String part : mapBitToChannel) {
            String[] splits = part.split(SPLIT_STRING);
            if (splits.length != SPLIT_LENGTH) {
                throw new ConfigurationException("createBitToChannelMap", this.id() + " Config wrong at mapToBitChannel: " + part);
            } else {
                String channel = splits[SPLIT_CHANNEL_POSITION];
                String bitPosition = splits[SPLIT_BIT_POSITION];
                ChannelAddress outputAddress;
                if (channel.contains("/")) {
                    outputAddress = ChannelAddress.fromString(channel);
                } else if (this.useOtherComponent) {
                    outputAddress = new ChannelAddress(this.otherComponent, channel);
                } else {
                    throw new ConfigurationException("createBitToChannelMap", this.id() + " Config wrong at mapToBitChannel: " + part);
                }
                this.bitToChannelMap.put(Integer.parseInt(bitPosition), outputAddress);
            }
        }
    }

    /**
     * Creates the channelId entries.
     *
     * @param config the configuration
     * @throws OpenemsError.OpenemsNamedException thrown if the other component cannot be accessed.
     * @throws IOException                        on ConfigurationUpdate fail.
     */
    private void displayComponent(IntegerToBitConverterConfig config) throws OpenemsError.OpenemsNamedException, IOException {
        List<Channel<?>> channels =
                this.cpm.getComponent(config.otherComponentId()).channels().stream().filter(entry ->
                        !entry.channelId().id().startsWith("_Property")
                ).collect(Collectors.toList());
        List<String> channelToDisplay = new ArrayList<>();
        channels.sort(Comparator.comparing(a -> a.channelId().id()));
        channels.forEach(channel -> {
            String channelId = channel.channelId().id();
            channelToDisplay.add(channelId);
        });
        Map<String, Object> propertyMap = new HashMap<>();
        if (!channelToDisplay.isEmpty() && channelToDisplay.size() != config.channelIds().length) {
            propertyMap.put("channelIds", channelToDisplay);
            ConfigurationUpdate.updateConfig(this.cm, this.servicePid(), propertyMap);
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled()) {
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
                if (this.configSuccess) {
                    Integer input = this.getInput();
                    if (input != null) {
                        boolean[] output = IntegerBitConverter.getBitsFromInteger(input);
                        this.mapOutputToChannel(output);
                    }
                } else {
                    this.activationOrModifiedRoutine(this.config);
                }
            }
        }
    }

    /**
     * Get the input. Either from internal channel or from otherComponent Channel.
     * Depends on the configuration.
     *
     * @return the input.
     */
    private Integer getInput() {
        Integer returnValue = this.integerValueChannel().getNextWriteValueAndReset().orElse(0);
        if (this.useOtherComponent) {
            Channel<?> inputChannel;
            Object inputValue = null;
            try {
                inputChannel = this.cpm.getChannel(this.input);
                switch (this.inputType) {

                    case VALUE:
                        inputValue = inputChannel.value().orElse(null);
                        break;
                    case NEXT_VALUE:
                        inputValue = inputChannel.getNextValue().orElse(null);
                        break;
                    case NEXT_WRITE_VALUE:
                        if (inputChannel instanceof WriteChannel<?>) {
                            inputValue = ((WriteChannel<?>) inputChannel).getNextWriteValue().orElse(null);
                        } else {
                            inputValue = inputChannel.getNextValue().orElse(null);
                        }
                        break;
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.info(this.id() + " Couldn't access Input Channel! " + e.getMessage());
            }
            String tempValue = TypeUtils.getAsType(OpenemsType.STRING, inputValue);
            if (tempValue != null) {
                returnValue = Integer.parseUnsignedInt(tempValue);
            }
        }
        this.integerValueChannel().setNextValue(returnValue);
        this.longValueChannel().setNextValue(Integer.toUnsignedLong(returnValue));
        return returnValue;
    }

    /**
     * The Integer input was converted to a boolean output.
     * Write the output to corresponding channel.
     * Start with bit 1 -> at position 0.
     *
     * @param output bits of the Integer input as booleans.
     */
    private void mapOutputToChannel(boolean[] output) {
        for (int i = 1; i <= output.length; i++) {
            boolean value = output[i - 1];
            this.writeBoolToInternalChannel(i, value);
            if (this.writeToOutput && this.bitToChannelMap.containsKey(i)) {
                Channel<?> outputChannel = null;
                try {
                    outputChannel = this.cpm.getChannel(this.bitToChannelMap.get(i));
                    if (outputChannel instanceof WriteChannel<?>) {
                        ((WriteChannel<?>) outputChannel).setNextWriteValueFromObject(value);
                    } else {
                        outputChannel.setNextValue(value);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn(this.id() + "Couldn't access Channel: " + this.bitToChannelMap.get(i));
                }

            }
        }
    }
}
