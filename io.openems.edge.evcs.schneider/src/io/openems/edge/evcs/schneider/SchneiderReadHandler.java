package io.openems.edge.evcs.schneider;


import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.evcs.api.ManagedEvcs;
import java.util.Optional;

/**
 * This reads the Values from the OpenEms Channels and writes them into the correct Internal Channels.
 * Internal OpenEms Channel -> External READ_WRITE REGISTER
 */
public class SchneiderReadHandler {

    private final SchneiderImpl parent;
    private static final int GRID_VOLTAGE = 230;
    private Integer lastCurrent = null;
    private RemoteCommand command;
    private boolean acknowledgeFlag;
    private RemoteCommand status;
    private boolean errorFlag;

    public SchneiderReadHandler(SchneiderImpl parent) {
        this.parent = parent;
    }


    protected void run() throws Throwable {
        this.setPower();

        if (this.errorFlag) {
            this.parent.setRemoteCommand(RemoteCommand.FORCE_STOP_CHARGE);
            this.parent._setSetChargePowerLimit(0);
            this.acknowledgeFlag = true;
            this.errorFlag = false;
        }
        this.checkEnergySession();

    }

    /**
     * Checks if the EnergyLimit for the Session was reached.
     */
    private void checkEnergySession() {
        if (this.parent.getSetEnergyLimit().isDefined()) {
            int energyLimit = this.parent.getSetEnergyLimit().get();
            if (this.parent.getEnergySession().orElse(0) > energyLimit) {
                this.errorFlag = true;
            }
        }
    }

    /**
     * Sets the current from SET_CHARGE_POWER channel.
     *
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private void setPower() throws OpenemsError.OpenemsNamedException {
        WriteChannel<Integer> channel = this.parent.channel(ManagedEvcs.ChannelId.SET_CHARGE_POWER_LIMIT);
        Optional<Integer> valueOpt = channel.getNextWriteValueAndReset();

        if (valueOpt.isPresent()) {
            Integer power = valueOpt.get();
            int phases = this.parent.getPhases().orElse(3);
            Integer current = (power / phases) / GRID_VOLTAGE;
            int maxHwPower = this.parent.getMaximumHardwarePower().get();
            int maxSwPower = this.parent.getMaxPower();
            int maxPower = Math.min(maxHwPower, maxSwPower);
            if (current > maxPower / GRID_VOLTAGE) {
                current = maxPower / GRID_VOLTAGE;
            }
            int minHwPower = this.parent.getMinimumHardwarePower().get();
            int minSwPower = this.parent.getMinPower();
            int minPower = Math.min(minHwPower, minSwPower);
            if (current < minPower) {
                current = 0;
            }
            if (!current.equals(this.lastCurrent)) {
                this.lastCurrent = current;
                this.parent.setMaxIntensitySocket(current);
                this.parent._setSetChargePowerLimit(current * GRID_VOLTAGE);
                //TODO:this may be redundant
                if (current == 0) {
                    this.acknowledgeFlag = true;
                    this.command = RemoteCommand.SUSPEND_CHARGING;
                    this.parent.setRemoteCommand(this.command);
                } else if (this.status == RemoteCommand.SUSPEND_CHARGING) {
                    this.acknowledgeFlag = true;
                    this.command = RemoteCommand.RESTART_CHARGING;
                    this.parent.setRemoteCommand(this.command);
                }
            }
            if (this.acknowledgeFlag) {
                int remoteStatus = this.parent.getRemoteCommandStatus();
                if (remoteStatus == this.command.getValue()) {
                    this.status = this.command;
                    this.command = RemoteCommand.ACKNOWLEDGE_COMMAND;
                    this.parent.setRemoteCommand(this.command);
                    this.acknowledgeFlag = false;

                } else if (remoteStatus == (0x8000 | this.command.getValue())) {
                    this.errorFlag = true;
                }
            }

        }


    }

}
