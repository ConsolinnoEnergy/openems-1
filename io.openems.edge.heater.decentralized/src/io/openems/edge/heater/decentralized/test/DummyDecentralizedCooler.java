package io.openems.edge.heater.decentralized.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.decentralized.api.DecentralizedCooler;

public class DummyDecentralizedCooler extends AbstractOpenemsComponent implements OpenemsComponent, DecentralizedCooler, ExceptionalState, Heater {


    public DummyDecentralizedCooler(String id) {

        super(OpenemsComponent.ChannelId.values(),
                DecentralizedCooler.ChannelId.values(),
                ExceptionalState.ChannelId.values(),
                Heater.ChannelId.values()
        );
        super.activate(null, id, null, true);
    }
}
