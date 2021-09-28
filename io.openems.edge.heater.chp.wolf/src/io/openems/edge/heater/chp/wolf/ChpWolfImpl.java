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
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.Chp;
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heater.chp.wolf.api.ChpWolfChannel;
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

import java.util.Optional;


/**
 * This module reads the most important variables available via Modbus from a Wolf chp and maps them to OpenEMS
 * channels. The module is written to be used with the Heater interface EnableSignal methods and ExceptionalState.
 * When setEnableSignal() from the Heater interface is set to true with no other parameters like electric power specified,
 * the chp will turn on with default settings. The default settings are configurable in the config.
 * The chp can be controlled with setElectricPowerSetpoint() (set power in kW). Set point methods from the Heater
 * interface (setTemperatureSetpoint(), setHeatingPowerSetpoint() and setHeatingPowerPercentSetpoint()) are not supported.
 * This chp has two other control methods besides setElectricPowerSetpoint(), which are setFeedInSetpoint() and
 * setReserveSetpoint(). The chp changes to the corresponding mode when a set point method is used.
 * If different types of set point methods are used in the same cycle, the hierarchy is: electric power > feed-in > reserve.
 * The lower hierarchy setting will be ignored.
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
		ExceptionalState, ChpWolfChannel {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager cpm;

	private final Logger log = LoggerFactory.getLogger(ChpWolfImpl.class);
	private boolean printInfoToLog;
	private int commandCycler = 0;
	private static final int ENGINE_RPM_RUNNING_INDICATOR = 10; // If rpm is above this value, consider the chp to be running.

	private boolean readOnly = false;
	private boolean startupStateChecked = false;
	private boolean readyForCommands = false;
	private int chpMaxElectricPower;

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
				ChpWolfChannel.ChannelId.values(),
				Chp.ChannelId.values(),
				Heater.ChannelId.values(),
				ExceptionalState.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
		this.printInfoToLog = config.printInfoToLog();
		this.chpMaxElectricPower = config.chpMaxElectricPower();
		this.readOnly = config.readOnly();
		if (this.isEnabled() == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		}

		if (this.readOnly == false) {
			this.startupStateChecked = false;
			this.setElectricPowerSetpoint(config.defaultSetPointElectricPower());
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
						m(ChpWolfChannel.ChannelId.HR2_STATUS_BITS1, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(11, Priority.HIGH,
						m(ChpWolfChannel.ChannelId.HR11_STATUS_BITS2, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(27, Priority.LOW,
						m(Heater.ChannelId.FLOW_TEMPERATURE, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(32, Priority.LOW,
						m(ChpWolfChannel.ChannelId.HR32_BUFFERTANK_TEMP_TOP, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolfChannel.ChannelId.HR33_BUFFERTANK_TEMP_MIDDLE, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolfChannel.ChannelId.HR34_BUFFERTANK_TEMP_BOTTOM, new UnsignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(263, Priority.HIGH,
						m(Chp.ChannelId.EFFECTIVE_ELECTRIC_POWER, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(314, Priority.HIGH,
						m(ChpWolfChannel.ChannelId.HR314_RPM, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(3588, Priority.HIGH,
						m(ChpWolfChannel.ChannelId.HR3588_RUNTIME, new UnsignedDoublewordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolfChannel.ChannelId.HR3590_ENGINE_STARTS, new UnsignedWordElement(2),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(3596, Priority.LOW,
						m(ChpWolfChannel.ChannelId.HR3596_ELECTRICAL_WORK, new UnsignedDoublewordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				)
		);
		if (this.readOnly == false) {
			protocol.addTasks(
					new FC16WriteRegistersTask(6358,
							m(ChpWolfChannel.ChannelId.HR6358_WRITE_BITS1, new UnsignedWordElement(0),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(ChpWolfChannel.ChannelId.HR6359_WRITE_BITS2, new UnsignedWordElement(1),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(ChpWolfChannel.ChannelId.HR6360_WRITE_BITS3, new UnsignedWordElement(2),
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
		if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
			this.channelmapping();
			if (this.printInfoToLog) {
				this.printInfo();
			}
		}
		if (this.readOnly == false && this.readyForCommands && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
			this.writeCommands();
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
			if ((statusBits40003 & 0b0100000000000) == 0) {    // Handbuch sagt hier 0=ein, also vermutlich Störung ist wenn hier 0 ist.
				warnMessage = warnMessage + "No flow detected at coolant circuit (Durchflußwaechter Kuehlwasserkreis Gasmotor misst keinen Fluß), ";
			}
			if ((statusBits40003 & 0b01000000000000) == 0b01000000000000) {
				warnMessage = warnMessage + "Engine temperature protection triggered (Sicherheitstempaturbegrenzer Gasmotor geschaltet), ";
			}
			if ((statusBits40003 & 0b010000000000000) == 0b010000000000000) {
				warnMessage = warnMessage + "Collant pump protection triggered (Motorschutz Pumpe Kuehlwasser ausgeloest), ";
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
		this.readyForCommands = false;
		if (getRpm().isDefined()) {
			this.readyForCommands = true;
			if (getRpm().get() > ENGINE_RPM_RUNNING_INDICATOR) {
				this._setHeaterState(HeaterState.HEATING.getValue());
			} else {
				if (statusError) {
					this._setHeaterState(HeaterState.BLOCKED.getValue());
				} else if (statusReady) {
					this._setHeaterState(HeaterState.STANDBY.getValue());
				} else {
					this._setHeaterState(HeaterState.UNDEFINED.getValue());
				}
			}
		} else {
			this._setHeaterState(HeaterState.UNDEFINED.getValue());
		}
		this.getHeaterStateChannel().nextProcessImage();
	}

	/**
	 * Determine commands and send them to the heater.
	 */
	protected void writeCommands() {

		// Handle EnableSignal.
		boolean turnOnChp = this.enableSignalHandler.deviceShouldBeHeating(this);

		// At startup, check if chp is already running. If yes, keep it running by sending 'EnableSignal = true' to
		// yourself once. This gives controllers until the EnableSignal timer runs out to decide the state of the chp.
		// This avoids a chp restart if the controllers want the chp to stay on. -> Longer chp lifetime.
		// Without this function, the chp will always switch off at startup because EnableSignal starts as ’false’.
		if (this.startupStateChecked == false) {
			this.startupStateChecked = true;
			turnOnChp = (HeaterState.valueOf(this.getHeaterState().orElse(-1)) == HeaterState.HEATING);
			if (turnOnChp) {
				try {
					this.getEnableSignalChannel().setNextWriteValue(true);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.log.warn("Couldn't write in Channel " + e.getMessage());
				}
			}
		}

		// Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
		if (this.useExceptionalState) {
			boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
			if (exceptionalStateActive) {
				int exceptionalStateValue = this.getExceptionalStateValue();
				if (exceptionalStateValue <= 0) {
					turnOnChp = false;
				} else {
					// When ExceptionalStateValue is between 0 and 100, set Chp to this PowerPercentage.
					turnOnChp = true;
					exceptionalStateValue = Math.min(exceptionalStateValue, 100);
					int electricPowerSetpoint = (int)Math.round(this.chpMaxElectricPower * exceptionalStateValue / 100.0);
					try {
						this.setElectricPowerSetpoint(electricPowerSetpoint);
					} catch (OpenemsNamedException e) {
						this.log.warn("Couldn't write in Channel " + e.getMessage());
					}
				}
			}
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
		this.commandCycler++;
		if (this.commandCycler == 1) {
			int onOffValue = 0;
			if (turnOnChp) {
				onOffValue = 1;
			}
			try {
				setWriteBits1(2);
				setWriteBits2(onOffValue);
				setWriteBits3(setOnOff);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "Error setting next write value: " + e);
			}
		} else {
			this.commandCycler = 0;
			Optional<Double> electricPowerOptional = this.getElectricPowerSetpointChannel().getNextWriteValueAndReset();
			Optional<Integer> feedInOptional = this.getFeedInSetpointChannel().getNextWriteValueAndReset();
			Optional<Integer> reserveOptional = this.getReserveSetpointChannel().getNextWriteValueAndReset();

			// Hierarchy: electric power > feed-in > reserve.
			if (electricPowerOptional.isPresent()) {
				int electricPowerSetpoint = (int)Math.round(electricPowerOptional.get());
				// Update channel.
				this._setElectricPowerSetpoint(electricPowerSetpoint);
				try {
					setWriteBits1(2);
					setWriteBits2(electricPowerSetpoint);
					setWriteBits3(setElectricPower);
				} catch (OpenemsNamedException e) {
					this.logError(this.log, "Error setting next write value: " + e);
				}
			} else if (feedInOptional.isPresent()) {
				int feedInSetpoint = feedInOptional.get();
				// Update channel.
				this.getFeedInSetpointChannel().setNextValue(feedInSetpoint);
				try {
					setWriteBits1(2);
					setWriteBits2(feedInSetpoint);
					setWriteBits3(setFeedInManagement);
				} catch (OpenemsNamedException e) {
					this.logError(this.log, "Error setting next write value: " + e);
				}
			} else if (reserveOptional.isPresent()) {
				int reserveSetpoint = reserveOptional.get();
				// Update channel.
				this.getReserveSetpointChannel().setNextValue(reserveSetpoint);
				try {
					setWriteBits1(2);
					setWriteBits2(reserveSetpoint);
					setWriteBits3(setReserve);
				} catch (OpenemsNamedException e) {
					this.logError(this.log, "Error setting next write value: " + e);
				}
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
		this.logInfo(this.log, "Buffer tank temp top: " + (this.getBufferTankTempTop().orElse(0) / 10.0) + "°C");
		this.logInfo(this.log, "Buffer tank temp middle: " + (this.getBufferTankTempMiddle().orElse(0) / 10.0) + "°C");
		this.logInfo(this.log, "Buffer tank temp bottom: " + (this.getBufferTankTempBottom().orElse(0) / 10.0) + "°C");
		this.logInfo(this.log, "Electric power: " + this.getEffectiveElectricPower());
		this.logInfo(this.log, "Engine rpm: " + this.getRpm().get() + " rpm");
		this.logInfo(this.log, "Runtime: " + this.getRuntime().get() + " h");
		this.logInfo(this.log, "Engine starts: " + this.getEngineStarts().get());
		this.logInfo(this.log, "Produced electric work total: " + this.getElectricalWork().get() + " kWh");
		this.logInfo(this.log, "Warning message: " + this.getWarningMessage().get());
		this.logInfo(this.log, "Error message: " + this.getErrorMessage().get());
		this.logInfo(this.log, "");
		this.logInfo(this.log, "--Writable values--");
		this.logInfo(this.log, "EnableSignal: " + this.getEnableSignal());
		this.logInfo(this.log, "Set point electric power [kW]: " + this.getElectricPowerSetpoint().get());
		this.logInfo(this.log, "");
	}
}
