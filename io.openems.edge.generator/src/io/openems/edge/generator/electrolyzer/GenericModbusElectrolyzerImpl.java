package io.openems.edge.generator.electrolyzer;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.generic.AbstractGenericModbusComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.generator.api.ElectrolyzerAccessMode;
import io.openems.edge.generator.api.ControlMode;
import io.openems.edge.generator.api.Electrolyzer;
import io.openems.edge.generator.api.ElectrolyzerModbusGeneric;
import io.openems.edge.generator.api.Generator;
import io.openems.edge.generator.api.GeneratorModbusGeneric;
import io.openems.edge.heater.Heater;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationAdmin;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Generator.Modbus.HydrogenGenerator.Electrolyzer.Generic",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        properties = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})
public class GenericModbusElectrolyzerImpl extends AbstractGenericModbusComponent implements Electrolyzer, OpenemsComponent,
        Heater, ExceptionalState, EventHandler, Generator, GeneratorModbusGeneric, ElectrolyzerModbusGeneric {

    private final Logger log = LoggerFactory.getLogger(GenericModbusElectrolyzerImpl.class);

    private TimerHandler timerHandler;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "ELECTROLYZER_ENABLE_SIGNAL_IDENTIFIER";
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "ELECTROLYZER_EXCEPTIONAL_STATE_IDENTIFIER";
    private boolean useExceptionalState;
    private ControlMode controlMode = ControlMode.READ;
    private ElectrolyzerAccessMode accessMode = ElectrolyzerAccessMode.HEATER;

    @Reference
    protected ConfigurationAdmin cm;

    @Reference
    ComponentManager cpm;
    private boolean isRunning;

    // This is essential for Modbus to work, but the compiler does not warn you when it is missing!
    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    public GenericModbusElectrolyzerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Electrolyzer.ChannelId.values(),
                ElectrolyzerModbusGeneric.ChannelId.values(),
                GeneratorModbusGeneric.ChannelId.values(),
                Generator.ChannelId.values(),
                Heater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    protected Config config;

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException, IOException {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, config.modbusBridgeId(), this.cpm, Arrays.asList(config.configurationList()));
        if (super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length)) {
            baseConfiguration();
        }

    }

    private void baseConfiguration() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.controlMode = this.config.controlMode();
        this.accessMode = this.config.accessMode();
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


    @Override
    public void handleEvent(Event event) {
        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
                handleChannelUpdate(this.getPowerPercent(), this._hasPowerPercent());
                handleChannelUpdate(this.getPowerChannel(), this._hasPower());
                handleChannelUpdate(this.getWMZEnergyProducedChannel(), this._hasWMZEnergyProduced());
                handleChannelUpdate(this.getWMZTempSourceChannel(), this._hasWMZTempSource());
                handleChannelUpdate(this.getWMZTempSinkChannel(), this._hasWMZTempSink());
                handleChannelUpdate(this.getWMZPowerChannel(), this._hasWMZPower());
                handleChannelUpdate(this.getEffectivePowerChannel(), this._hasPower());
                handleChannelUpdate(this.getEffectivePowerPercentChannel(), this._hasPowerPercent());
                handleChannelUpdate(this.getFlowTemperatureChannel(), this._hasWMZTempSource());
                handleChannelUpdate(this.getReturnTemperatureChannel(), this._hasWMZTempSink());
                break;
            case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
                if (this.controlMode.equals(ControlMode.READ_WRITE)) {
                    if (this.useExceptionalState) {
                        boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
                        if (exceptionalStateActive) {
                            this.handleExceptionalState();
                            return;
                        }
                    }
                    try {
                        /*
                         only check this condition if :
                         Electrolyzer is handled by controller accessing Heater or if heater and
                         Electrolyzer controller controls them and the Electrolyzer signal is missing)*/
                        if ((this.accessMode.equals(ElectrolyzerAccessMode.HEATER) || this.accessMode.equals(ElectrolyzerAccessMode.HEATER_AND_ELECTROLYZER)
                                && this.getEnableElectrolyzer().getNextWriteValue().isPresent() == false) && (this.getEnableSignalChannel().getNextWriteValue().isPresent()
                                || this.isRunning && this.timerHandler.checkTimeIsUp(ENABLE_SIGNAL_IDENTIFIER) == false)) {
                            this.isRunning = true;
                            this.timerHandler.resetTimer(ENABLE_SIGNAL_IDENTIFIER);

                            this.getEnableElectrolyzer().setNextWriteValueFromObject(this.getEnableSignalChannel().getNextWriteValueAndReset().orElse(false));
                            this.getPowerPercent().setNextWriteValueFromObject(this.getSetPointPowerChannel().getNextValue().orElse(100.d));

                        } else {
                            this.isRunning = false;
                            this.getEnableElectrolyzer().setNextWriteValue(false);
                            this.getPowerPercent().setNextWriteValue(0.f);
                        }


                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.warn("Couldn't proceed to write EnableSignal etc in " + super.id() + " Reason: " + e.getMessage());
                    }

                    this.updateWriteValuesToModbus();

                    break;
                }
        }
    }

    /**
     * Updates the PowerPercentValue to the ModbusChannel, that will be written and handled by the FC task.
     * Remember to configure the Channel. In that case external Components can write into an Electrolyzer interface / EnableSignal
     * And the ModbusElectrolyzer will map the PercentValue and EnableSignal to the ModbusChannel.
     */
    private void updateWriteValuesToModbus() {
        this.getEnableElectrolyzer().setNextValue(this.getEnableElectrolyzer().getNextWriteValue().orElse(false));
        if (this.getPowerPercent().getNextWriteValue().isPresent()) {
            this.getPowerPercent().setNextValue(this.getPowerPercent().getNextWriteValue().get());
            if (this.getModbusConfig().containsKey(this._getPowerPercentLongChannel().channelId())) {
                this.handleChannelWriteFromOriginalToModbus(this._getPowerPercentLongChannel(), this.getPowerPercent());
            } else if (this.getModbusConfig().containsKey(this._getPowerPercentDoubleChannel().channelId())) {
                this.handleChannelWriteFromOriginalToModbus(this._getPowerPercentDoubleChannel(), this.getPowerPercent());
            }
        }
    }

    private void handleExceptionalState() {
        try {
            int signalValue = this.getExceptionalStateValue();
            this.getEnableElectrolyzer().setNextWriteValue(signalValue > 0);
            this.getPowerPercent().setNextWriteValueFromObject(signalValue);
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't apply Exceptional State in : " + super.id() + " Reason: " + e.getMessage());
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
