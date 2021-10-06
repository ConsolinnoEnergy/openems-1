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
 * HeaterOnly enables the Heater if the activationConditions apply.
 * Equivalent thing happen for the valveControllerOnly setting, enabling a ValveController.
 * If HeaterAndValveController setting is enabled, the Heater will be enabled at first, and after a configured WaitTime
 * The ValveController will be additionally enabled.
 */
@Designate(ocd = ConfigTempSurveillanceHeating.class, factory = true)
@Component(name = "Controller.Temperature.Surveillance.Heating",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TemperatureSurveillanceControllerHeatingImpl extends AbstractTemperatureSurveillanceController implements OpenemsComponent,
        Controller, TemperatureSurveillanceController {

    private final Logger log = LoggerFactory.getLogger(TemperatureSurveillanceControllerHeatingImpl.class);

    private ConfigTempSurveillanceHeating config;


    public TemperatureSurveillanceControllerHeatingImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, ConfigTempSurveillanceHeating config) throws ConfigurationException {
        this.config = config;
        super.activate(context, this.config.id(), this.config.alias(), this.config.enabled(),
                this.config.thermometerActivateId(), this.config.thermometerDeactivateId(),
                this.config.referenceThermometerId(), AbstractTemperatureSurveillanceController.SurveillanceHeatingType.HEATING,
                this.config.surveillanceType(), this.config.offsetActivate(), this.config.offsetDeactivate(),
                this.config.heaterId(), this.config.hydraulicControllerId(),
                this.config.timerId(), this.config.deltaTimeDelay());
    }

    @Modified
    void modified(ComponentContext context, ConfigTempSurveillanceHeating config) throws ConfigurationException {
        this.config = config;
        super.modified(context, this.config.id(), this.config.alias(), this.config.enabled(),
                this.config.thermometerActivateId(), this.config.thermometerDeactivateId(),
                this.config.referenceThermometerId(),
                AbstractTemperatureSurveillanceController.SurveillanceHeatingType.HEATING,
                this.config.surveillanceType(), this.config.offsetActivate(),
                this.config.offsetDeactivate(), this.config.heaterId(),
                this.config.hydraulicControllerId(),
                this.config.timerId(), this.config.deltaTimeDelay());
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    /**
     * Depending on SurveillanceType Different "Controlling" applies
     * If ActivationConditions apply (Same for every SurveillanceType).
     * <p>
     * Either: Enable HEATER (on Heater ONLY mode)
     * OR
     * Enable ValveController (on ValveController ONLY Mode
     * OR
     * First Enable Heater and then after certain WaitTime Enable ValveController (HEATER_AND_VALVE_CONTROLLER).
     * on deactivation: Disable corresponding Components
     * </p>
     */
    @Override
    public void run() {
        if (!super.configSuccess) {
            try {
                super.activationOrModifiedRoutine(this.config.thermometerActivateId(), this.config.thermometerDeactivateId(),
                        this.config.referenceThermometerId(), this.config.offsetActivate(), this.config.offsetDeactivate(),
                        this.config.heaterId(), this.config.hydraulicControllerId(),
                        this.config.timerId(), this.config.deltaTimeDelay());
            } catch (ConfigurationException e) {
                this.log.warn("Please make sure to have 3 different Thermometer configured!");
            }
        } else {
            super.abstractRun();
        }
    }

}
