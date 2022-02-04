package io.openems.edge.heater.analogue.component;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;


import io.openems.edge.heater.analogue.ControlType;
import io.openems.edge.io.api.AnalogInputOutput;
import org.osgi.service.cm.ConfigurationException;

/**
 * The AnalogueHeaterAIO. An Extending class of the {@link AbstractAnalogueHeaterOrCoolerComponent}.
 * It is responsible for getting the {@link AnalogInputOutput} component and the ChannelAddress.
 * Initializing the AbstractAnalogueHeaterComponent and Returning the correct currently applied PowerValue.
 */
public class AnalogueHeaterOrCoolerAio extends AbstractAnalogueHeaterOrCoolerComponent implements AnalogueHeaterOrCoolerComponent {
    private final String analogueId;

    public AnalogueHeaterOrCoolerAio(ComponentManager cpm, String analogueId, int maxPowerKw, ControlType controlType, int defaultMinPower) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        OpenemsComponent component = cpm.getComponent(analogueId);
        if (component instanceof AnalogInputOutput) {
            ChannelAddress address = new ChannelAddress(analogueId, ((AnalogInputOutput) component).setWriteThousandthChannel().channelId().id());
            super.initialize(cpm, address, controlType, maxPowerKw, defaultMinPower);
        } else {
            throw new ConfigurationException("AnalogueHeaterAIO", "The Component is not an instance of AIO, please check the id: " + analogueId);
        }
        this.analogueId = analogueId;
        super.multiplier = 10;

    }

    /**
     * Gets the currently Applied Power to the analogueDevice.
     * The Value will always be a percent Value.
     *
     * @return the percentPowerValue Applied
     * @throws OpenemsError.OpenemsNamedException if ChannelAddress not found.
     */

    @Override
    public int getCurrentPowerApplied() throws OpenemsError.OpenemsNamedException {
        return Math.max(Math.round(((AnalogInputOutput) super.cpm.getComponent(this.analogueId)).getPercentValue()), 0);
    }

}
