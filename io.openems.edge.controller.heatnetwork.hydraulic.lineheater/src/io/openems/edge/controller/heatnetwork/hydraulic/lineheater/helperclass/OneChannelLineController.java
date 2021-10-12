package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.heatsystem.components.HydraulicComponent;

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
        this.isRunning = this.writeToChannel(HydraulicComponent.DEFAULT_MAX_POWER_VALUE);
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
        boolean applied = this.writeToChannel(this.isBooleanControlled() ? 0 : HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
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
