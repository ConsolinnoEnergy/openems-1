package io.openems.edge.evcs.keba.kecontact.simulated;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.keba.kecontact.KebaChannelId;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;

import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Arrays;

/**
 * This provides a Simulated Keba KeContact EVCS.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "SimulatedKebaContact", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class SimulatedKebaContact extends AbstractOpenemsComponent implements ManagedEvcs, Evcs, OpenemsComponent, EventHandler {

    private Config config;
    private int[] phases;

    @Reference
    private EvcsPower evcsPower;

    @Reference
    protected ComponentManager cpm;
    private Channel<Integer> l1;
    private Channel<Integer> l2;
    private Channel<Integer> l3;
    private int l1Power;
    private int l2Power;
    private int l3Power;
    private int phaseCount;

    public SimulatedKebaContact() {
        super(//
                OpenemsComponent.ChannelId.values(), //
                ManagedEvcs.ChannelId.values(), //
                Evcs.ChannelId.values(), //
                KebaChannelId.values() //
        );
    }

    @Activate
    void activate(ComponentContext context, Config config) throws javax.naming.ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.channel(KebaChannelId.ALIAS).setNextValue(config.alias());

        this.config = config;
        this.phases = config.phases();
        if (!this.checkPhases()) {
            throw new javax.naming.ConfigurationException();
        }
        this.l1 = this.channel(KebaChannelId.CURRENT_L1);
        this.l2 = this.channel(KebaChannelId.CURRENT_L2);
        this.l3 = this.channel(KebaChannelId.CURRENT_L3);
        this._setPhases(0);
        this._setPowerPrecision(0.23);

    }

    private boolean checkPhases() {
        String phases = Arrays.toString(this.phases);
        return phases.contains("1") && phases.contains("2") && phases.contains("3");
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());

    }

    @Override
    public String debugLog() {
        return "Simulated Keba:"
                + " L1: " + this.l1.getNextValue().orElse(0)
                + " L2: " + this.l2.getNextValue().orElse(0)
                + " L3: " + this.l3.getNextValue().orElse(0);
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public EvcsPower getEvcsPower() {
        return this.evcsPower;
    }

    @Override
    public void handleEvent(Event event) {
        this._setPhases(this.phaseCount);
        this._setChargePower((this.l1Power + this.l2Power + this.l3Power) * 230);

        int chargeLimit = this.getSetChargePowerLimit().orElse(0);
        if (chargeLimit != 0) {
            this.limitPower((chargeLimit / 230));
        }
    }

    private void limitPower(int chargeLimit) {

        int amountToReduce = chargeLimit / this.phaseCount;
        this.phaseCount = 0;
        for (int i = 0; i < this.phaseCount; i++) {
            switch (this.phases[i]) {
                case 1:
                    this.l1Power -= amountToReduce;
                    this.l1.setNextValue(this.l1Power);
                    this.phaseCount++;
                    break;
                case 2:
                    this.l2Power -= amountToReduce;
                    this.l2.setNextValue(this.l2Power);
                    this.phaseCount++;
                    break;
                case 3:
                    this.l3Power -= amountToReduce;
                    this.l3.setNextValue(this.l3Power);
                    this.phaseCount++;
                    break;
            }
        }
    }

    public void applyPower(int phase, int chargePower) throws OpenemsError.OpenemsNamedException {
        if (chargePower <= 6 && chargePower > 0) {
            chargePower = 0;
        }
        this.phaseCount = 0;
        switch (this.phases[phase]) {
            case 1:
                this.l1Power += chargePower;
                this.l1.setNextValue(this.l1Power);
                this.phaseCount++;
                break;
            case 2:
                this.l2Power += chargePower;
                this.l2.setNextValue(this.l2Power);
                this.phaseCount++;
                break;
            case 3:
                this.l3Power += chargePower;
                this.l3.setNextValue(this.l3Power);
                this.phaseCount++;
                break;
        }
    }

    @Override
    public int[] getPhaseConfiguration() {
        return this.phases;
    }
}
