package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api.HydraulicLineController;

public class DummyHydraulicLineController extends AbstractOpenemsComponent implements OpenemsComponent, HydraulicLineController {

    public DummyHydraulicLineController(String id) {
        super(OpenemsComponent.ChannelId.values(),
                HydraulicLineController.ChannelId.values());
        super.activate(null, id, "", true);
    }


}
