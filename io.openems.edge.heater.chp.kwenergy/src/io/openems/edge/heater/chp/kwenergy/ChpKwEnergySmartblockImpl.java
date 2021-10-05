package io.openems.edge.heater.chp.kwenergy;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.heater.chp.kwenergy.api.ChpKwEnergySmartblock;
import io.openems.edge.heater.chp.kwenergy.api.ControlMode;
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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;


/**
 * This module reads the most important variables available via Modbus from a KW Energy Smartblock chp and maps them to
 * OpenEMS channels. The module is written to be used with the Heater interface EnableSignal methods and ExceptionalState.
 * When setEnableSignal() from the Heater interface is set to true with no other parameters like setPointPowerPercent()
 * specified, the chp will turn on with default settings. The default settings are configurable in the config.
 * The chp can be controlled with setHeatingPowerPercentSetpoint(), setElectricPowerSetpoint() or
 * setTemperatureSetpoint() and setHeatingPowerSetpoint() are not supported by this CHP.
 * If the chp is activated by ExceptionalState, it will switch to control mode power percent and use the
 * setHeatingPowerPercentSetpoint() specified by the ExceptionalStateValue. The chp will NOT automatically switch back
 * to its prior state when ExceptionalState ends.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Chp.KwEnergySmartblock",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS //
		})
public class ChpKwEnergySmartblockImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		ExceptionalState, ChpKwEnergySmartblock {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager cpm;

	private final Logger log = LoggerFactory.getLogger(ChpKwEnergySmartblockImpl.class);
	private boolean printInfoToLog;
	private LocalDateTime connectionTimestamp;
	private boolean connectionAlive = false;
	private boolean readOnly = false;
	private boolean startupStateChecked = false;
	private int maxChpPower;
	private int lastReceivedHandshake = 0;

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

	public ChpKwEnergySmartblockImpl() {
		super(OpenemsComponent.ChannelId.values(),
				ChpKwEnergySmartblock.ChannelId.values(),
				Chp.ChannelId.values(),
				Heater.ChannelId.values(),
				ExceptionalState.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbusBridgeId());

		this.printInfoToLog = config.printInfoToLog();
		this.connectionTimestamp = LocalDateTime.now().minusMinutes(5);	// Initialize with past time value so connection test is negative at start.
		this.maxChpPower = config.maxChpPower();
		this.readOnly = config.readOnly();
		this.startupStateChecked = false;
		if (this.isEnabled() == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		}

		if (this.readOnly == false) {
			this.initializeTimers(config);
			switch (config.controlMode()) {
				case "powerPercent":
					this.setControlMode(ControlMode.POWER_PERCENT.getValue());
					this.setHeatingPowerPercentSetpoint(config.defaultSetPointPowerPercent());
					break;
				case "power":
					this.setControlMode(ControlMode.POWER.getValue());
					double setpointValue = config.defaultSetPointElectricPower();
					setpointValue = Math.min(setpointValue, this.maxChpPower);
					setpointValue = Math.max(setpointValue, this.maxChpPower / 2.0);
					double calculatedPercent = setpointValue / this.maxChpPower;
					this.setHeatingPowerPercentSetpoint(calculatedPercent);
					break;
				case "consumption":
					this.setControlMode(ControlMode.CONSUMPTION.getValue());
					break;
			}
			this.getControlModeChannel().nextProcessImage();    // So ’value’ field of channel is filled immediately.
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
				// Holding register read.
				new FC3ReadRegistersTask(0, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR0_ERROR_BITS_1_to_16, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(16, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR16_STATUS_BITS_1_to_16, new UnsignedWordElement(16),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(20, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR20_STATUS_BITS_65_to_80, new UnsignedWordElement(20),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(24, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR24_ENGINE_TEMPERATURE, new UnsignedWordElement(24),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new UnsignedWordElement(25),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.FLOW_TEMPERATURE, new UnsignedWordElement(26),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(31, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR31_ENGINE_RPM, new UnsignedWordElement(31),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
				),
				new FC3ReadRegistersTask(34, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR34_EFFECTIVE_ELECTRIC_POWER, new UnsignedWordElement(34),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(48, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR48_CHP_MODEL, new UnsignedWordElement(48),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(62, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR62_OPERATING_HOURS, new UnsignedDoublewordElement(62),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpKwEnergySmartblock.ChannelId.HR64_ENGINE_START_COUNTER, new UnsignedDoublewordElement(64),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(70, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR70_ACTIVE_ENERGY, new UnsignedDoublewordElement(70),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpKwEnergySmartblock.ChannelId.HR72_MAINTENANCE_INTERVAL1, new UnsignedWordElement(72),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpKwEnergySmartblock.ChannelId.HR73_MAINTENANCE_INTERVAL2, new UnsignedWordElement(73),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpKwEnergySmartblock.ChannelId.HR74_MAINTENANCE_INTERVAL3, new UnsignedWordElement(74),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpKwEnergySmartblock.ChannelId.HR75_PRODUCED_HEAT, new UnsignedDoublewordElement(75),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(81, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR81_OPERATING_MODE, new UnsignedWordElement(81),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpKwEnergySmartblock.ChannelId.HR82_POWER_SETPOINT, new UnsignedWordElement(82),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(108, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR108_HANDSHAKE_OUT, new UnsignedWordElement(108),
								ElementToChannelConverter.DIRECT_1_TO_1)
						/*
						Makes no sense to read the command bits.
						m(ChpKwEnergySmartblockChannel.ChannelId.HR109_COMMAND_BITS_1_to_16, new UnsignedWordElement(109),
								ElementToChannelConverter.DIRECT_1_TO_1)
						No read for SET_POINT_POWER_PERCENT and SET_POINT_POWER, since those channels immediately
						copy setNextWrite to setNextValue.
						*/
				),
				new FC3ReadRegistersTask(112, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR112_GRID_POWER_DRAW, new UnsignedWordElement(112),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
				)
				/*
				new FC3ReadRegistersTask(119, Priority.HIGH,
						m(ChpKwEnergySmartblockChannel.ChannelId.HR119_HANDSHAKE_IN, new UnsignedWordElement(119),
								ElementToChannelConverter.DIRECT_1_TO_1)
				)
				*/

		);
		if (this.readOnly == false) {
			protocol.addTasks(
					new FC16WriteRegistersTask(109,
							m(ChpKwEnergySmartblock.ChannelId.HR109_COMMAND_BITS_1_to_16, new UnsignedWordElement(109),
									ElementToChannelConverter.DIRECT_1_TO_1),
							new DummyRegisterElement(110),
							m(Heater.ChannelId.SET_POINT_HEATING_POWER_PERCENT, new UnsignedWordElement(111),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
							m(ChpKwEnergySmartblock.ChannelId.HR112_GRID_POWER_DRAW, new UnsignedWordElement(112),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
					),
					new FC16WriteRegistersTask(119,
							m(ChpKwEnergySmartblock.ChannelId.HR119_HANDSHAKE_IN, new UnsignedWordElement(119),
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
		if (this.readOnly == false && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
			this.writeCommands();
		}
	}

	// Put values in channels that are not directly Modbus read values but derivatives.
	protected void channelmapping() {

		// Pass effective electric power to Chp interface channel.
		if (this.getModbusEffectiveElectricPower().isDefined()) {
			// ToDo: test if last digit is lost if channel from chp interface is used directly with channelconverter.
			//  If last digit is lost, channel HR112_GRID_POWER_DRAW needs to be changed.
			double powerInKiloWatt = this.getModbusEffectiveElectricPower().get() / 10.0;
			this._setEffectiveElectricPower(powerInKiloWatt);
			this.getEffectiveElectricPowerChannel().nextProcessImage();
		}

		// Parse error bits 1 to 16.
		String errorMessage = "";
		String warningMessage = "";
		String statusMessage = "";
		int errorBits1to16 = 0;
		if (this.getErrorBits1to16().isDefined()) {
			errorBits1to16 = this.getErrorBits1to16().get();
		}
		if ((errorBits1to16 & 0b01) == 0b01) {
			warningMessage = "Warning signal active, ";
		}
		boolean chpError = (errorBits1to16 & 0b010) == 0b010;
		this._setChpError(chpError);
		this.getChpErrorChannel().nextProcessImage();
		if (chpError) {
			errorMessage = "Error signal active, ";
		}
		if ((errorBits1to16 & 0b0100) == 0b0100) {
			warningMessage = warningMessage + "Maintenance needed, ";
			statusMessage = "Maintenance needed, ";
		}
		if ((errorBits1to16 & 0b0100000) == 0b0100000) {
			errorMessage = errorMessage + "Emergency shutdown active, ";
			statusMessage = statusMessage + "Emergency shutdown active, ";
		}
		if ((errorBits1to16 & 0b01000000) == 0b01000000) {
			warningMessage = warningMessage + "Coolant low, ";
			statusMessage = statusMessage + "Coolant low, ";
		}
		if ((errorBits1to16 & 0b0100000000) == 0b0100000000) {
			warningMessage = warningMessage + "CHP is leaking, ";
			statusMessage = statusMessage + "CHP is leaking, ";
		}

		// Parse status bits 1 to 16.
		int statusBits1to16 = 0;
		boolean statusBits1to16received = false;
		if (this.getStatusBits1to16().isDefined()) {
			statusBits1to16 = this.getStatusBits1to16().get();
			statusBits1to16received = true;
		}
		if (this.readOnly) {
			// Heartbeat is not used in readOnly mode. Use this to determine connectionAlive instead.
			this.connectionAlive = statusBits1to16received;
		}

		if ((statusBits1to16 & 0b01) == 0b01) {
			statusMessage = statusMessage + "CHP in automatic mode, ";
		}
		if ((statusBits1to16 & 0b010) == 0b010) {
			statusMessage = statusMessage + "Run clearance by external (Freigabe BHKW durch externes Signal), ";
		}
		if ((statusBits1to16 & 0b0100) == 0b0100) {
			statusMessage = statusMessage + "CHP in manual mode, ";
		}
		boolean chpReadySignal = (statusBits1to16 & 0b01000) == 0b01000;
		if (chpReadySignal) {
			statusMessage = statusMessage + "CHP ready, ";
		}
		boolean chpRunSignalRegistered = (statusBits1to16 & 0b010000) == 0b010000;
		this._setChpRelease(chpRunSignalRegistered);
		this.getChpReleaseChannel().nextProcessImage();
		if (chpRunSignalRegistered) {
			statusMessage = statusMessage + "Run signal received (Anforderung steht an), ";
		}
		boolean chpStartingUp = (statusBits1to16 & 0b0100000) == 0b0100000;
		if (chpStartingUp) {
			statusMessage = statusMessage + "Engine starting up, ";
		}
		boolean chpEngineRunning = (statusBits1to16 & 0b010000000) == 0b010000000;
		this._setChpEngineRunning(chpEngineRunning);
		this.getChpEngineRunningChannel().nextProcessImage();
		if (chpEngineRunning) {
			statusMessage = statusMessage + "Engine running, ";
		}

		// Parse status bits 65 to 80.
		int statusBits65to80 = 0;
		if (this.getStatusBits65to80().isDefined()) {
			statusBits65to80 = this.getStatusBits65to80().get();
		}
		if ((statusBits65to80 & 0b01000000000000) == 0b01000000000000) {
			statusMessage = statusMessage + "Operating mode 0 - fixed value (Betriebsart 0 - Festwert aktiv), ";
		}
		if ((statusBits65to80 & 0b010000000000000) == 0b010000000000000) {
			statusMessage = statusMessage + "Operating mode 0 - set point (Betriebsart 0 - Gleitwert aktiv), ";
		}
		if ((statusBits65to80 & 0b0100000000000000) == 0b0100000000000000) {
			statusMessage = statusMessage + "Operating mode 0 - grid power draw (Betriebsart 0 - Netzbezug aktiv), ";
		}

		// Parse operating mode.
		if (this.getOperatingMode().isDefined()) {
			int operatingModeCode = this.getStatusBits65to80().get();
			/*
			The manual has a list for the operating mode codes, but apparently they are wrong. Observed codes:
            2052 = 0b0100000000100  (mode ’power percent' (Freigabe durch Extern – Gleitwert), standby)
            41028 = 0b01010000001000100 (mode ’power percent' (Freigabe durch Extern – Gleitwert), running)
            This does not match what is listed in the manual. Because of that, parsing the operating mode code is not
            possible.
            */
			statusMessage = statusMessage + "Operating mode code: " + operatingModeCode + ", ";
		}

		// Set Heater interface STATUS channel
		if (this.connectionAlive == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		} else if (chpEngineRunning) {
			this._setHeaterState(HeaterState.HEATING.getValue());
		} else if (chpStartingUp) {
			this._setHeaterState(HeaterState.STARTING_UP_OR_PREHEAT.getValue());
		} else if (chpReadySignal) {
			this._setHeaterState(HeaterState.STANDBY.getValue());
		} else {
			// If the code gets to here, the state is undefined.
			this._setHeaterState(HeaterState.UNDEFINED.getValue());
		}
		this.getHeaterStateChannel().nextProcessImage();

		if (statusMessage.length() > 0) {
			statusMessage = statusMessage.substring(0, statusMessage.length() - 2) + ".";
		}
		this._setStatusMessage(statusMessage);
		this.getStatusMessageChannel().nextProcessImage();
		if (errorMessage.length() > 0) {
			errorMessage = errorMessage.substring(0, errorMessage.length() - 2) + ".";
		} else {
			errorMessage = "No error";
		}
		this._setErrorMessage(errorMessage);
		this.getErrorMessageChannel().nextProcessImage();
		if (warningMessage.length() > 0) {
			warningMessage = warningMessage.substring(0, warningMessage.length() - 2) + ".";
		} else {
			warningMessage = "No warning";
		}
		this._setWarningMessage(warningMessage);
		this.getWarningMessageChannel().nextProcessImage();
	}

	/**
	 * Determine commands and send them to the heater.
	 */
	protected void writeCommands() {

		// Handshake. Get value, send it back. If handshake is not sent, no writing of commands is possible.
		if (getHandshakeOut().isDefined()) {
			int receivedHandshakeCounter = getHandshakeOut().get();    // Get receivedHandshakeCounter value.
			if (receivedHandshakeCounter > this.lastReceivedHandshake) {
				/* In case of overflow (receivedHandshakeCounter = 0, lastReceivedHandshake = 9999), will not work
                   for ~1 cycle. But will work again next cycle, because then lastReceivedHandshake = 0. */
				this.connectionTimestamp = LocalDateTime.now();
				this.connectionAlive = true;
			}
			this.lastReceivedHandshake = receivedHandshakeCounter;
			try {
				setHandshakeIn(receivedHandshakeCounter);    // Send receivedHandshakeCounter back.
			} catch (OpenemsError.OpenemsNamedException e) {
				this.log.warn("Couldn't write in Channel " + e.getMessage());
			}
		}
		if (ChronoUnit.SECONDS.between(this.connectionTimestamp, LocalDateTime.now()) >= 30) {    // No heart beat match for 30 seconds means connection is dead.
			this.connectionAlive = false;
		}

		// Can only send commands if handshake is successful.
		if (this.connectionAlive) {

			boolean exceptionalStateActive = false;
			int exceptionalStateValue = 0;

			// Handle EnableSignal.
			boolean turnOnChp = this.enableSignalHandler.deviceShouldBeHeating(this);

			// Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
			if (this.useExceptionalState) {
				exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
				if (exceptionalStateActive) {
					exceptionalStateValue = this.getExceptionalStateValue();
					if (exceptionalStateValue <= 0) {
						turnOnChp = false;
					} else {
						turnOnChp = true;
						exceptionalStateValue = Math.min(exceptionalStateValue, 100);
					}
				}
			}

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

			// Send command bits, based on settings. ’turnOnChp’ can be set to true by config, EnableSignal or ExceptionalState.
			int commandBits1to16 = 0;
			if (turnOnChp) {
				ControlMode controlMode = this.getControlMode().asEnum();

				// Map EffectiveElectricPowerSetpoint channel from Chp interface.
				if (exceptionalStateActive == false && controlMode == ControlMode.POWER
						&& this.getElectricPowerSetpointChannel().getNextWriteValue().isPresent()) {
					double setpointValue = this.getElectricPowerSetpointChannel().getNextWriteValue().get();
					setpointValue = Math.min(setpointValue, this.maxChpPower);
					setpointValue = Math.max(setpointValue, this.maxChpPower / 2.0);
					double calculatedPercent = setpointValue / this.maxChpPower;
					try {
						this.setHeatingPowerPercentSetpoint(calculatedPercent);
						this._setElectricPowerSetpoint(setpointValue);
					} catch (OpenemsError.OpenemsNamedException e) {
						this.log.warn("Couldn't write in Channel " + e.getMessage());
					}
				} else if (exceptionalStateActive && exceptionalStateValue > 0) {
					// When ExceptionalStateValue is between 0 and 100, set Chp to this PowerPercentage.
					try {
						this.setHeatingPowerPercentSetpoint(exceptionalStateValue);
						controlMode = ControlMode.POWER_PERCENT;
						this.setControlMode(ControlMode.POWER_PERCENT.getValue());
					} catch (OpenemsError.OpenemsNamedException e) {
						this.log.warn("Couldn't write in Channel " + e.getMessage());
					}
				}

				// Command bits:
				// 0 - Control over bus, always needs to be 1 to send commands. (Steuerung über Bussystem (muss für Steuerung immer 1 sein))
				// 1 - Clear errors, only turn on for a short time. (Fehler-Quittierung (darf nur kurzzeitig angelegt werden))
				// 2 - Start chp, stop if 0. (Startanforderung (Stop bei 0))
				// 3 - Operation mode: grid power draw (Betriebsart - Startanfoderung durch Netzbezug)
				// 4 - Operation mode: buffer tank (Betriebsart - Startanfoderung durch Puffer)
				// 5 - Operation mode: grid power draw set point (Betriebsart - Regelung durch Netzbezugssollwert)
				// 6 - Operation mode: set point (Betriebsart - Regelung durch Gleitwert)
				// 7 - Set point transmitted by bus. (Gleitwert kommt über Bus)
				// 8 - Grid power draw transmitted by bus. (Netzleistungswert kommt über Bus)
				// 9 - Automatic mode. (Automatikbetrieb BHKW (Flanke))

				switch (controlMode) {
					case CONSUMPTION:
						commandBits1to16 = 0b0100100101;
						break;
					case POWER_PERCENT:
					case POWER:
					default:
						commandBits1to16 = 0b011000101;
						break;
				}

			} else {
				commandBits1to16 = 0b01;    // Turn off chp.
			}

			try {
				setCommandBits1to16(commandBits1to16);
			} catch (OpenemsError.OpenemsNamedException e) {
				this.log.warn("Couldn't write in Channel " + e.getMessage());
			}
		}
	}

	/**
	 * Information that is printed to the log if ’print info to log’ option is enabled.
	 */
	protected void printInfo() {
		this.logInfo(this.log, "--CHP KW Energy Smartblock--");
		this.logInfo(this.log, "Engine rpm: " + this.getEngineRpm());
		this.logInfo(this.log, "Engine temperature: " + this.getEngineTemperature());
		this.logInfo(this.log, "Effective electrical power: " + this.getEffectiveElectricPower() + " of max "
				+ this.maxChpPower + " kW (" + (1.0 * this.getEffectiveElectricPower().orElse(0.0) / this.maxChpPower) + "%)");
		this.logInfo(this.log, "Power set point: " + this.getPowerSetpoint());
		this.logInfo(this.log, "Flow temperature: " + this.getFlowTemperature() + " d°C");
		this.logInfo(this.log, "Return temperature: " + this.getReturnTemperature() + " d°C");
		this.logInfo(this.log, "CHP model: " + this.getChpModel());
		this.logInfo(this.log, "Operating hours: " + this.getOperatingHours());
		this.logInfo(this.log, "Engine start counter: " + this.getEngineStartCounter());
		this.logInfo(this.log, "Produced active energy (Wirkleistung): " + this.getActiveEnergy());
		this.logInfo(this.log, "Maintenance interval 1: " + this.getMaintenanceInterval1());
		this.logInfo(this.log, "Maintenance interval 2: " + this.getMaintenanceInterval2());
		this.logInfo(this.log, "Maintenance interval 3: " + this.getMaintenanceInterval3());
		this.logInfo(this.log, "Produced heat: " + this.getProducedHeat());
		this.logInfo(this.log, "");
		this.logInfo(this.log, "Heater state: " + this.getHeaterState());
		this.logInfo(this.log, "Control mode OpenEMS: " + this.getControlMode().asEnum().getName());
		this.logInfo(this.log, "Received handshake counter: " + this.lastReceivedHandshake);
		this.logInfo(this.log, "Status message: " + this.getStatusMessage().get());
		this.logInfo(this.log, "Warning message: " + this.getWarningMessage().get());
		this.logInfo(this.log, "Error message: " + this.getErrorMessage().get());
		this.logInfo(this.log, "");
	}
}
