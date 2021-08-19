package io.openems.edge.powerplant.combined;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.generic.AbstractGenericModbusComponent;
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
import io.openems.edge.powerplant.api.CombinedHeatPowerPlantModbus;
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
@Component(name = "Heater.PowerPlant.CombinedHeatPower",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})
public class CombinedHeatPowerPlantImpl extends AbstractGenericModbusComponent implements OpenemsComponent,
        CombinedHeatPowerPlant, Heater, HydrogenGenerator, ExceptionalState, EventHandler, CombinedHeatPowerPlantModbus {

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
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException, IOException {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());

        if (super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length)) {
            this.baseConfiguration();
        }
        this.hydrogenMode = config.hydrogenMode();
    }

    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException, IOException {
        super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                config.modbusBridgeId(), this.cpm, Arrays.asList(config.configurationList()));
        super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length);
        this.config = config;
        this.baseConfiguration();
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


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public void handleEvent(Event event) {

        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
            //handleChannelUpdate(Concrete..., _hasValue)
            //note -> Enable Signal stays as enable signal and hydrogen enable stays as hydrogen
        }

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

                //update WriteValueToModbus


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
