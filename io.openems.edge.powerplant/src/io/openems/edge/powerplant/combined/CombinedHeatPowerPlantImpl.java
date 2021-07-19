package io.openems.edge.powerplant.combined;

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
import io.openems.edge.generator.api.Generator;
import io.openems.edge.generator.api.HydrogenGenerator;
import io.openems.edge.generator.api.HydrogenMode;
import io.openems.edge.heater.Heater;
import io.openems.edge.powerplant.api.CombinedHeatPowerPlant;
import io.openems.edge.powerplant.api.PowerPlant;
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
@Component(name = "Heater.PowerPlant.CombinedHeatPower",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        properties = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})
public class CombinedHeatPowerPlantImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, CombinedHeatPowerPlant, Heater, HydrogenGenerator, ExceptionalState, EventHandler {

    private final Logger log = LoggerFactory.getLogger(CombinedHeatPowerPlantImpl.class);


    private TimerHandler timerHandler;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "ELECTROLYZER_ENABLE_SIGNAL_IDENTIFIER";
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "ELECTROLYZER_EXCEPTIONAL_STATE_IDENTIFIER";
    private boolean useExceptionalState;
    private HydrogenMode hydrogenMode;

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

    public CombinedHeatPowerPlantImpl() {
        super(OpenemsComponent.ChannelId.values(),
                CombinedHeatPowerPlant.ChannelId.values(),
                PowerPlant.ChannelId.values(),
                Generator.ChannelId.values(),
                HydrogenGenerator.ChannelId.values());
    }

    protected Config config;

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
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
            this.hydrogenMode = config.hydrogenMode();
        }
    }

    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
        //TODO Filter for -1 --> any Channel is optional if address < 0
        ModbusProtocol protocolToReturn = new ModbusProtocol(this,
                new FC4ReadInputRegistersTask(this.config.modbusRegisterWMZEnergyAmount(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.WMZ_ENERGY_AMOUNT, new UnsignedWordElement(this.config.modbusRegisterWMZEnergyAmount()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterWMZTempSource(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.WMZ_TEMP_SOURCE, new UnsignedWordElement(this.config.modbusRegisterWMZTempSource()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterWMZTempSink(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.WMZ_TEMP_SINK, new UnsignedWordElement(this.config.modbusRegisterWMZTempSink()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterWMZTPower(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.WMZ_POWER, new UnsignedWordElement(this.config.modbusRegisterWMZTPower()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterGasMeter(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.WMZ_GAS_METER, new UnsignedWordElement(this.config.modbusRegisterGasMeter()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterGasKind(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.GAS_KIND, new UnsignedWordElement(this.config.modbusRegisterGasKind()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterCurrentPower(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.CURRENT_POWER, new UnsignedWordElement(this.config.modbusRegisterCurrentPower()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterTargetPower(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.TARGET_POWER, new UnsignedWordElement(this.config.modbusRegisterTargetPower()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterHoursAfterLastService(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.HOURS_AFTER_LAST_SERVICE, new UnsignedWordElement(this.config.modbusRegisterHoursAfterLastService()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterSecurityOffExtern(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.SECURITY_OFF_EXTERN, new UnsignedWordElement(this.config.modbusRegisterSecurityOffExtern()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterStartRequestEvu(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.REQUIRED_ON_EVU, new UnsignedWordElement(this.config.modbusRegisterStartRequestEvu()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterStartRequestExtern(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.REQUIRED_ON_EXTERN, new UnsignedWordElement(this.config.modbusRegisterStartRequestExtern()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterStopRequestEVUExtern(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.SECURITY_OFF_EVU, new UnsignedWordElement(this.config.modbusRegisterStopRequestEVUExtern()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterStopRequestEVUExternGridDisconnect(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.SECURITY_OFF_GRID_FAIL, new UnsignedWordElement(this.config.modbusRegisterStopRequestEVUExternGridDisconnect()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterElectricityProduced(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.ELECTRICITY_ENERGY_PRODUCED, new UnsignedWordElement(this.config.modbusRegisterElectricityProduced()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                ),
                new FC4ReadInputRegistersTask(this.config.modbusRegisterElectricityPower(), Priority.HIGH,
                        m(CombinedHeatPowerPlant.ChannelId.ELECTRICITY_POWER, new UnsignedWordElement(this.config.modbusRegisterElectricityPower()),
                                ElementToChannelConverter.DIRECT_1_TO_1)
                )
        );
        if (this.config.controlMode().equals(ControlMode.READ_WRITE)) {
            protocolToReturn.addTasks(new FC5WriteCoilTask(this.config.modbusRegisterEnableSignal(), m(PowerPlant.ChannelId.SET_EXTERNAL_ENABLE_SIGNAL,
                    new CoilElement(this.config.modbusRegisterEnableSignal()))),
                    new FC6WriteRegisterTask(this.config.modbusRegisterWritePower(), m(PowerPlant.ChannelId.SET_EXTERNAL_POWER_LEVEL_PERCENT,
                            new UnsignedWordElement(this.config.modbusRegisterWritePower()), ElementToChannelConverter.DIRECT_1_TO_1)));
        }
        if (this.config.hydrogenMode().equals(HydrogenMode.ACTIVE)) {
            protocolToReturn.addTask(new FC5WriteCoilTask(this.config.modbusRegisterHydrogenUse(),
                    m(HydrogenGenerator.ChannelId.ENABLE_HYDROGEN_USE, new CoilElement(this.config.modbusRegisterHydrogenUse()))));
        }
        return protocolToReturn;

    }



    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            try {
                if (this.useExceptionalState) {
                    boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
                    if (exceptionalStateActive) {
                        this.handleExceptionalState();
                        return;
                    }
                }
                if (this.getEnableSignalChannel().getNextWriteValue().isPresent()
                        || this.isRunning && this.timerHandler.checkTimeIsUp(ENABLE_SIGNAL_IDENTIFIER) == false) {
                    this.isRunning = true;
                    this.timerHandler.resetTimer(ENABLE_SIGNAL_IDENTIFIER);
                    this.writeIntoComponents(this.getEnableSignalChannel().getNextWriteValueAndReset().orElse(false),
                            this.getSetPointPowerChannel().getNextValue().orElse(0.d), true);
                } else {
                    this.isRunning = false;
                    this.writeIntoComponents(false, 0.d, false);
                }


            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't proceed to write EnableSignal etc in " + super.id() + " Reason: " + e.getMessage());
            }
        }

    }

    private void writeIntoComponents(Boolean enableSignal, Double powerPercent, boolean hydrogenUse) throws OpenemsError.OpenemsNamedException {
        this.setExternalEnableSignalChannel().setNextWriteValueFromObject(enableSignal);
        this.setExternalPowerLevelChannel().setNextWriteValueFromObject(powerPercent);
        if (this.hydrogenMode.equals(HydrogenMode.ACTIVE)) {
            this.getEnableHydrogenUse().setNextWriteValueFromObject(hydrogenUse);
        }

    }

    private void handleExceptionalState() throws OpenemsError.OpenemsNamedException {
        int exceptionalStateValue = this.getExceptionalStateValue();
        boolean enableSignal = exceptionalStateValue > 0;
        this.writeIntoComponents(enableSignal, (double) exceptionalStateValue, enableSignal);
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
