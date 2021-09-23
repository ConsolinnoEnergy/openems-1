package io.openems.edge.controller.hydrauliccomponent.api;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * A Dummy HydraulicController used for UnitTests.
 */
public class DummyHydraulicController extends AbstractOpenemsComponent implements OpenemsComponent, HydraulicController {
    public DummyHydraulicController(String id) {
        super(
                OpenemsComponent.ChannelId.values(),
                HydraulicController.ChannelId.values()
        );
        for (Channel<?> channel : this.channels()) {
            channel.nextProcessImage();
        }
        super.activate(null, id, "", true);
    }

    @Override
    public double getCurrentPositionOfComponent() {
        return 0;
    }
}
