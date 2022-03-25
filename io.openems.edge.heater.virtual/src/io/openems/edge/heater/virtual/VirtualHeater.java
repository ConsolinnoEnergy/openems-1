package io.openems.edge.heater.virtual;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Optional;

/**
 * A Virtual Heater.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Virtual", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)

public class VirtualHeater extends AbstractOpenemsComponent implements OpenemsComponent, Heater, EventHandler {


    public VirtualHeater() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values(),
                ChannelId.values());
    }

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // read/write channels
        MAINTENANCE_TIME(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }


    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this._setHeaterState(2);
        this.channel(ChannelId.MAINTENANCE_TIME).setNextValue(config.defaultMaintenanceInterval());
        this.getEffectiveHeatingPowerChannel().setNextValue(config.defaultPower());
        try {
            this.getHeatingPowerPercentSetpointChannel().setNextWriteValueFromObject(config.defaultSetPoint());
        } catch (OpenemsError.OpenemsNamedException e) {
            //ignored
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.channel(ChannelId.MAINTENANCE_TIME).setNextValue(config.defaultMaintenanceInterval());
        this.getEffectiveHeatingPowerChannel().setNextValue(config.defaultPower());
        try {
            this.getHeatingPowerPercentSetpointChannel().setNextWriteValueFromObject(config.defaultSetPoint());
        } catch (OpenemsError.OpenemsNamedException e) {
            //ignored
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            Optional<Boolean> enabled = this.getEnableSignalChannel().getNextWriteValueAndReset();
            if (enabled.isPresent()) {
                this._setHeaterState(HeaterState.RUNNING);
                this.getEnableSignalChannel().setNextValue(enabled.get());
            } else {
                this._setHeaterState(HeaterState.STANDBY);
                this.getEnableSignalChannel().setNextValue(false);
            }
        }
    }
}
