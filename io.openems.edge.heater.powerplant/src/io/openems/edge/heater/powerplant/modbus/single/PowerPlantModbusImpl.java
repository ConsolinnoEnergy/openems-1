package io.openems.edge.heater.powerplant.modbus.single;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

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

import io.openems.edge.heater.api.EnergyControlMode;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterModbus;
import io.openems.edge.heater.electrolyzer.api.ControlMode;
import io.openems.edge.heater.electrolyzer.api.HydrogenMode;
import io.openems.edge.heater.powerplant.api.PowerPlant;
import io.openems.edge.heater.powerplant.api.PowerPlantModbus;

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

/**
 * A Generic PowerPlant Modbus Implementation.
 * On Configuration, you can Map Channel to
 * ModbusAddresses to evaluate ChannelValues etc.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.PowerPlant.Analog",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS})
public class PowerPlantModbusImpl extends AbstractGenericModbusComponent implements PowerPlant, OpenemsComponent, PowerPlantModbus, EventHandler,
        Heater, HeaterModbus, ExceptionalState {

    private final Logger log = LoggerFactory.getLogger(PowerPlantModbusImpl.class);
    private boolean isAutoRun;
    private HydrogenMode hydrogenMode = HydrogenMode.DEACTIVATED;

    @Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
    protected void setModbus(BridgeModbus modbus) {
        super.setModbus(modbus);
    }

    private TimerHandler timerHandler;
    private ExceptionalStateHandler exceptionalStateHandler;
    private static final String ENABLE_SIGNAL_IDENTIFIER = "ELECTROLYZER_ENABLE_SIGNAL_IDENTIFIER";
    private static final String EXCEPTIONAL_STATE_IDENTIFIER = "ELECTROLYZER_EXCEPTIONAL_STATE_IDENTIFIER";
    private boolean useExceptionalState;
    private EnergyControlMode energyControlMode = EnergyControlMode.KW;
    private ControlMode controlMode = ControlMode.READ;
    private boolean isRunning;

    @Reference
    ConfigurationAdmin cm;

    @Reference
    ComponentManager cpm;

    Config config;

    public PowerPlantModbusImpl() {
        super(OpenemsComponent.ChannelId.values(),
                PowerPlant.ChannelId.values(),
                PowerPlantModbus.ChannelId.values(),
                Heater.ChannelId.values(),
                HeaterModbus.ChannelId.values(),
                ExceptionalState.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, IOException, ConfigurationException {
        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
                config.modbusBridgeId(), this.cpm, Arrays.asList(config.configurationList()));
        if (super.update(this.cm, "channelIds", new ArrayList<>(this.channels()), this.config.channelIds().length)) {
            this.baseConfiguration();
        }
    }

    /**
     * Base configuration for a Heater extending from {@link AbstractGenericModbusComponent}.
     *
     * @throws OpenemsError.OpenemsNamedException if a component cannot be found.
     * @throws ConfigurationException             if existing component is of a wrong instance
     */
    private void baseConfiguration() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        this.controlMode = this.config.controlMode();
        this.energyControlMode = this.config.energyControlMode();
        this.hydrogenMode = this.config.hydrogenMode();
        this.timerHandler = new TimerHandlerImpl(super.id(), this.cpm);
        this.timerHandler.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, this.config.timerNeedHeatResponse(), this.config.timeNeedHeatResponse());
        this.useExceptionalState = this.config.enableExceptionalStateHandling();
        if (this.useExceptionalState) {
            this.timerHandler.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, this.config.timerExceptionalState(), this.config.timeToWaitExceptionalState());
            this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(this.timerHandler, EXCEPTIONAL_STATE_IDENTIFIER);
        }
        this.isAutoRun = this.config.autoRun();
        this.getDefaultActivePowerChannel().setNextValue(this.config.defaultRunPower());
        this.getDefaultActivePowerChannel().nextProcessImage();
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
        if (this.isEnabled()) {
            if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
                handleChannelUpdate(this.getEffectiveHeatingPowerChannel(), this._hasEffectivePowerKw());
                handleChannelUpdate(this.getEffectiveHeatingPowerPercentChannel(), this._hasEffectivePowerPercent());
                handleChannelUpdate(this.getFlowTemperatureChannel(), this._hasFlowTemp());
                handleChannelUpdate(this.getReturnTemperatureChannel(), this._hasReturnTemp());
                handleChannelUpdate(this.getMaximumKwChannel(), this._hasMaximumKw());
                handleChannelUpdate(this.getErrorOccurredChannel(), this._hasErrorOccurred());
                handleChannelUpdate(this.getHeatingPowerSetpointChannel(), this._hasReadSetPoint());
                handleChannelUpdate(this.getHeatingPowerPercentSetpointChannel(), this._hasReadSetPointPercent());
                handleChannelUpdate(this.getTemperatureSetpointChannel(), this._hasReadSetPointTemperature());
            } else if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
                if (this.controlMode.equals(ControlMode.READ_WRITE)) {
                    if (this.useExceptionalState) {
                        boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
                        if (exceptionalStateActive) {
                            this.handleExceptionalState();
                            return;
                        }
                    }
                    if (this.isAutoRun || (this.getEnableSignalChannel().getNextWriteValue().isPresent())
                            || (this.isRunning && this.timerHandler.checkTimeIsUp(ENABLE_SIGNAL_IDENTIFIER) == false)) {
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
                        //No Reset -> handled later
                        this.getEnableSignalChannel().setNextValue(this.getEnableSignalChannel().getNextWriteValue().orElse(false));
                    } else {
                        this.isRunning = false;
                        this.timerHandler.resetTimer(ENABLE_SIGNAL_IDENTIFIER);
                    }
                    this.updateWriteValuesToModbus();
                }
            }
        }
    }

    /**
     * Handles the ExceptionalState.
     */
    private void handleExceptionalState() {
        try {
            int signalValue = this.getExceptionalStateValue();
            this.getEnableSignalChannel().setNextWriteValueFromObject(signalValue > ExceptionalState.DEFAULT_MIN_EXCEPTIONAL_VALUE);
            switch (this.energyControlMode) {
                case KW:
                    this.getHeatingPowerSetpointChannel().setNextWriteValueFromObject(signalValue);
                    break;
                case PERCENT:
                    this.getHeatingPowerPercentSetpointChannel().setNextWriteValueFromObject(signalValue);
                    break;
                case TEMPERATURE:
                    this.getTemperatureSetpointChannel().setNextWriteValueFromObject(signalValue);
                    break;
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't apply Exceptional State in : " + super.id() + " Reason: " + e.getMessage());
        }
    }

    /**
     * Updates the PowerPercentValue to the ModbusChannel, that will be written and handled by the FC task.
     * Remember to configure the Channel. In that case external Components can write into an Electrolyzer interface / EnableSignal
     * And the ModbusElectrolyzer will map the PercentValue and EnableSignal to the ModbusChannel.
     */
    private void updateWriteValuesToModbus() {

        if (this.getModbusConfig().containsKey(this._getEnableSignalBoolean().channelId())) {
            this.handleChannelWriteFromOriginalToModbus(this._getEnableSignalBoolean(), this.getEnableSignalChannel());
        } else if (this.getModbusConfig().containsKey(this._getEnableSignalLong().channelId())) {
            boolean enabled = this.getEnableSignalChannel().getNextWriteValueAndReset().orElse(false);
            try {
                if (enabled) {
                    this._getEnableSignalLong().setNextWriteValueFromObject((long) this.config.defaultEnableSignalValue());
                } else {
                    this._getEnableSignalLong().setNextWriteValueFromObject((long) this.config.defaultDisableSignalValue());
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't apply EnableSignal");
            }
        }

        WriteChannel<?> choosenChannel = this.getDefaultActivePowerChannel();
        try {
            this.getDefaultActivePowerChannel().setNextWriteValueFromObject((long) this.config.defaultRunPower());
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't set DefaultRunPower Again");
        }
        switch (this.energyControlMode) {
            case KW:
                //TODO IF BOTH CONTROLMODES: HEAT AND ELECTROLYZER -> WHAT TO DO -> PRIORITY ETC
                if (this.getHeatingPowerSetpointChannel().getNextWriteValue().isPresent()) {
                    choosenChannel = this.getHeatingPowerSetpointChannel();
                }
                if (this.getModbusConfig().containsKey(this._getSetPointPowerLevelKwLong().channelId())) {
                    this.handleChannelWriteFromOriginalToModbus(this._getSetPointPowerLevelKwLong(), choosenChannel);
                } else if (this.getModbusConfig().containsKey(this._getSetPointPowerLevelKwDouble().channelId())) {
                    this.handleChannelWriteFromOriginalToModbus(this._getSetPointPowerLevelKwDouble(), choosenChannel);
                }
                break;
            case PERCENT:
                if (this.getHeatingPowerPercentSetpointChannel().getNextWriteValue().isPresent()) {
                    choosenChannel = this.getHeatingPowerPercentSetpointChannel();
                }
                if (this.getModbusConfig().containsKey(this._getSetPointPowerLevelPercentLong().channelId())) {
                    this.handleChannelWriteFromOriginalToModbus(this._getSetPointPowerLevelPercentLong(), choosenChannel);
                } else if (this.getModbusConfig().containsKey(this._getSetPointPowerLevelPercentDouble().channelId())) {
                    this.handleChannelWriteFromOriginalToModbus(this._getSetPointPowerLevelPercentDouble(), choosenChannel);
                }
                break;
            case TEMPERATURE:
                if (this.getTemperatureSetpointChannel().getNextWriteValue().isPresent()) {
                    choosenChannel = this.getTemperatureSetpointChannel();
                }
                if (this.getModbusConfig().containsKey(this._getSetPointTemperatureLong().channelId())) {
                    this.handleChannelWriteFromOriginalToModbus(this._getSetPointTemperatureLong(), choosenChannel);
                } else if (this.getModbusConfig().containsKey(this._getSetPointTemperatureDouble().channelId())) {
                    this.handleChannelWriteFromOriginalToModbus(this._getSetPointTemperatureDouble(), choosenChannel);
                }
                break;
        }
    }
}
