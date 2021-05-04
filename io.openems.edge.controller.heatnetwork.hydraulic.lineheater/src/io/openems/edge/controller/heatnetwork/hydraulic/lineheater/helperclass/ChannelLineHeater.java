package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import org.joda.time.DateTime;

public class ChannelLineHeater extends AbstractLineHeater {

    private final ChannelAddress writeAddress;
    private final ChannelAddress readAddress;
    private final ChannelAddress maxAddress;
    private final ChannelAddress minAddress;
    private final ComponentManager cpm;
    private Double max;
    private Double min;

    public ChannelLineHeater(boolean booleanControlled, ChannelAddress readAddress, ChannelAddress writeAddress,
                             ChannelAddress maxAddress, ChannelAddress minAddress, ComponentManager cpm) {
        super(booleanControlled, true);
        this.writeAddress = writeAddress;
        this.readAddress = readAddress;
        this.maxAddress = maxAddress;
        this.minAddress = minAddress;
        this.cpm = cpm;

    }

    public ChannelLineHeater(boolean booleanControlled, ChannelAddress writeAddress, ComponentManager cpm) {
        super(booleanControlled, false);
        this.writeAddress = writeAddress;
        this.readAddress = null;
        this.maxAddress = null;
        this.minAddress = null;
        this.cpm = cpm;
    }

    @Override
    public boolean startHeating() throws OpenemsError.OpenemsNamedException {
        if (this.isBooleanControlled() && super.useMinMax == false) {
            return this.writeToChannel(1);
        }
        double lastPowerDouble = getLastPower();
        if (this.isRunning == false || lastPowerDouble < LAST_POWER_CHECK_VALUE) {
            if (this.writeToChannel(FULL_POWER)) {
                this.isRunning = true;
                return true;
            }
        }
        return false;
    }

    private double getLastPower() throws OpenemsError.OpenemsNamedException {
        Object lastPower = readFromChannel();
         if (lastPower instanceof Double) {
            return (Double) lastPower;
        } else {
            return Double.parseDouble(lastPower.toString());
        }
    }

    private Object readFromChannel() throws OpenemsError.OpenemsNamedException {

        return this.cpm.getChannel(readAddress).value().isDefined()
                ? this.cpm.getChannel(readAddress).value().get() : DEFAULT_LAST_POWER_VALUE;
    }

    private boolean writeToChannel(double lastPower) throws OpenemsError.OpenemsNamedException {
        if (this.useMinMax) {
            WriteChannel<Double> doubleMaxWriteChannel = this.cpm.getChannel(this.maxAddress);
            WriteChannel<Double> doubleMinWriteChannel = this.cpm.getChannel(this.minAddress);
            doubleMaxWriteChannel.setNextWriteValue(this.max);
            doubleMinWriteChannel.setNextWriteValue(this.min);
        }
        if (this.isBooleanControlled()) {
            WriteChannel<Boolean> booleanWriteChannel = this.cpm.getChannel(this.writeAddress);
            booleanWriteChannel.setNextWriteValue(lastPower >= 0);
            return true;
        } else {
            Channel<?> writeChannel = this.cpm.getChannel(this.writeAddress);
            if (writeChannel instanceof WriteChannel<?>) {
                ((WriteChannel<?>) writeChannel).setNextWriteValueFromObject(lastPower);
            } else {
                writeChannel.setNextValue(lastPower);
            }
        }
        return false;
    }

    @Override
    public boolean stopHeating(DateTime lifecycle) throws OpenemsError.OpenemsNamedException {

        if (this.isRunning && this.isBooleanControlled()) {
            return this.writeToChannel(-1);
        }

        double lastPower;
        lastPower = (double) this.readFromChannel();
        if (this.isRunning || lastPower > LAST_POWER_CHECK_VALUE) {
            this.writeToChannel(this.isBooleanControlled() ? -1 : 0);
            this.setLifeCycle(lifecycle);
            this.isRunning = false;
            return true;
        }
        return false;
    }

    @Override
    public void setMaxAndMin(Double max, Double min) {
        this.max = max;
        this.min = min;
    }

    @Override
    public void onlySetMaxMin() {
        try {
            WriteChannel<Double> doubleMaxWriteChannel = this.cpm.getChannel(this.maxAddress);
            WriteChannel<Double> doubleMinWriteChannel = this.cpm.getChannel(this.minAddress);
            doubleMaxWriteChannel.setNextWriteValue(this.max);
            doubleMinWriteChannel.setNextWriteValue(this.min);
        } catch (Exception ignored) {
        }
    }
}
