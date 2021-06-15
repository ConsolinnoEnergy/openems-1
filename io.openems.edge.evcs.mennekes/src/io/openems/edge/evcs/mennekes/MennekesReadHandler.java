package io.openems.edge.evcs.mennekes;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.evcs.api.ManagedEvcs;

import java.util.Optional;

/**
 * This reads the Values from the OpenEms Channels and writes them into the correct Internal Channels.
 * Internal OpenEms Channel -> External READ_WRITE REGISTER
 */

public class MennekesReadHandler {
    private final MennekesImpl parent;
    private static final int GRID_VOLTAGE = 230;
    private boolean overLimit;

    MennekesReadHandler(MennekesImpl parent) {
        this.parent = parent;
    }

    void run() throws OpenemsError.OpenemsNamedException {
        this.setPower();
        this.checkEnergySession();

    }

    /**
     * Checks if the EnergyLimit for the Session was reached.
     */
    private void checkEnergySession() {
        if (this.parent.getSetEnergyLimit().isDefined()) {
            int energyLimit = this.parent.getSetEnergyLimit().get();
            if (this.parent.getEnergySession().orElse(0) > energyLimit) {
                this.overLimit = true;
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
            int current = (power / phases) / GRID_VOLTAGE;
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
            if (this.overLimit) {
                this.parent.setCurrentLimit(0);
                this.parent._setSetChargePowerLimit(0);
            } else {
                this.parent.setCurrentLimit(current);
                this.parent._setSetChargePowerLimit(current * GRID_VOLTAGE);
            }
        }
    }


}
