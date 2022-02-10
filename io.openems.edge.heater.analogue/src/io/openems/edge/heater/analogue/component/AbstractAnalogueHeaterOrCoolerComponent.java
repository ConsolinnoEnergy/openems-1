package io.openems.edge.heater.analogue.component;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;

/**
 * The AbstractAnalogueHeaterComponent. This Component is a HelperClass for the {@link io.openems.edge.heater.analogue.AnalogueHeater}
 * It implements the {@link AnalogueHeaterOrCoolerComponent} interface, providing the Methods the AnalogueHeater needs to set a
 * PowerValue or get a current PowerLevelValue (just bc. u set a Value doesn't have to mean, the AnalogueComponent reacts correctly
 * -> build in Debug)
 */
public abstract class AbstractAnalogueHeaterOrCoolerComponent implements AnalogueHeaterOrCoolerComponent {

    protected ComponentManager cpm;
    protected ChannelAddress address;
    protected ControlType controlType;
    protected int powerApplied = 0;
    protected int minPower = 100;
    private static final int HUNDRED_PERCENT = 100;
    //only important when controlType is KW and not % -> always convert to %
    protected int maxPower = 0;
    //set this if channel is from type Thousandth and not Percent
    protected int multiplier = 1;

    /**
     * Initializes the AbstractAnalogueHeater. Allows to Create an Instance of this and later on, setting the variables,
     * the Component needs to function properly.
     *
     * @param cpm         the ComponentManager passed by the {@link io.openems.edge.heater.analogue.AnalogueHeater}
     * @param address     the ChannelAddress, determined by the {@link io.openems.edge.heater.analogue.AnalogueType} and ComponentId
     * @param controlType the ControlType -> Percent or KW -> needs to be set for Conversion ->
     * @param maxPower    the Maximum available Power
     * @param minPower    the min Power
     */
    protected void initialize(ComponentManager cpm, ChannelAddress address, ControlType controlType, int maxPower, int minPower) {
        this.cpm = cpm;
        this.address = address;
        this.controlType = controlType;
        this.maxPower = Math.max(maxPower, 0);
        this.minPower = Math.max(minPower, 0);
    }

    /**
     * Starts the Heating Process with a given PowerValue. This can either be percent or a KW value depending on the
     * {@link ControlType}.
     *
     * @param powerToApply the powerValue that will be applied
     * @throws OpenemsError.OpenemsNamedException if the ChannelAddress couldn't be found
     */

    @Override
    public void startProcess(int powerToApply) throws OpenemsError.OpenemsNamedException {
        this.writeToChannelAddress(powerToApply);
    }

    /**
     * Writes a Value to the ChannelAddress. Either called by the {@link #startProcess(int)} or {@link #stopProcess()}
     * process.
     * NOTE: The Multiplier is usually 1 but on instances such as PWM or AIO the Multiplier is 10, since it expects
     * the value in thousandth not percent.
     *
     * @param powerToApply the value written to the ChannelAddress
     * @throws OpenemsError.OpenemsNamedException when the ChannelAddress could not be found.
     */
    protected void writeToChannelAddress(int powerToApply) throws OpenemsError.OpenemsNamedException {
        int power = powerToApply;
        if (this.controlType.equals(ControlType.KW)) {
            power = Math.min(power, this.maxPower);
            power = (power * HUNDRED_PERCENT / this.maxPower);
        }
        power = power * this.multiplier;

        Channel<?> channel = this.cpm.getChannel(this.address);
        if (channel instanceof WriteChannel<?>) {
            ((WriteChannel<?>) channel).setNextWriteValueFromObject(power);
        } else {
            channel.setNextValue(powerToApply);
        }
        this.powerApplied = powerToApply;

    }

    /**
     * Stops the Heating Process with the MinPowerValue.
     *
     * @throws OpenemsError.OpenemsNamedException if the ChannelAddress cannot be found.
     */
    @Override
    public void stopProcess() throws OpenemsError.OpenemsNamedException {
        this.writeToChannelAddress(this.minPower);
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
        return this.powerApplied;
    }
}
