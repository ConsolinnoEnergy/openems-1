package io.openems.edge.evcs.keba.kecontact.simulated;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
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
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Arrays;

/**
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "SimulatedKebaContact", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)

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

    }

    public void applyPower(int phase, int chargePower) throws OpenemsError.OpenemsNamedException {
        switch (this.phases[phase]) {
            case 1:
                this.l1Power += chargePower;
                this.l1.setNextValue(this.l1Power);
                break;
            case 2:
                this.l2Power += chargePower;
                this.l2.setNextValue(this.l2Power);
                break;
            case 3:
                this.l3Power += chargePower;
                this.l3.setNextValue(this.l3Power);
                break;

        }
    }
}
