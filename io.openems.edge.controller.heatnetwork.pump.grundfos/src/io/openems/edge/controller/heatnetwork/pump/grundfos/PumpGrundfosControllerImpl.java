package io.openems.edge.controller.heatnetwork.pump.grundfos;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.configupdate.ConfigurationUpdate;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.pump.grundfos.api.ControlModeSetting;
import io.openems.edge.controller.heatnetwork.pump.grundfos.api.PumpGrundfosController;
import io.openems.edge.pump.grundfos.api.PumpGrundfos;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * A controller to operate a Grundfos pump via GENIbus in constant pressure mode or constant frequency mode.
 * A note on the channels: The [write] value of the channels is intended for REST/JSON writes. Doing so will copy the
 * written value into the config (so it is not reset when the software restarts) and restart the module.
 * If you want another module to send values to the pump, you can write into [next] of the channels. This will not
 * restart the module and will work just as well. Values written in that way are not saved in the config though.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Pump.Grundfos", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PumpGrundfosControllerImpl extends AbstractOpenemsComponent implements Controller, OpenemsComponent, PumpGrundfosController {

    private PumpGrundfos pumpGrundfos;
    private Config config;

    @Reference
    ComponentManager cpm;

    private final Logger log = LoggerFactory.getLogger(PumpGrundfosControllerImpl.class);
    private final DecimalFormat formatter2 = new DecimalFormat("#0.00");
    private final DecimalFormat formatter1 = new DecimalFormat("#0.0");

    private double setpoint = 0;
    double pressureSetpoint;
    double frequencySetpoint;
    private ControlModeSetting controlModeSetting;
    private boolean stopPump;
    private boolean verbose;
    private boolean onlyRead;
    private int testCounter = 0;
    private boolean updateDefaultConfig;

    public PumpGrundfosControllerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                PumpGrundfosController.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());

        this.config = config;

        // Allocate components.
        if (this.cpm.getComponent(config.pumpId()) instanceof PumpGrundfos) {
            this.pumpGrundfos = this.cpm.getComponent(config.pumpId());
        } else {
            throw new ConfigurationException("Pump not correct instance, check Id!", "Incorrect Id in Config");
        }

        this.controlModeSetting = config.controlMode();
        this.stopPump = config.stopPump();
        this.pressureSetpoint = config.pressureSetpoint();
        this.frequencySetpoint = config.frequencySetpoint();
        this.onlyRead = config.onlyRead();
        this.updateDefaultConfig = config.updateDefaultConfig();
        try {
            // Fill all containers of the channels with values. This is needed since "run()" takes the "value" container
            // of the channels.
            setControlMode().setNextValue(this.controlModeSetting.getValue());
            setControlMode().nextProcessImage();
            setStopPump().setNextValue(this.stopPump);
            setStopPump().nextProcessImage();
            setPressureSetpoint().setNextValue(this.pressureSetpoint);
            setPressureSetpoint().nextProcessImage();
            setFrequencySetpoint().setNextValue(this.frequencySetpoint);
            setFrequencySetpoint().nextProcessImage();
            setOnlyRead().setNextValue(this.onlyRead);
            setOnlyRead().nextProcessImage();

            if (this.onlyRead == false) {
                this.changeControlMode();
                this.startStopPump();
            }

        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }

        this.verbose = config.printPumpStatus();

    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    private void changeControlMode() throws OpenemsError.OpenemsNamedException {
        if (this.stopPump == false) {
            switch (this.controlModeSetting) {
                case MIN_MOTOR_CURVE:
                    this.pumpGrundfos.setMinMotorCurve().setNextWriteValue(true);
                    break;
                case MAX_MOTOR_CURVE:
                    this.pumpGrundfos.setMaxMotorCurve().setNextWriteValue(true);
                    break;
                case AUTO_ADAPT:
                    this.pumpGrundfos.setAutoAdapt().setNextWriteValue(true);
                    break;
                case CONST_FREQUENCY:
                    this.pumpGrundfos.setConstFrequency().setNextWriteValue(true);
                    // Set interval to maximum. Change this if more precision is needed. Fmin minimum is 52% for MAGNA3.
                    // You can set Fmin lower than that, but this will have no effect. Motor can't run slower than 52%.
                    this.pumpGrundfos.getFminChannel().setNextWriteValue(0.52);
                    this.pumpGrundfos.getFmaxChannel().setNextWriteValue(1.0);
                    break;
                case CONST_PRESSURE:
                    this.pumpGrundfos.setConstPressure().setNextWriteValue(true);
                    // Set interval to sensor interval. Change this if more precision is needed.
                    this.pumpGrundfos.setConstRefMinH().setNextWriteValue(0.0);
                    this.pumpGrundfos.setConstRefMaxH().setNextWriteValue(1.0);
                    break;
            }
        }
    }

    private void startStopPump() throws OpenemsError.OpenemsNamedException {
        if (this.stopPump) {
            this.pumpGrundfos.setStop().setNextWriteValue(true);
        } else {
            this.pumpGrundfos.setStart().setNextWriteValue(true);
        }
    }

    private boolean componentIsMissing() {
        try {
            if (this.pumpGrundfos.isEnabled() == false) {
                this.pumpGrundfos = this.cpm.getComponent(this.config.pumpId());
            }
            return false;
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
            return true;
        }
    }

    @Reference
    ConfigurationAdmin ca;

    private void updateConfig() {
        Map<String, Object> valuesForConfig = new HashMap<>();
        this.checkForDifferencesAndAddToMap("stopPump", this.stopPump, this.setStopPump(), valuesForConfig);
        if (this.updateDefaultConfig) {
            this.checkForDifferencesAndAddToMap("controlMode", this.controlModeSetting.getValue(), this.setControlMode(), valuesForConfig);
            this.checkForDifferencesAndAddToMap("pressureSetpoint", this.pressureSetpoint, this.setPressureSetpoint(), valuesForConfig);
            this.checkForDifferencesAndAddToMap("frequencySetpoint", this.frequencySetpoint, this.setFrequencySetpoint(), valuesForConfig);
            this.checkForDifferencesAndAddToMap("onlyRead", this.onlyRead, this.setOnlyRead(), valuesForConfig);
        }

        if (!valuesForConfig.isEmpty()) {
            try {
                ConfigurationUpdate.updateConfig(this.ca, this.servicePid(), valuesForConfig);
            } catch (IOException e) {
                this.log.warn("Couldn't update Config in: " + this.id() + " Reason: " + e.getMessage());
            }
        }
    }

    /**
     * Gets the Commands usually from config; or REST/JSON Request and writes ReferenceValues in channels.
     */
    @Override
    public void run() throws OpenemsError.OpenemsNamedException {

        if (this.componentIsMissing()) {
            this.log.warn("Missing component in: " + super.id());
            return;
        }
        // Check if something was written by REST.
        boolean restchange = this.setControlMode().getNextWriteValue().isPresent();
        restchange |= this.setStopPump().getNextWriteValue().isPresent();
        restchange |= this.setPressureSetpoint().getNextWriteValue().isPresent();
        restchange |= this.setFrequencySetpoint().getNextWriteValue().isPresent();
        restchange |= this.setOnlyRead().getNextWriteValue().isPresent();
        if (restchange) {
            this.updateConfig();
        }


        if (this.onlyRead == false) {
            // Puts the pump in remote mode. Send every second.
            this.pumpGrundfos.setRemote().setNextWriteValue(true);

            boolean channelsHaveValues = setControlMode().value().isDefined()
                    && setStopPump().value().isDefined()
                    && setPressureSetpoint().value().isDefined()
                    && setFrequencySetpoint().value().isDefined()
                    && this.pumpGrundfos.isConnectionOk().value().isDefined();
            if (channelsHaveValues) {

                // Copy values from channels
                this.controlModeSetting = setControlMode().value().asEnum();
                this.stopPump = setStopPump().value().get();
                this.pressureSetpoint = setPressureSetpoint().value().get();
                this.frequencySetpoint = setFrequencySetpoint().value().get();

                // In case of a connection loss, all commands and configuration values need to be sent again.
                // Because connection loss can also mean pump was turned off and restarted, or it may even be a different
                // pump at this address.
                if (this.pumpGrundfos.isConnectionOk().value().get()) {

                    // Compare pump status with controller settings. Send commands if there is a difference.
                    if (this.stopPump) {
                        if (this.pumpGrundfos.getMotorFrequencyChannel().value().orElse(0.0) > 0) {
                            this.startStopPump();
                        }
                    } else {
                        if (this.pumpGrundfos.getMotorFrequencyChannel().value().orElse(0.0) <= 0) {
                            this.startStopPump();
                        }
                        switch (this.controlModeSetting) {
                            case CONST_PRESSURE:
                                if (this.pumpGrundfos.getControlModeStringChannel().value().orElse("").equals("Constant pressure") == false) {
                                    this.changeControlMode();
                                }
                                break;
                            case CONST_FREQUENCY:
                                if (this.pumpGrundfos.getControlModeStringChannel().value().orElse("").equals("Constant frequency") == false) {
                                    this.changeControlMode();
                                }
                                break;
                            case MIN_MOTOR_CURVE:
                                if (this.pumpGrundfos.getControlModeStringChannel().value().orElse("").equals("Constant frequency - Min") == false) {
                                    this.changeControlMode();
                                }
                                break;
                            case MAX_MOTOR_CURVE:
                                if (this.pumpGrundfos.getControlModeStringChannel().value().orElse("").equals("Constant frequency - Max") == false) {
                                    this.changeControlMode();
                                }
                                break;
                            case AUTO_ADAPT:
                                if (this.pumpGrundfos.getControlModeStringChannel().value().orElse("").equals("AutoAdapt or FlowAdapt") == false) {
                                    this.changeControlMode();
                                }
                                break;
                        }

                        // Send setpoint to pump, depending on control mode. Do this every cycle.
                        switch (this.controlModeSetting) {
                            case MIN_MOTOR_CURVE:
                            case MAX_MOTOR_CURVE:
                            case AUTO_ADAPT:
                                break;
                            case CONST_FREQUENCY:
                                double minFrequencySetpoint = this.pumpGrundfos.getFminChannel().value().orElse(0.0);
                                ;
                                if (this.frequencySetpoint < minFrequencySetpoint) {
                                    this.frequencySetpoint = minFrequencySetpoint;
                                    setFrequencySetpoint().setNextWriteValue(this.frequencySetpoint);  // Update both containers to have correct values next cycle.
                                    setFrequencySetpoint().setNextValue(this.frequencySetpoint);
                                }
                                if (this.frequencySetpoint > 100) {
                                    this.frequencySetpoint = 100;
                                    setFrequencySetpoint().setNextWriteValue(100.0);
                                    setFrequencySetpoint().setNextValue(100.0);
                                }
                                this.setpoint = this.frequencySetpoint;
                                this.pumpGrundfos.setRefRem().setNextWriteValue(this.setpoint);
                                break;
                            case CONST_PRESSURE:
                                Unit channelUnit = setPressureSetpoint().channelDoc().getUnit();
                                Unit sensorUnit = this.pumpGrundfos.getPumpDevice().getSensorUnit();
                                int scaleFactor = channelUnit.getScaleFactor() - sensorUnit.getScaleFactor();
                                double intervalHrange = this.pumpGrundfos.getPumpDevice().getPressureSensorRange() * Math.pow(10, -scaleFactor);
                                double intervalHmin = this.pumpGrundfos.getPumpDevice().getPressureSensorMin() * Math.pow(10, -scaleFactor);

                                // Test if INFO of pressure sensor is available. If yes, range is not 0.
                                if (intervalHrange > 0) {
                                    if (this.pressureSetpoint > intervalHrange + intervalHmin) {
                                        this.logWarn(this.log, "Value for pressure setpoint = " + this.pressureSetpoint + " bar is above the interval range. "
                                                + "Resetting to maximum valid value " + intervalHrange + intervalHmin + " bar.");
                                        this.pressureSetpoint = intervalHrange + intervalHmin;
                                        setPressureSetpoint().setNextWriteValue(this.pressureSetpoint);
                                        setPressureSetpoint().setNextValue(this.pressureSetpoint);   // Need to set this, otherwise warn message is displayed twice.
                                    }
                                    if (this.pressureSetpoint < intervalHmin) {
                                        this.logWarn(this.log, "Value for pressure setpoint = " + this.pressureSetpoint + " bar is below the interval range. "
                                                + "Resetting to minimum valid value " + intervalHmin + " bar.");
                                        this.pressureSetpoint = intervalHmin;
                                        setPressureSetpoint().setNextWriteValue(this.pressureSetpoint);
                                        setPressureSetpoint().setNextValue(this.pressureSetpoint);
                                    }

                                    // Don't need to convert to 0-254. The GENIbus bridge does that.
                                    // ref_rem is a percentage value and you write the percentage in the channel. To send 100%, write 100
                                    // to the channel.
                                    this.setpoint = 100 * (this.pressureSetpoint - intervalHmin) / intervalHrange;
                                    this.pumpGrundfos.setRefRem().setNextWriteValue(this.setpoint);
                                } else {
                                    this.logWarn(this.log, "Can't send pressure setpoint to pump. INFO of pressure "
                                            + "sensor not yet available, but needed to calculate setpoint.");
                                }
                                break;
                        }
                    }

                    if (this.verbose) {
                        this.channelOutput();
                    }
                } else {
                    this.logWarn(this.log, "Warning: Pump " + this.pumpGrundfos.getPumpDevice().getPumpDeviceId()
                            + " at GENIbus address " + this.pumpGrundfos.getPumpDevice().getGenibusAddress() + " has no connection.");
                }
            }
        } else {
            boolean pumpOnline = this.pumpGrundfos.isConnectionOk().value().isDefined() && this.pumpGrundfos.isConnectionOk().value().get();
            if (pumpOnline) {
                if (this.verbose) {
                    this.channelOutput();
                }
            } else {
                this.logWarn(this.log, "Warning: Pump " + this.pumpGrundfos.getPumpDevice().getPumpDeviceId()
                        + " at GENIbus address " + this.pumpGrundfos.getPumpDevice().getGenibusAddress() + " has no connection.");
            }
        }

    }

    private boolean checkForDifferencesAndAddToMap(String key, Double defaultValue, WriteChannel<Double> channel, Map<String, Object> valuesForConfig) {
        Optional<?> optionalValue = channel.getNextWriteValueAndReset();
        if (optionalValue.isPresent()) {
            if (!optionalValue.get().equals(defaultValue)) {
                valuesForConfig.put(key, optionalValue.get());
                return true;
            }
        }
        return false;
    }

    private boolean checkForDifferencesAndAddToMap(String key, Integer defaultValue, WriteChannel<Integer> channel, Map<String, Object> valuesForConfig) {
        Optional<?> optionalValue = channel.getNextWriteValueAndReset();
        if (optionalValue.isPresent()) {
            if (!optionalValue.get().equals(defaultValue)) {
                valuesForConfig.put(key, optionalValue.get());
                return true;
            }
        }
        return false;
    }

    private boolean checkForDifferencesAndAddToMap(String key, Boolean defaultValue, WriteChannel<Boolean> channel, Map<String, Object> valuesForConfig) {
        Optional<Boolean> optionalValue = channel.getNextWriteValueAndReset();
        if (optionalValue.isPresent()) {
            if (!optionalValue.get().equals(defaultValue)) {
                valuesForConfig.put(key, optionalValue.get());
                return true;
            }
        }
        return false;
    }

    private void channelOutput() {
        double motorSpeedPercent = 0;
        boolean motorSpeedValueAvailable = false;
        if (this.pumpGrundfos.getMotorFrequencyChannel().value().isDefined() && this.pumpGrundfos.getFupperChannel().value().isDefined()) {
            double maxFrequency = this.pumpGrundfos.getFupperChannel().value().get();
            if (maxFrequency > 0) {
                motorSpeedPercent = 100 * this.pumpGrundfos.getMotorFrequencyChannel().value().get() / maxFrequency;
                motorSpeedValueAvailable = true;
            }
        }

        //this.logInfo(this.log, "--Status of pump " + pumpChannels.getPumpDevice().getPumpDeviceId() + "--");
        this.logInfo(this.log, "GENIbus address: " + this.pumpGrundfos.getPumpDevice().getGenibusAddress()
                + ", product number: " + this.pumpGrundfos.getProductNumber().value().get() + ", "
                + "serial number: " + this.pumpGrundfos.getSerialNumber().value().get());
        this.logInfo(this.log, "Twinpump Status: " + this.pumpGrundfos.getTwinpumpStatusStringChannel().value().get());
        this.logInfo(this.log, "Multipump Mode: " + this.pumpGrundfos.getTpModeString().value().get());
        this.logInfo(this.log, "Power consumption: " + this.formatter2.format(this.pumpGrundfos.getPowerConsumptionChannel().value().orElse(0.0)) + " W");

        if (motorSpeedValueAvailable) {
            this.logInfo(this.log, "Motor speed: " + this.formatter1.format(motorSpeedPercent) + " %.");
        } else {
            this.logInfo(this.log, "Motor speed: not available");
        }

        this.logInfo(this.log, "Pump pressure: " + this.formatter2.format(this.pumpGrundfos.getPressureChannel().value().orElse(0.0)) + " bar or "
                + this.formatter2.format(this.pumpGrundfos.getPressureChannel().value().orElse(0.0) * 10) + " m");
        this.logInfo(this.log, "Pump max pressure: " + this.formatter2.format(this.pumpGrundfos.setMaxPressure().value().orElse(0.0)) + " bar or "
                + this.formatter2.format(this.pumpGrundfos.setMaxPressure().value().orElse(0.0) * 10) + " m");
        this.logInfo(this.log, "Pump flow: " + this.formatter2.format(this.pumpGrundfos.getPercolationChannel().value().orElse(0.0)) + " m³/h");
        this.logInfo(this.log, "Pump flow max: " + this.formatter2.format(this.pumpGrundfos.getPumpMaxFlowChannel().value().orElse(0.0)) + " m³/h");
        this.logInfo(this.log, "Pumped medium temperature: " + this.formatter1.format(this.pumpGrundfos.getPumpedFluidTemperatureChannel().value().orElse(0.0) / 10) + "°C");
        this.logInfo(this.log, "Control mode: " + this.pumpGrundfos.getControlModeStringChannel().value().get());
        }

}
