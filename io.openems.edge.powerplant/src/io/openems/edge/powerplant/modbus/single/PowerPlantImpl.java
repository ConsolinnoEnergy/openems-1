package io.openems.edge.powerplant.modbus.single;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.generic.AbstractGenericModbusComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

import io.openems.edge.consolinno.leaflet.sensor.signal.api.SignalSensor;

import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.generator.api.Generator;
import io.openems.edge.heater.Heater;
import io.openems.edge.io.api.AnalogInputOutput;
import io.openems.edge.powerplant.api.PowerPlant;

import io.openems.edge.powerplant.api.PowerPlantModbus;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.PowerPlant.Analog",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class PowerPlantImpl extends AbstractGenericModbusComponent implements PowerPlant, OpenemsComponent, PowerPlantModbus, EventHandler,
        Generator, Heater {

    private final Logger log = LoggerFactory.getLogger(PowerPlantImpl.class);

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private TimerHandler timerHandler;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "ELECTROLYZER_ENABLE_SIGNAL_IDENTIFIER";
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "ELECTROLYZER_EXCEPTIONAL_STATE_IDENTIFIER";
    private boolean useExceptionalState;

    @Reference
    ComponentManager cpm;

    private AnalogInputOutput output;
    private final List<SignalSensor> sensors = new ArrayList<>();
    private int lastPercentValue = -1;
    private int lastKiloWattValue = -1;

    Config config;

    public PowerPlantImpl() {
        super(OpenemsComponent.ChannelId.values(),
                PowerPlant.ChannelId.values(),
                PowerPlantModbus.ChannelId.values(),
                Generator.ChannelId.values(),
                Heater.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());

        if (super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length)) {
            this.baseConfiguration();
        }
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

    private void baseConfiguration() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.timerHandler = new TimerHandlerImpl(super.id(), this.cpm);
        this.timerHandler.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, this.config.timerNeedHeatResponse(), this.config.timeNeedHeatResponse());
        this.useExceptionalState = this.config.enableExceptionalStateHandling();
        if (this.useExceptionalState) {
            this.timerHandler.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, this.config.timerExceptionalState(), this.config.timeToWaitExceptionalState());
            this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(this.timerHandler, EXCEPTIONAL_STATE_IDENTIFIER);
        }
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException, IOException {
        super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                config.modbusBridgeId(), this.cpm, Arrays.asList(config.configurationList()));
        super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length);
        this.config = config;
        this.baseConfiguration();
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

    @Override
    public boolean setPointPowerPercentAvailable() {
        return false;
    }

    @Override
    public boolean setPointPowerAvailable() {
        return false;
    }

    @Override
    public boolean setPointTemperatureAvailable() {
        return false;
    }

    @Override
    public int calculateProvidedPower(int demand, float bufferValue) throws OpenemsError.OpenemsNamedException {
        return 0;
    }

    @Override
    public int getMaximumThermalOutput() {
        return 0;
    }

    @Override
    public void setOffline() throws OpenemsError.OpenemsNamedException {

    }

    @Override
    public boolean hasError() {
        return false;
    }

    @Override
    public void requestMaximumPower() {

    }

    @Override
    public void setIdle() {

    }
}
