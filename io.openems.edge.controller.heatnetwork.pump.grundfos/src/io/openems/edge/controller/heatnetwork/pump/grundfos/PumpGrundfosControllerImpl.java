package io.openems.edge.controller.heatnetwork.pump.grundfos;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
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
import java.util.Optional;


/**
 * A controller to operate a Grundfos pump via GENIbus in constant pressure mode or constant frequency mode.
 * A note on the channels: The [write] value of the channels is intended for REST/JSON writes. Doing so will copy the
 * written value into the config (so it is not reset when the software restarts) and restart the module.
 * If you want another module to send values to the pump, you can write into [next] of the channels. This will not
 * restart the module and will work just as well. Values written in that way are not saved in the config though.
 *
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Pump.Grundfos", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PumpGrundfosControllerImpl extends AbstractOpenemsComponent implements Controller, OpenemsComponent, PumpGrundfosController {

    private PumpGrundfos pumpChannels;
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
            this.pumpChannels = this.cpm.getComponent(config.pumpId());
        } else {
            throw new ConfigurationException("Pump not correct instance, check Id!", "Incorrect Id in Config");
        }

        this.controlModeSetting = config.controlMode();
        this.stopPump = config.stopPump();
        this.pressureSetpoint = config.pressureSetpoint();
        this.frequencySetpoint = config.frequencySetpoint();
        this.onlyRead = config.onlyRead();
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
                    this.pumpChannels.setMinMotorCurve().setNextWriteValue(true);
                    break;
                case MAX_MOTOR_CURVE:
                    this.pumpChannels.setMaxMotorCurve().setNextWriteValue(true);
                    break;
                case AUTO_ADAPT:
                    this.pumpChannels.setAutoAdapt().setNextWriteValue(true);
                    break;
                case CONST_FREQUENCY:
                    this.pumpChannels.setConstFrequency().setNextWriteValue(true);
                    // Set interval to maximum. Change this if more precision is needed. Fmin minimum is 52% for MAGNA3.
                    // You can set Fmin lower than that, but this will have no effect. Motor can't run slower than 52%.
                    this.pumpChannels.setFmin().setNextWriteValue(0.52);
                    this.pumpChannels.setFmax().setNextWriteValue(1.0);
                    break;
                case CONST_PRESSURE:
                    this.pumpChannels.setConstPressure().setNextWriteValue(true);
                    // Set interval to sensor interval. Change this if more precision is needed.
                    this.pumpChannels.setConstRefMinH().setNextWriteValue(0.0);
                    this.pumpChannels.setConstRefMaxH().setNextWriteValue(1.0);
                    break;
            }
        }
    }

    private void startStopPump() throws OpenemsError.OpenemsNamedException {
        if (this.stopPump) {
            this.pumpChannels.setStop().setNextWriteValue(true);
        } else {
            this.pumpChannels.setStart().setNextWriteValue(true);
        }
    }

    private boolean componentIsMissing() {
        try {
            if (this.pumpChannels.isEnabled() == false) {
                this.pumpChannels = this.cpm.getComponent(this.config.pumpId());
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
        Configuration c;

        try {
            c = this.ca.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();
            Optional t = this.setControlMode().getNextWriteValueAndReset();
            if (t.isPresent()) {
                properties.put("controlMode", t.get());
            }
            t = this.setStopPump().getNextWriteValueAndReset();
            if (t.isPresent()) {
                properties.put("stopPump", t.get());
            }
            t = this.setPressureSetpoint().getNextWriteValueAndReset();
            if (t.isPresent()) {
                properties.put("pressureSetpoint", t.get());
            }
            t = this.setFrequencySetpoint().getNextWriteValueAndReset();
            if (t.isPresent()) {
                properties.put("frequencySetpoint", t.get());
            }
            t = this.setOnlyRead().getNextWriteValueAndReset();
            if (t.isPresent()) {
                properties.put("onlyRead", t.get());
            }
            c.update(properties);

        } catch (IOException e) {
            this.log.warn("Couldn't update config: " + e);
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
            this.pumpChannels.setRemote().setNextWriteValue(true);

            boolean channelsHaveValues = setControlMode().value().isDefined()
                    && setStopPump().value().isDefined()
                    && setPressureSetpoint().value().isDefined()
                    && setFrequencySetpoint().value().isDefined()
                    && this.pumpChannels.isConnectionOk().value().isDefined();
            if (channelsHaveValues) {

                // Copy values from channels
                this.controlModeSetting = setControlMode().value().asEnum();
                this.stopPump = setStopPump().value().get();
                this.pressureSetpoint = setPressureSetpoint().value().get();
                this.frequencySetpoint = setFrequencySetpoint().value().get();

                // In case of a connection loss, all commands and configuration values need to be sent again.
                // Because connection loss can also mean pump was turned off and restarted, or it may even be a different
                // pump at this address.
                if (this.pumpChannels.isConnectionOk().value().get()) {

                    // Compare pump status with controller settings. Send commands if there is a difference.
                    if (this.stopPump) {
                        if (this.pumpChannels.getMotorFrequency().value().orElse(0.0) > 0) {
                            this.startStopPump();
                        }
                    } else {
                        if (this.pumpChannels.getMotorFrequency().value().orElse(0.0) <= 0) {
                            this.startStopPump();
                        }
                        switch (this.controlModeSetting) {
                            case CONST_PRESSURE:
                                if (this.pumpChannels.getActualControlMode().value().orElse("").equals("Constant pressure") == false) {
                                    this.changeControlMode();
                                }
                                break;
                            case CONST_FREQUENCY:
                                if (this.pumpChannels.getActualControlMode().value().orElse("").equals("Constant frequency") == false) {
                                    this.changeControlMode();
                                }
                                break;
                            case MIN_MOTOR_CURVE:
                                if (this.pumpChannels.getActualControlMode().value().orElse("").equals("Constant frequency - Min") == false) {
                                    this.changeControlMode();
                                }
                                break;
                            case MAX_MOTOR_CURVE:
                                if (this.pumpChannels.getActualControlMode().value().orElse("").equals("Constant frequency - Max") == false) {
                                    this.changeControlMode();
                                }
                                break;
                            case AUTO_ADAPT:
                                if (this.pumpChannels.getActualControlMode().value().orElse("").equals("AutoAdapt or FlowAdapt") == false) {
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
                                double minFrequencySetpoint = this.pumpChannels.setFmin().value().orElse(0.0);
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
                                this.pumpChannels.setRefRem().setNextWriteValue(this.setpoint);
                                break;
                            case CONST_PRESSURE:
                                Unit channelUnit = setPressureSetpoint().channelDoc().getUnit();
                                Unit sensorUnit = this.pumpChannels.getPumpDevice().getSensorUnit();
                                int scaleFactor = channelUnit.getScaleFactor() - sensorUnit.getScaleFactor();
                                double intervalHrange = this.pumpChannels.getPumpDevice().getPressureSensorRange() * Math.pow(10, -scaleFactor);
                                double intervalHmin = this.pumpChannels.getPumpDevice().getPressureSensorMin() * Math.pow(10, -scaleFactor);

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
                                    this.pumpChannels.setRefRem().setNextWriteValue(this.setpoint);
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
                    this.logWarn(this.log, "Warning: Pump " + this.pumpChannels.getPumpDevice().getPumpDeviceId()
                            + " at GENIbus address " + this.pumpChannels.getPumpDevice().getGenibusAddress() + " has no connection.");
                }
            }
        } else {
            boolean pumpOnline = this.pumpChannels.isConnectionOk().value().isDefined() && this.pumpChannels.isConnectionOk().value().get();
            if (pumpOnline) {
                if (this.verbose) {
                    this.channelOutput();
                }
            } else {
                this.logWarn(this.log, "Warning: Pump " + this.pumpChannels.getPumpDevice().getPumpDeviceId()
                        + " at GENIbus address " + this.pumpChannels.getPumpDevice().getGenibusAddress() + " has no connection.");
            }
        }

    }

    private void channelOutput() {
        double motorSpeedPercent = 0;
        boolean motorSpeedValueAvailable = false;
        if (this.pumpChannels.getMotorFrequency().value().isDefined() && this.pumpChannels.setFupper().value().isDefined()) {
            double maxFrequency = this.pumpChannels.setFupper().value().get();
            if (maxFrequency > 0) {
                motorSpeedPercent = 100 * this.pumpChannels.getMotorFrequency().value().get() / maxFrequency;
                motorSpeedValueAvailable = true;
            }
        }

        //this.logInfo(this.log, "--Status of pump " + pumpChannels.getPumpDevice().getPumpDeviceId() + "--");
        this.logInfo(this.log, "GENIbus address: " + this.pumpChannels.getPumpDevice().getGenibusAddress()
                + ", product number: " + this.pumpChannels.getProductNumber().value().get() + ", "
                + "serial number: " + this.pumpChannels.getSerialNumber().value().get());
        this.logInfo(this.log, "Twinpump Status: " + this.pumpChannels.getTwinpumpStatusString().value().get());
        this.logInfo(this.log, "Multipump Mode: " + this.pumpChannels.getTpModeString().value().get());
        this.logInfo(this.log, "Power consumption: " + this.formatter2.format(this.pumpChannels.getPowerConsumption().value().orElse(0.0)) + " W");

        if (motorSpeedValueAvailable) {
            this.logInfo(this.log, "Motor speed: " + this.formatter1.format(motorSpeedPercent) + " %.");
        } else {
            this.logInfo(this.log, "Motor speed: not available");
        }

        // Frequency values are probably not correct, so rather give speed %, which should be correct. Frequency values
        // are not correct because there seems to be an error involved with their unit. Wrong frequency readout was seen
        // on two pumps so far.
        /*
        //this.logInfo(this.log, "Motor frequency: " + formatter2.format(pumpChannels.getMotorFrequency().value().orElse(0.0)) + " Hz or "
                + formatter2.format(pumpChannels.getMotorFrequency().value().orElse(0.0) * 60) + " rpm");
        //this.logInfo(this.log, "Maximum motor frequency: " + formatter2.format(pumpChannels.setFupper().value().orElse(0.0)) + " Hz or "
                + formatter2.format(pumpChannels.setFupper().value().orElse(0.0) * 60) + " rpm");
        */

        this.logInfo(this.log, "Pump pressure: " + this.formatter2.format(this.pumpChannels.getCurrentPressure().value().orElse(0.0)) + " bar or "
                + this.formatter2.format(this.pumpChannels.getCurrentPressure().value().orElse(0.0) * 10) + " m");
        this.logInfo(this.log, "Pump max pressure: " + this.formatter2.format(this.pumpChannels.setMaxPressure().value().orElse(0.0)) + " bar or "
                + this.formatter2.format(this.pumpChannels.setMaxPressure().value().orElse(0.0) * 10) + " m");
        this.logInfo(this.log, "Pump flow: " + this.formatter2.format(this.pumpChannels.getCurrentPumpFlow().value().orElse(0.0)) + " m³/h");
        this.logInfo(this.log, "Pump flow max: " + this.formatter2.format(this.pumpChannels.setPumpMaxFlow().value().orElse(0.0)) + " m³/h");
        this.logInfo(this.log, "Pumped medium temperature: " + this.formatter1.format(this.pumpChannels.getPumpedWaterMediumTemperature().value().orElse(0.0) / 10) + "°C");
        this.logInfo(this.log, "Control mode: " + this.pumpChannels.getActualControlMode().value().get());
        //this.logInfo(this.log, pumpChannels.getControlSource().value().orElse("Command source:"));
        ////this.logInfo(this.log, "Buffer length: " + pumpChannels.getBufferLength().value().get());
        //this.logInfo(this.log, "AlarmCode: " + pumpChannels.getAlarmCode().value().get());
        //this.logInfo(this.log, "WarnCode: " + pumpChannels.getWarnCode().value().get());
        //this.logInfo(this.log, "Warn message: " + pumpChannels.getWarnMessage().value().get());
        //this.logInfo(this.log, "Alarm log1: " + pumpChannels.getAlarmLog1().value().get());
        //this.logInfo(this.log, "Alarm log2: " + pumpChannels.getAlarmLog2().value().get());
        //this.logInfo(this.log, "Alarm log3: " + pumpChannels.getAlarmLog3().value().get());
        //this.logInfo(this.log, "Alarm log4: " + pumpChannels.getAlarmLog4().value().get());
        //this.logInfo(this.log, "Alarm log5: " + pumpChannels.getAlarmLog5().value().get());
        switch (this.controlModeSetting) {
            case MIN_MOTOR_CURVE:
                //this.logInfo(this.log, "");
                //this.logInfo(this.log, "Controller setpoint: min motor curve = "
                //    + formatter2.format(pumpChannels.setFnom().value().orElse(0.0)
                //  * pumpChannels.setFmin().value().orElse(0.0)) + " Hz or "
                // + formatter2.format(pumpChannels.setFnom().value().orElse(0.0)
                //* pumpChannels.setFmin().value().orElse(0.0) * 60) + " rpm.");
                //this.logInfo(this.log, "");
                break;
            case MAX_MOTOR_CURVE:
                //this.logInfo(this.log, "");
                //this.logInfo(this.log, "Controller setpoint: max motor curve = "
                //+ formatter2.format(pumpChannels.setFnom().value().orElse(0.0)) + " Hz or "
                //   + formatter2.format(pumpChannels.setFnom().value().orElse(0.0) * 60) + " rpm.");
                //this.logInfo(this.log, "");
                break;
            case AUTO_ADAPT:
                //this.logInfo(this.log, "");
                //this.logInfo(this.log, "Controller setpoint: auto adapt");
                //this.logInfo(this.log, "");
                break;
            case CONST_FREQUENCY:
                //this.logInfo(this.log, "Actual setpoint (pump internal): " + formatter2.format(pumpChannels.getRefAct().value().orElse(0.0) * 100) + "% of interval range.");
                //this.logInfo(this.log, "Motor frequency setpoint maximum: " + formatter2.format(pumpChannels.setFnom().value().orElse(0.0)) + " Hz or "
                // + formatter2.format(pumpChannels.setFnom().value().orElse(0.0) * 60) + " rpm");
                //this.logInfo(this.log, "Minimum pump speed: " + formatter2.format(pumpChannels.getRmin().value().orElse(0.0) * 100) + "% of maximum.");
                //this.logInfo(this.log, "");
                //this.logInfo(this.log, "Controller setpoint: " + frequencySetpoint + "% of " + formatter2.format(pumpChannels.setFnom().value().orElse(0.0)) + " Hz.");
                //this.logInfo(this.log, "");
                break;
            case CONST_PRESSURE:
                //this.logInfo(this.log, "Actual setpoint (pump internal): " + formatter2.format(pumpChannels.getRefAct().value().orElse(0.0) * 100) + "% of interval range.");
                ////this.logInfo(this.log, "Interval min (internal): " + formatter2.format(pumpChannels.getRmin().value().orElse(0.0) * 100) + "% of maximum pumping head (Förderhöhe).");
                ////this.logInfo(this.log, "Interval max (internal): " + formatter2.format(pumpChannels.getRmax().value().orElse(0.0) * 100) + "% of maximum pumping head (Förderhöhe).");
                //this.logInfo(this.log, "Maximum pressure setpoint: " + (pumpChannels.getPumpDevice().getPressureSensorMinBar()
                // + pumpChannels.getPumpDevice().getPressureSensorRangeBar()) + " bar / "
                // + (pumpChannels.getPumpDevice().getPressureSensorMinBar() + pumpChannels.getPumpDevice().getPressureSensorRangeBar()) * 10 + " m.");
                //this.logInfo(this.log, "Minimum pressure setpoint: " + pumpChannels.getPumpDevice().getPressureSensorMinBar()
                // + " bar / " + pumpChannels.getPumpDevice().getPressureSensorMinBar() * 10 + " m.");
                //this.logInfo(this.log, "");
                //this.logInfo(this.log, "Controller setpoint: " + pressureSetpoint + " bar / " + pressureSetpoint * 10 + " m or "
                // + formatter2.format(pumpChannels.setRefRem().value().orElse(0.0) * 100) + "% of interval range.");
                //this.logInfo(this.log, "");
                break;
        }
        ////this.logInfo(this.log, "ActMode1Bits: " + pumpChannels.getActMode1Bits().value().get());
        ////this.logInfo(this.log, "ref_norm: " + pumpChannels.getRefNorm().value().get());


        /*
        //this.logInfo(this.log, "Sensor:");
        //this.logInfo(this.log, "ana_in_1_func: " + pumpChannels.setSensor1Func().value().get());
        //this.logInfo(this.log, "ana_in_1_applic: " + pumpChannels.setSensor1Applic().value().get());
        //this.logInfo(this.log, "ana_in_1_unit: " + pumpChannels.setSensor1Unit().value().get());
        //this.logInfo(this.log, "ana_in_1_min: " + pumpChannels.setSensor1Min().value().get());
        //this.logInfo(this.log, "ana_in_1_max: " + pumpChannels.setSensor1Max().value().get());



        //this.logInfo(this.log, "ref_norm: " + pumpChannels.getRefNorm().value().get());
        //this.logInfo(this.log, "Motor frequency: " + formatter2.format(pumpChannels.getMotorFrequency().value().orElse(0.0)) + " Hz or "
                + formatter2.format(pumpChannels.getMotorFrequency().value().orElse(0.0) * 60) + " rpm");
        //this.logInfo(this.log, "Maximum motor frequency: " + formatter2.format(pumpChannels.setFupper().value().orElse(0.0)) + " Hz or "
                + formatter2.format(pumpChannels.setFupper().value().orElse(0.0) * 60) + " rpm");
        //this.logInfo(this.log, "f_upper: " + pumpChannels.setFupper().value().get());
        //this.logInfo(this.log, "f_nom: " + pumpChannels.setFnom().value().get());
        //this.logInfo(this.log, "f_min: " + pumpChannels.setFmin().value().get());
        //this.logInfo(this.log, "f_max: " + pumpChannels.setFmax().value().get());
        //this.logInfo(this.log, "h_const_ref_min: " + pumpChannels.setConstRefMinH().value().get());
        //this.logInfo(this.log, "h_const_ref_max: " + pumpChannels.setConstRefMaxH().value().get());
        //this.logInfo(this.log, "h_range: " + pumpChannels.setHrange().value().get());
        //this.logInfo(this.log, "ref_rem: " + pumpChannels.setRefRem().value().get());
        ////this.logInfo(this.log, "ref_rem write: " + pumpChannels.setRefRem().getNextWriteValue().get());
        */


        // Motor übertakten.
        /*
        try {
            pumpChannels.setFupper().setNextWriteValue(48.0);   // Motor max Frequenz
            pumpChannels.setFnom().setNextWriteValue(48.0);
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }

         */


        // Just for testing
        /*
        if (testCounter == 10) {
            try {
                pumpChannels.setFnom().setNextWriteValue(40.0);
            } catch (OpenemsError.OpenemsNamedException e) {
                e.printStackTrace();
            }
        }

        if (testCounter == 20) {
            try {
                pumpChannels.setFnom().setNextWriteValue(48.0);
            } catch (OpenemsError.OpenemsNamedException e) {
                e.printStackTrace();
            }
        }

        testCounter++;
        */

    }
}
