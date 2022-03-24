package io.openems.edge.heatsystem.components.virtual;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.heatsystem.components.HydraulicComponent;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.EventConstants;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A virtual hydraulic component.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "HeatsystemComponent.Virtual", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)

public class VirtualHydraulicComponent extends AbstractOpenemsComponent implements OpenemsComponent, HydraulicComponent {

    private final Logger log = LoggerFactory.getLogger(VirtualHydraulicComponent.class);

    public VirtualHydraulicComponent() {
        super(OpenemsComponent.ChannelId.values(),
                HydraulicComponent.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.getPowerLevelChannel().setNextValue(0);
        this.getPowerLevelChannel().nextProcessImage();
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.getPowerLevelChannel().setNextValue(0);
        this.getPowerLevelChannel().nextProcessImage();
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public boolean readyToChange() {
        return true;
    }

    @Override
    public boolean changeByPercentage(double percentage) {
        double powerLevel = this.getPowerLevelValue();
        //Return if no percentage Change is made while PowerLevelValue is Defined -> no changes made
        if (percentage == 0 && this.getPowerLevelChannel().value().isDefined()) {
            return false;
        }
        //deactivate
        if ((powerLevel + percentage <= DEFAULT_MIN_POWER_VALUE)) {
            this.getLastPowerLevelChannel().setNextValue(powerLevel);
            this.getPowerLevelChannel().setNextValue(DEFAULT_MIN_POWER_VALUE);
            return true;
        }
        powerLevel += percentage;
        powerLevel = Math.max(DEFAULT_MIN_POWER_VALUE, powerLevel);
        powerLevel = Math.min(DEFAULT_MAX_POWER_VALUE, powerLevel);
        this.getLastPowerLevelChannel().setNextValue(this.getPowerLevelValue());
        this.getPowerLevelChannel().setNextValue(powerLevel);
        return true;
    }

    @Override
    public void forceClose() {
        this.setPowerLevel(0);
    }

    @Override
    public void forceOpen() {
        this.setPowerLevel(100);
    }

    @Override
    public boolean powerLevelReached() {
        return true;
    }

    @Override
    public boolean isChanging() {
        return false;
    }

    @Override
    public void reset() {
        this.forceClose();
    }

    @Override
    public boolean setPowerLevel(double percent) {
        if (percent >= DEFAULT_MIN_POWER_VALUE && percent != this.getPowerLevelValue()) {
            double changeByPercent = percent - getPowerLevelValue();
            this.changeByPercentage(changeByPercent);
            return true;
        }
        return false;
    }
}
