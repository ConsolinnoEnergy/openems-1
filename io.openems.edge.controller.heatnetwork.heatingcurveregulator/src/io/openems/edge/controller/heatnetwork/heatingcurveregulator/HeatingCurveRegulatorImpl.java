package io.openems.edge.controller.heatnetwork.heatingcurveregulator;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.configupdate.ConfigurationUpdate;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.heatingcurveregulator.api.HeatingCurveRegulator;
import io.openems.edge.thermometer.api.Thermometer;
import org.osgi.service.cm.ConfigurationAdmin;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This is a temperature dependent heating controller.
 * - It takes a temperature (usually outsideTemperature) as input and asks for the heating to be turned on or off based on the outside
 * temperature.
 * Additionally, it calculates a setPoint Temperature.
 * - If the outside temperature is below the activation threshold, a heating temperature is calculated based
 * on a parametrized heating curve.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Heatnetwork.HeatingCurve", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class HeatingCurveRegulatorImpl extends AbstractOpenemsComponent implements OpenemsComponent, HeatingCurveRegulator, Controller {

    private final Logger log = LoggerFactory.getLogger(HeatingCurveRegulatorImpl.class);
    @Reference
    ConfigurationAdmin ca;

    @Reference
    protected ComponentManager cpm;

    private ChannelAddress outsideThermometer;
    private int activationTemp;
    private int roomTemp;
    private double slope;
    private int offset;
    private LocalDateTime timestamp;
    private int measurementTimeMinutes;
    private int minimumStateTimeMinutes;
    private boolean measureAverage = false;
    private int measurementCounter = 0;
    private static final int MINUTE_IN_SECONDS = 60;
    private final Integer[] measurementDataOneMinute = new Integer[MINUTE_IN_SECONDS];
    private final List<Integer> measurementData = new ArrayList<>();
    private boolean shouldBeHeating = false;
    private static final int TOLERANCE_FOR_AVERAGE_MEASUREMENT = 30;
    private int calculatedHeatingCurveTemperature = 450;

    public HeatingCurveRegulatorImpl() {
        super(OpenemsComponent.ChannelId.values(),
                HeatingCurveRegulator.ChannelId.values(),
                Controller.ChannelId.values());
    }

    private boolean initial = true;

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.initial = true;
        this.activationOrModifiedRoutine(config);
        this.noErrorChannel().setNextValue(true);
        // Set timestamp so that logic part 1 executes right away.
        this.timestamp = LocalDateTime.now().minusMinutes(this.minimumStateTimeMinutes);

        this.measureAverage = false;
        this.measurementCounter = 0;
        this.measurementData.clear();
        this.initializeMeasurementMinuteData();
    }

    private void initializeMeasurementMinuteData() {
        Arrays.fill(this.measurementDataOneMinute, Thermometer.MISSING_TEMPERATURE);
    }

    private void activationOrModifiedRoutine(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.activationTemp = config.activation_temp();
        this.roomTemp = config.room_temp();
        // Activation temperature can not be higher than desired room temperature, otherwise the function will crash.
        if (this.activationTemp > this.roomTemp) {
            this.activationTemp = this.roomTemp;
        }
        // Convert to deci-degree, since sensor data is deci-degree too.
        this.activationTemp = this.activationTemp * HeatingCurveRegulator.CELSIUS_TO_DECI_DEGREE_CONVERTER;
        this.slope = config.slope();
        this.offset = config.offset();
        this.measurementTimeMinutes = config.measurement_time_minutes();
        this.minimumStateTimeMinutes = config.minimum_state_time_minutes();
        if (this.measurementTimeMinutes > this.minimumStateTimeMinutes) {
            this.measurementTimeMinutes = this.minimumStateTimeMinutes;
        }

        if (this.cpm.getComponent(config.thermometerId()) instanceof Thermometer) {
            Thermometer thermometer = this.cpm.getComponent(config.thermometerId());
            this.outsideThermometer = thermometer.getTemperatureChannel().address();
        } else {
            throw new ConfigurationException("The configured component is not a temperature sensor! Please check "
                    + config.thermometerId(), "configured component is incorrect!");
        }
        this.initializeChannel();
    }

    private void initializeChannel() {
        this.getRoomTemperature().setNextValue(this.roomTemp);
        this.getActivationTemperature().setNextValue(this.activationTemp / HeatingCurveRegulator.CELSIUS_TO_DECI_DEGREE_CONVERTER);
        this.getSlope().setNextValue(this.slope);
        this.getOffset().setNextValue(this.offset);
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(config);
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
        this.turnHeaterOff();
    }

    private void updateConfig() {

        Map<String, Object> properties = new HashMap<>();
        Optional<Integer> integerValue = this.getRoomTemperature().getNextWriteValueAndReset();
        if (integerValue.isPresent() && integerValue.get() != this.roomTemp) {
            properties.put("room_temp", integerValue.get());
        }
        integerValue = this.getActivationTemperature().getNextWriteValueAndReset();
        if (integerValue.isPresent() && this.activationTemp != (integerValue.get() * HeatingCurveRegulator.CELSIUS_TO_DECI_DEGREE_CONVERTER)) {
            properties.put("activation_temp", integerValue.get());
        }
        integerValue = this.getOffset().getNextWriteValueAndReset();
        if (integerValue.isPresent() && this.offset != integerValue.get()) {
            properties.put("offset", integerValue.get());
        }
        Optional<Double> doubleValue = this.getSlope().getNextWriteValueAndReset();
        if (doubleValue.isPresent() && this.slope != doubleValue.get()) {
            properties.put("slope", doubleValue.get());
        }
        if (!properties.isEmpty()) {
            try {
                ConfigurationUpdate.updateConfig(this.ca, this.servicePid(), properties);
            } catch (IOException e) {
                this.log.warn("Couldn't update Config from REST params.");
            }
        }
    }

    @Override
    public void run() {
        this.updateConfig();

        int outsideTemperature = Thermometer.MISSING_TEMPERATURE;
        try {
            //No check for Int needed, since it is actually the Thermometer -> getTemperatureChannel
            outsideTemperature = ((Channel<Integer>) this.cpm.getChannel(this.outsideThermometer)).value().orElse(Thermometer.MISSING_TEMPERATURE);
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.warn(this.id() + " Couldn't read Temperature from ChannelAddress. Might be missing.");
        }
        boolean temperatureIsMissing = this.hasMissingTemperature(outsideTemperature);
        if (!temperatureIsMissing) {

            this.checkToKeepCurrentState(outsideTemperature);

            this.measureAverageTemperatureOfMinute(outsideTemperature);

            this.checkAverageTemperatureAndDecideShouldBeHeating();

            if (this.shouldBeHeating) {
                this.turnHeaterOn();

                this.calculatedHeatingCurveTemperature = Math.round(HeatingCurveRegulator.calculateHeatingCurveTemperatureInDeciDegree(this.slope, this.offset, this.roomTemp, outsideTemperature));

                this.setHeatingTemperature(this.calculatedHeatingCurveTemperature);
            } else {
                this.turnHeaterOff();
            }
        }
    }

    /**
     * Part 1. Test temperature. Execution blocked by timestamp that is set when state changes (heating or no
     * heating). This means once state changes, it will keep that state for at least minimumStateTimeMinutes.
     *
     * @param outsideTemperature the current outsideTemperature
     */
    private void checkToKeepCurrentState(int outsideTemperature) {
        if (this.shouldBeHeating && ChronoUnit.MINUTES.between(this.timestamp, LocalDateTime.now()) > this.minimumStateTimeMinutes) {
            // Check if temperature is above this.activationTemp
            if (outsideTemperature > this.activationTemp) {
                this.resetMeasurement();
            }
        }
        if (this.initial || (this.shouldBeHeating == false && ChronoUnit.MINUTES.between(this.timestamp, LocalDateTime.now()) > this.minimumStateTimeMinutes)) {
            this.initial = false;
            // Check if temperature is below this.activationTemp
            if (outsideTemperature <= this.activationTemp) {
                this.resetMeasurement();
            }
        }
    }


    /**
     * Part 2. Get average temperature over a set time period (entered in config). Use that average to decide
     * heating state. Has a shortcut that decides faster if average temperature of 60 cycles is well above or
     * below activation temperature. The shortcut is for a controller restart in winter, so heating starts faster.
     *
     * @param outsideTemperature the measured outsideTemperature.
     */
    private void measureAverageTemperatureOfMinute(int outsideTemperature) {
        if (this.measureAverage) {
            int average = Thermometer.MISSING_TEMPERATURE;
            this.measurementDataOneMinute[this.measurementCounter] = outsideTemperature;
            this.measurementCounter = this.measurementCounter % MINUTE_IN_SECONDS;
            this.measurementCounter++;
            AtomicInteger count = new AtomicInteger(0);
            AtomicInteger sum = new AtomicInteger(0);
            if (this.measurementCounter == MINUTE_IN_SECONDS - 1) {
                Arrays.stream(this.measurementDataOneMinute).filter(entry -> entry != Thermometer.MISSING_TEMPERATURE).forEach(validTemperature -> {
                    count.getAndIncrement();
                    sum.getAndAdd(validTemperature);
                });

                if (count.get() > 0) {
                    average = (sum.get() / count.get());
                    this.measurementData.add(average);
                }
            }

            // Shortcut if average of one minute is 30dC above or below this.activationTemp.
            if (average != Thermometer.MISSING_TEMPERATURE) {
                if (this.shouldBeHeating) {
                    if (average > this.activationTemp + TOLERANCE_FOR_AVERAGE_MEASUREMENT) {
                        this.shouldBeHeating = false;
                        this.timestamp = LocalDateTime.now();
                        this.measureAverage = false;
                    }
                } else if (average <= this.activationTemp - TOLERANCE_FOR_AVERAGE_MEASUREMENT) {
                    this.shouldBeHeating = true;
                    this.timestamp = LocalDateTime.now();
                    this.measureAverage = false;
                }
            }
        }
    }

    /**
     * Part 3. Evaluation at end of measurement time.
     * Fail-safe: needs at least one entry in measurementData
     */
    private void checkAverageTemperatureAndDecideShouldBeHeating() {
        if (ChronoUnit.MINUTES.between(this.timestamp, LocalDateTime.now()) > this.measurementTimeMinutes
                && !this.measurementData.isEmpty()) {
            AtomicInteger sum = new AtomicInteger(0);
            this.measurementData.forEach(sum::getAndAdd);

            int totalAverage = sum.get() / this.measurementData.size();

            if (this.shouldBeHeating) {
                // Is heating right now. Should heating be turned off?
                if (totalAverage > this.activationTemp) {
                    this.shouldBeHeating = false;
                    this.timestamp = LocalDateTime.now();
                } else {
                    this.setTimeStampToExectuePartOne();
                }
            } else {
                // Is not heating right now. Should heating be turned on?
                if (totalAverage <= this.activationTemp) {
                    this.shouldBeHeating = true;
                    this.timestamp = LocalDateTime.now();
                } else {
                    this.setTimeStampToExectuePartOne();
                }
            }
            this.measureAverage = false;
        }


    }

    /**
     * Check before actual HeatingCurve "logic". If the outsideTemperature equals a MissingTemperature
     * The communication probably failed and the HeatingCurve enters an error state.
     *
     * @param outsideTemperature the measured outside temperature.
     * @return true if temperature equals {@link Thermometer#MISSING_TEMPERATURE}.
     */
    private boolean hasMissingTemperature(int outsideTemperature) {

        if (outsideTemperature != Thermometer.MISSING_TEMPERATURE) {
            // Error handling.
            if (this.hasError()) {
                this.noErrorChannel().setNextValue(true);
                this.logInfo(this.log, "Everything is fine now! Reading from the temperature sensor is "
                        + outsideTemperature / HeatingCurveRegulator.CELSIUS_TO_DECI_DEGREE_CONVERTER + "°C.");

            }
            return false;
        } else {
            // No data from the temperature sensor (null in channel). -> Error
            this.turnHeaterOff();
            this.noErrorChannel().setNextValue(false);
            this.logError(this.log, "Not getting any data from the outside temperature sensor " + this.outsideThermometer.getComponentId() + ".");
            return true;
        }
    }

    /**
     * Set timestamp so that part 1 executes again right away.
     */
    private void setTimeStampToExectuePartOne() {
        this.timestamp = LocalDateTime.now().minusMinutes(this.minimumStateTimeMinutes);
    }

    /**
     * Resets the current measurementData.
     */
    private void resetMeasurement() {
        this.measureAverage = true;
        this.timestamp = LocalDateTime.now();
        this.measurementCounter = 0;
        this.measurementData.clear();
    }

    /**
     * Set the {@link HeatingCurveRegulator.ChannelId#ACTIVATE_HEATER} to true.
     */
    private void turnHeaterOn() {
        this.signalTurnOnHeater().setNextValue(true);
    }


    /**
     * Set the {@link HeatingCurveRegulator.ChannelId#ACTIVATE_HEATER} to false.
     */

    private void turnHeaterOff() {
        this.signalTurnOnHeater().setNextValue(false);
    }


    /**
     * Sets the {@link HeatingCurveRegulator.ChannelId#HEATING_TEMPERATURE}.
     *
     * @param temperature the setPointTemperature
     */
    private void setHeatingTemperature(int temperature) {
        this.getHeatingTemperature().setNextValue(temperature);
    }

    @Override
    public String debugLog() {
        if (this.shouldBeHeating) {
            return this.id() + "HeatingCurve controller calculated temperature setPoint to "
                    + this.calculatedHeatingCurveTemperature / CELSIUS_TO_DECI_DEGREE_CONVERTER + Unit.DEGREE_CELSIUS.getSymbol();
        }
        return this.id() + " Not Heating";
    }
}
