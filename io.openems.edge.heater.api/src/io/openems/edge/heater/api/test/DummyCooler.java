package io.openems.edge.heater.api.test;


import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heater.api.Cooler;
import io.openems.edge.heater.api.Heater;

/**
 * A DummyCooler used in UnitTests for test cases.
 */
public class DummyCooler extends AbstractOpenemsComponent implements OpenemsComponent, Cooler, Heater {
    public DummyCooler(String id) {
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