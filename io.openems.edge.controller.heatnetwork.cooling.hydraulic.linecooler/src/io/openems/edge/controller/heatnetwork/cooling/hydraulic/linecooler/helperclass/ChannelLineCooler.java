package io.openems.edge.controller.heatnetwork.cooling.hydraulic.linecooler.helperclass;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import org.joda.time.DateTime;

/**
 * The Channel Line Cooler. It stores 4 ChannelAddresses and Writes, depending on the configuration, to the min and max
 * Address only or the WriteAddress too.
 */
public class ChannelLineCooler extends AbstractLineCooler {

    private final ChannelAddress writeAddress;
    private final ChannelAddress readAddress;
    private final ChannelAddress maxAddress;
    private final ChannelAddress minAddress;
    private final ComponentManager cpm;
    private Double max;
    private Double min;

    public ChannelLineCooler(boolean booleanControlled, ChannelAddress readAddress, ChannelAddress writeAddress,
                             ChannelAddress maxAddress, ChannelAddress minAddress, ComponentManager cpm, boolean useMinMax) {
        super(booleanControlled, useMinMax);
        this.writeAddress = writeAddress;
        this.readAddress = readAddress;
        this.maxAddress = maxAddress;
        this.minAddress = minAddress;
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
        double currentPowerDouble = this.getLastPower();
        if (this.isRunning == false || currentPowerDouble < previouslyCheckedPowerLevel) {
            if (this.writeToChannel(this.isBooleanControlled() ? 1 : FULL_POWER)) {
                this.isRunning = true;
                this.previouslyCheckedPowerLevel = currentPowerDouble;
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the LastPower Value from the readChannel.
     *
     * @return the lastPowerValue.
     * @throws OpenemsError.OpenemsNamedException if the Channel cannot be found.
     */

    private double getLastPower() throws OpenemsError.OpenemsNamedException {
        Object lastPower = this.readFromChannel();
        if (lastPower instanceof Double) {
            return (Double) lastPower;
        } else {
            return Double.parseDouble(lastPower.toString());
        }
    }

    /**
     * Gets the Object from the stores channel {@link #readAddress}.
     *
     * @return the Value of the Channel as Obj.
     * @throws OpenemsError.OpenemsNamedException if channel cannot be found.
     */
    private Object readFromChannel() throws OpenemsError.OpenemsNamedException {

        return this.cpm.getChannel(this.readAddress).value().isDefined()
                ? this.cpm.getChannel(this.readAddress).value().get() : DEFAULT_LAST_POWER_VALUE;
    }

    /**
     * Writes the setPoint Power to the writeChannel Address, as well as the stored MinMax Value if configured.
     *
     * @param setPointPower the Power that will be written to the WriteChannel
     * @return true n success.
     * @throws OpenemsError.OpenemsNamedException if the wirtechannel couldn't be found.
     */

    private boolean writeToChannel(double setPointPower) throws OpenemsError.OpenemsNamedException {
        if (this.useMinMax) {
            WriteChannel<Double> doubleMaxWriteChannel = this.cpm.getChannel(this.maxAddress);
            WriteChannel<Double> doubleMinWriteChannel = this.cpm.getChannel(this.minAddress);
            doubleMaxWriteChannel.setNextWriteValue(this.max);
            doubleMinWriteChannel.setNextWriteValue(this.min);
        }
        if (this.isBooleanControlled()) {
            WriteChannel<Boolean> booleanWriteChannel = this.cpm.getChannel(this.writeAddress);
            booleanWriteChannel.setNextWriteValue(setPointPower > 0);
        } else {
            Channel<?> writeChannel = this.cpm.getChannel(this.writeAddress);
            if (writeChannel instanceof WriteChannel<?>) {
                ((WriteChannel<?>) writeChannel).setNextWriteValueFromObject(setPointPower);
            } else {
                writeChannel.setNextValue(setPointPower);
            }
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

        double lastPower;
        lastPower = (double) this.readFromChannel();
        if (this.isRunning || lastPower > previouslyCheckedPowerLevel) {
            this.writeToChannel(this.isBooleanControlled() ? -1 : 0);
            this.setLifeCycle(lifecycle);
            this.isRunning = false;
            this.previouslyCheckedPowerLevel = lastPower;
            return true;
        }
        return false;
    }

    /**
     * Sets the Max and Min Value of either a {@link io.openems.edge.heatsystem.components.Valve} or Channel.
     * It does NOT Start a HeatingProcess.
     * By setting the values.
     *
     * @param max the max Value.
     * @param min the min Value.
     */

    @Override
    public void setMaxAndMinValues(Double max, Double min) {
        this.max = max;
        this.min = min;
    }

    /**
     * Writes the stored min and Max Value to the Component of the Line.
     */
    @Override
    public void onlyWriteMaxMinToLine() {
        try {
            WriteChannel<Double> doubleMaxWriteChannel = this.cpm.getChannel(this.maxAddress);
            WriteChannel<Double> doubleMinWriteChannel = this.cpm.getChannel(this.minAddress);
            doubleMaxWriteChannel.setNextWriteValue(this.max);
            doubleMinWriteChannel.setNextWriteValue(this.min);
        } catch (Exception e) {
            super.log.warn("Couldn't write to the Channel. Reason: " + e.getMessage());
        }
    }
}
