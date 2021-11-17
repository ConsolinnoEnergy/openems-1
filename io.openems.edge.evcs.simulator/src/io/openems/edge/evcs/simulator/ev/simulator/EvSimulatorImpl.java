package io.openems.edge.evcs.simulator.ev.simulator;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.api.GridVoltage;
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

/**
 * This simulates a generic Electric vehicle that communicates with the generic Evcs Simulator.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "EvSimulatorImpl", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

public class EvSimulatorImpl extends AbstractOpenemsComponent implements EvSimulator, OpenemsComponent, EventHandler {

    @Reference
    ComponentManager cpm;

    private EvcsSimulator parent;
    private int initialCharge;
    private int initialPhases;
    private int gridVoltage = GridVoltage.V_230_HZ_50.getValue();
    private Config config;

    public EvSimulatorImpl() {
        super(OpenemsComponent.ChannelId.values(), EvSimulator.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.activateOrModifiedRoutine();
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled());
    }

    private void activateOrModifiedRoutine() throws OpenemsError.OpenemsNamedException {
        this.parent = this.cpm.getComponent(this.config.parentId());
        this.initialCharge = this.config.chargePower();
        this.initialPhases = this.config.phases();
        this.setChargePower(this.initialCharge * this.gridVoltage);
        this.setPhases(this.initialPhases);
        this.gridVoltage = this.config.gridVoltage().getValue();

    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.activateOrModifiedRoutine();
        this.config = config;
        super.modified(context, config.id(), config.alias(), config.enabled());

    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (this.getChargePower() > 0 && this.getPhases() > 0) {
            this.parent.setCar(true);
            this.parent.setCharge(this.getChargePower());
            this.parent.setPhases(this.getPhases());
        } else {
            this.parent.setCar(false);
        }
    }
}
