package io.openems.edge.heater.analogue.component;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heater.analogue.ControlType;
import io.openems.edge.bridge.lucidcontrol.api.LucidControlDeviceOutput;
import org.osgi.service.cm.ConfigurationException;

public class AnalogueHeaterLucidControl extends AbstractAnalogueHeaterComponent implements AnalogueHeaterComponent {
    public AnalogueHeaterLucidControl(ComponentManager cpm, String analogueId, int maxPowerKw,
                                      ControlType controlType, int defaultMinPower) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        OpenemsComponent component = cpm.getComponent(analogueId);
        if (component instanceof LucidControlDeviceOutput) {
            ChannelAddress address = new ChannelAddress(analogueId, ((LucidControlDeviceOutput) component).getPercentageChannel().channelId().id());
            super.initialize(cpm, address, controlType, maxPowerKw, defaultMinPower);
        } else {
            throw new ConfigurationException("Constructor AnalogueHeaterLucidControl", "Configured Device is not an instance of LucidControlDeviceOutput");
        }
    }
}
