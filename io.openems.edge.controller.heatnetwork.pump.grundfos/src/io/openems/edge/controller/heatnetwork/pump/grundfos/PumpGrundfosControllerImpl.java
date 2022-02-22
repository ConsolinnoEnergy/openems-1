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
import io.openems.edge.pump.grundfos.api.PumpMode;
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
 * A controller to operate a Grundfos pump via GENIbus.
 * Changing the values of the write channels by using the setters or REST/JSON will save the new values to the appropriate
 * config value. Updating the config will trigger a restart of the module.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Pump.Grundfos", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PumpGrundfosControllerImpl extends AbstractOpenemsComponent implements Controller, OpenemsComponent, PumpGrundfosController {

    @Reference
    ComponentManager cpm;

    @Reference
    ConfigurationAdmin ca;

    private PumpGrundfos pumpChannels;
    private Config config;

    private final Logger log = LoggerFactory.getLogger(PumpGrundfosControllerImpl.class);
    private final DecimalFormat formatter2 = new DecimalFormat("#0.00");
    private final DecimalFormat formatter1 = new DecimalFormat("#0.0");

    /* The pump has a setting for the min motor speed. To prevent a wonky setting disrupting pump operation, write this
       value to the pump to ensure the range is what the code expects. You can write a value that is lower than the
       actual minimum, but sending set points to the pump below it's actual minimum will just result in the pump running
       at it's actual minimum speed.
       Value 52% is from a Magna3, could be that other pumps have a different minimum speed. */
    private static final double MIN_MOTOR_SPEED = 52.0;

    private double pressureSetpointToRefRem = 0;
    double pressureSetpoint;    // Unit of this variable is assumed to be that of channel PRESSURE_SETPOINT.
    double motorSpeedSetpoint;  // Unit is %. So 60 means 60%.
    private ControlModeSetting controlModeSetting;
    private boolean stopPump;
    private boolean printInfoToLog;
    private boolean onlyRead;
    private boolean pressureSetpointCalculationDone;
    private boolean motorSpeedClampDone;

    public PumpGrundfosControllerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                PumpGrundfosController.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());

        this.config = config;

        // Allocate components.
        if (this.cpm.getComponent(config.pumpId()) instanceof PumpGrundfos) {
            this.pumpChannels = this.cpm.getComponent(config.pumpId());
        } else {
            throw new ConfigurationException("Pump not correct instance, check Id!", "Incorrect Id in Config");
        }

        this.controlModeSetting = config.controlMode();
        if (this.controlModeSetting == ControlModeSetting.UNDEFINED) {
            this.controlModeSetting = ControlModeSetting.AUTO_ADAPT;
            this.updateConfig();
        }
        this.stopPump = config.stopPump();
        this.pressureSetpoint = config.pressureSetpoint();
        this.pressureSetpointCalculationDone = false;
        this.motorSpeedSetpoint = config.motorSpeedSetpoint();
        this.motorSpeedClampDone = false;
        this.onlyRead = config.onlyRead();

        // Set channel values.
        this._setControlMode(this.controlModeSetting);
        this._setStopPump(this.stopPump);
        this._setReadOnlySetting(this.onlyRead);

        this.printInfoToLog = config.printPumpStatus();
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Checks the ’controlModeSetting’ variable and then sends the required commands to set the pump to this control mode.
     *
     * @throws OpenemsError.OpenemsNamedException on error
     */
    private void changeControlMode() throws OpenemsError.OpenemsNamedException {
        if (this.stopPump == false) {
            switch (this.controlModeSetting) {
                case MIN_MOTOR_CURVE:
                    this.pumpChannels.setMinMotorCurve(true);
                    break;
                case MAX_MOTOR_CURVE:
                    this.pumpChannels.setMaxMotorCurve(true);
                    break;
                case AUTO_ADAPT:
                    this.pumpChannels.setAutoAdapt(true);
                    break;
                case CONST_FREQUENCY:
                    this.pumpChannels.setConstFrequency(true);
                    // Set interval to maximum. Change this if more precision is needed.
                    this.pumpChannels.setFmin(MIN_MOTOR_SPEED);
                    this.pumpChannels.setFmax(100.0);
                    break;
                case CONST_PRESSURE:
                    this.pumpChannels.setConstPressure(true);
                    // Set interval to maximum. Change this if more precision is needed.
                    this.pumpChannels.setHconstRefMin(0.0);
                    this.pumpChannels.setHconstRefMax(100.0);
                    break;
            }
        }
    }

    /**
     * Check if the configured pump module is enabled. If not, tries to reallocate the component.
     *
     * @return true if not found. False otherwise.
     */
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

    /**
     * Updates config values to current values.
     */
    private void updateConfig() {
        Configuration c;
        try {
            c = this.ca.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();
            properties.put("controlMode", this.controlModeSetting);
            properties.put("stopPump", this.stopPump);
            properties.put("pressureSetpoint", this.pressureSetpoint);
            properties.put("motorSpeedSetpoint", this.motorSpeedSetpoint);
            properties.put("onlyRead", this.onlyRead);
            c.update(properties);
        } catch (IOException e) {
            this.log.warn("Couldn't save new settings to config. " + e.getMessage());
        }
    }

    /**
     * React to writes in the channels and set the pump mode and set point.
     */
    @Override
    public void run() throws OpenemsError.OpenemsNamedException {

        if (this.componentIsMissing()) {
            this.log.warn("Missing component in: " + super.id());
            return;
        }

        boolean pumpOnline = this.pumpChannels.getConnectionOk().isDefined() && this.pumpChannels.getConnectionOk().get();
        if (pumpOnline) {

            if (this.onlyRead == false) {

                /* Collect write values. Compare with value in config and update if it is different. Updating the config
                   restarts the module, channels are filled with new values in activate(). Call update() when all
                   nextWrites have been collected, as module restart also means channels are initialized again, deleting
                   all values. */
                boolean updateConfig = false;
                Optional<Integer> controlModeOptional = this.getControlModeChannel().getNextWriteValueAndReset();
                if (controlModeOptional.isPresent()) {
                    int enumAsInt = controlModeOptional.get();
                    // Restrict to valid write values
                    if (enumAsInt >= 0 && enumAsInt <= 4) {
                        this.controlModeSetting = ControlModeSetting.valueOf(enumAsInt);
                        if (this.controlModeSetting != this.config.controlMode()) {
                            updateConfig = true;
                        }
                    }
                }
                Optional<Boolean> stopPumpOptional = this.getStopPumpChannel().getNextWriteValueAndReset();
                if (stopPumpOptional.isPresent()) {
                    this.stopPump = stopPumpOptional.get();
                    if (this.stopPump != this.config.stopPump()) {
                        updateConfig = true;
                    }
                }
                Optional<Double> motorSpeedSetpointOptional = this.getMotorSpeedSetpointChannel().getNextWriteValueAndReset();
                if (motorSpeedSetpointOptional.isPresent()) {
                    this.motorSpeedSetpoint = motorSpeedSetpointOptional.get();
                    this.motorSpeedClampDone = false;
                }
                if (this.motorSpeedClampDone == false) {
                    this.motorSpeedClampDone = true;
                    this.motorSpeedSetpoint = Math.max(this.motorSpeedSetpoint, MIN_MOTOR_SPEED);
                    this.motorSpeedSetpoint = Math.min(this.motorSpeedSetpoint, 100.0);
                    this._setMotorSpeedSetpoint(this.motorSpeedSetpoint);

                    /* Compare new set point with config, to see if config value is different and needs to be updated.
                       Set point is a double, so for comparison it should be rounded. Multiply by 1000 before rounding
                       to keep three digits after the decimal point. Then round to a long for comparison. */
                    long newSetpoint = Math.round(this.motorSpeedSetpoint * 1000);
                    long oldSetpoint = Math.round(this.config.motorSpeedSetpoint() * 1000);
                    if (newSetpoint != oldSetpoint) {
                        updateConfig = true;
                    }
                }
                Optional<Boolean> readOnlyOptional = this.getReadOnlySettingChannel().getNextWriteValueAndReset();
                if (readOnlyOptional.isPresent()) {
                    this.onlyRead = readOnlyOptional.get();
                    if (this.onlyRead != this.config.onlyRead()) {
                        updateConfig = true;
                    }
                }
                Optional<Double> pressureSetpointOptional = this.getPressureSetpointChannel().getNextWriteValueAndReset();
                if (pressureSetpointOptional.isPresent()) {
                    this.pressureSetpoint = pressureSetpointOptional.get();
                    this.pressureSetpointCalculationDone = false;
                }

                /* The pressure set point cannot simply be sent to Genibus. The set point value that is sent to Genibus
                   is ref_rem, which is a percentage value [0;100]. Depending on the operating mode, ref_rem is mapped to
                   different things. In constant pressure mode, the ref_rem is mapped to the pressure sensor range.
                   As an example, if that pressure sensor range is [0;2] bar, the mapping would be:
                   - ref_rem = 0 is 0 bar
                   - ref_rem = 50 is 1 bar
                   - ref_rem = 100 is 2 bar
                   To convert the pressure set point from a pressure unit to a ref_rem value, you first need to read the
                   pressure sensor range from the pump. */
                if (this.pressureSetpointCalculationDone == false) {
                    Unit channelUnit = getPressureSetpointChannel().channelDoc().getUnit(); // Unit of pressureSetpoint is this unit.
                    Unit sensorUnit = this.pumpChannels.getPumpDevice().getSensorUnit();
                    int scaleFactor = channelUnit.getScaleFactor() - sensorUnit.getScaleFactor();

                    // Calculate range in unit of pressureSetpoint. Range is [intervalHmin;intervalHmin+intervalHrange].
                    double intervalHrange = this.pumpChannels.getPumpDevice().getPressureSensorRange() * Math.pow(10, -scaleFactor);
                    double intervalHmin = this.pumpChannels.getPumpDevice().getPressureSensorMin() * Math.pow(10, -scaleFactor);

                    // Test if sensor range was received from the pump. If yes, range is bigger than 0.
                    if (intervalHrange > 0) {
                        this.pressureSetpointCalculationDone = true;

                        if (this.pressureSetpoint > intervalHrange + intervalHmin) {
                            this.logWarn(this.log, "Value for pressure setpoint = " + this.pressureSetpoint + " bar is above the interval range. "
                                    + "Resetting to maximum valid value " + intervalHrange + intervalHmin + " bar.");
                            this.pressureSetpoint = intervalHrange + intervalHmin;
                        }
                        if (this.pressureSetpoint < intervalHmin) {
                            this.logWarn(this.log, "Value for pressure setpoint = " + this.pressureSetpoint + " bar is below the interval range. "
                                    + "Resetting to minimum valid value " + intervalHmin + " bar.");
                            this.pressureSetpoint = intervalHmin;
                        }
                        this._setPressureSetpoint(this.pressureSetpoint);

                        // ref_rem is a percentage value. To send 100%, write 100 to the channel.
                        this.pressureSetpointToRefRem = 100 * (this.pressureSetpoint - intervalHmin) / intervalHrange;

                        /* Compare new set point with config, to see if config value is different and needs to be
                           updated. Set point is a double, so for comparison it should be rounded. Multiply by 1000
                           before rounding to keep three digits after the decimal point. Then round to a long for
                           comparison. */
                        long newSetpoint = Math.round(this.pressureSetpoint * 1000);
                        long oldSetpoint = Math.round(this.config.pressureSetpoint() * 1000);
                        if (newSetpoint != oldSetpoint) {
                            updateConfig = true;
                        }
                    }
                }

                // Puts the pump in remote mode. Send every second.
                this.pumpChannels.setRemote(true);

                // Compare pump status with controller settings. Send commands if there is a difference.
                if (this.stopPump) {
                    if (this.pumpChannels.getMotorFrequency().orElse(0.0) > 0) {
                        this.pumpChannels.setStop(true);    // Stop pump.
                    }
                } else {
                    if (this.pumpChannels.getMotorFrequencyChannel().value().orElse(0.0) <= 0) {
                        this.pumpChannels.setStart(true);   // Start pump.
                    }
                    switch (this.controlModeSetting) {
                        case MIN_MOTOR_CURVE:
                            if (this.pumpChannels.getControlModeString().orElse("").equals(PumpMode.CONST_FREQU_MIN.getName()) == false) {
                                this.changeControlMode();
                            }
                            break;
                        case MAX_MOTOR_CURVE:
                            if (this.pumpChannels.getControlModeString().orElse("").equals(PumpMode.CONST_FREQU_MAX.getName()) == false) {
                                this.changeControlMode();
                            }
                            break;
                        case AUTO_ADAPT:
                            if (this.pumpChannels.getControlModeString().orElse("").equals(PumpMode.AUTO_ADAPT_OR_FLOW_ADAPT.getName()) == false
                                    && this.pumpChannels.getControlModeString().orElse("").equals(PumpMode.AUTO_ADAPT.getName()) == false) {
                                this.changeControlMode();
                            }
                            break;
                        case CONST_FREQUENCY:
                            if (this.pumpChannels.getControlModeString().orElse("").equals(PumpMode.CONST_FREQU.getName()) == false) {
                                this.changeControlMode();
                            }
                            this.pumpChannels.setRefRem(this.motorSpeedSetpoint);
                            break;
                        case CONST_PRESSURE:
                            if (this.pumpChannels.getControlModeString().orElse("").equals(PumpMode.CONST_PRESSURE.getName()) == false) {
                                this.changeControlMode();
                            }
                            if (this.pressureSetpointCalculationDone) {
                                this.pumpChannels.setRefRem(this.pressureSetpointToRefRem);
                            } else {
                                this.logWarn(this.log, "Can't send pressure set point to pump. INFO of pressure "
                                        + "sensor not yet available, but needed to calculate set point.");
                            }
                            break;
                    }
                }
                if (updateConfig) {
                    this.updateConfig();
                }
            }
            if (this.printInfoToLog) {
                this.printInfo();
            }
        } else {
            this.logWarn(this.log, "Warning: Pump " + this.pumpChannels.getPumpDevice().getPumpDeviceId()
                    + " at GENIbus address " + this.pumpChannels.getPumpDevice().getGenibusAddress() + " has no connection.");
        }
    }

    /**
     * Information that is printed to the log if ’Write pump status to log’ option is enabled.
     * The method uses formatting because most genibus channels are of type double.
     */
    private void printInfo() {
        this.logInfo(this.log, "--Status of pump " + this.pumpChannels.getPumpDevice().getPumpDeviceId() + "--");
        this.logInfo(this.log, "GENIbus address: " + this.pumpChannels.getPumpDevice().getGenibusAddress()
                + ", product number: " + this.pumpChannels.getProductNumber().get() + ", "
                + "serial number: " + this.pumpChannels.getSerialNumber().get());
        this.logInfo(this.log, "Power consumption: "
                + this.formatter2.format(this.pumpChannels.getPowerConsumption().orElse(0.0))
                + " " + this.pumpChannels.getPowerConsumptionChannel().channelDoc().getUnit().toString());
        if (this.pumpChannels.getMotorFrequency().isDefined() && this.pumpChannels.getFupper().isDefined()) {
            double maxFrequency = this.pumpChannels.getFupper().get();
            if (maxFrequency > 0) {
                double motorSpeedPercent = 100 * this.pumpChannels.getMotorFrequency().get() / maxFrequency;
                this.logInfo(this.log, "Motor speed: " + this.formatter1.format(motorSpeedPercent) + "%");
            }
        } else {
            this.logInfo(this.log, "Motor speed: not available");
        }
        this.logInfo(this.log, "Pump pressure: "
                + this.formatter2.format(this.pumpChannels.getPressure().orElse(0.0))
                + " " + this.pumpChannels.getPressureChannel().channelDoc().getUnit().toString());
        this.logInfo(this.log, "Pump max pressure: "
                + this.formatter2.format(this.pumpChannels.getMaxPressure().orElse(0.0))
                + " " + this.pumpChannels.getMaxPressureChannel().channelDoc().getUnit().toString());
        this.logInfo(this.log, "Pump flow: "
                + this.formatter2.format(this.pumpChannels.getPercolation().orElse(0.0))
                + " " + this.pumpChannels.getPercolationChannel().channelDoc().getUnit().toString());
        this.logInfo(this.log, "Pump flow max: "
                + this.formatter2.format(this.pumpChannels.getPumpMaxFlow().orElse(0.0))
                + " " + this.pumpChannels.getPumpMaxFlowChannel().channelDoc().getUnit().toString());
        this.logInfo(this.log, "Pumped medium temperature: "
                + this.formatter1.format(this.pumpChannels.getPumpedFluidTemperature().orElse(0.0))
                + " " + this.pumpChannels.getPumpedFluidTemperatureChannel().channelDoc().getUnit().toString());
        this.logInfo(this.log, "Control mode: " + this.pumpChannels.getControlModeString().get());
        switch (this.controlModeSetting) {
            case CONST_FREQUENCY:
                this.logInfo(this.log, "Pump speed set point: " + this.formatter2.format(this.motorSpeedSetpoint) + "%");
                this.logInfo(this.log, "Minimum pump speed: "
                        + this.formatter2.format(this.pumpChannels.getRmin().orElse(0.0)) + "%");
                break;
            case CONST_PRESSURE:
                this.logInfo(this.log, "Pressure set point: " + this.pressureSetpoint
                        + " " + this.getPressureSetpointChannel().channelDoc().getUnit().toString());
                break;
        }
        this.logInfo(this.log, "Warn message: " + this.pumpChannels.getWarnMessage().get());
    }
}
