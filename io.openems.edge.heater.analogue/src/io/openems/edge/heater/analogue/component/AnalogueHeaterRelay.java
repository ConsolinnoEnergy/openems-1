package io.openems.edge.heater.analogue.component;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heater.analogue.ControlType;
import io.openems.edge.relay.api.Relay;
import org.osgi.service.cm.ConfigurationException;

public class AnalogueHeaterRelay extends AbstractAnalogueHeaterComponent implements AnalogueHeaterComponent {
    private boolean isConsolinnoRelay = false;
    private String deviceId;

    public AnalogueHeaterRelay(ComponentManager cpm, String analogueId, int maxPowerKw, ControlType controlType, int defaultMinPower) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        OpenemsComponent component = cpm.getComponent(analogueId);
        ChannelAddress address;
        if (component instanceof Relay) {
            this.isConsolinnoRelay = true;
            this.deviceId = analogueId;
            address = new ChannelAddress(analogueId, ((Relay) component).getRelaysWriteChannel().channelId().id());
        } else {
            throw new ConfigurationException("AnalogueHeaterRelay Contructor", "Configured Relay not an instance of Relay: " + analogueId);
        }
        super.initialize(cpm, address, controlType, maxPowerKw, defaultMinPower);
    }

    @Override
    public int getCurrentPowerApplied() throws OpenemsError.OpenemsNamedException {
        if (this.isConsolinnoRelay) {
            Channel<Boolean> channel = ((Relay) this.cpm.getComponent(this.deviceId)).getRelaysReadChannel();
            if (channel.value().orElse(false)) {
                return super.getCurrentPowerApplied();
            } else {
                return 0;
            }
        } else {
            return super.getCurrentPowerApplied();
        }
    }
}
