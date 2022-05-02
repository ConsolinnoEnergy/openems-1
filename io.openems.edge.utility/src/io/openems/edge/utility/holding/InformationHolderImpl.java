package io.openems.edge.utility.holding;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import io.openems.edge.utility.api.InformationHolder;
import io.openems.edge.utility.api.InputOutputType;
import io.openems.edge.utility.api.OutputWriter;
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

import java.util.Optional;

/**
 * This Component receives an EnableSignal. When the EnableSignal is set to true, the active Value will be written to the current Value.
 * When the EnableSignal is false, it stores the activeValue for a configured deltaTime (depending on the {@link io.openems.edge.timer.api.Timer}).
 * After the deltaTime is up, the inactiveValue will be written to the current value.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Utility.InformationHolder", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class InformationHolderImpl extends AbstractOpenemsComponent implements OpenemsComponent, InformationHolder, EventHandler {

    private final Logger log = LoggerFactory.getLogger(InformationHolderImpl.class);

    @Reference
    ComponentManager cpm;

    private String activeValue;
    private boolean activeValueIsChannel;
    private String inactiveValue;
    private boolean inactiveValueIsChannel;
    private String currentValue;
    private boolean isActive;
    private static final String IDENTIFIER = "INFORMATION_HOLDER_IDENTIFIER";
    private boolean writeToOutput;
    private InputOutputType outputType;
    private ChannelAddress output;
    private TimerHandler timerHandler;
    private boolean configSuccess;
    private Config config;


    public InformationHolderImpl() {
        super(OpenemsComponent.ChannelId.values(),
                InformationHolder.ChannelId.values());
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

    private void activationOrModifiedRoutine(Config config) throws OpenemsError.OpenemsNamedException {
        this.config = config;
        this.configSuccess = false;
        this.activeValue = config.activeValue();
        this.activeValueIsChannel = config.activeValueIsChannel();
        this.handleValueWrite(this.activeValue, this.activeValueIsChannel, this.getActiveValueChannel());
        this.inactiveValue = config.inactiveValue();
        this.inactiveValueIsChannel = config.inactiveValueIsChannel();
        this.currentValue = this.inactiveValue;
        this.handleValueWrite(this.inactiveValue, this.inactiveValueIsChannel, this.getInactiveValueChannel());
        this.currentValue = this.handleValueWrite(this.inactiveValue, this.inactiveValueIsChannel, this.getCurrentValue());
        this.isActive = false;
        this.isActive().setNextValue(false);
        if (this.timerHandler != null) {
            this.timerHandler.removeComponent();
        }
        try {
            this.timerHandler = new TimerHandlerImpl(this.id(), this.cpm);
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Unexpected Error. This shouldn't happen");
            return;
        }
        try {
            this.timerHandler.addOneIdentifier(IDENTIFIER, config.timerId(), config.deltaTime());
            this.writeToOutput = config.writeToOutput();
            if (this.writeToOutput) {
                this.outputType = config.outputType();
                this.output = ChannelAddress.fromString(config.outputAddress());
            }
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Error while configuring: " + this.id() + " Trying again later.");
            return;
        }
        this.configSuccess = true;
    }

    /**
     * This Method handles a ValueWrite depending on the given value.
     * If the value is a Channel, get the {@link io.openems.edge.common.channel.value.Value} from such channel.
     * Write the Value to the target Channel.
     *
     * @param value     the value or channelAddress.
     * @param isChannel declares if the value is a ChannelAddress.
     * @param target    where the result is write into.
     * @return the Value written into the target as a String.
     * @throws io.openems.common.exceptions.OpenemsError.OpenemsNamedException if channel cannot be found.
     */
    private String handleValueWrite(String value, boolean isChannel, Channel<String> target) throws OpenemsError.OpenemsNamedException {
        if (isChannel) {
            Channel<?> sourceChannel = this.cpm.getChannel(ChannelAddress.fromString(value));
            Object result = sourceChannel.value().orElse(null);
            if (result == null) {
                result = sourceChannel.getNextValue().orElse(null);
            }
            if (result != null) {
                target.setNextValue(result);
                return result.toString();
            } else {
                return "";
            }
        } else {
            target.setNextValue(value);
            return value;
        }
    }


    @Deactivate
    protected void deactivate() {
        this.timerHandler.removeComponent();
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            if (this.configSuccess) {
                boolean isEnabled = this.getEnableSignalChannel().getNextWriteValueAndReset().orElse(false);
                if (isEnabled) {
                    this.timerHandler.resetTimer(IDENTIFIER);
                    this.isActive = true;
                }
                if (this.timerHandler.checkTimeIsUp(IDENTIFIER)) {
                    this.isActive = false;
                    this.isActive().setNextValue(false);
                    try {
                        this.setCurrentValue(this.handleValueWrite(this.inactiveValue, this.inactiveValueIsChannel, this.getInactiveValueChannel()));
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Channel : " + this.inactiveValue + " Could not be parsed in : " + this.id() + " Channel might not be available");
                    }
                } else if (this.isActive) {
                    this.isActive().setNextValue(true);
                    try {
                        this.setCurrentValue(this.handleValueWrite(this.activeValue, this.activeValueIsChannel, this.getActiveValueChannel()));
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Channel : " + this.activeValue + " Could not be parsed in : " + this.id() + " Channel might not be available");
                    }
                }
                if (this.writeToOutput) {
                    OutputWriter.writeToOutput(this.currentValue, this.outputType, this.output, this.cpm);
                }
            } else {
                try {
                    this.activationOrModifiedRoutine(this.config);
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't apply config for : " + this.id());
                }
            }
        }
    }

    /**
     * Sets the {@link InformationHolder.ChannelId#CURRENT_VALUE} to the currentValue of this Class.
     *
     * @param value the current Value as a String.
     */
    private void setCurrentValue(String value) {
        this.getCurrentValue().setNextValue(value);
        this.currentValue = value;
    }

    @Override
    public String debugLog() {
        return "InformationHolder: "
                + this.id() + " is: " + (this.isActive ? "active" : "inactive") + " CurrentValue: " + this.currentValue;
    }
}
