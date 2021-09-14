package io.openems.edge.heatsystem.components.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.heatsystem.components.HydraulicChannel;
import io.openems.edge.io.api.Pwm;
import io.openems.edge.consolinno.leaflet.pwm.test.DummyPwm;
import io.openems.edge.relay.api.Relay;
import io.openems.edge.relay.api.test.DummyRelay;

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

    public DummyPump(String id, Relay relays, Pwm pwm, String type) {
        super(OpenemsComponent.ChannelId.values(), HydraulicChannel.ChannelId.values());

        super.activate(null, id, "", true);

        this.relays = relays;
        this.pwm = pwm;

        switch (type) {
            case "Relays":
                this.isRelays = true;
                break;

            case "Pwm":
                this.isPwm = true;
                break;

            default:
                this.isRelays = true;
                this.isPwm = true;
        }

        this.getIsBusyChannel().setNextValue(false);
        this.getPowerLevelChannel().setNextValue(0);
        this.getLastPowerLevelChannel().setNextValue(0);

    }

    public DummyPump(String id, String type) {
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
                if (this.getPowerLevelChannel().getNextValue().get() + percentage < 0) {
                    this.controlRelays(false);
                    System.out.println("Set Next WriteValue to 0.f");
                    this.getPowerLevelChannel().setNextValue(0);
                    return true;
                }
            } else {
                this.controlRelays((percentage > 0));
            }
        }
        if (this.isPwm) {
            double currentPowerLevel;
            this.getLastPowerLevelChannel().setNextValue(this.getPowerLevelChannel().getNextValue().get());
            currentPowerLevel = this.getPowerLevelChannel().getNextValue().get();
            currentPowerLevel += percentage;
            currentPowerLevel = currentPowerLevel > 100 ? 100 : currentPowerLevel;
            currentPowerLevel = currentPowerLevel < 0 ? 0 : currentPowerLevel;
            System.out.println("Set Next Write Value to " + currentPowerLevel + "in " + this.pwm.id());
            this.getPowerLevelChannel().setNextValue(currentPowerLevel);
        }
        return true;
    }


    private void controlRelays(boolean activate) {
        if (this.relays.isCloser().getNextValue().get()) {
            System.out.println("Relays is " + activate);
        } else {
            System.out.println("Relays is " + !activate);
        }
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
