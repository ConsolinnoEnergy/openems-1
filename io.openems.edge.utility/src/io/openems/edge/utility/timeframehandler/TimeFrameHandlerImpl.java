package io.openems.edge.utility.timeframehandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.TimeFrameHandler;
import io.openems.edge.utility.api.TimeFrameType;
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
 * This component is able to tell if your current Time is within a TimeFrame.
 * Configure the start and stop interval, as well as the {@link TimeFrameType} to determine if you want to look at
 * e.g. Minutes, Days, Months, Years etc.
 * The Result of this TimeFrame is stored within the {@link ChannelId#IS_WITHIN_TIME_FRAME} channel.
 * Optionally you can set the result of the timeFrame to an output (ChannelAddress).
 * This component uses the {@link TimeFrameHandler} interface for measurement.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Utility.TimeFrameHandler", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class TimeFrameHandlerImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

    @Reference
    ComponentManager cpm;

    private final Logger log = LoggerFactory.getLogger(TimeFrameHandlerImpl.class);

    private int start;
    private int stop;
    private TimeFrameType timeFrameType;
    private boolean useOutput;
    private ChannelAddress outputChannel;
    InputOutputType inputOutputType;
    private boolean isWithinTimeFrame;

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Is within Time Frame? When TimeFrameHandler checks TimeFrame and the current Time is within the TimeFrame.
         * Set this channel to true.
         *
         * <ul>
         * <li>Type: Boolean
         * </ul>
         */
        IS_WITHIN_TIME_FRAME(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Get the channel of {@link ChannelId#IS_WITHIN_TIME_FRAME}.
     *
     * @return the channel.
     */
    public Channel<Boolean> getIsWithinTimeFrameChannel() {
        return this.channel(ChannelId.IS_WITHIN_TIME_FRAME);
    }

    public TimeFrameHandlerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    /**
     * Basic operation to allocate the attributes.
     *
     * @param config the config of this component
     * @throws OpenemsError.OpenemsNamedException when the outputChannel is wrong.
     */
    private void activationOrModifiedRoutine(Config config) throws OpenemsError.OpenemsNamedException {
        this.start = Math.max(config.start(), 0);
        this.stop = Math.max(config.stop(), 0);
        this.timeFrameType = config.timeFrameType();
        this.useOutput = config.useOutput();
        if (this.useOutput) {
            this.outputChannel = ChannelAddress.fromString(config.channelOutput());
        }
    }


    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled() && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            this.isWithinTimeFrame = TimeFrameHandler.isWithinTimeFrame(this.timeFrameType, this.start, this.stop);
            this.getIsWithinTimeFrameChannel().setNextValue(this.isWithinTimeFrame);
            try {
                if (this.useOutput && this.outputChannel != null) {
                    Channel<?> outputChannel = this.cpm.getChannel(this.outputChannel);
                    switch (this.inputOutputType) {
                        case VALUE:
                            outputChannel.setNextValue(this.isWithinTimeFrame);
                            outputChannel.nextProcessImage();
                            break;
                        case NEXT_VALUE:
                            outputChannel.setNextValue(this.isWithinTimeFrame);
                            break;
                        case NEXT_WRITE_VALUE:
                            if (outputChannel instanceof WriteChannel<?>) {
                                ((WriteChannel<?>) outputChannel).setNextWriteValueFromObject(this.isWithinTimeFrame);
                            }
                            break;
                    }
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn(this.id() + " Couldn't set output! Reason: " + e.getMessage());
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public String debugLog() {
        return this.id() + " is " + (!this.isWithinTimeFrame ? "not " : "") + "within the Time Frame: "
                + this.timeFrameType + " Start: " + this.start + " Stop: " + this.stop;
    }
}
