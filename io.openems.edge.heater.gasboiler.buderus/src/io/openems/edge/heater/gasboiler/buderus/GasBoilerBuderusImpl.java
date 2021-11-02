package io.openems.edge.heater.gasboiler.buderus;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
import io.openems.edge.heater.api.EnableSignalHandler;
import io.openems.edge.heater.api.EnableSignalHandlerImpl;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.timer.api.TimerHandler;
import io.openems.edge.timer.api.TimerHandlerImpl;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.heater.gasboiler.buderus.api.OperatingMode;
import io.openems.edge.heater.gasboiler.buderus.api.GasBoilerBuderus;
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
import java.util.Optional;


/**
 * This module reads the most important variables available via Modbus from a Buderus gas boiler and maps them to OpenEMS
 * channels. The module is written to be used with the Heater interface methods (EnableSignal) and ExceptionalState.
 * When setEnableSignal() from the Heater interface is set to true with no other parameters like temperature specified,
 * the heater will turn on with default settings. The default settings are configurable in the config.
 * The heater can be controlled with setHeatingPowerPercentSetpoint() (set power in %) or setTemperatureSetpoint().
 * setHeatingPowerSetpoint() (set power in kW) and related methods are currently not supported by this heater.
 * When ExceptionalState is used, the heater will automatically switch to control mode ’heating power percent’.
 * With the current code, setTemperatureSetpoint() is then only usable when ExceptionalState is disabled.
 * If the heater is activated by ExceptionalState, it will switch to control mode power percent and use the
 * setHeatingPowerPercentSetpoint() specified by the ExceptionalStateValue. The heater will NOT automatically switch
 * back to its prior state when ExceptionalState ends.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.GasBoiler.Buderus",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS //
		})
public class GasBoilerBuderusImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		ExceptionalState, GasBoilerBuderus {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager cpm;

	private final Logger log = LoggerFactory.getLogger(GasBoilerBuderusImpl.class);
	private boolean printInfoToLog;
	private int heartbeatCounter = 0;
	private boolean connectionAlive = false;
	private LocalDateTime connectionTimestamp;
	private boolean readOnly;
	private double heatingPowerPercentSetting;

	// No successful handshake for this amount of seconds results in: heater state changing to ’off’, stop sending commands.
	private final int handshakeTimeoutSeconds = 30;

	private EnableSignalHandler enableSignalHandler;
	private static final String ENABLE_SIGNAL_IDENTIFIER = "BUDERUS_HEATER_ENABLE_SIGNAL_IDENTIFIER";
	private boolean useExceptionalState;
	private ExceptionalStateHandler exceptionalStateHandler;
	private static final String EXCEPTIONAL_STATE_IDENTIFIER = "BUDERUS_HEATER_EXCEPTIONAL_STATE_IDENTIFIER";

	// This is essential for Modbus to work, but the compiler does not warn you when it is missing!
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public GasBoilerBuderusImpl() {
		super(OpenemsComponent.ChannelId.values(),
				GasBoilerBuderus.ChannelId.values(),
				Heater.ChannelId.values(),
				ExceptionalState.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbusBridgeId());

		this.printInfoToLog = config.printInfoToLog();
		this.readOnly = config.readOnly();
		if (this.isEnabled() == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		}

		if (this.readOnly == false) {
			this.connectionTimestamp = LocalDateTime.now().minusSeconds(this.handshakeTimeoutSeconds + 1);	// Initialize with past time value so connection test is negative at start.
			this.setOperatingMode(config.operatingMode().getValue());
			this.setTemperatureSetpoint(config.defaultSetPointTemperature() * 10);	// Convert to d°C.
			this.heatingPowerPercentSetting = config.defaultSetPointPowerPercent();
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
				// Input register read.
				new FC4ReadInputRegistersTask(386, Priority.HIGH,
						m(GasBoilerBuderus.ChannelId.IR386_STATUS_STRATEGY, new UnsignedWordElement(386),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(390, Priority.HIGH,
						m(GasBoilerBuderus.ChannelId.IR390_RUNREQUEST_INITIATOR, new UnsignedWordElement(390),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(394, Priority.HIGH,
						m(GasBoilerBuderus.ChannelId.IR394_STRATEGY_BITBLOCK, new UnsignedWordElement(394),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.IR395_MAX_FLOW_TEMP_REQUESTED, new SignedWordElement(395),
								ElementToChannelConverter.SCALE_FACTOR_1)
				),
				new FC4ReadInputRegistersTask(476, Priority.HIGH,
						m(GasBoilerBuderus.ChannelId.IR476_ERROR_REGISTER1, new UnsignedDoublewordElement(476),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.IR478_ERROR_REGISTER2, new UnsignedDoublewordElement(478),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.IR480_ERROR_REGISTER3, new UnsignedDoublewordElement(480),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.IR482_ERROR_REGISTER4, new UnsignedDoublewordElement(482),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(8001, Priority.HIGH,
						m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(8001),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.IR8002_FLOW_TEMP_RATE_OF_CHANGE_BOILER1, new SignedWordElement(8002),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(8003),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.EFFECTIVE_HEATING_POWER_PERCENT, new UnsignedWordElement(8004),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.IR8005_HEATER_AT_LOAD_LIMIT_BOILER1, new UnsignedWordElement(8002),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(8006),
						m(GasBoilerBuderus.ChannelId.IR8007_MAXIMUM_POWER_BOILER1, new UnsignedWordElement(8007),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.IR8008_MINIMUM_POWER_PERCENT_BOILER1, new UnsignedWordElement(8008),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(8009),
						new DummyRegisterElement(8010),
						m(GasBoilerBuderus.ChannelId.IR8011_MAXIMUM_FLOW_TEMP_BOILER1, new UnsignedWordElement(8011),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(GasBoilerBuderus.ChannelId.IR8012_STATUS_BOILER1, new UnsignedWordElement(8012),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.IR8013_BITBLOCK_BOILER1, new UnsignedWordElement(8013),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(8014),
						m(GasBoilerBuderus.ChannelId.IR8015_REQUESTED_TEMPERATURE_SETPOINT_BOILER1, new UnsignedWordElement(8015),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(GasBoilerBuderus.ChannelId.IR8016_SETPOINT_POWER_PERCENT_BOILER1, new UnsignedWordElement(8016),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.IR8017_PRESSURE_BOILER1, new SignedWordElement(8017),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.IR8018_ERROR_CODE_BOILER1, new UnsignedWordElement(8018),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.IR8019_DISPLAY_ERROR_CODE_BOILER1, new UnsignedWordElement(8019),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(8020),
						m(GasBoilerBuderus.ChannelId.IR8021_NUMBER_OF_STARTS_BOILER1, new UnsignedDoublewordElement(8021),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.IR8023_RUNNING_TIME_BOILER1, new UnsignedDoublewordElement(8023),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),

				// Holding register read.
				new FC3ReadRegistersTask(0, Priority.HIGH,
						m(GasBoilerBuderus.ChannelId.HR0_HEARTBEAT_IN, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(GasBoilerBuderus.ChannelId.HR1_HEARTBEAT_OUT, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(402, Priority.HIGH,
						m(GasBoilerBuderus.ChannelId.HR402_RUN_PERMISSION, new UnsignedWordElement(402),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(405, Priority.HIGH,
						m(GasBoilerBuderus.ChannelId.HR405_COMMAND_BITS, new UnsignedWordElement(405),
								ElementToChannelConverter.DIRECT_1_TO_1)
				)
		);
		if (this.readOnly == false) {
			protocol.addTasks(
					new FC16WriteRegistersTask(0,
							m(GasBoilerBuderus.ChannelId.HR0_HEARTBEAT_IN, new UnsignedWordElement(0),
									ElementToChannelConverter.DIRECT_1_TO_1)
					),
					new FC16WriteRegistersTask(400,
							m(Heater.ChannelId.SET_POINT_TEMPERATURE, new UnsignedWordElement(400),
									ElementToChannelConverter.SCALE_FACTOR_1),
							m(GasBoilerBuderus.ChannelId.HR401_MODBUS, new UnsignedWordElement(401),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(GasBoilerBuderus.ChannelId.HR402_RUN_PERMISSION, new UnsignedWordElement(402),
									ElementToChannelConverter.DIRECT_1_TO_1)
					),
					new FC16WriteRegistersTask(405,
							m(GasBoilerBuderus.ChannelId.HR405_COMMAND_BITS, new UnsignedWordElement(405),
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
		} else if (this.readOnly == false && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
			this.writeCommands();
		}
	}

	/**
	 * Put values in channels that are not directly Modbus read values but derivatives.
	 */
	protected void channelmapping() {

		boolean heaterRunning = false;
		boolean heaterReadySignal = false;
		String statusMessage = "";
		String errorMessage = "";
		String warningMessage = "";

		int ir386Status = 0;
		if (this.getIR386StatusStrategy().isDefined()) {
			if (this.readOnly) {
				// readOnly disables the heartbeat register, so heartbeat can't work. But "connectionAlive = false"
				// overwrites any status message with "Modbus not connected". So get "connectionAlive = true" this way
				// to get status messages in readOnly mode.
				this.connectionAlive = true;
			}
			ir386Status = this.getIR386StatusStrategy().get();
		}
		switch (ir386Status) {
			case 1:
				warningMessage = "Warning flag active, ";
				statusMessage = "Heater state: warning, ";
				break;
			case 2:
				errorMessage = "Error flag active, ";
				statusMessage = "Heater state: error, ";
				break;
			case 3:
				heaterReadySignal = true;	// ir386Status ist also OK when heater is not running.
				statusMessage = "Heater state: OK, ";
				break;
			case 4:
				statusMessage = "Heater state: not active, ";
				break;
			case 5:
				errorMessage = "Heater state critical, ";
				statusMessage = "Heater state: critical, ";
				break;
			case 6:
				statusMessage = "Heater state: no info, ";
				break;
			case 0:
			default:
				statusMessage = "Heater state: unknown, ";
				break;
		}

		int ir390runrequestInitiator = 0;
		if (this.getIR390RunrequestInitiator().isDefined()) {
			ir390runrequestInitiator = this.getIR390RunrequestInitiator().get();
		}
		statusMessage = statusMessage + this.parseIr390runrequestInitiator(ir390runrequestInitiator);

		int ir394strategyBitblock = 0;
		if (this.getIR394StrategyBitblock().isDefined()) {
			ir394strategyBitblock = this.getIR394StrategyBitblock().get();
		}
		statusMessage = statusMessage + this.parseIr394strategieBitblock(ir394strategyBitblock);

		errorMessage = errorMessage + this.getErrorRegistersContent();

		int ir8013kessel1Bitblock = 0;
		if (this.getBitblockBoiler1().isDefined()) {
			ir8013kessel1Bitblock = this.getBitblockBoiler1().get();
		}
		if ((ir8013kessel1Bitblock & 0b01) == 0) {	// Bit NOT active
			warningMessage = warningMessage + "Heater not able to process commands, ";
			statusMessage = statusMessage + "Heater not able to process commands, ";
		}
		if ((ir8013kessel1Bitblock & 0b010) == 0b010) {
			statusMessage = statusMessage + "Forced current flow (Zwangsdurchstroemung), ";
		}
		if ((ir8013kessel1Bitblock & 0b0100) == 0b0100) {
			statusMessage = statusMessage + "Keep temperature function (Warmhaltefunktion), ";
		}
		if ((ir8013kessel1Bitblock & 0b01000) == 0b01000) {
			statusMessage = statusMessage + "Boiler blocked by contact switch (Kesselsperre durch Kontakt), ";
		}
		if ((ir8013kessel1Bitblock & 0b010000) == 0b010000) {
			statusMessage = statusMessage + "Boiler blocked because of set point decrease (Kesselsperre negativer Sollwertsprung), ";
		}
		if ((ir8013kessel1Bitblock & 0b0100000) == 0b0100000) {
			// Incinerator on.
			heaterRunning = true;
		}
		// 0b01000000 == Command requested (Fuehrung angefordert). Already transmitted by ir394strategyBitblock.
		// 0b010000000 == Priority requested (Prioritaet angefordert). Already transmitted by ir394strategyBitblock.
		// 0b0100000000 == Control mode flow temperature requested (Vorlaufregelung angefordert aktiv). Already transmitted by ir394strategyBitblock.
		// 0b01000000000 == Control mode power requested (Leistungsregelung angefordert aktiv). Already transmitted by ir394strategyBitblock.
		if ((ir8013kessel1Bitblock & 0b010000000000) == 0b010000000000) {
			errorMessage = errorMessage + "Heater locked because of error (Verriegelnde Stoerung), ";
		}
		if ((ir8013kessel1Bitblock & 0b0100000000000) == 0b0100000000000) {
			errorMessage = errorMessage + "Heater blocked because of error (Blockierende Stoerung), ";
		}
		if ((ir8013kessel1Bitblock & 0b01000000000000) == 0b01000000000000) {
			warningMessage = warningMessage + "Maintenance needed, ";
			statusMessage = statusMessage + "Maintenance needed, ";
		}

		if (this.getErrorCodeBoiler1().isDefined()) {
			if (this.getErrorCodeBoiler1().get() > 0) {
				errorMessage = errorMessage + "Error code boiler 1: " + this.getErrorCodeBoiler1().get() + ", ";
			}
		}
		if (this.getErrorCodeDisplayBoiler1().isDefined()) {
			if (this.getErrorCodeDisplayBoiler1().get() > 0) {
				// Doesn't seem to be a real error. The register gives an error code, but the boiler is running without
				// problems. Remove this indicator?
				// The error codes are not in the error list. Speculation: this is the code for what is shown in the
				// display of the device?
				statusMessage = statusMessage + "Display error code boiler 1 (Fehleranzeigecode im Display Kessel 1): " + this.getErrorCodeDisplayBoiler1().get() + ", ";
			}
		}


		// Set Heater interface STATUS channel
		if (this.connectionAlive == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		} else if (heaterRunning) {
			this._setHeaterState(HeaterState.RUNNING.getValue());
		} else if (heaterReadySignal) {
			this._setHeaterState(HeaterState.STANDBY.getValue());
		} else {
			// If the code gets to here, the state is undefined.
			this._setHeaterState(HeaterState.UNDEFINED.getValue());
		}
		this.getHeaterStateChannel().nextProcessImage();

		// Fill status channel.
		if (this.connectionAlive) {
			statusMessage = statusMessage.substring(0, statusMessage.length() - 2) + ".";
			this._setStatusMessage(statusMessage);

			if (errorMessage.length() > 0) {
				errorMessage = errorMessage.substring(0, errorMessage.length() - 2) + ".";
			} else {
				errorMessage = "No error";
			}
			this._setErrorMessage(errorMessage);

			if (warningMessage.length() > 0) {
				warningMessage = warningMessage.substring(0, warningMessage.length() - 2) + ".";
			} else {
				warningMessage = "No warning";
			}
			this._setWarningMessage(warningMessage);
		} else {
			this._setStatusMessage("Modbus not connected");
			this._setErrorMessage("Modbus not connected");
			this._setWarningMessage("Modbus not connected");
		}
		this.getStatusMessageChannel().nextProcessImage();
		this.getErrorMessageChannel().nextProcessImage();
		this.getWarningMessageChannel().nextProcessImage();
	}

	protected String parseIr390runrequestInitiator(int ir390runrequestInitiator) {
		switch (ir390runrequestInitiator) {
			case 1:
				return "running requested by controller (Regelgeraet), ";
			case 2:
				return "running requested by device (Intern), ";
			case 3:
				return "running requested by manual operation (Manueller Betrieb), ";
			case 4:
				return "running requested by external connection (Extern), ";
			case 5:
				return "running requested by device + external connection (Intern+Extern), ";
			case 0:
			default:
				return "currently off, ";
		}
	}

	protected String parseIr394strategieBitblock(int ir394strategieBitblock) {
		String returnString = "";
		if ((ir394strategieBitblock & 0b01) == 0b01) {
			returnString = "External heat source detected (Fremdwaerme erkannt), ";
		}
		if ((ir394strategieBitblock & 0b010) == 0b010) {
			returnString = returnString + "Frost protection active (Frostschutz aktiv), ";
		}
		if ((ir394strategieBitblock & 0b0100) == 0b0100) {
			returnString = returnString + "Priority requested (Prioritaet angefordert), ";	// Seems to be always active. Remove indicator?
		}
		if ((ir394strategieBitblock & 0b01000) == 0b01000) {
			returnString = returnString + "Command requested (Fuehrung angefordert), ";	// Seems to be always active. Remove indicator?
		}
		if ((ir394strategieBitblock & 0b010000) == 0b010000) {
			returnString = returnString + "Control mode flow temperature requested (Vorlaufregelung angefordert aktiv), ";
		}
		if ((ir394strategieBitblock & 0b0100000) == 0b0100000) {
			returnString = returnString + "Control mode power requested (Leistungsregelung angefordert aktiv), ";
		}
		if ((ir394strategieBitblock & 0b01000000) == 0b01000000) {
			returnString = returnString + "Heat request from external (Externe Waermeanforderung), ";
		}
		return returnString;
	}

	protected String getErrorRegistersContent() {
		String returnString = "";
		if (this.getIR476ErrorRegister1().isDefined()) {
			if (this.getIR476ErrorRegister1().get() > 0) {
				returnString = returnString + "Error code register 1: " + this.getIR476ErrorRegister1().get() + ", ";
			}
		}
		if (this.getIR478ErrorRegister2().isDefined()) {
			if (this.getIR478ErrorRegister2().get() > 0) {
				returnString = returnString + "Error code register 2: " + this.getIR476ErrorRegister1().get() + ", ";
			}
		}
		if (this.getIR480ErrorRegister3().isDefined()) {
			if (this.getIR480ErrorRegister3().get() > 0) {
				returnString = returnString + "Error code register 3: " + this.getIR476ErrorRegister1().get() + ", ";
			}
		}
		if (this.getIR482ErrorRegister4().isDefined()) {
			if (getIR482ErrorRegister4().get() > 0) {
				returnString = returnString + "Error code register 4: " + this.getIR476ErrorRegister1().get() + ", ";
			}
		}
		return returnString;
	}


	/**
	 * Determine commands and send them to the heater.
	 * The channel SET_POINT_HEATING_POWER_PERCENT gets special treatment, because that is changed by ExceptionalState.
	 * The write of that channel is not mapped to Modbus. This is done by a duplicate ’private’ channel. The write to
	 * the ’public’ channel SET_POINT_HEATING_POWER_PERCENT is stored in a local variable and sent to Modbus using the
	 * ’private’ channel.
	 * The benefit of this design is that when ExceptionalState is active and applies it's own heatingPowerPercentSetpoint,
	 * the previous set point is saved. Also, it is still possible to write to the channel during ExceptionalState.
	 * A write to SET_POINT_HEATING_POWER_PERCENT is still registered and the value saved, but not executed. The changed
	 * set point is then applied once ExceptionalState ends. This way you don't have to pay attention to the state of
	 * the heat pump when writing in the SET_POINT_HEATING_POWER_PERCENT channel.
	 */
	protected void writeCommands() {
		// Collect heatingPowerPercentSetpoint channel ’nextWrite’.
		Optional<Double> heatingPowerPercentOptional = this.getHeatingPowerPercentSetpointChannel().getNextWriteValueAndReset();
		if (heatingPowerPercentOptional.isPresent()) {
			double setpoint = heatingPowerPercentOptional.get();
			// Restrict to valid write values
			setpoint = Math.min(setpoint, 100);
			setpoint = Math.max(0, setpoint);
			this.heatingPowerPercentSetting = setpoint;
		}

		// Send and increment heartbeatCounter.
		try {
			this.setHeartBeatIn(this.heartbeatCounter);	// Send heartbeatCounter.
		} catch (OpenemsError.OpenemsNamedException e) {
			this.log.warn("Couldn't write in Channel " + e.getMessage());
		}
		if (this.getHeartBeatOut().isDefined()) {
			int receivedHeartbeatCounter = this.getHeartBeatOut().get();	// Get last received heartbeatCounter value.
			if (receivedHeartbeatCounter == this.heartbeatCounter) {		// Test if the sent value was received.
				this.connectionTimestamp = LocalDateTime.now();			// Now we know the connection is alive. Set timestamp.
				this.heartbeatCounter++;
			}
		}
		if (ChronoUnit.SECONDS.between(this.connectionTimestamp, LocalDateTime.now()) >= this.handshakeTimeoutSeconds) {
			// No heart beat match for this long means connection is dead.
			this.connectionAlive = false;
		} else {
			this.connectionAlive = true;
		}
		if (this.heartbeatCounter >= 100) {	// Overflow protection.
			this.heartbeatCounter = 1;
		}

		boolean turnOnHeater = this.enableSignalHandler.deviceShouldBeHeating(this);

		// Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
		double heatingPowerPercentSetpointToModbus = this.heatingPowerPercentSetting;
		boolean exceptionalStateActive = false;
		if (this.useExceptionalState) {
			exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
			if (exceptionalStateActive) {
				int exceptionalStateValue = this.getExceptionalStateValue();
				if (exceptionalStateValue <= this.DEFAULT_MIN_EXCEPTIONAL_VALUE) {
					turnOnHeater = false;
				} else {
					// When ExceptionalStateValue is between 0 and 100, set heater to this PowerPercentage.
					turnOnHeater = true;
					exceptionalStateValue = Math.min(exceptionalStateValue, this.DEFAULT_MAX_EXCEPTIONAL_VALUE);
					heatingPowerPercentSetpointToModbus = exceptionalStateValue;
				}
			}
		}

		// Wait for connection. Then turn on heater and send CommandBits when enableSignal == true.
		if (this.connectionAlive) {
			if (turnOnHeater) {
				try {
					this.setRunPermission(true);	// Not sure if this is correct. One manual says ’1 = on’, the other ’0 = on’.
				} catch (OpenemsError.OpenemsNamedException e) {
					this.log.warn("Couldn't write in Channel " + e.getMessage());
				}

				// If nothing is in the channel yet, take set point power percent as default behavior.
				boolean useSetPointTemperature = this.getOperatingMode().isDefined() && (this.getOperatingMode().asEnum() == OperatingMode.SET_POINT_TEMPERATURE);
				if (exceptionalStateActive) {
					// ExceptionalState uses SET_POINT_POWER_PERCENT, overwrites control mode.
					useSetPointTemperature = false;
				}
				try {
					if (useSetPointTemperature) {
						this.setCommandBits(0b0101); // Control mode temperature (Temperaturgefuehrte Regelung).
					} else {
						this.setCommandBits(0b1001);	// Control mode power (Leistungsgefuehrte Regelung).
						this.getHr401ModbusChannel().setNextWriteValue(heatingPowerPercentSetpointToModbus);
					}
				} catch (OpenemsError.OpenemsNamedException e) {
					this.log.warn("Couldn't write in Channel " + e.getMessage());
				}
			} else {
				try {
					this.setRunPermission(false);
				} catch (OpenemsError.OpenemsNamedException e) {
					this.log.warn("Couldn't write in Channel " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Information that is printed to the log if ’print info to log’ option is enabled.
	 */
	protected void printInfo() {
		this.logInfo(this.log, "--Buderus Kessel--");
		this.logInfo(this.log, "Heater STATE channel: " + this.getHeaterState());
		this.logInfo(this.log, "Heater flow temperature: " + this.getFlowTemperature());
		this.logInfo(this.log, "Heater maximum flow temperature: " + this.getMaximumFlowTempBoiler1());
		this.logInfo(this.log, "Heater flow temperature change speed: " + this.getIR8002FlowTempRateOfChange());
		this.logInfo(this.log, "Heater return temperature: " + this.getReturnTemperature());
		this.logInfo(this.log, "Heater effective power percent: " + this.getEffectiveHeatingPowerPercent());
		this.logInfo(this.log, "Heater minimum power percent: " + this.getMinimumPowerPercentBoiler1());
		this.logInfo(this.log, "Heater set point flow temp: " + this.getRequestedTemperatureSetPointBoiler1());
		this.logInfo(this.log, "Heater set point power percent: " + this.getRequestedPowerPercentSetPointBoiler1());
		this.logInfo(this.log, "Heater pressure: " + this.getPressureBoiler1());
		this.logInfo(this.log, "Heater number of startups: " + this.getNumberOfStartsBoiler1());
		this.logInfo(this.log, "Heater running time: " + this.getRunningTimeBoiler1());
		this.logInfo(this.log, "Run permission (register 402): " + this.getRunPermissionChannel().value().get());
		this.logInfo(this.log, "Heater status message: " + this.getStatusMessage().get());
		this.logInfo(this.log, "Heater warning message: " + this.getWarningMessage().get());
		this.logInfo(this.log, "Heater error message: " + this.getErrorMessage().get());
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
