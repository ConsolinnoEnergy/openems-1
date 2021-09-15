package io.openems.edge.controller.hydrauliccomponent.controller;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.filter.PidFilter;
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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This HydraulicController controls an hydraulicComponent, by applying a PID filtered SetPoint Value and adding it to the
 * current State of the Hydraulic component (+-SetPoint).
 * It needs an EnableSignal and a {@link PidHydraulicController.ChannelId#MIN_TEMPERATURE} to work.
 * The HydraulicController gets it's current temperature from a reference Thermometer and hand it over to the {@link PidFilter}
 * together with the MinTemperature.
 * <p>
 * To prevent too fast calculations (HeatNetworks are slow -> Valves need to change position and Temperature changes slowly)
 * a {@link PidConfig#waitTime()} value can be configured. This will delay the calculation.
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

    private TimerHandler timerHandler;
    private static final String WAIT_TIME_IDENTIFIER = "PID_HYDRAULIC_CONTROLLER_WAIT_TIME";

    public PidHydraulicControllerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                PidHydraulicController.ChannelId.values(),
                HydraulicController.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, PidConfig config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.config = config;
        if (componentIdNotAlreadyPresent()) {
            activationOrModifiedRoutine(context, config);
        }
        this.setTemperatureChannel();
        this.runStatus = RunStatus.OFF;
    }

    @Modified
    void modified(ComponentContext context, PidConfig config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activationOrModifiedRoutine(context, config);
    }

    /**
     * Sets a MinTemperature (only called by the {@link #activationOrModifiedRoutine(ComponentContext, PidConfig)} method.
     * This is the SetPoint, the PID Controller wants to reach on activation.
     */
    private void setTemperatureChannel() {
        this.setMinTemperature().setNextValue(this.config.setPoint_Temperature());
        this.setMinTemperature().nextProcessImage();
    }

    /**
     * Checks if another Component with the configured ID is already present -> prevent duplications
     *
     * @return true if No Duplication of ID is found.
     */
    private boolean componentIdNotAlreadyPresent() {
        AtomicBoolean noInstanceFound = new AtomicBoolean(true);

        this.cpm.getAllComponents().stream().filter(component -> component.id().equals(config.id())).findFirst().ifPresent(consumer -> {
            noInstanceFound.set(false);
        });
        if (noInstanceFound.get() == false) {
            this.log.warn("PidController with id: " + this.id() + "already an allocated Controller, please change the name");
        }
        return noInstanceFound.get();
    }

    /**
     * This Method will be either called by the activate or modified method.
     * It calls other methods or does certain tasks, that should always happen if the controller is either activated or modified.
     * It sets up PID Filter, the components, the TimerHandler, the SetPoint and if morePercent == cooling.
     * (the PID thinks -> more percent -> it gets warmer -> invert the value)
     *
     * @param context the ComponentContext
     * @param config  the configuration
     * @throws OpenemsError.OpenemsNamedException if a Component couldn't be found
     * @throws ConfigurationException             if a configured Component could be found but is a wrong instance
     */
    private void activationOrModifiedRoutine(ComponentContext context, PidConfig config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.config = config;
        super.activate(context, this.config.id(), this.config.alias(), this.config.enabled());
        allocateComponent(config.temperatureSensorId());
        allocateComponent(config.allocatedPassingDevice());
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
        if (componentIsMissing()) {
            this.log.warn("A Component is Missing in the Controller : " + super.id());
            return;
        }
        boolean enableSignal = this.getEnableSignalChannel().getNextWriteValue().orElse(false);
        this.getEnableSignalChannel().setNextValue(enableSignal);
        if (enableSignal) {
            if (this.timerHandler.checkTimeIsUp(WAIT_TIME_IDENTIFIER)) {
                // || config.debugMode()) {
                this.timerHandler.resetTimer(WAIT_TIME_IDENTIFIER);
                int referenceTemperature = this.thermometer.getTemperatureValue();
                int setPointTemperature = this.setMinTemperature().value().orElse(0);
                if (this.notWithinTemperatureRange(referenceTemperature, setPointTemperature)) {
                    //      && this.hydraulicComponentAtMaximum(referenceTemperature, setPointTemperature) == false) {
                    this.runStatus = RunStatus.RUNNING;
                    //calculated Percentage
                    double powerValueToApply = pidFilter.applyPidFilter(referenceTemperature, setPointTemperature);
                    // / 10 bc systems show good % value with this
                    powerValueToApply = powerValueToApply / 10;

                    // is percentage value fix if so subtract from current powerLevel?
                    if (this.morePercentEqualsCooling) {
                        powerValueToApply *= -1;
                    }
                    if (this.hydraulicComponent.getPowerLevelChannel().getNextValue().isDefined()) {
                        powerValueToApply += this.hydraulicComponent.getPowerLevelChannel().getNextValue().orElse(0.d);
                        powerValueToApply = Math.max(HydraulicComponent.DEFAULT_MIN_POWER_VALUE, powerValueToApply);
                    }
                    this.hydraulicComponent.setPointPowerLevelChannel().setNextWriteValueFromObject(powerValueToApply);
                }
            } else {
                this.runStatus = RunStatus.WAITING;
            }

            // }
        } else {
            this.runStatus = RunStatus.OFF;
            if (this.hydraulicComponent.readyToChange()) {
                this.hydraulicComponent.setPointPowerLevelChannel().setNextWriteValueFromObject(this.inactivePowerValue);
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
                this.hydraulicComponent = this.cpm.getComponent(config.allocatedPassingDevice());
            }
            if (this.thermometer.isEnabled() == false) {
                this.thermometer = this.cpm.getComponent(config.temperatureSensorId());
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
     * Defines the Run Status of the PidController
     */
    private enum RunStatus {
        RUNNING, WAITING, OFF
    }
}
