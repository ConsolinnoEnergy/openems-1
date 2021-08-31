package io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.helperclass;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import org.joda.time.DateTime;

/**
 * The OneChannelLineCooler. This is an extension of the AbstractLineCooler and enables the Ability to write
 * to only one Channel (e.g. EnableSignal).
 */
public class OneChannelLineCooler extends AbstractLineCooler {
    ComponentManager cpm;
    ChannelAddress writeAddress;


    public OneChannelLineCooler(boolean booleanControlled, ChannelAddress writeAddress, ComponentManager cpm) {
        super(booleanControlled, false);
        this.writeAddress = writeAddress;
        this.cpm = cpm;
    }

    /**
     * Starts the Cooling process.
     *
     * @return true if successful.
     * @throws OpenemsError.OpenemsNamedException if e.g. a ChannelAddress or Component does not exist.
     */

    @Override
    public boolean startCooling() throws OpenemsError.OpenemsNamedException {
        int value = FULL_POWER;
        if (this.isBooleanControlled()) {
            value = 1;
        }
        this.isRunning = this.writeToChannel(value);
        return this.isRunning;
    }

    /**
     * Writes the given Value to the stored ChannelAddress.
     *
     * @param value the value written to the ChannelAddress.
     * @return true on success.
     * @throws OpenemsError.OpenemsNamedException if the Channel cannot be found.
     */

    private boolean writeToChannel(double value) throws OpenemsError.OpenemsNamedException {

        Channel<?> channel = this.cpm.getChannel(this.writeAddress);
        if (channel instanceof WriteChannel<?>) {
            ((WriteChannel<?>) channel).setNextWriteValueFromObject(value);
        } else {
            channel.setNextValue(value);
        }
        return true;
    }

    /**
     * Stops the Cooling process.
     *
     * @param lifecycle the currentTime when the Stop Command was set -> prevent hysteresis, by checking the lifecycle
     *                  with the waitTime.
     * @return true on success.
     * @throws OpenemsError.OpenemsNamedException if the Component or Channel could not be found.
     */

    @Override
    public boolean stopCooling(DateTime lifecycle) throws OpenemsError.OpenemsNamedException {
        int value = 0;
        boolean applied = this.writeToChannel(value);
        if (applied) {
            this.isRunning = false;
            this.setLifeCycle(lifecycle);
        }
        return applied;
    }

    @Override
    public void setMaxAndMinValues(Double max, Double min) {
        //not needed
    }

    @Override
    public void onlyWriteMaxMinToLine() {
        //not needed
    }
}
