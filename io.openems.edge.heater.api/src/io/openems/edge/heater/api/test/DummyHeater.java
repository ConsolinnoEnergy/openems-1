package io.openems.edge.heater.api.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heater.api.Heater;


/**
 * A DummyHeater used in UnitTests for test cases.
 */
public class DummyHeater extends AbstractOpenemsComponent implements Heater {
    public DummyHeater(String id) {
        super(
                OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values()
        );
        for (Channel<?> channel : this.channels()) {
            channel.nextProcessImage();
        }
        super.activate(null, id, "", true);
    }

}

