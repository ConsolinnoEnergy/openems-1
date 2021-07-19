package io.openems.edge.powerplant.analog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

import io.openems.edge.consolinno.leaflet.sensor.signal.api.SignalSensor;

import io.openems.edge.io.api.AnalogInputOutput;
import io.openems.edge.powerplant.api.PowerPlant;

import org.osgi.service.component.ComponentContext;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Powerplant.Analog",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class PowerPlantImpl extends AbstractOpenemsComponent implements PowerPlant, OpenemsComponent, EventHandler {

    private final Logger log = LoggerFactory.getLogger(PowerPlantImpl.class);

    @Reference
    ComponentManager cpm;

    private AnalogInputOutput output;
    private final List<SignalSensor> sensors = new ArrayList<>();
    private int lastPercentValue = -1;
    private int lastKiloWattValue = -1;

    Config config;

    public PowerPlantImpl() {
        super(OpenemsComponent.ChannelId.values(), PowerPlant.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (this.cpm.getComponent(config.analogueDevice()) instanceof AnalogInputOutput) {
            this.output = this.cpm.getComponent(config.analogueDevice());
        }
        OpenemsError.OpenemsNamedException[] ex = {null};
        Arrays.stream(config.errorBits()).forEach(string -> {
            try {
                if (this.cpm.getComponent(string) instanceof SignalSensor) {
                    this.sensors.add(this.cpm.getComponent(string));
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                ex[0] = e;
            }
        });
        if (ex[0] != null) {
            throw ex[0];
        }
        this.getMaximumKw().setNextValue(config.maxKw());
        this.config = config;
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }



    //Returns False if no Value is defined or hasn't changed and previous CheckLastPercent was False;
    private boolean checkLastKilowattAndRefresh() {
        if (this.getPowerLevelKiloWatt().value().isDefined()) {
            int powerLevelKiloWatt = this.getPowerLevelKiloWatt().value().get();
            int maxKw = this.getMaximumKw().value().orElse(this.config.maxKw());
            int kiloWatt = Math.max(powerLevelKiloWatt, 0);
            if (kiloWatt != this.lastKiloWattValue) {
                int newPercent = kiloWatt * 100 / maxKw;
                this.lastPercentValue = newPercent;
                this.getPowerLevelPercent().setNextValue(newPercent);
                return true;
            }
        }
        return false;
    }

    //Returns false if last Percent Value hasn't changed to the current Value
    //If percentValue has changed --> set in LucidControl and refresh kW Value.
    private boolean checkLastPercentAndRefresh() {
        if (this.getPowerLevelPercent().value().isDefined()) {
            int percent = Math.max(this.getPowerLevelPercent().value().get(), 0);
            if (percent != this.lastPercentValue) {
                this.lastPercentValue = percent;
                int newKw = percent * this.getMaximumKw().value().get() / 100;
                this.lastKiloWattValue = newKw;
                this.getPowerLevelKiloWatt().setNextValue(newKw);
                return true;
            }
        }
        return false;
    }

    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            AtomicBoolean hasError = new AtomicBoolean(false);
            this.sensors.forEach(sensor -> {
                if (sensor.getSignalType().value().get().equals("Error")) {
                    if (sensor.signalActive().value().orElse(false)) {
                        hasError.set(true);
                    }
                }

            });
            if (hasError.get() == false) {
                this.getErrorOccured().setNextValue(false);

                if (this.checkLastPercentAndRefresh() && this.checkLastKilowattAndRefresh()) {
                    try {
                        this.output.setPercentChannel().setNextWriteValueFromObject(this.lastPercentValue);
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Failed to Write Percent Value to Output " + this.lastPercentValue + " in: " + super.id());
                    }
                }
            } else {
                this.getErrorOccured().setNextValue(true);
            }
        }
    }
}
