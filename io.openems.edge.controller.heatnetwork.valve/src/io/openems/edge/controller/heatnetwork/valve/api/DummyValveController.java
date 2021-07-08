package io.openems.edge.controller.heatnetwork.valve.api;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyValveController extends AbstractOpenemsComponent implements OpenemsComponent, ValveController {
    public DummyValveController(String id) {
        super(
                OpenemsComponent.ChannelId.values(),
                ValveController.ChannelId.values()
        );
        for (Channel<?> channel : this.channels()) {
            channel.nextProcessImage();
        }
        super.activate(null, id, "", true);
    }

    @Override
    public double getCurrentPositionOfValve() {
        return 0;
    }
}
