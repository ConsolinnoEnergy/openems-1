package io.openems.edge.utility.virtualcomponent;

import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Optional;

/**
 * This component represents a virtual component.
 * This can be used to monitor an optimization before applying it at the real device.
 * Or alternatively use this to temporarily save the optimization part and remap the optimization to different "real" components.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Utility.Virtual.Component", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
property = { EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE } )

public class VirtualComponentOptimizedImpl extends AbstractOpenemsComponent implements OpenemsComponent, VirtualComponentOptimized, EventHandler {


    public VirtualComponentOptimizedImpl() {
        super(VirtualComponentOptimized.ChannelId.values(),
                OpenemsComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if(event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            Optional<Boolean> enabled = this.getEnableSignal().getNextWriteValueAndReset();
            if (enabled.isPresent()) {
                this.getEnableSignal().setNextValue(enabled.get());
            } else {
                this.getEnableSignal().setNextValue(false);
            }
        }
    }
}
