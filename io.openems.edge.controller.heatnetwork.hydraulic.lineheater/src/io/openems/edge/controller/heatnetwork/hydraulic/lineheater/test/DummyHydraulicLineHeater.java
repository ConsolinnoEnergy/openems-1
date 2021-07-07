package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api.HydraulicLineHeater;

public class DummyHydraulicLineHeater extends AbstractOpenemsComponent implements OpenemsComponent, HydraulicLineHeater {

    public DummyHydraulicLineHeater(String id) {
        super(OpenemsComponent.ChannelId.values(),
                HydraulicLineHeater.ChannelId.values());
        super.activate(null, id, "", true);
    }


}
