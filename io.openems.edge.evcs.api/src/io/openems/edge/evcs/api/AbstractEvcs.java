package io.openems.edge.evcs.api;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import java.util.Arrays;
import java.util.Optional;

/**
 * This is the Abstract Implementation of the EVCS. The Child will only need to Fill in the Constructor with the values from the config, and provide a ModbusRegisterTable.
 * This will initialize the Read and Write Handler for all the necessary logic.
 */
public abstract class AbstractEvcs extends AbstractOpenemsModbusComponent implements Evcs, ManagedEvcs, OpenemsComponent, AbstractEvcsModbusChannel, EventHandler {

    private int minPower;
    private int maxPower;
    private int[] phases;
    private EvcsWriteHandler writeHandler;
    private EvcsReadHandler readHandler;
    private int readScaleFactor;
    private int writeCurrentScaleFactor;


    public AbstractEvcs(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
                        io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
        super(firstInitialChannelIds, furtherInitialChannelIds);
    }

    protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int modbusUnitId, ConfigurationAdmin cm,
                               String modbusReference, String modbusId, int minCurrent, int maxCurrent, int[] phases, boolean priority, int readScaleFactor, int writePowerScaleFactor, int writeCurrentScaleFactor) throws OpenemsException {
        this.minPower = minCurrent;
        this.maxPower = maxCurrent;
        this.phases = phases;
        this.readScaleFactor=readScaleFactor;
        this.writeCurrentScaleFactor = writeCurrentScaleFactor;
        if (!this.checkPhases()) {
            throw new OpenemsException("Phase Configuration is not valid!" + " Configuration must only contain 1,2 and 3.");
        }
        this._setMinimumHardwarePower(this.minPower * GridVoltage.V_230_HZ_50.getValue());
        this._setMaximumPower(this.maxPower);
        this._setMaximumHardwarePower(this.maxPower * GridVoltage.V_230_HZ_50.getValue());
        this._setMinimumPower(this.minPower);
        this._setPowerPrecision(GridVoltage.V_230_HZ_50.getValue());
        this._setIsPriority(priority);
        this.readHandler = new EvcsReadHandler(this, readScaleFactor);
        this.writeHandler = new EvcsWriteHandler(this, writePowerScaleFactor,writeCurrentScaleFactor);
        super.activate(context, id, alias, enabled, modbusUnitId, cm, modbusReference, modbusId);
        return true;
    }

    /**
     * Checks if the Phase Configuration of the Config is valid.
     *
     * @return true if valid
     */
    private boolean checkPhases() {
        String phases = Arrays.toString(this.phases);
        return phases.contains("1") && phases.contains("2") && phases.contains("3") && this.phases.length == 3;
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public String debugLog() {
        return "Total: " + this.getChargePower().orElse(0) * this.readScaleFactor + " " + this.getChargePowerChannel().channelDoc().getUnit().getSymbol() + " L1: " + this.getCurrentL1() * writeCurrentScaleFactor + " " +this.getCurrentL1Channel().channelDoc().getUnit().getSymbol()+" L2: " + this.getCurrentL2() * writeCurrentScaleFactor + " " +this.getCurrentL2Channel().channelDoc().getUnit().getSymbol()+" L3: " + this.getCurrentL3() * writeCurrentScaleFactor + " " +this.getCurrentL3Channel().channelDoc().getUnit().getSymbol();
    }

    @Override
    public int[] getPhaseConfiguration() {
        return this.phases;
    }

    @Override
    public void handleEvent(Event event) {
        this.writeHandler.run();
        try {
            this.readHandler.run();
        } catch (Throwable throwable) {
            //
        }
    }

    /**
     * Returns the minimum Software Power.
     *
     * @return minPower as int
     */
    public int getMinPower() {
        return this.minPower;
    }

    /**
     * Returns the maximum Software Power.
     *
     * @return maxPower as int
     */
    public int getMaxPower() {
        return this.maxPower;
    }



}

/**
 * The Read Handler will read the ChargeLimit from a connected Management (e.g. EvcsLimiter) and write it to the Evcs over Modbus.
 */
class EvcsReadHandler {
    private final AbstractEvcs parent;
    private boolean overLimit;
    private static final int OFF = 0;
    private final int scaleFactor;
    private static final int MAX_PHASES = 3;

    EvcsReadHandler(AbstractEvcs parent, int scaleFactor) {
        this.parent = parent;
        this.scaleFactor = scaleFactor;
    }

    /**
     * TODO: the EnergySession is not fully implemented yet
     * @throws OpenemsError.OpenemsNamedException This should not happen
     */
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
            channel.setNextValue(power);
            int phases = this.parent.getPhases().orElse(MAX_PHASES);
            int current = ((power / phases) / GridVoltage.V_230_HZ_50.getValue());
            int maxHwPower = this.parent.getMaximumHardwarePower().get();
            int maxSwPower = this.parent.getMaxPower();
            int maxPower = Math.min(maxHwPower, maxSwPower);
            if (current > maxPower / GridVoltage.V_230_HZ_50.getValue()) {
                current = maxPower / GridVoltage.V_230_HZ_50.getValue();
            }
            int minHwPower = this.parent.getMinimumHardwarePower().get();
            int minSwPower = this.parent.getMinPower();
            int minPower = Math.min(minHwPower, minSwPower);
            if (current < minPower) {
                current = OFF;
            }
            if (this.overLimit) {
                this.parent.setMaximumChargePower(OFF);
                this.parent._setSetChargePowerLimit(OFF);
            } else {
                this.parent.setMaximumChargePower((current * scaleFactor));
                this.parent._setSetChargePowerLimit(current * GridVoltage.V_230_HZ_50.getValue());
            }
        }
    }


}

/**
 * The WriteHandler will write the Information retrieved from the Evcs into the Corresponding Channels of the Evcs/ManagedEvcs interfaces.
 */
class EvcsWriteHandler {
    private final AbstractEvcs parent;
    private final int powerScaleFactor;
    private final int currentScaleFactor;

    EvcsWriteHandler(AbstractEvcs parent, int powerScaleFactor,int currentScaleFactor) {
        this.parent = parent;
        this.powerScaleFactor = powerScaleFactor;
        this.currentScaleFactor = currentScaleFactor;
    }

    void run() {
        this.setPhaseCount();
        this.setStatus();
        this.parent._setChargePower(this.parent.getInternalChargePower() / powerScaleFactor);
        this.parent._setChargingType(ChargingType.AC);
    }

    /**
     * Writes the Amount of Phases in the Phase channel.
     */
    private void setPhaseCount() {
        int phases = 0;

        if (this.parent.getCurrentL1() * currentScaleFactor >= 1) {
            phases += 1;
        }
        if (this.parent.getCurrentL2() * currentScaleFactor >= 1) {
            phases += 1;
        }
        if (this.parent.getCurrentL3() * currentScaleFactor >= 1) {
            phases += 1;
        }
        this.parent._setPhases(phases);

    }

    /**
     * Some Evcs do not have a Register for the Charge Status. In this case, the WriteHandler will put UNDEFINED if there is no Charge detected.
     * Charge otherwise.
     */
    private void setStatus() {
        if (this.parent.getEvStatus() != -1) {
            switch (this.parent.getEvStatus()) {
                case (1):
                    this.parent._setStatus(Status.STARTING);
                    break;
                case (2):
                    this.parent._setStatus(Status.READY_FOR_CHARGING);
                    break;
                case (3):
                    this.parent._setStatus(Status.CHARGING);
                    break;
                case (4):
                    this.parent._setStatus(Status.ERROR);
                    break;
                case (5):
                    this.parent._setStatus(Status.CHARGING_REJECTED);
            }
        } else {
            if (this.parent.getInternalChargePower() > 0) {
                this.parent._setStatus(Status.CHARGING);
            } else {
                this.parent._setStatus(Status.UNDEFINED);
            }
        }
    }
}