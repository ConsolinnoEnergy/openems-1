package io.openems.edge.heater.chp.wolf;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.configupdate.ConfigurationUpdate;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.Chp;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.StartupCheckHandler;
import io.openems.edge.heater.chp.wolf.api.ChpWolf;
import io.openems.edge.heater.chp.wolf.api.OperatingMode;
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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


/**
 * This module reads the most important variables available via Modbus from a Wolf chp and maps them to OpenEMS
 * channels. The module is written to be used with the Heater interface EnableSignal methods and ExceptionalState.
 * When setEnableSignal() from the Heater interface is set to true with no other parameters like electric power specified,
 * the chp will turn on with default settings. The default settings are configurable in the config.
 * The chp can be controlled with setElectricPowerSetpoint() (set power in kW). Set point methods from the Heater
 * interface (setTemperatureSetpoint(), setHeatingPowerSetpoint() and setHeatingPowerPercentSetpoint()) are not supported.
 * This chp has two other control methods besides setElectricPowerSetpoint(), which are setFeedInSetpoint() and
 * setReserveSetpoint(). The control method is specified in the config or by using the channel ’OperatingMode’.
 * If the chp is activated by ExceptionalState, it will convert the ExceptionalStateValue into a
 * setElectricPowerSetpoint() value.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Chp.Wolf",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS //
		})
public class ChpWolfImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		ExceptionalState, ChpWolf {

	@Reference
	protected ComponentManager cpm;

	@Reference
	protected ConfigurationAdmin ca;

	private final Logger log = LoggerFactory.getLogger(ChpWolfImpl.class);
	private boolean printInfoToLog;
	private int commandCycler = 0;
	private static final int ENGINE_RPM_RUNNING_INDICATOR = 10; // If rpm is above this value, consider the chp to be running.

	private boolean readOnly = false;
	private boolean startupStateChecked = false;
	private boolean readyForCommands = false;
	private int chpMaxElectricPower;
	private int electricPowerSetpoint;
	private int exceptionalStateValue;
	private boolean exceptionalStateActive;

	private EnableSignalHandler enableSignalHandler;
	private static final String ENABLE_SIGNAL_IDENTIFIER = "KW_ENERGY_SMARTBLOCK_ENABLE_SIGNAL_IDENTIFIER";
	private boolean useExceptionalState;
	private ExceptionalStateHandler exceptionalStateHandler;
	private static final String EXCEPTIONAL_STATE_IDENTIFIER = "KW_ENERGY_SMARTBLOCK_EXCEPTIONAL_STATE_IDENTIFIER";

	// This is essential for Modbus to work, but the compiler does not warn you when it is missing!
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public ChpWolfImpl() {
		super(OpenemsComponent.ChannelId.values(),
				ChpWolf.ChannelId.values(),
				Chp.ChannelId.values(),
				Heater.ChannelId.values(),
				ExceptionalState.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.ca,
				"Modbus", config.modbus_id());
		this.printInfoToLog = config.printInfoToLog();
		this.chpMaxElectricPower = config.chpMaxElectricPower();
		this.readOnly = config.readOnly();
		if (this.isEnabled() == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		}

		if (this.readOnly == false) {
			this.startupStateChecked = false;
			OperatingMode operatingMode = config.defaultOperatingMode();
			Map<String, Object> keyValueMap = new HashMap<>();

			// In case of invalid setting, default to ElectricPower mode and change value in config.
			if (operatingMode == OperatingMode.UNDEFINED) {
				keyValueMap.put("defaultOperatingMode", OperatingMode.ELECTRIC_POWER);
			}

			this.electricPowerSetpoint = config.defaultSetPointElectricPower();
			if (this.electricPowerSetpoint > this.chpMaxElectricPower) {	// If value was out of range, put corrected value in config.
				this.electricPowerSetpoint = this.chpMaxElectricPower;
				keyValueMap.put("defaultSetPointElectricPower", this.electricPowerSetpoint);
			}
			if (this.electricPowerSetpoint < 0) {
				this.electricPowerSetpoint = 0;
				keyValueMap.put("defaultSetPointElectricPower", this.electricPowerSetpoint);
			}

			if (keyValueMap.isEmpty() == false) { // Updating config restarts the module.
				try {
					ConfigurationUpdate.updateConfig(ca, this.servicePid(), keyValueMap);
				} catch (IOException e) {
					this.log.warn("Couldn't save new settings to config. " + e.getMessage());
				}
			}

			this._setOperatingMode(operatingMode);
			this.getOperatingModeChannel().nextProcessImage();
			this.initializeTimers(config);
		}
	}

	private void initializeTimers(Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
		timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, config.enableSignalTimerId(), config.waitTimeEnableSignal());
		this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
		this.useExceptionalState = config.useExceptionalState();
		if (this.useExceptionalState) {
			timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, config.exceptionalStateTimerId(), config.waitTimeExceptionalState());
			this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
		}
	}


	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		ModbusProtocol protocol = new ModbusProtocol(this,
				new FC3ReadRegistersTask(2, Priority.HIGH,
						m(ChpWolf.ChannelId.HR2_STATUS_BITS1, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(11, Priority.HIGH,
						m(ChpWolf.ChannelId.HR11_STATUS_BITS2, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(27, Priority.HIGH,
						m(Heater.ChannelId.FLOW_TEMPERATURE, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(32, Priority.HIGH,
						m(ChpWolf.ChannelId.HR32_BUFFERTANK_TEMP_TOP, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolf.ChannelId.HR33_BUFFERTANK_TEMP_MIDDLE, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolf.ChannelId.HR34_BUFFERTANK_TEMP_BOTTOM, new UnsignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(263, Priority.HIGH,
						m(Chp.ChannelId.EFFECTIVE_ELECTRIC_POWER, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(314, Priority.HIGH,
						m(ChpWolf.ChannelId.HR314_RPM, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(3588, Priority.LOW,
						m(ChpWolf.ChannelId.HR3588_RUNTIME, new UnsignedDoublewordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolf.ChannelId.HR3590_ENGINE_STARTS, new UnsignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(3596, Priority.LOW,
						m(ChpWolf.ChannelId.HR3596_ELECTRICAL_WORK, new UnsignedDoublewordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				)
		);
		if (this.readOnly == false) {
			protocol.addTasks(
					new FC16WriteRegistersTask(6358,
							m(ChpWolf.ChannelId.HR6358_WRITE_BITS1, new UnsignedWordElement(0),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(ChpWolf.ChannelId.HR6359_WRITE_BITS2, new UnsignedWordElement(1),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(ChpWolf.ChannelId.HR6360_WRITE_BITS3, new UnsignedWordElement(2),
									ElementToChannelConverter.DIRECT_1_TO_1)
					)
			);

		}
		return protocol;
	}

	@Override
	public void handleEvent(Event event) {
		if (this.isEnabled() == false) {
			return;
		}
		switch (event.getTopic()) {
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
				this.channelmapping();
				if (this.printInfoToLog) {
					this.printInfo();
				}
				break;
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
				if (this.readOnly == false && this.readyForCommands) {
					this.writeCommands();
				}
				break;
		}
	}

	/**
	 * Put values in channels that are not directly Modbus read values but derivatives.
	 */
	protected void channelmapping() {

		// Parse status bits.
		int statusBits40003 = 0;
		int statusBits40012 = 0;
		boolean statusReady = false;
		boolean statusError = false;
		if (getStatusBits40003Channel().value().isDefined() && getStatusBits40012Channel().value().isDefined()) {
			String warnMessage = "";
			statusBits40003 = getStatusBits40003().get();
			if ((statusBits40003 & 0b0100000) == 0b0100000) {
				warnMessage = warnMessage + "Coolant pressure gas engine at minimum (Kuehlwasserdruck minimum Gasmotor), ";
			}
			if ((statusBits40003 & 0b01000000) == 0b01000000) {
				warnMessage = warnMessage + "Oil tank at minimum (Tank Oelvorlage auf Minimum), ";
			}
			if ((statusBits40003 & 0b010000000) == 0b010000000) {
				warnMessage = warnMessage + "Oil pressure gas engine at minimum (Schmieroeldruck Gasmotor minimum), ";
			}
			if ((statusBits40003 & 0b0100000000) == 0b0100000000) {
				warnMessage = warnMessage + "Generator protection triggered (Generatorschutz ausgeloest), ";
			}
			if ((statusBits40003 & 0b01000000000) == 0b01000000000) {
				warnMessage = warnMessage + "Grid and device protection triggered by external (NA-Schutz Extern ausgeloest), ";
			}
			if ((statusBits40003 & 0b010000000000) == 0b010000000000) {
				warnMessage = warnMessage + "Gas pressure too low (Erdgasdruck an der Gasregelstrecke zu gering), ";
			}
			if ((statusBits40003 & 0b0100000000000) == 0) {    // Manual says 0=on, so the warning should probably be trigger when this is off.
				warnMessage = warnMessage + "No flow detected at coolant circuit (Durchflußwaechter Kuehlwasserkreis Gasmotor misst keinen Fluß), ";
			}
			if ((statusBits40003 & 0b01000000000000) == 0b01000000000000) {
				warnMessage = warnMessage + "Engine temperature protection triggered (Sicherheitstempaturbegrenzer Gasmotor geschaltet), ";
			}
			if ((statusBits40003 & 0b010000000000000) == 0b010000000000000) {
				warnMessage = warnMessage + "Coolant pump protection triggered (Motorschutz Pumpe Kuehlwasser ausgeloest), ";
			}
			if ((statusBits40003 & 0b0100000000000000) == 0b0100000000000000) {
				warnMessage = warnMessage + "Heating circuit pump protection triggered (Motorschutz Pumpe Heizung zum Verteiler ausgeloest), ";
			}
			if ((statusBits40003 & 0b01000000000000000) == 0b01000000000000000) {
				warnMessage = warnMessage + "Noise cover fan protection triggered (Sicherungsautomat Luefter Schallhaube ausgeloest), ";
			}

			statusBits40012 = getStatusBits40012().get();
			if ((statusBits40012 & 0b010000000000000) == 0b010000000000000) {
				warnMessage = warnMessage + "Maintenance interval reached (Meldung Wartungintervall erreicht), ";
			}
			if (warnMessage.length() > 0) {
				warnMessage = warnMessage.substring(0, warnMessage.length() - 2) + ".";
			} else {
				warnMessage = "No warning";
			}
			this._setWarningMessage(warnMessage);

			if ((statusBits40012 & 0b01000000000) == 0b01000000000) {
				statusReady = true;
			}
			if ((statusBits40012 & 0b0100000000000) == 0b0100000000000) {
				statusError = true;
				this._setErrorMessage("An unknown error ocurred");
			} else {
				this._setErrorMessage("No error");
			}
		} else {
			this._setWarningMessage("Modbus not connected");
			this._setErrorMessage("Modbus not connected");
		}
		this.getWarningMessageChannel().nextProcessImage();
		this.getErrorMessageChannel().nextProcessImage();
		
		// Parse status by checking engine rpm.
		if (getRpm().isDefined()) {
			this.readyForCommands = true;
			if (getRpm().get() > ENGINE_RPM_RUNNING_INDICATOR) {
				this._setHeaterState(HeaterState.RUNNING.getValue());
			} else {
				if (statusError) {
					this._setHeaterState(HeaterState.BLOCKED_OR_ERROR.getValue());
				} else if (statusReady) {
					this._setHeaterState(HeaterState.STANDBY.getValue());
				} else {
					this._setHeaterState(HeaterState.UNDEFINED.getValue());
				}
			}
		} else {
			this.readyForCommands = false;
			this._setHeaterState(HeaterState.UNDEFINED.getValue());
		}
		this.getHeaterStateChannel().nextProcessImage();
	}

	/**
	 * Determine commands and send them to the heater.
	 * The channel EFFECTIVE_ELECTRIC_POWER_SETPOINT is not directly mapped to modbus. Its nextWriteValue is collected
	 * manually, so the value can be stored locally and manipulated before sending it to the heater. A duplicate ’private’
	 * channel is then used for the modbus writes.
	 * The benefit of this design is that when ExceptionalState is active and applies it's own heatingPowerPercentSetpoint,
	 * the previous set point is saved. Also, it is still possible to write to the channel during ExceptionalState, to
	 * change the value that will be used after exceptional state ends.
	 */
	protected void writeCommands() {

		// Update channel.
		Optional<Double> electricPowerOptional = this.getElectricPowerSetpointChannel().getNextWriteValueAndReset();
		electricPowerOptional.ifPresent(value ->
				this.electricPowerSetpoint = TypeUtils.fitWithin(0, this.chpMaxElectricPower, (int) Math.round(value)));


		// Collect operating mode channel ’nextWrite’.
		Optional<Integer> operatingModeOptional = this.getOperatingModeChannel().getNextWriteValueAndReset();
		if (operatingModeOptional.isPresent()) {
			int enumAsInt = operatingModeOptional.get();
			// Restrict to valid write values.
			if (enumAsInt >= 0 && enumAsInt <= 2) {
				OperatingMode operatingMode = OperatingMode.valueOf(enumAsInt);

				/* Check if value is different. If yes do config update, so mode does not change back on restart. Doing
				   a config update restarts the module, the new operating mode is then applied in activate(). */
				if (getOperatingMode().asEnum() != operatingMode) {
					try {
						ConfigurationUpdate.updateConfig(ca, this.servicePid(), "defaultOperatingMode", operatingMode);
					} catch (IOException e) {
						this.log.warn("Couldn't save new operating mode setting to config. " + e.getMessage());
					}
				}
			}
		}

		// Handle EnableSignal.
		boolean turnOnChp = this.enableSignalHandler.deviceShouldBeHeating(this);

		// Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
		this.exceptionalStateValue = 0;
		this.exceptionalStateActive = false;
		if (this.useExceptionalState) {
			this.exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
			if (this.exceptionalStateActive) {
				this.exceptionalStateValue = this.getExceptionalStateValue();
				if (this.exceptionalStateValue <= this.DEFAULT_MIN_EXCEPTIONAL_VALUE) {
					turnOnChp = false;
				} else {
					turnOnChp = true;
					this.exceptionalStateValue = Math.min(this.exceptionalStateValue, this.DEFAULT_MAX_EXCEPTIONAL_VALUE);
				}
			}
		}

		// Check heater state at startup. Avoid turning off heater just because EnableSignal initial value is ’false’.
		if (this.startupStateChecked == false) {
			this.startupStateChecked = true;
			turnOnChp = StartupCheckHandler.deviceAlreadyHeating(this, this.log);
		}
		
		/*
		Write bits mapping.
		This chp has an unusual way of handling write commands. Instead of mapping one command to one
		register, four commands are mapped to three registers that need to be set simultaneously.
		HR6358 - always 2.
		HR6359 - the value you want to write.
		HR6360 - code deciding which command it is.
				35 = set point electric power [kW] (Sollwert elektrische Leistung in kW).
				36 = set point feed-in management, optional (Sollwert Einspeisemanagement).
				37 = Reserve (?)
				38 = on/off, send 1 for on, 0 for off.
		You can only send one command per cycle (need to write all three registers for one command).
		 */
		final int setElectricPower = 35;
		final int setFeedInManagement = 36;
		final int setReserve = 37;
		final int setOnOff = 38;

		int bits2value = 0;
		int bits3value = 0;
		boolean setValues = false;
		this.commandCycler++;
		if (this.commandCycler == 1) {
			int onOffValue = 0;
			if (turnOnChp) {
				onOffValue = 1;
			}
			bits2value = onOffValue;
			bits3value = setOnOff;
			setValues = true;
		} else {
			this.commandCycler = 0;
			if (this.getOperatingMode().isDefined()) {
				OperatingMode operatingMode = this.getOperatingMode().asEnum();
				if (operatingMode == OperatingMode.UNDEFINED || exceptionalStateActive) {
					// Exceptional state uses set point power.
					operatingMode = OperatingMode.ELECTRIC_POWER;
				}
				switch (operatingMode) {
					case FEED_IN_MANAGEMENT:
						// Update channel.
						Optional<Integer> feedInOptional = this.getFeedInSetpointChannel().getNextWriteValueAndReset();
						if (feedInOptional.isPresent()) {
							this._setFeedInSetpoint(feedInOptional.get());
							this.getFeedInSetpointChannel().nextProcessImage();
						}
						if (getFeedInSetpoint().isDefined()) {
							bits2value = getFeedInSetpoint().get();
							bits3value = setFeedInManagement;
							setValues = true;
						}
						break;
					case RESERVE:
						// Update channel.
						Optional<Integer> reserveOptional = this.getReserveSetpointChannel().getNextWriteValueAndReset();
						if (reserveOptional.isPresent()) {
							this._setReserveSetpoint(reserveOptional.get());
							this.getReserveSetpointChannel().nextProcessImage();
						}
						if (getReserveSetpoint().isDefined()) {
							bits2value = getReserveSetpoint().get();
							bits3value = setReserve;
							setValues = true;
						}
						break;
					case ELECTRIC_POWER:
					default:
						int electricPowerSetpointWrite = this.electricPowerSetpoint;
						if (exceptionalStateActive) {
							electricPowerSetpointWrite = (int) Math.round(this.chpMaxElectricPower * exceptionalStateValue / 100.0);
						}
						this._setElectricPowerSetpoint(electricPowerSetpointWrite);
						bits2value = electricPowerSetpointWrite;
						bits3value = setElectricPower;
						setValues = true;
				}
			}
		}
		if (setValues) {
			try {
				setWriteBits1(2);
				setWriteBits2(bits2value);
				setWriteBits3(bits3value);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Error setting next write value: " + e);
			}
		}
	}

	/**
	 * Information that is printed to the log if ’print info to log’ option is enabled.
	 */
	protected void printInfo() {
		this.logInfo(this.log, "--Chp Wolf--");
		this.logInfo(this.log, "Chp state: " + this.getHeaterState());
		this.logInfo(this.log, "Flow temperature: " + this.getFlowTemperature());
		this.logInfo(this.log, "Return temperature: " + this.getReturnTemperature());
		this.logInfo(this.log, "Buffer tank temp top: " + this.getBufferTankTempTop());
		this.logInfo(this.log, "Buffer tank temp middle: " + this.getBufferTankTempMiddle());
		this.logInfo(this.log, "Buffer tank temp bottom: " + this.getBufferTankTempBottom());
		this.logInfo(this.log, "Electric power: " + this.getEffectiveElectricPower());
		this.logInfo(this.log, "Set point electric power: " + this.getElectricPowerSetpoint());
		this.logInfo(this.log, "Engine rpm: " + this.getRpm());
		this.logInfo(this.log, "Runtime: " + this.getRuntime());
		this.logInfo(this.log, "Engine starts: " + this.getEngineStarts());
		this.logInfo(this.log, "Produced electric work total: " + this.getElectricalWork());
		this.logInfo(this.log, "Heater state: " + this.getHeaterState());
		if (this.useExceptionalState) {
			this.logInfo(this.log, "Exceptional state: " + this.exceptionalStateActive + ", value: " + this.exceptionalStateValue);
		}
		this.logInfo(this.log, "Warning message: " + this.getWarningMessage().get());
		this.logInfo(this.log, "Error message: " + this.getErrorMessage().get());
		this.logInfo(this.log, "");
	}

	/**
	 * Returns the debug message.
	 *
	 * @return the debug message.
	 */
	public String debugLog() {
		String debugMessage = this.getHeaterState().asEnum().asCamelCase() //
				+ "|F:" + this.getFlowTemperature().asString() //
				+ "|R:" + this.getReturnTemperature().asString(); //
		if (this.getWarningMessage().get().equals("No warning") == false) {
			debugMessage = debugMessage + "|Warning";
		}
		if (this.getErrorMessage().get().equals("No error") == false) {
			debugMessage = debugMessage + "|Error";
		}
		return debugMessage;
	}
}
