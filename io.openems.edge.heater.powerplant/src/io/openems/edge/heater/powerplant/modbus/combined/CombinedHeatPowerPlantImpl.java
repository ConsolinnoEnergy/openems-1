package io.openems.edge.heater.powerplant.modbus.combined;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.generic.AbstractGenericModbusComponent;

import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.EnergyControlMode;
import io.openems.edge.heater.electrolyzer.api.ControlMode;
import io.openems.edge.heater.electrolyzer.api.HydrogenMode;
import io.openems.edge.heater.Heater;
import io.openems.edge.heater.powerplant.api.PowerPlant;
import io.openems.edge.heater.powerplant.api.CombinedHeatPowerPlant;
import io.openems.edge.heater.powerplant.api.CombinedHeatPowerPlantModbus;
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
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})
public class CombinedHeatPowerPlantImpl extends AbstractGenericModbusComponent implements OpenemsComponent,
        CombinedHeatPowerPlant, Heater, ExceptionalState, EventHandler, CombinedHeatPowerPlantModbus {

    private final Logger log = LoggerFactory.getLogger(CombinedHeatPowerPlantImpl.class);
    private boolean isAutoRun;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private TimerHandler timerHandler;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "ELECTROLYZER_ENABLE_SIGNAL_IDENTIFIER";
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "ELECTROLYZER_EXCEPTIONAL_STATE_IDENTIFIER";
    private boolean useExceptionalState;
    private HydrogenMode hydrogenMode = HydrogenMode.DEACTIVATED;
    private EnergyControlMode energyControlMode = EnergyControlMode.KW;
    private ControlMode controlMode = ControlMode.READ;
    private boolean isRunning;

    @Reference
    protected ConfigurationAdmin cm;


    @Reference
    ComponentManager cpm;

    Config config;


    public CombinedHeatPowerPlantImpl() {
        super(OpenemsComponent.ChannelId.values(),
                CombinedHeatPowerPlant.ChannelId.values(),
                PowerPlant.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException, IOException {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                "Modbus", config.modbusBridgeId());

        if (super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length)) {
            this.baseConfiguration();
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

    private void baseConfiguration() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.controlMode = this.config.controlMode();
        this.energyControlMode = this.config.energyControlMode();
        this.timerHandler = new TimerHandlerImpl(super.id(), this.cpm);
        this.timerHandler.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, this.config.timerNeedHeatResponse(), this.config.timeNeedHeatResponse());
        this.useExceptionalState = this.config.enableExceptionalStateHandling();
        if (this.useExceptionalState) {
            this.timerHandler.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, this.config.timerExceptionalState(), this.config.timeToWaitExceptionalState());
            this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(this.timerHandler, EXCEPTIONAL_STATE_IDENTIFIER);
        }
        this.hydrogenMode = this.config.hydrogenMode();
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
            handleChannelUpdate(this.getElectricityPowerChannel(), this._hasElectricityPower());
            handleChannelUpdate(this.getElectricityEnergyProducedChannel(), this._hasElectricityEnergyProduced());
            handleChannelUpdate(this.getElectricityEnergyProducedChannel(), this._hasElectricityEnergyProduced());
            handleChannelUpdate(this.getSecurityOffGridFailChannel(), this._hasSecurityOffGridFail());
            handleChannelUpdate(this.getSecurityOffEVUChannel(), this._hasSecurityOffEVU());
            handleChannelUpdate(this.getSecurityOffGridFailChannel(), this._hasSecurityOffGridFail());
            handleChannelUpdate(this.getSecurityOffEVUChannel(), this._hasSecurityOffEVU());
            handleChannelUpdate(this.getRequiredOnExternChannel(), this._hasRequiredOnExtern());
            handleChannelUpdate(this.getRequiredOnEVUChannel(), this._hasRequiredOnEVU());
            handleChannelUpdate(this.getSecurityOffExternChannel(), this._hasSecurityOffExtern());
            handleChannelUpdate(this.getHoursAfterLastServiceChannel(), this._hasHoursAfterService());
            handleChannelUpdate(this.getWmzGasMeterPowerChannel(), this._hasWMZGasMeterPower());
            handleChannelUpdate(this.getWmzPowerChannel(), this._hasWMZPower());
            handleChannelUpdate(this.getWmzTempSinkChannel(), this._hasWMZTempSink());
            handleChannelUpdate(this.getWmzTempSourceChannel(), this._hasWMZTempSource());
            handleChannelUpdate(this.getWmzEnergyAmountChannel(), this._hasWMZEnergyAmount());

            handleChannelUpdate(this.getEffectivePowerChannel(), this._hasEffectivePowerKw());
            handleChannelUpdate(this.getEffectivePowerPercentChannel(), this._hasEffectivePowerPercent());
            handleChannelUpdate(this.getFlowTemperatureChannel(), this._hasFlowTemp());
            handleChannelUpdate(this.getReturnTemperatureChannel(), this._hasReturnTemp());
            handleChannelUpdate(this.getMaximumKwChannel(), this._hasMaximumKw());
            handleChannelUpdate(this.getErrorOccurredChannel(), this._hasErrorOccurred());

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
                    if (this.getEnableSignalChannel().getNextWriteValue().isPresent()) {
                        this.timerHandler.resetTimer(ENABLE_SIGNAL_IDENTIFIER);
                    } else {
                        try {
                            this.getEnableSignalChannel().setNextWriteValueFromObject(false);
                        } catch (OpenemsError.OpenemsNamedException e) {
                            this.log.warn("Couldn't apply false value to own enableSignal");
                        }
                    }
                    this.getEnableSignalChannel().setNextValue(this.getEnableSignalChannel().getNextWriteValue().orElse(false));
                } else {
                    this.isRunning = false;
                    this.timerHandler.resetTimer(ENABLE_SIGNAL_IDENTIFIER);
                }

                //update WriteValueToModbus
                this.updateWriteValueToModbus();

            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't proceed to write EnableSignal etc in " + super.id() + " Reason: " + e.getMessage());
            }
        }

    }

    private void updateWriteValueToModbus() {
        if (this.getModbusConfig().containsKey(this._getEnableSignalBoolean().channelId())) {
            this.handleChannelWriteFromOriginalToModbus(this._getEnableSignalBoolean(), this.getEnableSignalChannel());
        } else if (this.getModbusConfig().containsKey(this._getEnableSignalLong().channelId())) {
            this.handleChannelWriteFromOriginalToModbus(this._getEnableSignalLong(), this.getEnableSignalChannel());
        }

        WriteChannel<?> choosenChannel = this.getDefaultRunPower();
        switch (this.energyControlMode) {
            case KW:
                //TODO IF BOTH CONTROLMODES: HEAT AND ELECTROLYZER -> WHAT TO DO -> PRIORITY ETC
                if (this.getSetPointPowerChannel().getNextWriteValue().isPresent()) {
                    choosenChannel = this.getSetPointPowerChannel();
                }
                if (this.getModbusConfig().containsKey(this._getSetPointPowerLevelKwLong().channelId())) {
                    this.handleChannelWriteFromOriginalToModbus(this._getSetPointPowerLevelKwLong(), choosenChannel);
                } else if (this.getModbusConfig().containsKey(this._getSetPointPowerLevelKwDouble().channelId())) {
                    this.handleChannelWriteFromOriginalToModbus(this._getSetPointPowerLevelKwDouble(), choosenChannel);
                }
                break;
            case PERCENT:
                if (this.getSetPointPowerPercentChannel().getNextWriteValue().isPresent()) {
                    choosenChannel = this.getSetPointPowerPercentChannel();
                }
                if (this.getModbusConfig().containsKey(this._getSetPointPowerLevelPercentLong().channelId())) {
                    this.handleChannelWriteFromOriginalToModbus(this._getSetPointPowerLevelPercentLong(), choosenChannel);
                } else if (this.getModbusConfig().containsKey(this._getSetPointPowerLevelPercentDouble().channelId())) {
                    this.handleChannelWriteFromOriginalToModbus(this._getSetPointPowerLevelPercentDouble(), choosenChannel);
                }
                break;
            case TEMPERATURE:
                if (this.getSetPointTemperatureChannel().getNextWriteValue().isPresent()) {
                    choosenChannel = this.getSetPointTemperatureChannel();
                }
                if (this.getModbusConfig().containsKey(this._getSetPointTemperatureLong().channelId())) {
                    this.handleChannelWriteFromOriginalToModbus(this._getSetPointTemperatureLong(), choosenChannel);
                } else if (this.getModbusConfig().containsKey(this._getSetPointTemperatureDouble().channelId())) {
                    this.handleChannelWriteFromOriginalToModbus(this._getSetPointTemperatureDouble(), choosenChannel);
                }
                break;
        }
    }

    private void writeIntoComponents(Boolean enableSignal, Double powerPercent, boolean hydrogenUse) throws OpenemsError.OpenemsNamedException {
        this.getEnableSignalChannel().setNextWriteValueFromObject(enableSignal);
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
