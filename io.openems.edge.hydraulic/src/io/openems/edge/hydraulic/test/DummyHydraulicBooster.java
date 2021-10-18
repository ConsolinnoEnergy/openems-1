package io.openems.edge.hydraulic.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.hydraulic.api.HeatBooster;

public class DummyHydraulicBooster extends AbstractOpenemsComponent implements HeatBooster {
    public DummyHydraulicBooster(String id) {

        super(OpenemsComponent.ChannelId.values(), HeatBooster.ChannelId.values());
        super.activate(null, id, "", true);
    }
}
