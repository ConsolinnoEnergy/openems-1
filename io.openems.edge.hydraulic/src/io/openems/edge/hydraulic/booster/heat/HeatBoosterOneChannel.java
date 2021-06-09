package io.openems.edge.hydraulic.booster.heat;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.hydraulic.api.HeatBooster;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A HeatBooster that Activates on an active EnableSignal and writes an active Value into a Channel.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "HeatBoosterOneRelay",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)
public class HeatBoosterOneChannel extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler, HeatBooster {

    private final Logger logger = LoggerFactory.getLogger(HeatBoosterOneChannel.class);

    @Reference
    ComponentManager cpm;

    private static final String IDENTIFIER = "HEAT_BOOSTER_ENABLE";
    private TimerHandler timer;
    private ChannelAddress channelAddress;
    private boolean isActive = false;
    private int activeValue;
    private int disableValue;

    public HeatBoosterOneChannel() {
        super(OpenemsComponent.ChannelId.values(),
                HeatBooster.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.getChannelAddress(config.channelString());
        this.timer = new TimerHandlerImpl(super.id(), this.cpm);
        this.timer.addOneIdentifier(IDENTIFIER, config.timerId(), config.expiration());
        this.activeValue = config.value();
        this.disableValue = config.passiveValue();
    }

    private void getChannelAddress(String channelString) throws OpenemsError.OpenemsNamedException {
        this.channelAddress = ChannelAddress.fromString(channelString);
    }

    @Deactivate
    protected void deactivate() {
        this.timer.removeComponent();
        try {
            Channel<?> channelToWriteInto = this.cpm.getChannel(this.channelAddress);
            if (channelToWriteInto instanceof WriteChannel<?>) {
                ((WriteChannel<?>) channelToWriteInto).setNextWriteValueFromObject(this.disableValue);
            } else {
                channelToWriteInto.setNextValue(this.disableValue);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.logger.warn("Couldn't write into Channel: " + this.channelAddress);
        }
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            try {
                //normal reaction
                if (this.getHeatBoosterEnableSignalPresent()) {
                    if (this.getHeatBoosterEnableSignal()) {
                        this.isActive = true;
                        this.timer.resetTimer(IDENTIFIER);
                        this.setChannelValue(this.activeValue);
                    } else {
                        this.isActive = false;
                        this.timer.resetTimer(IDENTIFIER);
                        this.setChannelValue(this.disableValue);
                    }
                    this._resetEnableSignal();
                }
                // Time is up && active --> Deactivate and reset
                else if (this.isActive && this.timer.checkTimeIsUp(IDENTIFIER)) {
                    this.setChannelValue(this.disableValue);
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.logger.warn("Couldn't write into Channel: " + this.channelAddress + " of Component: " + super.id());
            }
        }
    }

    /**
     * This set the Channel Value by a value.
     *
     * @param value the value that will be set into the channel.
     * @throws OpenemsError.OpenemsNamedException if write fails.
     */
    private void setChannelValue(Object value) throws OpenemsError.OpenemsNamedException {
        Channel<?> channelToWriteInto = this.cpm.getChannel(this.channelAddress);
        if (channelToWriteInto instanceof WriteChannel<?>) {
            ((WriteChannel<?>) channelToWriteInto).setNextWriteValueFromObject(value);
        } else {
            channelToWriteInto.setNextValue(value);
        }
    }
}
