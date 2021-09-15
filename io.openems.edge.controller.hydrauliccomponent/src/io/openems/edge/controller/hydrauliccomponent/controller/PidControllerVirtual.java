package io.openems.edge.controller.hydrauliccomponent.controller;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

import io.openems.edge.controller.hydrauliccomponent.api.PidHydraulicController;
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
 *
 */

@Designate(ocd = VirtualPidConfig.class, factory = true)
@Component(name = "PidControllerVirtual", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class PidControllerVirtual extends AbstractOpenemsComponent implements OpenemsComponent, PidHydraulicController {

    private Logger log = LoggerFactory.getLogger(PidControllerVirtual.class);

    public PidControllerVirtual() {
        super(OpenemsComponent.ChannelId.values(),
                PidHydraulicController.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, VirtualPidConfig config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Modified
    void modified(ComponentContext context, VirtualPidConfig config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public double getCurrentPositionOfComponent() {
        return 0;
    }
}
