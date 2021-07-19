package io.openems.edge.generator.electrolyzer;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;

import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.generator.api.ControlMode;
import io.openems.edge.generator.api.Electrolyzer;
import io.openems.edge.generator.api.Generator;
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
@Component(name = "Generator.HydrogenGenerator.Electrolyzer",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        properties = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})
public class ElectrolyzerImpl extends AbstractOpenemsModbusComponent implements Electrolyzer, OpenemsComponent, Heater, ExceptionalState, EventHandler, Generator {

    private final Logger log = LoggerFactory.getLogger(ElectrolyzerImpl.class);

    private TimerHandler timerHandler;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "ELECTROLYZER_ENABLE_SIGNAL_IDENTIFIER";
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "ELECTROLYZER_EXCEPTIONAL_STATE_IDENTIFIER";
    private boolean useExceptionalState;

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

    public ElectrolyzerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Electrolyzer.ChannelId.values(),
                Generator.ChannelId.values(),
                Heater.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    protected Config config;

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.config = config;
        if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId()) == false) {
            this.timerHandler = new TimerHandlerImpl(super.id(), this.cpm);
            this.timerHandler.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, config.timerNeedHeatResponse(), config.timeNeedHeatResponse());
            this.useExceptionalState = config.enableExceptionalStateHandling();
            if (this.useExceptionalState) {
                this.timerHandler.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, config.timerExceptionalState(), config.timeToWaitExceptionalState());
                this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(this.timerHandler, EXCEPTIONAL_STATE_IDENTIFIER);
            }
        }

    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

        ModbusProtocol protocolToReturn = new ModbusProtocol(this,
                new FC4ReadInputRegistersTask(this.config.modbusRegisterEnergyProduced(), Priority.HIGH,
                        m(Electrolyzer.ChannelId.WMZ_ENERGY_PRODUCED, new UnsignedWordElement(this.config.modbusRegisterEnergyProduced()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterWMZTempSource(), Priority.HIGH,
                        m(Electrolyzer.ChannelId.WMZ_TEMP_SOURCE, new UnsignedWordElement(this.config.modbusRegisterWMZTempSource()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterWMZTempSink(), Priority.HIGH,
                        m(Electrolyzer.ChannelId.WMZ_TEMP_SINK, new UnsignedWordElement(this.config.modbusRegisterWMZTempSink()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterWMZPower(), Priority.HIGH,
                        m(Electrolyzer.ChannelId.WMZ_POWER, new UnsignedWordElement(this.config.modbusRegisterWMZPower()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                )
        );

        if (this.config.controlMode().equals(ControlMode.READ_WRITE)) {
            protocolToReturn.addTasks(new FC5WriteCoilTask(this.config.modbusRegisterEnableSignal(), m(Electrolyzer.ChannelId.ENABLE_ELECTROLYZER,
                    new CoilElement(this.config.modbusRegisterEnableSignal()))),
                    new FC6WriteRegisterTask(this.config.modbusRegisterWritePower(), m(Electrolyzer.ChannelId.POWER_PERCENT,
                            new UnsignedWordElement(this.config.modbusRegisterWritePower()), ElementToChannelConverter.DIRECT_1_TO_1)));
        }

        return protocolToReturn;
    }


    @Override
    public void handleEvent(Event event) {
        switch (event.getTopic()) {
            case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
                break;
            case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
                if (this.useExceptionalState) {
                    boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
                    if (exceptionalStateActive) {
                        this.handleExceptionalState();
                        return;
                    }
                }
                try {
                    if (this.getEnableSignalChannel().getNextWriteValue().isPresent()
                            || this.isRunning && this.timerHandler.checkTimeIsUp(ENABLE_SIGNAL_IDENTIFIER) == false) {
                        this.isRunning = true;
                        this.timerHandler.resetTimer(ENABLE_SIGNAL_IDENTIFIER);

                        this.getEnableElectrolyzer().setNextWriteValueFromObject(this.getEnableSignalChannel().getNextWriteValueAndReset().orElse(false));
                        this.getPowerPercent().setNextWriteValueFromObject(this.getSetPointPowerChannel().getNextValue().orElse(0.d));

                    } else {
                        this.isRunning = false;
                        this.getEnableElectrolyzer().setNextWriteValue(false);
                        this.getPowerPercent().setNextWriteValue(0.f);
                    }


                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.warn("Couldn't proceed to write EnableSignal etc in " + super.id() + " Reason: " + e.getMessage());
                }
                break;
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
