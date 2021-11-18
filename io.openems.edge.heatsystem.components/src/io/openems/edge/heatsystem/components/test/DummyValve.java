package io.openems.edge.heatsystem.components.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import io.openems.edge.io.api.Relay;
import io.openems.edge.io.test.DummyRelay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Class works the same way as the ValveImpl except it's constructor and lacking of the
 * Osgi Activate and Deactivate Methods for obv Reasons. It should be combined with the Passing Station + the DummyPump.
 */

public class DummyValve extends AbstractOpenemsComponent implements HydraulicComponent, OpenemsComponent {

    private final Logger log = LoggerFactory.getLogger(DummyValve.class);

    private final Relay opens;
    private final Relay closing;
    private final double secondsPerPercentage;
    private boolean percentageWasSet = false;
    private long timeStampValve;


    public DummyValve(Relay valveOpen, Relay valveClose, String id, double valveTimeInSeconds) {
        super(OpenemsComponent.ChannelId.values(), HydraulicComponent.ChannelId.values());
        super.activate(null, id, "", true);
        this.opens = valveOpen;
        this.closing = valveClose;
        this.secondsPerPercentage = valveTimeInSeconds / 100.d;
        this.getLastPowerLevelChannel().setNextValue(0);
        this.getPowerLevelChannel().setNextValue(0);
        this.timeChannel().setNextValue(0);
        this.getIsBusyChannel().setNextValue(false);
    }

    public DummyValve(String id) {
        this(new DummyRelay(id + "OpenRelay"), new DummyRelay(id + "closingRelay"), id, 10);
    }

    private void valveClose() {
        if (!this.getIsBusyChannel().getNextValue().get()) {
            this.controlRelays(false, "Open");
            this.controlRelays(true, "Closed");
            this.getIsBusyChannel().setNextValue(true);
            this.timeStampValve = System.currentTimeMillis();
        }
    }


    private void valveOpen() {
        //opens will be set true when closing is done
        if (!this.getIsBusyChannel().getNextValue().get()) {
            this.controlRelays(false, "Closed");
            this.controlRelays(true, "Open");
            this.getIsBusyChannel().setNextValue(true);
            this.timeStampValve = System.currentTimeMillis();
        }
    }


    @Override
    public boolean readyToChange() {
        return System.currentTimeMillis() - this.timeStampValve >= (this.timeChannel().getNextValue().get() * 1000);
    }

    @Override
    public boolean changeByPercentage(double percentage) {
        double currentPowerLevel;

        //opens / closes valve by a certain percentage value
        if ((this.getIsBusyChannel().getNextValue().get()) || percentage == 0) {
            return false;
        } else {
            currentPowerLevel = this.getPowerLevelChannel().getNextValue().get();
            this.getLastPowerLevelChannel().setNextValue(currentPowerLevel);
            currentPowerLevel += percentage;

            currentPowerLevel = currentPowerLevel > 100 ? 100
                    : currentPowerLevel < 0 ? 0 : currentPowerLevel;

            this.getPowerLevelChannel().setNextValue(currentPowerLevel);
            System.out.println("Next PowerLevel Value is " + currentPowerLevel);
            if (Math.abs(percentage) >= 100) {
                this.timeChannel().setNextValue(100 * this.secondsPerPercentage);
            } else {
                this.timeChannel().setNextValue(Math.abs(percentage) * this.secondsPerPercentage);
            }
            if (percentage < 0) {
                this.controlRelays(false, "Open");
                this.valveClose();
            } else {
                this.controlRelays(false, "Closed");
                this.valveOpen();
            }
            this.percentageWasSet = true;
            return true;
        }
    }


    private void controlRelays(boolean activate, String whichRelays) {
        switch (whichRelays) {
            case "Open":
                if (this.opens.isCloser().getNextValue().get()) {
                    System.out.println(activate);
                } else {
                    System.out.println(!activate);
                }
                break;
            case "Closed":
                if (this.closing.isCloser().getNextValue().get()) {
                    this.log.info(super.id() + activate);
                } else {
                    this.log.info(super.id() + !activate);
                }
                break;
        }
        if (!activate) {
            this.getIsBusyChannel().setNextValue(false);
        }
    }


    // Added because of changes to valve interface. Won't compile otherwise.
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
        percent -= this.getPowerLevelValue();
        if (this.changeByPercentage(percent)) {
            this.setPointPowerLevelChannel().setNextValue(-1);
        }
    }

    @Override
    public void forceClose() {
    }
}
