package io.openems.edge.thermometer.threshold;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerState;
import io.openems.edge.thermometer.api.ThermometerThreshold;
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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This Component Helps to Handle Thermometer and their thresholds.
 * It has a reference Thermometer to get the "real" Temperature and works furthermore with it.
 * "Jumping" / Fluctuations on the Thermometer are prevented, other HeatsystemComponents/HeatnetworkController can use this Thermometer for easier decisioning.
 * Sometimes there are TemperatureFluctuations; The IntervalCounter prevents "jumping" of temperature
 * Example: The temperature is usually 500 dC; the maxInterval is (for easier explanation) 1
 * if for one cycle the temperature falls to 490 dC and the current interval counter is 0 ; nothing will happen.
 * If however in the next cycle the temperature stays at 490 temperature will be set, otherwise if temperature stays at 500 interval reset
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Thermometer.Threshold",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})

public class ThermometerThresholdImpl extends AbstractOpenemsComponent implements OpenemsComponent, ThermometerThreshold, EventHandler {

    Logger log = LoggerFactory.getLogger(ThermometerThresholdImpl.class);

    @Reference
    ComponentManager cpm;

    private final AtomicInteger intervalToWaitCounter = new AtomicInteger(0);
    private static final int RESET = 0;

    private int maxIntervalToWait;
    private boolean configSuccess;

    private Thermometer referenceThermometer;
    private boolean defaultActive = true;

    public ThermometerThresholdImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Thermometer.ChannelId.values(),
                ThermometerThreshold.ChannelId.values());
    }

    Config config;

    @Activate
    void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
        this.setThermometerState(ThermometerState.RISING);
    }

    /**
     * This method sets up the basic config on activation/modified.
     *
     * @param config the Config of this Component.
     */
    private void activationOrModifiedRoutine(Config config) {
        try {
            this.config = config;
            int defaultTemperature = config.defaultTemperature();
            if (this.cpm.getComponent(config.thermometerId()) instanceof Thermometer) {
                this.referenceThermometer = this.cpm.getComponent(config.thermometerId());
                int referenceTemperature = this.referenceThermometer.getTemperatureValue();
                if (referenceTemperature == MISSING_TEMPERATURE) {
                    referenceTemperature = defaultTemperature;
                    this.defaultActive = true;
                }
                int threshold = config.thresholdTemperature();
                int fictionalTemperature = (referenceTemperature / threshold) * threshold;
                this.getTemperatureChannel().setNextValue(fictionalTemperature);
                this.maxIntervalToWait = config.maxInterval();
                this.setThresholdInDeciDegree(config.thresholdTemperature());
                if (this.getSetPointTemperature() == Integer.MIN_VALUE) {
                    this.getSetPointTemperatureChannel().setNextValue(config.startSetPointTemperature());
                }
            } else {
                throw new ConfigurationException("Activate Method ThresholdTemperatureImpl " + config.id(),
                        "Activate Method: ThermometerId" + config.thermometerId() + " Not an Instance of Thermometer");
            }
            this.configSuccess = true;
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Couldn't apply config. Trying again later");
            this.configSuccess = false;
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Modified
    void modified(ComponentContext context, Config config) {
        this.configSuccess = false;
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }

    /**
     * Assert that TemperatureSensor is always active/enabled and correct
     * And update the Thermometer.
     *
     * @param event BeforeProcess Image
     */
    @Override
    public void handleEvent(Event event) {
        if (this.configSuccess) {
            if (this.isEnabled()) {
                if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
                    if (this.referenceThermometer != null) {
                        try {
                            OpenemsComponent component = this.cpm.getComponent(this.referenceThermometer.id());
                            if (this.referenceThermometer.isEnabled() == false
                                    || !this.referenceThermometer.equals(component)) {
                                if (component instanceof Thermometer) {
                                    this.referenceThermometer = (Thermometer) component;
                                }
                            }
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.warn("Couldn't find the ReferenceThermometer!");
                        }
                        if (this.referenceThermometer.getTemperatureValue() != Thermometer.MISSING_TEMPERATURE) {
                            this.calculateTemperatureValue();
                        }
                    }
                }
            }
        } else {
            this.activationOrModifiedRoutine(this.config);
        }
    }

    /**
     * Calculate the Fictional Temperature and sets to the TemperatureChannel.
     * Depends on: Falling or Rising Thermometer State; intervalCount; Threshold
     * <p>
     * Example: Lets say we have an initial Temperature of 500 dC (comes from a temperatureSensor)
     * And our Temperature is in the state of  RISING
     * Our Threshold is 10dC (1°C)
     * And we have an interval of 1 (1 fluctuation of our thresholdLimit is allowed)
     * Now our thermometer measures 495 dC
     * What happens to our fictional temperature? nothing
     * what happens if thermometer is 491? nothing
     * what happens if thermometer is 490 or 489? --> set temperature to 490 IF and only IF -> interval is at the limit
     * otherwise nothing happens
     * </p>
     * If the temperature is FALLING same thing applies.
     */
    private void calculateTemperatureValue() {
        int currentTemperature = this.getTemperatureValue();
        float thermometerValue = (float) this.referenceThermometer.getTemperatureValue();
        if (this.referenceThermometer.getTemperatureValue() == MISSING_TEMPERATURE) {
            return;
        }
        int incomingTemperature;
        boolean roundUp = false;
        float threshold = this.getThreshold();
        //IF temperature is 580 dC Threshold = 10dC ; Thermometer shows 574dC --> round up 574/10 = 58;
        // thermometer show 589dC <- rond down thermometer/threshold ---> 589/10 = 58;
        //Otherwise --> 574/10 would be 57 and 589/10 stays 58
        if (thermometerValue < currentTemperature) {
            roundUp = true;
        }
        // now we know how to round -> ceil or floor
        incomingTemperature = roundUp ? (int) (Math.ceil(thermometerValue / threshold) * threshold)
                : (int) (Math.floor(thermometerValue / threshold) * threshold);
        //Check if it's not equals --> e.g. 580 dC was before now its 570dC
        if (incomingTemperature != currentTemperature) {
            //Check if we have to wait until we can set the new Temperature
            if (this.intervalToWaitCounter.get() < this.maxIntervalToWait && !this.defaultActive) {
                this.intervalToWaitCounter.getAndIncrement();
            } else {
                //Apply new Values -> Is Thermometer rising/falling, whats the new temperature etc etc
                boolean falling = incomingTemperature < currentTemperature;
                ThermometerState newThermometerState = ThermometerState.RISING;
                if (falling) {
                    newThermometerState = ThermometerState.FALLING;
                }
                this.getTemperatureChannel().setNextValue(incomingTemperature);
                this.setThermometerState(newThermometerState);
                this.intervalToWaitCounter.getAndSet(RESET);
                this.defaultActive = false;
            }
        } else {
            this.intervalToWaitCounter.getAndSet(RESET);
        }
    }

    @Override
    public String debugLog() {
        return "Temperature: " + this.getDebugStringTemperature(this.getTemperatureValue()) + " Reference Temperature: "
                + this.getDebugStringTemperature(this.referenceThermometer.getTemperatureValue());
    }

    private String getDebugStringTemperature(int temperatureValue) {
        return (temperatureValue == Thermometer.MISSING_TEMPERATURE ? "NotDefined" : temperatureValue)
                + this.getTemperatureChannel().channelDoc().getUnit().getSymbol();
    }
}
