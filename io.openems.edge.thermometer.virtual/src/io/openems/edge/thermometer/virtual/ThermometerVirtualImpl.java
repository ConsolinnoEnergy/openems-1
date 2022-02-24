package io.openems.edge.thermometer.virtual;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerVirtual;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This Virtual Thermometer represents a Thermometer and the temperature can be manipulated by other classes/Components.
 * If a WriteValue is defined, it will be set in the TemperatureChannel.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Thermometer.Virtual",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})
public class ThermometerVirtualImpl extends AbstractOpenemsComponent implements OpenemsComponent, ThermometerVirtual, Thermometer, EventHandler {

    private final Logger log = LoggerFactory.getLogger(ThermometerVirtualImpl.class);
    private int offset = 0;

    @Reference
    ComponentManager cpm;

    ChannelAddress refThermometer;
    boolean useAnotherChannelAsTemp;
    boolean configSuccess;

    Config config;

    public ThermometerVirtualImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ThermometerVirtual.ChannelId.values(),
                Thermometer.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        this.activationOrModifiedRoutine(config);
        this.getTemperatureChannel().setNextValue(MISSING_TEMPERATURE);
        this.getTemperatureChannel().nextProcessImage();

    }

    /**
     * Basic operation on Activation or Modification of this component.
     *
     * @param config the config of this component.
     */
    private void activationOrModifiedRoutine(Config config) {
        try {
            this.getTemperatureChannel().setNextValue(Integer.MIN_VALUE);
            this.useAnotherChannelAsTemp = config.useAnotherChannelAsTemperature();
            if (this.useAnotherChannelAsTemp) {
                this.refThermometer = ChannelAddress.fromString(config.channelAddress());
            }
            this.configSuccess = true;
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't apply Config. Try again later.");
            this.configSuccess = false;
        }
        this.offset = config.offSet();

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Modified
    void modified(ComponentContext context, Config config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.configSuccess = false;
        this.config = config;
        this.activationOrModifiedRoutine(config);
    }

    @Override
    public void handleEvent(Event event) {
        if (this.isEnabled()) {
            if (this.configSuccess) {
                if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
                    if (!this.useAnotherChannelAsTemp) {
                        Optional<Integer> currentTemp = this.getVirtualTemperature();
                        currentTemp.ifPresent(integer -> this.getTemperatureChannel().setNextValue(integer + this.offset));
                    } else {
                        try {
                            Channel<?> temperatureChannel = this.cpm.getChannel(this.refThermometer);
                            Integer temperature = TypeUtils.getAsType(OpenemsType.INTEGER, temperatureChannel.value());

                            if (temperature != null && temperature != MISSING_TEMPERATURE) {
                                this.getTemperatureChannel().setNextValue(temperature + this.offset);
                            }
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.warn("Couldn't find Channel: " + this.refThermometer.toString());
                        }
                    }
                }
            } else {
                this.activationOrModifiedRoutine(this.config);
            }
        }
    }

    @Override
    public String debugLog() {
        Optional<Integer> currentTemp = Optional.ofNullable(this.getTemperatureChannel().value().isDefined() ? this.getTemperatureChannel().value().get() : null);
        AtomicReference<String> returnString = new AtomicReference<>("Temperature not Defined for: " + super.id());
        currentTemp.ifPresent(integer -> {
            if (integer != Integer.MIN_VALUE) {
                returnString.set(integer.toString() + this.getTemperatureChannel().channelDoc().getUnit().getSymbol());
            }
        });
        return returnString.get() + " dC" + "\n";
    }
}
