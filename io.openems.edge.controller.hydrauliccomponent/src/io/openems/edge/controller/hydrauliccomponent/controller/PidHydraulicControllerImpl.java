package io.openems.edge.controller.hydrauliccomponent.controller;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.filter.PidFilter;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.hydrauliccomponent.api.HydraulicController;
import io.openems.edge.controller.hydrauliccomponent.api.PidHydraulicController;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * This HydraulicController controls an hydraulicComponent, by applying a PID filtered SetPoint Value and adding it to the
 * current State of the Hydraulic component (+-SetPoint).
 * It needs an EnableSignal and a {@link PidHydraulicController.ChannelId#MIN_TEMPERATURE} to work.
 * The HydraulicController gets it's current temperature from a reference Thermometer and hand it over to the {@link PidFilter}
 * together with the MinTemperature.
 * </p>
 *
 * <p>
 * To prevent too fast calculations (HeatNetworks are slow -> Valves need to change position and Temperature changes slowly)
 * a {@link PidConfig#waitTime()} value can be configured. This will delay the calculation.
 * </p>
 */
@Designate(ocd = PidConfig.class, factory = true)
@Component(name = "Controller.Hydraulic.Pid")
public class PidHydraulicControllerImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, PidHydraulicController {

    private static final int BOUNDARY = 5;
    private final Logger log = LoggerFactory.getLogger(PidHydraulicControllerImpl.class);

    @Reference
    ComponentManager cpm;

    private PidFilter pidFilter;
    private PidConfig config;

    private Thermometer thermometer;
    private HydraulicComponent hydraulicComponent;
    private int inactivePowerValue = 0;
    private boolean morePercentEqualsCooling;
    private RunStatus runStatus;
    private boolean configSuccess;

    private TimerHandler timerHandler;
    private static final String WAIT_TIME_IDENTIFIER = "PID_HYDRAULIC_CONTROLLER_WAIT_TIME";

    public PidHydraulicControllerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                PidHydraulicController.ChannelId.values(),
                HydraulicController.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, PidConfig config) {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled());
        try {
            this.activationOrModifiedRoutine(config);
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Couldn't apply config properly. Try again later!");
            this.configSuccess = false;
        }
        this.setTemperatureChannel();
        this.runStatus = RunStatus.OFF;
    }

    @Modified
    void modified(ComponentContext context, PidConfig config) {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.configSuccess = false;
        try {
            this.activationOrModifiedRoutine(config);
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            this.log.warn("Couldn't apply config properly. Try again later!");
            this.configSuccess = false;
        }
    }

    /**
     * Sets a MinTemperature. Only called by the {@link #activationOrModifiedRoutine(PidConfig)} method.
     * This is the SetPoint, the PID Controller wants to reach on activation.
     */
    private void setTemperatureChannel() {
        this.setMinTemperature().setNextValue(this.config.setPoint_Temperature());
        this.setMinTemperature().nextProcessImage();
    }


    /**
     * This Method will be either called by activate or modified method.
     * It calls other methods or does certain tasks, that should always happen if the controller is either activated or modified.
     * It sets up PID Filter, the components, the TimerHandler, the SetPoint and if morePercent == cooling.
     * (the PID thinks -> more percent -> it gets warmer -> invert the value)
     *
     * @param config the configuration
     * @throws OpenemsError.OpenemsNamedException if a Component couldn't be found
     * @throws ConfigurationException             if a configured Component could be found but is a wrong instance
     */
    private void activationOrModifiedRoutine(PidConfig config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.config = config;
        this.allocateComponent(config.temperatureSensorId());
        this.allocateComponent(config.allocatedHydraulicComponent());
        this.pidFilter = new PidFilter(this.config.proportionalGain(), this.config.integralGain(), this.config.derivativeGain());
        this.pidFilter.setLimits(config.lowerLimit(), config.upperLimit());
        this.inactivePowerValue = this.config.offPercentage();
        this.morePercentEqualsCooling = config.morePercentEqualsCooling();
        if (this.timerHandler != null) {
            this.timerHandler.removeComponent();
        }
        this.timerHandler = new TimerHandlerImpl(this.id(), this.cpm);
        this.timerHandler.addOneIdentifier(WAIT_TIME_IDENTIFIER, config.timerId(), config.waitTime());
        this.setTemperatureChannel();
        this.configSuccess = true;
        this.autoRunChannel().setNextValue(config.autoRun());
        this.autoRunChannel().nextProcessImage();
    }

    /**
     * Allocates a Component.
     *
     * @param device String from Config; needs to be an instance of PassingForPid/Thermometer/ControllerPassingChannel.
     *               <p>
     *               Allocate the Component --> Access to Channels
     *               </p>
     * @throws OpenemsError.OpenemsNamedException when cpm can't access / somethings wrong with cpm.
     * @throws ConfigurationException             when cpm tries to access device but it's not correct instance.
     */
    private void allocateComponent(String device) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        OpenemsComponent component = this.cpm.getComponent(device);
        if (component instanceof HydraulicComponent) {
            this.hydraulicComponent = (HydraulicComponent) component;
        } else if (component instanceof Thermometer) {
            this.thermometer = (Thermometer) component;
        } else {
            throw new ConfigurationException("Allocate Component", "Configured Component is incorrect!: " + device);
        }

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Controls a pump or valve via PID.
     *
     * <p>
     * Only execute Logic if this is enabled!
     * If the Controller is ready / should calculate a PercentValue through PID Calculation
     * Get the calculated value add it to the HydraulicComponents PowerLevel.
     * After that wait some cycles/seconds (depends on the configured timer)
     * -> temperature can adapt.
     * Look up the {@link #notWithinTemperatureRange(int, int)}
     * for details on the conditions to calculate PID value.
     * </p>
     */

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        if (this.configSuccess) {
            if (this.componentIsMissing()) {
                this.log.warn("A Component is Missing in the Controller : " + super.id());
                return;
            }
            boolean enableSignal = this.getEnableSignalChannel().getNextWriteValue().orElse(false) || this.isAutorun();
            this.getEnableSignalChannel().setNextValue(enableSignal);
            if (enableSignal) {
                if (this.timerHandler.checkTimeIsUp(WAIT_TIME_IDENTIFIER)) {
                    this.timerHandler.resetTimer(WAIT_TIME_IDENTIFIER);
                    int referenceTemperature = this.thermometer.getTemperatureValue();
                    int setPointTemperature = this.setMinTemperature().value().orElse(0);
                    if (referenceTemperature != Thermometer.MISSING_TEMPERATURE &&
                            referenceTemperature != TypeUtils.fitWithin(setPointTemperature - BOUNDARY,
                                    setPointTemperature + BOUNDARY, referenceTemperature)) {
                        //      && this.hydraulicComponentAtMaximum(referenceTemperature, setPointTemperature) == false) {
                        this.runStatus = RunStatus.RUNNING;
                        //calculated Percentage
                        double powerValueToApply = this.pidFilter.applyPidFilter(referenceTemperature, setPointTemperature);
                        // it seems, that the calculated powerValue is a setPoint instead of adding/subtracting
                        // by giving a min and max range in dC you have to divide the result by 10
                        powerValueToApply = powerValueToApply / 10;
                        //since pid thinks more % gets this component to the setPoint -> invert the applied powerValue
                        if (this.morePercentEqualsCooling) {
                            powerValueToApply = HydraulicComponent.DEFAULT_MAX_POWER_VALUE - powerValueToApply;
                        }
                        powerValueToApply = TypeUtils.fitWithin(HydraulicComponent.DEFAULT_MIN_POWER_VALUE, HydraulicComponent.DEFAULT_MAX_POWER_VALUE, (int) powerValueToApply);
                        this.hydraulicComponent.setPointPowerLevelChannel().setNextWriteValueFromObject(powerValueToApply);
                    }
                } else {
                    this.runStatus = RunStatus.WAITING;
                }
            } else {
                this.runStatus = RunStatus.OFF;
                if (this.hydraulicComponent.readyToChange()) {
                    this.hydraulicComponent.setPointPowerLevelChannel().setNextWriteValueFromObject(this.inactivePowerValue);
                }
            }
        } else {
            try {
                this.activationOrModifiedRoutine(this.config);
            } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
                this.log.warn("Couldn't apply config properly. Try again later!");
                this.configSuccess = false;
            }
        }
    }


    /**
     * Since we can't adjust temperatures to the exact point a boundary/tolerance is implemented.
     * The Boundary is set to a fix 5 dC -> 0.5°C
     *
     * @param referenceTemperature the current Temperature
     * @param setPointTemperature  the setPoint we should reach.
     * @return if reference within setPointRange.
     */
    private boolean notWithinTemperatureRange(int referenceTemperature, int setPointTemperature) {
        int upperLimit = setPointTemperature + BOUNDARY;
        int lowerLimit = setPointTemperature - BOUNDARY;
        return referenceTemperature > upperLimit || referenceTemperature < lowerLimit;
    }

    /**
     * Check if a Component is missing and refresh the reference.
     *
     * @return true if a Component couldn't be found.
     */
    private boolean componentIsMissing() {
        try {
            if (this.hydraulicComponent.isEnabled() == false) {
                this.hydraulicComponent = this.cpm.getComponent(this.config.allocatedHydraulicComponent());
            }
            if (this.thermometer.isEnabled() == false) {
                this.thermometer = this.cpm.getComponent(this.config.temperatureSensorId());
            }
            return false;
        } catch (OpenemsError.OpenemsNamedException e) {
            return true;
        }
    }

    @Override
    public String debugLog() {

        String debugMessage = "The Controller is: " + this.runStatus + " The EnableSignal is: " + this.getEnableSignalChannel().value();
        if (this.runStatus.equals(RunStatus.OFF) == false) {
            debugMessage += " This SetPoint Temperature is: " + this.setMinTemperature().value().toString();
        }
        return debugMessage + "\n";
    }

    /**
     * Gets the current Position of the hydraulicComponent.
     *
     * @return the current Position.
     */
    @Override
    public double getCurrentPositionOfComponent() {
        return this.hydraulicComponent.getPowerLevelValue();
    }

    /**
     * Defines the Run Status of the PidController.
     */
    private enum RunStatus {
        RUNNING, WAITING, OFF
    }
}
