package io.openems.edge.heater.analogue.component;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.heater.analogue.ControlType;

public abstract class AbstractAnalogueHeaterComponent implements AnalogueHeaterComponent {

    protected ComponentManager cpm;
    protected ChannelAddress address;
    protected ControlType controlType;
    protected int powerApplied = 0;
    protected int minPower = 100;
    //only important when controlType is KW and not % -> always convert to %
    protected int maxPower = 0;
    //set this if channel is from type Thousandth and not Percent
    protected int multiplier = 1;

    protected void initialize(ComponentManager cpm, ChannelAddress address, ControlType controlType, int maxPower, int minPower) {
        this.cpm = cpm;
        this.address = address;
        this.controlType = controlType;
        this.maxPower = Math.max(maxPower, 0);
        this.minPower = Math.max(minPower, 0);
    }

    @Override
    public void startHeating(int powerToApply) throws OpenemsError.OpenemsNamedException {
        this.writeToChannelAddress(powerToApply);
    }

    protected void writeToChannelAddress(int powerToApply) throws OpenemsError.OpenemsNamedException {
        int power = powerToApply;
        if (this.controlType.equals(ControlType.KW)) {
            power = Math.min(power, this.maxPower);
            power = (power * 100 / this.maxPower);
        }
        power = power * multiplier;

        Channel<?> channel = this.cpm.getChannel(this.address);
        if (channel instanceof WriteChannel<?>) {
            ((WriteChannel<?>) channel).setNextWriteValueFromObject(power);
        } else {
            channel.setNextValue(powerToApply);
        }
        this.powerApplied = powerToApply;

    }


    @Override
    public void stopHeating() throws OpenemsError.OpenemsNamedException {
        this.writeToChannelAddress(this.minPower);
    }

    //Overwrite if necessary
    @Override
    public int getCurrentPowerApplied() throws OpenemsError.OpenemsNamedException {
        return this.powerApplied;
    }
}
