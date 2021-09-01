package io.openems.edge.heater.analogue.component;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.Pwm;
import io.openems.edge.heater.analogue.ControlType;
import org.osgi.service.cm.ConfigurationException;

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
        } else if (component instanceof Pwm) {
            this.deviceId = analogueId;
            address = new ChannelAddress(analogueId, ((Pwm) component).getWritePwmPowerLevelChannel().channelId().id());
        } else {
            throw new ConfigurationException("AnalogueHeaterPWM Constructor", "Selected Device not an instance of PWM : " + analogueId);
        }
        super.initialize(cpm, address, controlType, maxPowerKw, defaultMinPower);
    }

    @Override
    public int getCurrentPowerApplied() throws OpenemsError.OpenemsNamedException {
        if (this.isConsolinnoPwm) {
            return Math.max(Math.round(((Pwm) super.cpm.getComponent(deviceId)).getPowerLevelValue()), 0);
        } else {
            return super.getCurrentPowerApplied();
        }
    }
}
