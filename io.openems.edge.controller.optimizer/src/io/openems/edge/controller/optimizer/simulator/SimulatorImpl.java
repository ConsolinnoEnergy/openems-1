package io.openems.edge.controller.optimizer.simulator;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(name = "io.openems.edge.controller.optimizer.simulator", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

public class SimulatorImpl extends AbstractOpenemsComponent implements OpenemsComponent, Simulator {
    public SimulatorImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Simulator.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) {
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();

    }

    @Override
    public String debugLog() {
        return "Write " + getWriteString() +  getWriteFloatString() + " Enable " + getEnableString();
    }
}
