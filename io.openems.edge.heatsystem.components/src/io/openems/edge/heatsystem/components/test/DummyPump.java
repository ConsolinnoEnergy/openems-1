package io.openems.edge.heatsystem.components.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.heatsystem.components.PumpType;
import io.openems.edge.io.api.Pwm;
import io.openems.edge.io.test.DummyPwm;
import io.openems.edge.io.api.Relay;
import io.openems.edge.io.test.DummyRelay;

import java.util.Random;

/**
 * This Device acts as a Dummy for Unittests.
 */
public class DummyPump extends AbstractOpenemsComponent implements OpenemsComponent, HydraulicComponent {

    private final Relay relays;
    //private PwmPowerLevelChannel pwm;
    private boolean isRelays = false;
    private boolean isPwm = false;
    private final Pwm pwm;

    public DummyPump(String id, Relay relays, Pwm pwm, PumpType type) {
        super(OpenemsComponent.ChannelId.values(), HydraulicComponent.ChannelId.values());

        super.activate(null, id, "", true);

        this.relays = relays;
        this.pwm = pwm;

        switch (type) {
            case RELAY:
                this.isRelays = true;
                break;

            case PWM_OR_AIO:
                this.isPwm = true;
                break;

            case RELAY_AND_PWM_OR_AIO:
            default:
                this.isRelays = true;
                this.isPwm = true;
        }

        this.getIsBusyChannel().setNextValue(false);
        this.getPowerLevelChannel().setNextValue(0);
        this.getLastPowerLevelChannel().setNextValue(0);

    }

    public DummyPump(String id, PumpType type) {
        this(id, new DummyRelay(String.valueOf(new Random().nextInt())), new DummyPwm(String.valueOf(new Random().nextInt())), type);
    }

    @Override
    public boolean readyToChange() {
        return true;
    }


    /**
     * Like the original changeByPercentage just a bit adjusted.
     *
     * @param percentage change the PowerLevel by this value.
     */
    @Override
    public boolean changeByPercentage(double percentage) {
        if (this.isRelays) {
            if (this.isPwm) {
                if (this.getPowerLevelChannel().getNextValue().get() + percentage < HydraulicComponent.DEFAULT_MIN_POWER_VALUE) {
                    this.controlRelays(false);
                    System.out.println("Set Next WriteValue to 0.f");
                    this.getPowerLevelChannel().setNextValue(HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
                    return true;
                }
            } else {
                this.controlRelays((percentage > HydraulicComponent.DEFAULT_MIN_POWER_VALUE));
            }
        }
        if (this.isPwm) {
            double currentPowerLevel;
            this.getLastPowerLevelChannel().setNextValue(this.getPowerLevelChannel().getNextValue().get());
            currentPowerLevel = this.getPowerLevelChannel().getNextValue().get();
            currentPowerLevel += percentage;
            currentPowerLevel = Math.min(currentPowerLevel, HydraulicComponent.DEFAULT_MAX_POWER_VALUE);
            currentPowerLevel = Math.max(currentPowerLevel, HydraulicComponent.DEFAULT_MIN_POWER_VALUE);
            System.out.println("Set Next Write Value to " + currentPowerLevel + "in " + this.pwm.id());
            this.getPowerLevelChannel().setNextValue(currentPowerLevel);
        }
        return true;
    }


    private void controlRelays(boolean activate) {
            System.out.println("Relays is " + activate);
    }

    @Override
    public void forceClose() {

    }

    @Override
    public void forceOpen() {

    }

    @Override
    public boolean powerLevelReached() {
        return false;
    }

    @Override
    public boolean isChanging() {
        return false;
    }

    @Override
    public void reset() {

    }

    @Override
    public void setPowerLevel(double percent) {
        this.changeByPercentage(percent - this.getPowerLevelValue());
    }
}
