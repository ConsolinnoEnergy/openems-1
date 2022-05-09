package io.openems.edge.controller.heatnetwork.controlcenter;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.controlcenter.api.ControlCenter;
import io.openems.edge.controller.heatnetwork.heatingcurveregulator.api.HeatingCurveRegulator;
import io.openems.edge.controller.heatnetwork.warmup.api.ControllerWarmup;
import io.openems.edge.controller.hydrauliccomponent.api.HydraulicController;
import io.openems.edge.controller.hydrauliccomponent.api.PidHydraulicController;
import io.openems.edge.thermometer.api.Thermometer;
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
 * This is the Implementation of the ControlCenter.
 * It manages the hierarchy of heating controllers. By receiving a HeatingCurve, an optional WarmupProgram Controller
 * and also by receiving Overrides.
 * - The output of a heating controller is a temperature and a boolean to signal if the controller wants to heat or not.
 * - This controller polls three heating controllers by hierarchy and passes on the result (heating or not heating plus
 * the temperature) to the next module(s).
 * The Hierarchy is:
 * 1. Check if an Override is active -> if so use the temperature of the override.
 * 2. If Override is not available check if the warmup program is configured and if so -> use the given temperature of the warmup program.
 * 3. If 1. and 2. do not apply check if the HeatingCurve set's a temperature.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Heatnetwork.ControlCenter", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ControlCenterImpl extends AbstractOpenemsComponent implements OpenemsComponent, ControlCenter, Controller {

    private final Logger log = LoggerFactory.getLogger(ControlCenterImpl.class);

    @Reference
    protected ComponentManager cpm;

    private PidHydraulicController pidController;
    private ControllerWarmup warmupController;
    private HeatingCurveRegulator heatingCurveRegulator;
    private HydraulicController hydraulicController;

    private int setPointTemperature;
    private ControlType controlType;


    enum ControlType {
        OVERRIDE, WARMUP_PROGRAM, HEATING_CURVE, NONE
    }

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
        if (config.run_warmup_program() && this.warmupController != null) {
            this.warmupController.playPauseWarmupController().setNextWriteValue(true);
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void run() {
        if (this.componentIsMissing()) {
            this.log.warn("A Component is missing in: " + super.id());
            return;
        }
        // For the Overrides, copy values from the WriteValue to the NextValue fields.
        if (this.activateTemperatureOverride().getNextWriteValue().isPresent()) {
            this.activateTemperatureOverride().setNextValue(this.activateTemperatureOverride().getNextValue().orElse(false));
        }
        if (this.setOverrideTemperature().getNextWriteValue().isPresent()) {
            this.setOverrideTemperature().setNextValue(this.setOverrideTemperature().getNextWriteValue().get());
        }
        this.controlType = this.getCurrentControlType();
        this.setActiveTemperatureDependingOnControlType(this.controlType);
        if (!this.controlType.equals(ControlType.NONE)) {
            try {
                this.turnOnHeater();
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn(this.id() + " Couldn't ENABLE HydraulicController! Please check your config. A component might be missing");
            }
        } else {
            try {
                this.turnOffHeater();
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn(this.id() + " Couldn't DISABLE HydraulicController! Please check your config. A component might be missing");
            }
        }
    }

    /**
     * Depending on the ControlType a SetPointTemperature will be selected.
     *
     * @param controlType the controlType.
     */
    private void setActiveTemperatureDependingOnControlType(ControlType controlType) {

        switch (controlType) {

            case OVERRIDE:
                this.setPointTemperature = this.setOverrideTemperature().value().get();
                break;
            case WARMUP_PROGRAM:
                this.setPointTemperature = this.warmupController.getWarmupTemperature().value().get();
                break;
            case HEATING_CURVE:
                this.setPointTemperature = this.heatingCurveRegulator.getHeatingTemperature().value().get();
                break;
            case NONE:
                this.setPointTemperature = Thermometer.MISSING_TEMPERATURE;
                break;
        }

    }

    /**
     * Depending on the Channel the current ControlType of this Controller will be set.
     *
     * @return the controlType
     */
    private ControlType getCurrentControlType() {
        if (this.activateTemperatureOverride().value().orElse(false)
                && this.setOverrideTemperature().value().isDefined()) {
            return ControlType.OVERRIDE;
        } else if (this.config.run_warmup_program() && this.warmupController.playPauseWarmupController().value().orElse(false)
                && this.warmupController.getWarmupTemperature().value().isDefined()
                && this.warmupController.noError().value().orElse(false)) {
            return ControlType.WARMUP_PROGRAM;
        } else if (this.heatingCurveRegulator.signalTurnOnHeater().value().orElse(false)
                && this.heatingCurveRegulator.getHeatingTemperature().value().isDefined()
                && this.heatingCurveRegulator.noErrorChannel().value().orElse(false)) {
            return ControlType.HEATING_CURVE;
        }
        return ControlType.NONE;
    }

    /**
     * Because Components can be updated, get new Instances of Components.
     *
     * @return true if every component could be reached etc.
     */
    private boolean componentIsMissing() {
        try {
            OpenemsComponent component = this.cpm.getComponent(this.config.allocated_Pid_Controller());
            if (this.pidController != component && component instanceof PidHydraulicController) {
                this.pidController = (PidHydraulicController) component;
            }
            if (this.config.run_warmup_program()) {
                component = this.cpm.getComponent(this.config.allocated_Warmup_Controller());
                if (this.warmupController != component && component instanceof ControllerWarmup) {
                    this.warmupController = (ControllerWarmup) component;
                }
            }
            component = this.cpm.getComponent(this.config.allocated_Heating_Curve_Regulator());
            if (this.heatingCurveRegulator != component && component instanceof HeatingCurveRegulator) {
                this.heatingCurveRegulator = (HeatingCurveRegulator) component;
            }
            component = this.cpm.getComponent(this.config.allocated_HydraulicController());
            if (this.hydraulicController != component && component instanceof HydraulicController) {
                this.hydraulicController = (HydraulicController) component;
            }
            return false;
        } catch (OpenemsError.OpenemsNamedException e) {
            return true;
        }
    }

    /**
     * Activates the (PID) HydraulicController and sets the setPointTemperature in the PID Controller.
     *
     * @throws OpenemsError.OpenemsNamedException when a Channel cannot be found or written into.
     */
    private void turnOnHeater() throws OpenemsError.OpenemsNamedException {
        if (this.setPointTemperature != Thermometer.MISSING_TEMPERATURE) {
            this.activateHeater().setNextValue(true);
            this.temperatureHeating().setNextValue(this.setPointTemperature);
            this.pidController.setEnableSignal(true);
            this.pidController.setMinTemperature().setNextWriteValueFromObject(this.setPointTemperature);
            this.hydraulicController.setEnableSignal(true);
        }
    }

    /**
     * Deactivates HydraulicController.
     *
     * @throws OpenemsError.OpenemsNamedException thrown when Channel cannot be found or written into.
     */
    private void turnOffHeater() throws OpenemsError.OpenemsNamedException {
        this.activateHeater().setNextValue(false);
        this.temperatureHeating().setNextValue(0);
        this.pidController.setEnableSignal(false);
        this.hydraulicController.setEnableSignal(false);
    }

    /**
     * Allocates all Components from the config.
     *
     * @throws OpenemsError.OpenemsNamedException when a Component cannot be found.
     * @throws ConfigurationException             if the Configuration is wrong (Component is the wrong instance)
     */
    void allocateComponents() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (this.cpm.getComponent(this.config.allocated_Pid_Controller()) instanceof PidHydraulicController) {
            this.pidController = this.cpm.getComponent(this.config.allocated_Pid_Controller());
        } else {
            throw new ConfigurationException(this.config.allocated_Pid_Controller(),
                    "Allocated Passing Controller not a Pid for Passing Controller; Check if Name is correct and try again.");
        }
        if (this.config.run_warmup_program()) {
            if (this.cpm.getComponent(this.config.allocated_Warmup_Controller()) instanceof ControllerWarmup) {
                this.warmupController = this.cpm.getComponent(this.config.allocated_Warmup_Controller());
            } else {
                throw new ConfigurationException(this.config.allocated_Warmup_Controller(),
                        "Allocated Warmup Controller not a WarmupPassing Controller; Check if Name is correct and try again.");
            }
        }
        if (this.cpm.getComponent(this.config.allocated_Heating_Curve_Regulator()) instanceof HeatingCurveRegulator) {
            this.heatingCurveRegulator = this.cpm.getComponent(this.config.allocated_Heating_Curve_Regulator());
        } else {
            throw new ConfigurationException(this.config.allocated_Warmup_Controller(),
                    "Allocated Heating Controller not a Heating Curve Regulator; Check if Name is correct and try again.");
        }
        if (this.cpm.getComponent(this.config.allocated_HydraulicController()) instanceof HydraulicController) {
            this.hydraulicController = this.cpm.getComponent(this.config.allocated_HydraulicController());
        } else {
            throw new ConfigurationException(this.config.allocated_Warmup_Controller(),
                    "Allocated Heating Controller not a Heating Curve Regulator; Check if Name is correct and try again.");
        }

    }

    @Override
    public String debugLog() {
        if (this.controlType.equals(ControlType.NONE)) {
            return this.id() + " Currently NOT Heating";
        }
        return this.id() + " Currently Heating by Hierarchy: " + this.controlType.name() + " with SetPoint: " + this.setPointTemperature;
    }
}
