package io.openems.edge.heater.decentralized.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.heater.decentralized.api.DecentralizedHeater;


public class DummyDecentralizedHeater extends AbstractOpenemsComponent implements OpenemsComponent, DecentralizedHeater, ExceptionalState {


    public DummyDecentralizedHeater(String id) {

        super(OpenemsComponent.ChannelId.values(),
                DecentralizedHeater.ChannelId.values(), ExceptionalState.ChannelId.values()
        );
               super.activate(null, id, null, true);
    }
}
