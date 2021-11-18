package io.openems.edge.io.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.Relay;


public class DummyRelay extends AbstractOpenemsComponent implements Relay, OpenemsComponent {

    public DummyRelay(String id) {
        super(OpenemsComponent.ChannelId.values(),
                Relay.ChannelId.values()
        );
        super.activate(null, id, "", true);

    }
}
