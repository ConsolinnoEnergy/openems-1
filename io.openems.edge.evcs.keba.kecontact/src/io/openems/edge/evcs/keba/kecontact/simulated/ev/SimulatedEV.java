package io.openems.edge.evcs.keba.kecontact.simulated.ev;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.evcs.keba.kecontact.simulated.SimulatedKebaContact;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "SimulatedEV", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class SimulatedEV extends AbstractOpenemsComponent implements OpenemsComponent {

    private int chargePower;
    private int phase;
    private SimulatedKebaContact evcsId;
    private final Logger log = LoggerFactory.getLogger(SimulatedEV.class);
    @Reference
    protected ComponentManager cpm;

    public SimulatedEV() {
        super(ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.chargePower = config.charge();
        this.phase = config.phase();
        this.evcsId = this.cpm.getComponent(config.EVCSId());
        super.activate(context, config.id(), config.alias(), config.enabled());
        try {
            for (int i = 0; i < this.phase; i++) {
                this.evcsId.applyPower(i, this.chargePower);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("ApplyPower Failed.");
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());

    }

    @Deactivate
    protected void deactivate() {
        try {
            for (int i = 0; i < this.phase; i++) {
                this.evcsId.applyPower(i, this.chargePower * -1);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("ApplyPower Failed.");
        }
        super.deactivate();
    }


}
