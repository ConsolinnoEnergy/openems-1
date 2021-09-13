package io.openems.edge.heater.analogue.component;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.consolinno.pwm.api.Pwm;
import io.openems.edge.heater.analogue.ControlType;
import io.openems.edge.pwm.device.api.PwmPowerLevelChannel;
import org.osgi.service.cm.ConfigurationException;

/**
 * The AnalogueHeaterPwm. An Extension of the {@link AbstractAnalogueHeaterComponent}
 * It provides the ability to use a PWM Device as a Heater.
 * Since there are atm 2 different PWM Interfaces (in future obsolete -> only {@link Pwm}) those 2 cases will be considered.
 * The {@link Pwm} expectes thousandth values and therefore the multiplier is set to 10, while the {@link PwmPowerLevelChannel}
 * expects Percent values (multiplier does not change).
 */
public class AnalogueHeaterPWM extends AbstractAnalogueHeaterComponent implements AnalogueHeaterComponent {
    private boolean isConsolinnoPwm;
    private String deviceId;

    public AnalogueHeaterPWM(ComponentManager cpm, String analogueId, int maxPowerKw, ControlType controlType, int defaultMinPower) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        OpenemsComponent component = cpm.getComponent(analogueId);
        ChannelAddress address;
        if (component instanceof Pwm) {
            isConsolinnoPwm = true;
            address = new ChannelAddress(analogueId, ((Pwm) component).getWritePwmPowerLevelChannel().channelId().id());
            super.multiplier = 10;
        } else if (component instanceof PwmPowerLevelChannel) {
            this.deviceId = analogueId;
            address = new ChannelAddress(analogueId, ((PwmPowerLevelChannel) component).getPwmPowerLevelChannel().channelId().id());
        } else {
            throw new ConfigurationException("AnalogueHeaterPWM Constructor", "Selected Device not an instance of PWM : " + analogueId);
        }
        super.initialize(cpm, address, controlType, maxPowerKw, defaultMinPower);
    }
    /**
     * Gets the currently Applied Power to the analogueDevice.
     * The Value will always be a percent Value.
     * @return the percentPowerValue Applied
     * @throws OpenemsError.OpenemsNamedException if ChannelAddress not found.
     */
    @Override
    public int getCurrentPowerApplied() throws OpenemsError.OpenemsNamedException {
        if (this.isConsolinnoPwm) {
            return Math.max(Math.round(((Pwm) super.cpm.getComponent(deviceId)).getPowerLevelValue()), 0);
        } else {
            return super.getCurrentPowerApplied();
        }
    }
}
