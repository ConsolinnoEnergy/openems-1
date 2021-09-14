package io.openems.edge.heater.decentral.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.heater.decentral.api.DecentralHeater;


public class DummyDecentralizedHeater extends AbstractOpenemsComponent implements OpenemsComponent, DecentralHeater, ExceptionalState {


    public DummyDecentralizedHeater(String id) {

        super(OpenemsComponent.ChannelId.values(),
                DecentralHeater.ChannelId.values(), ExceptionalState.ChannelId.values()
        );
               super.activate(null, id, null, true);
    }
}
