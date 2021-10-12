package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;

public class OneChannelLineController extends AbstractLineController {
    ComponentManager cpm;
    ChannelAddress writeAddress;


    public OneChannelLineController(boolean booleanControlled, ChannelAddress writeAddress, ComponentManager cpm) {
        super(booleanControlled, false);
        this.writeAddress = writeAddress;
        this.cpm = cpm;
    }


    @Override
    public boolean startProcess() throws OpenemsError.OpenemsNamedException {
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
    public boolean stopProcess() throws OpenemsError.OpenemsNamedException {
        int value = 0;
        boolean applied = this.writeToChannel(value);
        if (applied) {
            this.isRunning = false;
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
