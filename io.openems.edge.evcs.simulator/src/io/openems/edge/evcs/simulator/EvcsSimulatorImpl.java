package io.openems.edge.evcs.simulator;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.GridVoltage;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.simulator.api.EvcsSimulator;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Arrays;

/**
 * This simulates a generic EVCS, using the internal ManagedEvcs interface.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "EvcsSimulatorImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

public class EvcsSimulatorImpl extends AbstractOpenemsComponent implements Evcs, ManagedEvcs, OpenemsComponent, EvcsSimulator, EventHandler {

    private Config config;
    private int[] phaseConfiguration;
    private double l1 = 0;
    private double l2 = 0;
    private double l3 = 0;
    private int gridVoltage = GridVoltage.V_230_HZ_50.getValue();


    public EvcsSimulatorImpl() {
        super(OpenemsComponent.ChannelId.values(), ManagedEvcs.ChannelId.values(), EvcsSimulator.ChannelId.values(), Evcs.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) {
        this.config = config;
        this.activateOrModifiedRoutine();
        super.activate(context, config.id(), config.alias(), config.enabled());
    }


    @Modified
    void modified(ComponentContext context, Config config) {
        this.config = config;
        this.activateOrModifiedRoutine();
        super.modified(context, config.id(), config.alias(), config.enabled());

    }

    private void activateOrModifiedRoutine() {
        this._setMaximumHardwarePower(this.config.maxHwPower());
        this._setMaximumPower(this.config.maxSwPower());
        this._setMinimumHardwarePower(this.config.minHwPower());
        this._setMinimumPower(this.config.minSwPower());

        this._setIsPriority(this.config.priority());
        this.gridVoltage = this.config.gridVoltage().getValue();
        if (this.checkPhases()) {
            this.phaseConfiguration = this.config.phases();
        }
    }

    private boolean checkPhases() {
        String phases = Arrays.toString(this.config.phases());
        return phases.contains("1") && phases.contains("2") && phases.contains("3");
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public EvcsPower getEvcsPower() {
        return null;
    }

    @Override
    public void handleEvent(Event event) {
        if (!this.config.ocpp()) {
            if (this.getCarValue()) {
                if (this.getChargeValue() > 0) {
                    this._setStatus(Status.CHARGING);
                    this._setPhases(this.getPhasesValue());
                    int charge = this.getChargeValue();
                    if (charge > this.getSetChargePowerLimit().orElse(0)) {
                        charge = this.getSetChargePowerLimit().orElse(0);
                    }
                    this._setChargePower(charge);
                } else {
                    this._setStatus(Status.READY_FOR_CHARGING);
                    this._setPhases(0);
                    this._setChargePower(0);
                }
            } else {
                this._setStatus(Status.NOT_READY_FOR_CHARGING);
                this._setPhases(0);
                this._setChargePower(0);
            }
            this.setPower();
            double powerUse = Math.random() * 0.1;
            if (powerUse < 0.001) {
                powerUse = 0.01246526789;
            }
            this.l1 += powerUse;
        }
    }

    private void setPower() {
        this.l1 = 0;
        this.l2 = 0;
        this.l3 = 0;
        int phaseCount = this.getPhases().orElse(1);
        for (int n = 0; n < phaseCount; n++) {
            switch (this.phaseConfiguration[n]) {
                case 1:
                    this.l1 = (this.getChargePower().orElse(//
                            this.getChargePowerChannel().getNextValue().orElse(0))//
                            / this.gridVoltage) / phaseCount;
                    break;
                case 2:
                    this.l2 = (this.getChargePower().orElse(//
                            this.getChargePowerChannel().getNextValue().orElse(0))//
                            / this.gridVoltage) / phaseCount;
                    break;
                case 3:
                    this.l3 = (this.getChargePower().orElse(//
                            this.getChargePowerChannel().getNextValue().orElse(0))//
                            / this.gridVoltage) / phaseCount;
                    break;
            }
        }
    }

    @Override
    public String debugLog() {
        return "Total: " + this.getChargePower().get() + " W "
                + "| L1 " + this.l1 + " A | L2 " + this.l2  + " A | L3 " + this.l3  + " A";
    }
}
