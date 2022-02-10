package io.openems.edge.controller.heatnetwork.multipleheatercombined;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.multipleheatercombined.api.MultipleCoolerCombinedController;
import io.openems.edge.heater.api.Cooler;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The MultipleCoolerCombined controller allows the monitoring and enabling of any {@link Cooler}.
 * Each Cooler gets an activation and deactivation Thermometer as well as temperature.
 * For Example: Cooler A has an (activation) Thermometer B and a (deactivation) Thermometer C with an activation Temperature
 * of 400 dC and a deactivation Temperature of 200dC.
 * The thermometer C will be checked if it's Temperature is <= than the deactivation Temp. of 200 dC.
 * If so -> disable the Cooler (Don't write in the enable Signal) -> set the {@link ActiveWrapper#setActive(boolean)}}
 * to false therefore don't write in the corresponding cooler EnableSignal Channel.
 * Else if the Activation Thermometer B is above the activation Temperature of 400dC set the {@link ActiveWrapper#setActive(boolean)}
 * to true and therefore write into the cooler EnableSignal Channel.
 */
@Designate(ocd = ConfigMultipleCooler.class, factory = true)
@Component(name = "Controller.Heatnetwork.MultipleCoolerCombined",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class MultipleCoolerCombinedControllerImpl extends AbstractMultiCombinedController implements OpenemsComponent,
        Controller, MultipleCoolerCombinedController {

    private final Logger log = LoggerFactory.getLogger(MultipleCoolerCombinedControllerImpl.class);

    private ConfigMultipleCooler config;

    public MultipleCoolerCombinedControllerImpl() {

        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                MultipleCoolerCombinedController.ChannelId.values());

    }

    @Reference
    ComponentManager cpm;

    @Activate
    void activate(ComponentContext context, ConfigMultipleCooler config) {

        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.useTimer(), config.timerId(),
                config.timeDelta(), ControlType.COOLER, config.coolerIds(),
                config.activationThermometers(), config.activationTemperatures(),
                config.deactivationThermometers(), config.deactivationTemperatures(),
                config.useOverrideValue(), config.overrideValue(),
                this.cpm);
    }

    @Modified
    void modified(ComponentContext context, ConfigMultipleCooler config) {
        this.config = config;
        super.modified(context, config.id(), config.alias(), config.enabled(), config.useTimer(), config.timerId(),
                config.timeDelta(), ControlType.COOLER, config.coolerIds(),
                config.activationThermometers(), config.activationTemperatures(),
                config.deactivationThermometers(), config.deactivationTemperatures(),
                config.useOverrideValue(), config.overrideValue(),
                this.cpm);
    }


    /**
     * MultipleCoolerCombined logic.
     * <p>
     * Each Cooler got a Temperature where they should activate and deactivate.
     * When the activationThermometer reaches the activationTemperature (or exceeds), the Cooler activates / {@link io.openems.edge.heater.api.Heater#getEnableSignalChannel()}
     * will receive a nextWriteValue of true.
     * The Cooler stays active, till the deactivationThermometer reaches the deactivationTemperature (or falls below).
     * When the Deactivation Temperature is reached, the {@link io.openems.edge.heater.api.Heater#getEnableSignalChannel()} won't be set in any way.
     * This way the Cooler stays deactivated, until the activationTemperature is reached again.
     * </p>
     */
    @Override
    public void run() {
        if (this.configurationSuccess) {
            super.abstractRun();
            //Sets both error and ok
            this.setIsCooling(super.isHeatingOrCooling.get());
            this.setHasError(super.heaterError.get());
        } else {
            try {
                this.allocateConfig(ControlType.COOLER, this.config.timerId(), this.config.timeDelta(), this.config.coolerIds(), this.config.activationThermometers(), this.config.activationTemperatures(),
                        this.config.deactivationThermometers(), this.config.deactivationTemperatures(), this.config.useOverrideValue(), this.config.overrideValue());
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
