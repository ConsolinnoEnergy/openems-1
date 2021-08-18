package io.openems.edge.thermometer.api.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerVirtual;

public class DummyVirtualThermometer extends AbstractOpenemsComponent implements OpenemsComponent, ThermometerVirtual, Thermometer {

    public DummyVirtualThermometer(String id) {
        super(OpenemsComponent.ChannelId.values(),
                ThermometerVirtual.ChannelId.values(),
                Thermometer.ChannelId.values());
        super.activate(null, id, "", true);
    }

}
