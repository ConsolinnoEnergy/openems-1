package io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.heatsystem.components.HydraulicComponent;

public class ChannelLineController extends AbstractLineController {

    private final ChannelAddress writeAddress;
    private final ChannelAddress readAddress;
    private final ChannelAddress maxAddress;
    private final ChannelAddress minAddress;
    private Double max;
    private Double min;

    public ChannelLineController(boolean booleanControlled, ChannelAddress readAddress, ChannelAddress writeAddress,
                                 ChannelAddress maxAddress, ChannelAddress minAddress, ComponentManager cpm, boolean useMinMax) {
        super(booleanControlled, useMinMax, cpm);
        this.writeAddress = writeAddress;
        this.readAddress = readAddress;
        this.maxAddress = maxAddress;
        this.minAddress = minAddress;
    }

    @Override
    public boolean startProcess() throws OpenemsError.OpenemsNamedException {
        if (this.useMinMax) {
            WriteChannel<Double> doubleMaxWriteChannel = this.cpm.getChannel(this.maxAddress);
            WriteChannel<Double> doubleMinWriteChannel = this.cpm.getChannel(this.minAddress);
            doubleMaxWriteChannel.setNextWriteValue(this.max);
            doubleMinWriteChannel.setNextWriteValue(this.min);
        }
        double currentPowerDouble = this.getLastPower();
        if (this.startConditionsApply(currentPowerDouble)) {
            if (this.writeToChannel(this.isBooleanControlled() ? 1 : HydraulicComponent.DEFAULT_MAX_POWER_VALUE)) {
                this.isRunning = true;
                this.previouslyCheckedPowerLevel = currentPowerDouble;
                return true;
            }
        }
        return false;
    }

    private boolean startConditionsApply(double currentPowerDouble) {

        return this.isRunning == false || currentPowerDouble < previouslyCheckedPowerLevel
                || currentPowerDouble < HydraulicComponent.DEFAULT_MAX_POWER_VALUE && !this.useMinMax
                || (this.useMinMax && currentPowerDouble != this.max) || currentPowerDouble == DEFAULT_LAST_POWER_VALUE;
    }

    private double getLastPower() throws OpenemsError.OpenemsNamedException {
        Object lastPower = this.readFromChannel();
        if (lastPower instanceof Double) {
            return (Double) lastPower;
        } else {
            return Double.parseDouble(lastPower.toString());
        }
    }

    private Object readFromChannel() throws OpenemsError.OpenemsNamedException {

        return this.cpm.getChannel(this.readAddress).value().isDefined()
                ? this.cpm.getChannel(this.readAddress).value().get() : DEFAULT_LAST_POWER_VALUE;
    }

    private boolean writeToChannel(double lastPower) throws OpenemsError.OpenemsNamedException {
        if (this.isBooleanControlled()) {
            WriteChannel<Boolean> booleanWriteChannel = this.cpm.getChannel(this.writeAddress);
            booleanWriteChannel.setNextWriteValue(lastPower > 0);
        } else {
            Channel<?> writeChannel = this.cpm.getChannel(this.writeAddress);
            if (writeChannel instanceof WriteChannel<?>) {
                ((WriteChannel<?>) writeChannel).setNextWriteValueFromObject(lastPower);
            } else {
                writeChannel.setNextValue(lastPower);
            }
        }
        return true;
    }

    @Override
    public boolean stopProcess() throws OpenemsError.OpenemsNamedException {

        double currentPower;
        currentPower = Double.parseDouble(this.readFromChannel().toString());
        if (this.stopConditionsApply(currentPower)) {
            this.writeToChannel(this.isBooleanControlled() ? -1 : 0);
            this.isRunning = false;
            this.previouslyCheckedPowerLevel = currentPower;
            return true;
        }
        return false;
    }

    private boolean stopConditionsApply(double currentPower) {

        return this.isRunning || currentPower > previouslyCheckedPowerLevel
                || currentPower > HydraulicComponent.DEFAULT_MIN_POWER_VALUE && !this.useMinMax
                || (this.useMinMax && (currentPower != this.min)) || currentPower == DEFAULT_LAST_POWER_VALUE;
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
