package io.openems.edge.thermometer.api.test;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerThreshold;

public class DummyThermometer extends AbstractOpenemsComponent implements Thermometer, ThermometerThreshold, OpenemsComponent {

    public DummyThermometer(String id) {
        super(
                OpenemsComponent.ChannelId.values(),
                Thermometer.ChannelId.values(),
                ThermometerThreshold.ChannelId.values()

        );
        for (Channel<?> channel : this.channels()) {
            channel.nextProcessImage();
        }
        super.activate(null, id, "", true);
    }
}
