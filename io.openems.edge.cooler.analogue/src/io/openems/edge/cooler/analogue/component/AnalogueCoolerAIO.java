package io.openems.edge.cooler.analogue.component;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.cooler.analogue.ControlType;
import io.openems.edge.io.api.AnalogInputOutput;

public class AnalogueCoolerAIO extends AbstractAnalogueCoolerComponent implements AnalogueCoolerComponent {
    private final String analogueId;

    public AnalogueCoolerAIO(ComponentManager cpm, String analogueId, int maxPowerKw, ControlType controlType, int defaultMinPower) throws OpenemsError.OpenemsNamedException {
        OpenemsComponent component = cpm.getComponent(analogueId);
        if (component instanceof AnalogInputOutput) {
            ChannelAddress address = new ChannelAddress(analogueId, ((AnalogInputOutput) component).setPercentChannel().channelId().id());
            super.initialize(cpm, address, controlType, maxPowerKw, defaultMinPower);
        }
        this.analogueId = analogueId;
        super.multiplier = 10;

    }

    @Override
    public int getCurrentPowerApplied() throws OpenemsError.OpenemsNamedException {
        return Math.max(Math.round(((AnalogInputOutput) super.cpm.getComponent(this.analogueId)).getPercentValue()), 0);
    }

}
