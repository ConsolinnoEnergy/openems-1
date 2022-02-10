package io.openems.edge.heater.analogue.component;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.Pwm;
import org.osgi.service.cm.ConfigurationException;

/**
 * The AnalogueHeaterPwm. An Extension of the {@link AbstractAnalogueHeaterOrCoolerComponent}
 * It provides the ability to use a PWM Device as a Heater.
 * expects Percent values (multiplier does not change).
 */
public class AnalogueHeaterOrCoolerPwm extends AbstractAnalogueHeaterOrCoolerComponent implements AnalogueHeaterOrCoolerComponent {

    private final String componentId;

    public AnalogueHeaterOrCoolerPwm(ComponentManager cpm, String analogueId, int maxPowerKw, ControlType controlType, int defaultMinPower) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        OpenemsComponent component = cpm.getComponent(analogueId);
        ChannelAddress address;
        if (component instanceof Pwm) {
            this.componentId = analogueId;
            address = new ChannelAddress(analogueId, ((Pwm) component).getWritePwmPowerLevelChannel().channelId().id());
            super.multiplier = 10;
        } else {
            throw new ConfigurationException("AnalogueHeaterPWM Constructor", "Selected Device not an instance of PWM : " + analogueId);
        }
        super.initialize(cpm, address, controlType, maxPowerKw, defaultMinPower);
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
        return Math.max(Math.round(((Pwm) super.cpm.getComponent(this.componentId)).getPowerLevelPercentValue()), 0);
    }
}
