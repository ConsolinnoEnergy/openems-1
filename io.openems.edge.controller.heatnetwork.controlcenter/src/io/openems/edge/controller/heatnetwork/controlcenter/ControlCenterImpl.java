package io.openems.edge.controller.heatnetwork.controlcenter;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.controlcenter.api.ControlCenter;
import io.openems.edge.controller.heatnetwork.heatingcurveregulator.api.HeatingCurveRegulatorChannel;
import io.openems.edge.controller.heatnetwork.warmup.api.ControllerWarmup;
import io.openems.edge.controller.hydrauliccomponent.api.PidHydraulicController;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.io.api.Relay;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the Consolinno Control Center module. It manages the hierarchy of heating controllers.
 * - The output of a heating controller is a temperature and a boolean to signal if the controller wants to heat or not.
 * - This controller polls three heating controllers by hierarchy and passes on the result (heating or not heating plus
 * the temperature) to the next module(s).
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "ControlCenter", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ControlCenterImpl extends AbstractOpenemsComponent implements OpenemsComponent, ControlCenter, Controller {

    private final Logger log = LoggerFactory.getLogger(ControlCenterImpl.class);

    @Reference
    protected ComponentManager cpm;

    private PidHydraulicController pidControllerChannel;
    private ControllerWarmup warmupControllerChannel;
    private HeatingCurveRegulatorChannel heatingCurveRegulatorChannel;
    private HydraulicComponent hydraulicComponent;

    // Variables for channel readout
    private boolean activateTemperatureOverride;
    private int overrideTemperature;
    private boolean warmupControllerIsOn;
    private boolean warmupControllerNoError;
    private int warmupControllerTemperature;
    private boolean heatingCurveRegulatorAskingForHeating;
    private boolean heatingCurveRegulatorNoError;
    private int heatingCurveRegulatorTemperature;

    private Config config;


    public ControlCenterImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ControlCenter.ChannelId.values(),
                Controller.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.config = config;
        //allocate components
        this.allocateComponents();
        this.activateHeater().setNextValue(false);
        // This allows to start the warmupController from this module.
        if (config.run_warmup_program()) {
            this.warmupControllerChannel.playPauseWarmupController().setNextWriteValue(true);
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        if (this.componentIsMissing()) {
            this.log.warn("A Component is missing in: " + super.id());
            return;
        }
        // For the Overrides, copy values from the WriteValue to the NextValue fields.
        if (this.activateTemperatureOverride().getNextWriteValue().isPresent()) {
            this.activateTemperatureOverride().setNextValue(this.activateTemperatureOverride().getNextWriteValue().get());
        }
        if (this.setOverrideTemperature().getNextWriteValue().isPresent()) {
            this.setOverrideTemperature().setNextValue(this.setOverrideTemperature().getNextWriteValue().get());
        }

        // Check all channels if they have values in them.
        boolean overrideChannelHasValues = this.activateTemperatureOverride().value().isDefined()
                && this.setOverrideTemperature().value().isDefined();

        boolean warmupControllerChannelHasValues = this.config.run_warmup_program() && this.warmupControllerChannel.playPauseWarmupController().value().isDefined()
                && this.warmupControllerChannel.getWarmupTemperature().value().isDefined()
                && this.warmupControllerChannel.noError().value().isDefined();

        boolean heatingCurveRegulatorChannelHasValues = this.heatingCurveRegulatorChannel.signalTurnOnHeater().value().isDefined()
                && this.heatingCurveRegulatorChannel.getHeatingTemperature().value().isDefined()
                && this.heatingCurveRegulatorChannel.noError().value().isDefined();

        // Transfer channel data to local variables for better readability of logic code.
        if (overrideChannelHasValues) {
            this.activateTemperatureOverride = this.activateTemperatureOverride().value().get();
            this.overrideTemperature = this.setOverrideTemperature().value().get();
        }
        if (warmupControllerChannelHasValues) {
            this.warmupControllerIsOn = this.warmupControllerChannel.playPauseWarmupController().value().get();
            this.warmupControllerNoError = this.warmupControllerChannel.noError().value().get();
            this.warmupControllerTemperature = this.warmupControllerChannel.getWarmupTemperature().value().get();
        }
        if (heatingCurveRegulatorChannelHasValues) {
            // The HeatingCurveRegulator is asking for heating based on outside temperature. Heating in winter, no
            // heating in summer.
            this.heatingCurveRegulatorAskingForHeating = this.heatingCurveRegulatorChannel.signalTurnOnHeater().value().get();
            this.heatingCurveRegulatorNoError = this.heatingCurveRegulatorChannel.noError().value().get();
            this.heatingCurveRegulatorTemperature = this.heatingCurveRegulatorChannel.getHeatingTemperature().value().get();
        }

        // Control logic. Execute controllers by priority. From high to low: override, warmup, heatingCurve
        if (overrideChannelHasValues && this.activateTemperatureOverride) {
            this.turnOnHeater(this.overrideTemperature);
        } else if (warmupControllerChannelHasValues && this.warmupControllerIsOn && this.warmupControllerNoError) {
            this.turnOnHeater(this.warmupControllerTemperature);
        } else if (heatingCurveRegulatorChannelHasValues && this.heatingCurveRegulatorAskingForHeating
                && this.heatingCurveRegulatorNoError) {
            this.turnOnHeater(this.heatingCurveRegulatorTemperature);
        } else {
            this.turnOffHeater();
        }
    }

    /**
     * Because Components can be updated, get new Instances of Components.
     *
     * @return true if every component could be reached etc.
     */
    private boolean componentIsMissing() {
        try {
            OpenemsComponent component = this.cpm.getComponent(this.config.allocated_Pid_Controller());
            if (this.pidControllerChannel != component && component instanceof PidHydraulicController) {
                this.pidControllerChannel = (PidHydraulicController) component;
            }
            if (this.config.run_warmup_program()) {
                component = this.cpm.getComponent(this.config.allocated_Warmup_Controller());
                if (this.warmupControllerChannel != component && component instanceof ControllerWarmup) {
                    this.warmupControllerChannel = (ControllerWarmup) component;
                }
            }
            component = this.cpm.getComponent(this.config.allocated_Heating_Curve_Regulator());
            if (this.heatingCurveRegulatorChannel != component && component instanceof HeatingCurveRegulatorChannel) {
                this.heatingCurveRegulatorChannel = (HeatingCurveRegulatorChannel) component;
            }
            component = this.cpm.getComponent(this.config.allocated_Pump());
            if (this.hydraulicComponent != component && component instanceof HydraulicComponent) {
                this.hydraulicComponent = (HydraulicComponent) component;
            }
            return false;
        } catch (OpenemsError.OpenemsNamedException e) {
            return true;
        }
    }


    private void turnOnHeater(int temperatureInDezidegree) throws OpenemsError.OpenemsNamedException {
        this.activateHeater().setNextValue(true);
        this.temperatureHeating().setNextValue(temperatureInDezidegree);
        this.pidControllerChannel.setEnableSignal(true);
        this.pidControllerChannel.setMinTemperature().setNextWriteValue(temperatureInDezidegree);
        this.hydraulicComponent.setPointPowerLevelChannel().setNextWriteValueFromObject(HydraulicComponent.DEFAULT_MAX_POWER_VALUE);
    }

    private void turnOffHeater() throws OpenemsError.OpenemsNamedException {
        this.activateHeater().setNextValue(false);
        this.temperatureHeating().setNextValue(0);
        this.pidControllerChannel.setEnableSignal(false);
        this.hydraulicComponent.setPointPowerLevelChannel().setNextWriteValueFromObject(HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
    }

    void allocateComponents() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (this.cpm.getComponent(this.config.allocated_Pid_Controller()) instanceof PidHydraulicController) {
            this.pidControllerChannel = this.cpm.getComponent(this.config.allocated_Pid_Controller());
        } else {
            throw new ConfigurationException(this.config.allocated_Pid_Controller(),
                    "Allocated Passing Controller not a Pid for Passing Controller; Check if Name is correct and try again.");
        }
        if (this.config.run_warmup_program() && this.cpm.getComponent(this.config.allocated_Warmup_Controller()) instanceof ControllerWarmup) {
            this.warmupControllerChannel = this.cpm.getComponent(this.config.allocated_Warmup_Controller());
        } else {
            throw new ConfigurationException(this.config.allocated_Warmup_Controller(),
                    "Allocated Warmup Controller not a WarmupPassing Controller; Check if Name is correct and try again.");
        }
        if (this.cpm.getComponent(this.config.allocated_Heating_Curve_Regulator()) instanceof HeatingCurveRegulatorChannel) {
            this.heatingCurveRegulatorChannel = this.cpm.getComponent(this.config.allocated_Heating_Curve_Regulator());
        } else {
            throw new ConfigurationException(this.config.allocated_Warmup_Controller(),
                    "Allocated Heating Controller not a Heating Curve Regulator; Check if Name is correct and try again.");
        }
        if (this.cpm.getComponent(this.config.allocated_Pump()) instanceof HydraulicComponent) {
            this.hydraulicComponent = this.cpm.getComponent(this.config.allocated_Pump());
        } else {
            throw new ConfigurationException(this.config.allocated_Warmup_Controller(),
                    "Allocated Heating Controller not a Heating Curve Regulator; Check if Name is correct and try again.");
        }

    }

}
