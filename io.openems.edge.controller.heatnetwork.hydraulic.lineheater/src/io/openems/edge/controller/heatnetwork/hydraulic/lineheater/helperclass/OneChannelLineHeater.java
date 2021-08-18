package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import org.joda.time.DateTime;

public class OneChannelLineHeater extends AbstractLineHeater {
    ComponentManager cpm;
    ChannelAddress writeAddress;


    public OneChannelLineHeater(boolean booleanControlled, ChannelAddress writeAddress, ComponentManager cpm) {
        super(booleanControlled, false);
        this.writeAddress = writeAddress;
        this.cpm = cpm;
    }


    @Override
    public boolean startHeating() throws OpenemsError.OpenemsNamedException {
        int value = FULL_POWER;
        if (this.isBooleanControlled()) {
            value = 1;
        }
        this.isRunning = this.writeToChannel(value);
        return this.isRunning;
    }

    private boolean writeToChannel(double value) throws OpenemsError.OpenemsNamedException {

        Channel<?> channel = this.cpm.getChannel(this.writeAddress);
        if (channel instanceof WriteChannel<?>) {
            ((WriteChannel<?>) channel).setNextWriteValueFromObject(value);
        } else {
            channel.setNextValue(value);
        }
        return true;
    }

    @Override
    public boolean stopHeating(DateTime lifecycle) throws OpenemsError.OpenemsNamedException {
        int value = 0;
        if (this.isBooleanControlled()) {
            value = -1;
        }
        boolean applied = this.writeToChannel(value);
        if (applied) {
            this.isRunning = false;
            this.setLifeCycle(lifecycle);
        }
        return applied;
    }

    @Override
    public void setMaxAndMin(Double max, Double min) {
    //not needed
    }

    @Override
    public void onlySetMaxMin() {
    //not needed
    }
}
