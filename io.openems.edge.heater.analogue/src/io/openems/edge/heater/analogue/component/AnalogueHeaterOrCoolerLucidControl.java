package io.openems.edge.heater.analogue.component;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.bridge.lucidcontrol.api.LucidControlDeviceOutput;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heater.analogue.ControlType;
import org.osgi.service.cm.ConfigurationException;

/**
 * The AnalogueHeaterLucidControl. An Extension of the {@link AbstractAnalogueHeaterOrCoolerComponent}
 * It gets the {@link LucidControlDeviceOutput},
 * the Corresponding ChannelAddress and initializes the abstractAnalogueHeaterComponent.
 */
public class AnalogueHeaterOrCoolerLucidControl extends AbstractAnalogueHeaterOrCoolerComponent implements AnalogueHeaterOrCoolerComponent {
    public AnalogueHeaterOrCoolerLucidControl(ComponentManager cpm, String analogueId, int maxPowerKw,
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