package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.multipleheatercombined.api.MultipleHeaterCombinedController;
import io.openems.edge.heater.api.Heater;
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
 * The MultipleHeaterCombined controller allows the monitoring and enabling of any {@link Heater}.
 * Each Heater gets an activation and deactivation Thermometer as well as activation/deactivation temperature.
 * For Example: Heater A has an (activation) Thermometer B and a (deactivation) Thermometer C with an activation Temperature
 * of 400dC and a deactivation Temperature of 600dC.
 * The thermometer C will be checked if it's Temperature is > than the deactivation Temp. of 600 dC.
 * If so -> disable the Heater (Don't write in the enable Signal) -> set the {@link ActiveWrapper#setActive(boolean)}}
 * to false therefore don't write in the corresponding heater EnableSignal Channel.
 * Else if the Activation Thermometer B is beneath the activation Temperature of 400dC set the {@link ActiveWrapper#setActive(boolean)}
 * to true and therefore write into the heater EnableSignal Channel.
 * NOTE: Activation/Deactivation Temperatures can be ChannelAddresses as well!
 */
@Designate(ocd = ConfigMultipleHeater.class, factory = true)
@Component(name = "Controller.Heatnetwork.MultipleHeaterCombined",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class MultipleHeaterCombinedControllerImpl extends AbstractMultiCombinedController implements OpenemsComponent,
        Controller, MultipleHeaterCombinedController {

    private final Logger log = LoggerFactory.getLogger(MultipleHeaterCombinedControllerImpl.class);

    private ConfigMultipleHeater config;

    public MultipleHeaterCombinedControllerImpl() {

        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                MultipleHeaterCombinedController.ChannelId.values());

    }

    @Activate
    void activate(ComponentContext context, ConfigMultipleHeater config) {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.useTimer(), config.timerId(), config.timeDelta(), ControlType.HEATER, config.heaterIds(),
                config.activationThermometers(), config.activationTemperatures(), config.deactivationThermometers(), config.deactivationTemperatures(), this.cpm);
    }

    @Modified
    void modified(ComponentContext context, ConfigMultipleHeater config) {
        this.config = config;
        super.modified(context, config.id(), config.alias(), config.enabled(), config.useTimer(), config.timerId(), config.timeDelta(), ControlType.HEATER, config.heaterIds(),
                config.activationThermometers(), config.activationTemperatures(), config.deactivationThermometers(), config.deactivationTemperatures(), this.cpm);
    }

    /**
     * MultipleHeaterCombined logic.
     * <p>
     * Each Heater got a Temperature where they should activate and deactivate.
     * When the activationThermometer reaches the activationTemperature (or is beneath), the Heater activates
     * / {@link io.openems.edge.heater.api.Heater#getEnableSignalChannel()}
     * will receive a nextWriteValue of true.
     * The Heater stays active, until the deactivationThermometer reaches the deactivationTemperature or exceeds it.
     * When the Deactivation Temperature is reached, the {@link io.openems.edge.heater.api.Heater#getEnableSignalChannel()} won't be set in any way.
     * This way the Heater stays deactivated, until the activationTemperature is reached again.
     * </p>
     */

    @Override
    public void run() {
        if (this.configurationSuccess) {
            super.abstractRun();
            //Sets both error and ok
            this.setIsHeating(super.isHeatingOrCooling.get());
            this.setHasError(super.heaterError.get());
        } else {
            try {
                this.allocateConfig(ControlType.HEATER, this.config.timerId(), this.config.timeDelta(), this.config.heaterIds(), this.config.activationThermometers(), this.config.activationTemperatures(),
                        this.config.deactivationThermometers(), this.config.deactivationTemperatures());
                this.configurationSuccess = true;
                this.setHasError(false);
            } catch (ConfigurationException | OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't set Configuration for Controller : " + super.id() + " Check the Configuration of this Controller!");
                this.log.info("NOTE: When Restarting OpenEMS some Instances may not be initialized yet.");
                this.setHasError(true);
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }
}
