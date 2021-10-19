package io.openems.edge.controller.heatnetwork.surveillance.temperature;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.surveillance.temperature.api.TemperatureSurveillanceController;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The TemperatureSurveillanceController is a Controller that monitors 3 different Temperatures and enables/disables it's
 * Components, depending on the Configuration.
 * There are 3 "valid" SurveillanceTypes.
 * HEATER_ONLY, VALVE_CONTROLLER_ONLY, HEATER_AND_VALVE_CONTROLLER
 * CoolerOnly enables the Cooler if the activationConditions apply.
 * Equivalent thing happen for the valveControllerOnly setting, enabling a ValveController.
 * If CoolerAndValveController setting is enabled, the Cooler will be enabled at first, and after a configured WaitTime
 * The ValveController will be additionally enabled.
 */
@Designate(ocd = ConfigTempSurveillanceCooling.class, factory = true)
@Component(name = "Controller.Temperature.Surveillance.Cooling",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TemperatureSurveillanceControllerCoolingImpl extends AbstractTemperatureSurveillanceController implements OpenemsComponent,
        Controller, TemperatureSurveillanceController {

    private final Logger log = LoggerFactory.getLogger(TemperatureSurveillanceControllerCoolingImpl.class);

    private ConfigTempSurveillanceCooling config;


    public TemperatureSurveillanceControllerCoolingImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, ConfigTempSurveillanceCooling config) throws ConfigurationException {
        this.config = config;
        super.activate(context, this.config.id(), this.config.alias(), this.config.enabled(),
                this.config.thermometerActivateId(), this.config.thermometerDeactivateId(),
                this.config.referenceThermometerId(), SurveillanceHeatingType.COOLING,
                this.config.surveillanceType(), this.config.offsetActivate(), this.config.offsetDeactivate(),
                this.config.coolerId(), this.config.hydraulicControllerId(),
                this.config.timerId(), this.config.deltaTimeDelay(), this.cpm);
    }

    @Modified
    void modified(ComponentContext context, ConfigTempSurveillanceCooling config) throws ConfigurationException {
        this.config = config;
        super.modified(context, this.config.id(), this.config.alias(), this.config.enabled(),
                this.config.thermometerActivateId(), this.config.thermometerDeactivateId(),
                this.config.referenceThermometerId(),
                SurveillanceHeatingType.COOLING,
                this.config.surveillanceType(), this.config.offsetActivate(),
                this.config.offsetDeactivate(), this.config.coolerId(),
                this.config.hydraulicControllerId(),
                this.config.timerId(), this.config.deltaTimeDelay(), this.cpm);
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Depending on SurveillanceType Different "Controlling" applies
     * If ActivationConditions apply (Same for every SurveillanceType).
     * <p>
     * Either: Enable HEATER (on Cooler ONLY mode)
     * OR
     * Enable ValveController (on ValveController ONLY Mode
     * OR
     * First Enable Cooler and then after certain WaitTime Enable ValveController (HEATER_AND_VALVE_CONTROLLER).
     * on deactivation: Disable corresponding Components
     * </p>
     */
    @Override
    public void run() {

        if (super.configSuccess) {
            super.abstractRun();
        } else {
            try {
                super.activationOrModifiedRoutine(this.config.thermometerActivateId(), this.config.thermometerDeactivateId(),
                        this.config.referenceThermometerId(), this.config.offsetActivate(), this.config.offsetDeactivate(),
                        this.config.coolerId(), this.config.hydraulicControllerId(),
                        this.config.timerId(), this.config.deltaTimeDelay());
            } catch (ConfigurationException e) {
                this.log.warn("Please make sure to have 3 different Thermometer configured!");
            }
        }
    }

}
