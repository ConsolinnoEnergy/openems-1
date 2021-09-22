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
import io.openems.edge.heater.gasboiler.buderus.api.HeaterBuderus;
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
 * This module reads the most important variables available via Modbus from a Buderus heater and maps them to OpenEMS
 * channels. The module is written to be used with the Heater interface methods.
 * When setEnableSignal() from the Heater interface is set to true with no other parameters like temperature specified,
 * the heater will turn on with default settings. The default settings are configurable in the config.
 * The heater can be controlled with setSetPointPowerPercent() or setSetPointTemperature().
 * setSetPointPower() and related methods are not supported by this heater.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Buderus.GasBoiler",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)
public class HeaterBuderusImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		ExceptionalState, HeaterBuderus {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager cpm;

	private final Logger log = LoggerFactory.getLogger(HeaterBuderusImpl.class);
	private boolean printInfoToLog;
	private boolean componentEnabled;
	private int heartbeatCounter = 0;
	private boolean connectionAlive = false;
	private LocalDateTime connectionTimestamp;
	private boolean readOnly;

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

	public HeaterBuderusImpl() {
		super(OpenemsComponent.ChannelId.values(),
				HeaterBuderus.ChannelId.values(),
				Heater.ChannelId.values(),
				ExceptionalState.ChannelId.values());
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbusBridgeId());
		this.componentEnabled = config.enabled();
		this.printInfoToLog = config.printInfoToLog();
		this.connectionTimestamp = LocalDateTime.now().minusMinutes(5);	// Initialize with past time value so connection test is negative at start.
		this.readOnly = config.readOnly();

		this.setOperatingMode(config.operatingMode().getValue());

		if (this.readOnly == false) {
			this.setTemperatureSetpoint(config.defaultSetPointTemperature() * 10);	// Convert to d°C.
			this.setHeatingPowerPercentSetpoint(config.defaultSetPointPowerPercent());

			TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
			timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, config.enableSignalTimerId(), config.waitTimeEnableSignal());
			this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
			this.useExceptionalState = config.useExceptionalState();
			if (this.useExceptionalState) {
				timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, config.exceptionalStateTimerId(), config.waitTimeExceptionalState());
				this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
			}
		}

		if (this.componentEnabled == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
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
						m(HeaterBuderus.ChannelId.IR386_STATUS_STRATEGIE, new UnsignedWordElement(386),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(390, Priority.HIGH,
						m(HeaterBuderus.ChannelId.IR390_RUNREQUEST_INITIATOR, new UnsignedWordElement(390),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(394, Priority.HIGH,
						m(HeaterBuderus.ChannelId.IR394_STRATEGIE_BITBLOCK, new UnsignedWordElement(394),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.IR395_MAX_FLOW_TEMP_ANGEFORDERT, new SignedWordElement(395),
								ElementToChannelConverter.SCALE_FACTOR_1)
				),
				new FC4ReadInputRegistersTask(476, Priority.HIGH,
						m(HeaterBuderus.ChannelId.IR476_FEHLERREGISTER1, new UnsignedDoublewordElement(476),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.IR478_FEHLERREGISTER2, new UnsignedDoublewordElement(478),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.IR480_FEHLERREGISTER3, new UnsignedDoublewordElement(480),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.IR482_FEHLERREGISTER4, new UnsignedDoublewordElement(482),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC4ReadInputRegistersTask(8001, Priority.HIGH,
						m(Heater.ChannelId.FLOW_TEMPERATURE, new SignedWordElement(8001),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.IR8002_FLOW_TEMP_AENDERUNGSGESCHWINDIGKEIT_KESSEL1, new SignedWordElement(8002),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.RETURN_TEMPERATURE, new SignedWordElement(8003),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(Heater.ChannelId.EFFECTIVE_HEATING_POWER_PERCENT, new UnsignedWordElement(8004),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.IR8005_WAERMEERZEUGER_IN_LASTBEGRENZUNG_KESSEL1, new UnsignedWordElement(8002),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(8006),
						m(HeaterBuderus.ChannelId.IR8007_MAXIMUM_POWER_KESSEL1, new UnsignedWordElement(8007),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.IR8008_MINIMUM_POWER_PERCENT_KESSEL1, new UnsignedWordElement(8008),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(8009),
						new DummyRegisterElement(8010),
						m(HeaterBuderus.ChannelId.IR8011_MAXIMALE_VORLAUFTEMP_KESSEL1, new UnsignedWordElement(8011),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(HeaterBuderus.ChannelId.IR8012_STATUS_KESSEL1, new UnsignedWordElement(8012),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.IR8013_BITBLOCK_KESSEL1, new UnsignedWordElement(8013),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(8014),
						m(HeaterBuderus.ChannelId.IR8015_ANGEFORDERTE_SOLLWERTTEMP_KESSEL1, new UnsignedWordElement(8015),
								ElementToChannelConverter.SCALE_FACTOR_1),
						m(HeaterBuderus.ChannelId.IR8016_SOLLWERT_LEISTUNG_KESSEL1, new UnsignedWordElement(8016),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.IR8017_DRUCK_KESSEL1, new SignedWordElement(8017),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.IR8018_FEHLERCODE_KESSEL1, new UnsignedWordElement(8018),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.IR8019_FEHLERCODE_DISPLAY_KESSEL1, new UnsignedWordElement(8019),
								ElementToChannelConverter.DIRECT_1_TO_1),
						new DummyRegisterElement(8020),
						m(HeaterBuderus.ChannelId.IR8021_ANZAHL_STARTS_KESSEL1, new UnsignedDoublewordElement(8021),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.IR8023_BETRIEBSZEIT_KESSEL1, new UnsignedDoublewordElement(8023),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),

				// Holding register read.
				new FC3ReadRegistersTask(0, Priority.HIGH,
						m(HeaterBuderus.ChannelId.HR0_HEARTBEAT_IN, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(HeaterBuderus.ChannelId.HR1_HEARTBEAT_OUT, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(402, Priority.HIGH,
						m(HeaterBuderus.ChannelId.HR402_RUN_PERMISSION, new UnsignedWordElement(402),
								ElementToChannelConverter.DIRECT_1_TO_1)
				),
				new FC3ReadRegistersTask(405, Priority.HIGH,
						m(HeaterBuderus.ChannelId.HR405_COMMAND_BITS, new UnsignedWordElement(405),
								ElementToChannelConverter.DIRECT_1_TO_1)
				)
		);
		if (this.readOnly == false) {
			protocol.addTasks(
					new FC16WriteRegistersTask(0,
							m(HeaterBuderus.ChannelId.HR0_HEARTBEAT_IN, new UnsignedWordElement(0),
									ElementToChannelConverter.DIRECT_1_TO_1)
					),
					new FC16WriteRegistersTask(400,
							m(Heater.ChannelId.SET_POINT_TEMPERATURE, new UnsignedWordElement(400),
									ElementToChannelConverter.SCALE_FACTOR_1),
							m(Heater.ChannelId.SET_POINT_HEATING_POWER_PERCENT, new UnsignedWordElement(401),
									ElementToChannelConverter.DIRECT_1_TO_1),
							m(HeaterBuderus.ChannelId.HR402_RUN_PERMISSION, new UnsignedWordElement(402),
									ElementToChannelConverter.DIRECT_1_TO_1)
					),
					new FC16WriteRegistersTask(405,
							m(HeaterBuderus.ChannelId.HR405_COMMAND_BITS, new UnsignedWordElement(405),
									ElementToChannelConverter.DIRECT_1_TO_1)
					)
			);
		}
		return protocol;
	}

	@Override
	public void handleEvent(Event event) {
		if (this.componentEnabled && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
			this.channelmapping();
		}
	}

	/**
	 * Put values in channels that are not directly Modbus read values but derivatives.
	 */
	protected void channelmapping() {

		if (this.readOnly == false) {

			// Send and increment heartbeatCounter.
			try {
				setHeartBeatIn(this.heartbeatCounter);	// Send heartbeatCounter.
			} catch (OpenemsError.OpenemsNamedException e) {
				this.log.warn("Couldn't write in Channel " + e.getMessage());
			}
			if (getHeartBeatOut().isDefined()) {
				int receivedHeartbeatCounter = getHeartBeatOut().get();	// Get last received heartbeatCounter value.
				if (receivedHeartbeatCounter == heartbeatCounter) {		// Test if the sent value was received.
					this.connectionTimestamp = LocalDateTime.now();			// Now we know the connection is alive. Set timestamp.
					this.heartbeatCounter++;
				}
			}
			if (ChronoUnit.SECONDS.between(this.connectionTimestamp, LocalDateTime.now()) >= 30) {	// No heart beat match for 30 seconds means connection is dead.
				this.connectionAlive = false;
			} else {
				this.connectionAlive = true;
			}
			if (this.heartbeatCounter >= 100) {	// Overflow protection.
				this.heartbeatCounter = 1;
			}

			boolean turnOnHeater = this.enableSignalHandler.deviceShouldBeHeating(this);

			// Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
			int exceptionalStateValue = 0;
			boolean exceptionalStateActive = false;
			if (this.useExceptionalState) {
				exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
				if (exceptionalStateActive) {
					exceptionalStateValue = this.getExceptionalStateValue();
					if (exceptionalStateValue <= 0) {
						// Turn off heater when ExceptionalStateValue = 0.
						turnOnHeater = false;
					} else {
						// When ExceptionalStateValue is between 0 and 100, set heater to this PowerPercentage.
						turnOnHeater = true;
						if (exceptionalStateValue > 100) {
							exceptionalStateValue = 100;
						}
						try {
							this.setHeatingPowerPercentSetpoint(exceptionalStateValue);

							// Todo: Should heater switch to control mode power percent in exceptional state?
							// ExceptoinalStateValue is Setting power percent setpoint. If heater is in control mode
							// temperature, this won't do anything.
						} catch (OpenemsError.OpenemsNamedException e) {
							this.log.warn("Couldn't write in Channel " + e.getMessage());
						}
					}
				}
			}

			// Wait for connection. Then turn on heater and send CommandBits when enableSignal == true.
			if (this.connectionAlive) {
				if (turnOnHeater) {

					/* //ToDo: This is done in activate(). Test if it works, then delete this.
					// Set default set point (defined in config) if the selected control mode does not have a set point specified.
					boolean noSetPoint =
							// Control mode is SetPointPowerPercent, and SET_POINT_POWER_PERCENT channel is empty.
							(((getOperatingMode().isDefined() == false) || (getOperatingMode().asEnum() == OperatingMode.SET_POINT_POWER_PERCENT)) // No value in getOperatingMode() counts as "Control mode is SetPointPowerPercent", which is the default.
									&& (getHeatingPowerPercentSetpointChannel().getNextWriteValue().isPresent() == false))
									||	// or
									// Control mode is SetPointTemperature, and SET_POINT_TEMPERATURE channel is empty.
									(((getOperatingMode().isDefined()) && (getOperatingMode().asEnum() == OperatingMode.SET_POINT_TEMPERATURE))
											&& (getTemperatureSetpointChannel().getNextWriteValue().isPresent() == false));
					if (noSetPoint) {
						try {
							setTemperatureSetpoint(this.defaultSetPointTemperature);
							setHeatingPowerPercentSetpoint(this.defaultSetPointPowerPercent);
						} catch (OpenemsError.OpenemsNamedException e) {
							this.log.warn("Couldn't write in Channel " + e.getMessage());
						}
					} */

					try {
						setRunPermission(true);	// Nicht sicher ob das so stimmt. Das eine Handbuch sagt 0 = an, das andere sagt 1 = an.
					} catch (OpenemsError.OpenemsNamedException e) {
						this.log.warn("Couldn't write in Channel " + e.getMessage());
					}

					// If nothing is in the channel yet, take set point power percent as default behavior.
					boolean useSetPointTemperature = getOperatingMode().isDefined() && (getOperatingMode().asEnum() == OperatingMode.SET_POINT_TEMPERATURE);
					try {
						if (useSetPointTemperature) {
							setCommandBits(0b0101); // Temperaturgefuehrte Regelung
						} else {
							setCommandBits(0b1001);	// Leistungsgefuehrte Regelung
						}
					} catch (OpenemsError.OpenemsNamedException e) {
						this.log.warn("Couldn't write in Channel " + e.getMessage());
					}
				} else {
					try {
						setRunPermission(false);
					} catch (OpenemsError.OpenemsNamedException e) {
						log.warn("Couldn't write in Channel " + e.getMessage());
					}
				}
			}

		}

		boolean heaterRunning = false;
		boolean heaterReadySignal = false;	// Bedeutet status "4-Nicht aktiv" bereit? Kann Status "3-OK" sein wenn der Kessel aus ist?
		String statusMessage = "";
		String errorMessage = "";
		String warningMessage = "";

		int ir386Status = 0;
		if (getIR386StatusStrategie().isDefined()) {
			if (this.readOnly) {
				// readOnly disables the heartbeat register, so heartbeat can't work. But "connectionAlive = false"
				// overwrites any status message with "Modbus not connected". So get "connectionAlive = true" this way
				// to get status messages in readOnly mode.
				this.connectionAlive = true;
			}
			ir386Status = getIR386StatusStrategie().get();
		}
		switch (ir386Status) {
			case 1:
				warningMessage = "Warning flag active, ";
				statusMessage = "Heater status: Warnung, ";
				break;
			case 2:
				errorMessage = "Error flag active, ";
				statusMessage = "Heater status: Störung, ";
				break;
			case 3:
				heaterReadySignal = true;	// ir386Status ist OK auch wenn Heater aus.
				statusMessage = "Heater status: OK, ";
				break;
			case 4:
				statusMessage = "Heater status: Nicht aktiv, ";
				break;
			case 5:
				errorMessage = "Heater state critical, ";
				statusMessage = "Heater status: Kritisch, ";
				break;
			case 6:
				statusMessage = "Heater status: Keine Info, ";
				break;
			case 0:
			default:
				statusMessage = "Heater status: Unbekannt, ";
				break;
		}

		int ir390runrequestInitiator = 0;
		if (getIR390RunrequestInitiator().isDefined()) {
			ir390runrequestInitiator = getIR390RunrequestInitiator().get();
		}
		switch (ir390runrequestInitiator) {
			case 1:
				statusMessage = statusMessage + "running requested by Regelgerät, ";
				break;
			case 2:
				statusMessage = statusMessage + "running requested by Intern, ";
				break;
			case 3:
				statusMessage = statusMessage + "running requested by Manueller Betrieb, ";
				break;
			case 4:
				statusMessage = statusMessage + "running requested by Extern, ";
				break;
			case 5:
				statusMessage = statusMessage + "running requested by Intern+Extern, ";
				break;
			case 0:
			default:
				statusMessage = statusMessage + "currently off, ";
				break;
		}

		int ir394strategieBitblock = 0;
		if (getIR394StrategieBitblock().isDefined()) {
			ir394strategieBitblock = getIR394StrategieBitblock().get();
		}
		if ((ir394strategieBitblock & 0b01) == 0b01) {
			statusMessage = statusMessage + "Fremdwärme erkannt, ";
		}
		if ((ir394strategieBitblock & 0b010) == 0b010) {
			statusMessage = statusMessage + "Frostschutz aktiv, ";
		}
		if ((ir394strategieBitblock & 0b0100) == 0b0100) {
			statusMessage = statusMessage + "Priorität angefordert, ";	// Scheint immer aktiv zu sein. Evtl. rausnehmen.
		}
		if ((ir394strategieBitblock & 0b01000) == 0b01000) {
			statusMessage = statusMessage + "Führung angefordert, ";	// Scheint immer aktiv zu sein. Evtl. rausnehmen.
		}
		if ((ir394strategieBitblock & 0b010000) == 0b010000) {
			statusMessage = statusMessage + "Vorlaufregelung angefordert aktiv, ";
		}
		if ((ir394strategieBitblock & 0b0100000) == 0b0100000) {
			statusMessage = statusMessage + "Leistungsregelung angefordert aktiv, ";
		}
		if ((ir394strategieBitblock & 0b01000000) == 0b01000000) {
			statusMessage = statusMessage + "Externe Wärmeanforderung, ";
		}

		if (getIR476Fehlerregister1().isDefined()) {
			if (getIR476Fehlerregister1().get() > 0) {
				errorMessage = errorMessage + "Error code register 2: " + getIR476Fehlerregister1().get() + ", ";
				statusMessage = statusMessage + "Fehlerregister 1: " + getIR476Fehlerregister1().get() + ", ";
			}
		}
		if (getIR478Fehlerregister2().isDefined()) {
			if (getIR478Fehlerregister2().get() > 0) {
				errorMessage = errorMessage + "Error code register 2: " + getIR476Fehlerregister1().get() + ", ";
				statusMessage = statusMessage + "Fehlerregister 2: " + getIR478Fehlerregister2().get() + ", ";
			}
		}
		if (getIR480Fehlerregister3().isDefined()) {
			if (getIR480Fehlerregister3().get() > 0) {
				errorMessage = errorMessage + "Error code register 3: " + getIR476Fehlerregister1().get() + ", ";
				statusMessage = statusMessage + "Fehlerregister 3: " + getIR480Fehlerregister3().get() + ", ";
			}
		}
		if (getIR482Fehlerregister4().isDefined()) {
			if (getIR482Fehlerregister4().get() > 0) {
				errorMessage = errorMessage + "Error code register 4: " + getIR476Fehlerregister1().get() + ", ";
				statusMessage = statusMessage + "Fehlerregister 4: " + getIR482Fehlerregister4().get() + ", ";
			}
		}

		int ir8013kessel1Bitblock = 0;
		if (getBitblockKessel1().isDefined()) {
			ir8013kessel1Bitblock = getBitblockKessel1().get();
		}
		if ((ir8013kessel1Bitblock & 0b01) == 0) {	// Bit NOT active
			warningMessage = warningMessage + "Heater not able to process commands, ";
			statusMessage = statusMessage + "Warnung: Wärmeerzeuger nicht steuerbar, ";
		}
		if ((ir8013kessel1Bitblock & 0b010) == 0b010) {
			statusMessage = statusMessage + "Zwangsdurchströmung, ";
		}
		if ((ir8013kessel1Bitblock & 0b0100) == 0b0100) {
			statusMessage = statusMessage + "Warmhaltefunktion, ";
		}
		if ((ir8013kessel1Bitblock & 0b01000) == 0b01000) {
			statusMessage = statusMessage + "Kesselsperre durch Kontakt, ";
		}
		if ((ir8013kessel1Bitblock & 0b010000) == 0b010000) {
			statusMessage = statusMessage + "Kesselsperre negativer Sollwertsprung, ";
		}
		if ((ir8013kessel1Bitblock & 0b0100000) == 0b0100000) {
			// Brenner an.
			heaterRunning = true;
		}
		// 0b01000000 == Führung angefordert. Schon bei ir394strategieBitblock drin.
		// 0b010000000 == Priorität angefordert. Schon bei ir394strategieBitblock drin.
		// 0b0100000000 == Vorlaufregelung angefordert. Schon bei ir394strategieBitblock drin.
		// 0b01000000000 == Leistungsregelung angefordert. Schon bei ir394strategieBitblock drin.
		if ((ir8013kessel1Bitblock & 0b010000000000) == 0b010000000000) {
			errorMessage = errorMessage + "Heater locked because of error: ";
			statusMessage = statusMessage + "Verriegelnde Störung, ";
		}
		if ((ir8013kessel1Bitblock & 0b0100000000000) == 0b0100000000000) {
			errorMessage = errorMessage + "Heater blocked because of error: ";
			statusMessage = statusMessage + "Blockierende Störung, ";
		}
		if ((ir8013kessel1Bitblock & 0b01000000000000) == 0b01000000000000) {
			warningMessage = warningMessage + "Maintenance needed, ";
			statusMessage = statusMessage + "Wartungsmeldung anstehend, ";
		}

		if (getErrorCodeKessel1().isDefined()) {
			if (getErrorCodeKessel1().get() > 0) {
				errorMessage = errorMessage + "Error code boiler 1: " + getErrorCodeKessel1().get() + ", ";
				statusMessage = statusMessage + "Fehlercode Kessel 1: " + getErrorCodeKessel1().get() + ", ";
			}
		}
		if (getErrorCodeDisplayKessel1().isDefined()) {
			if (getErrorCodeDisplayKessel1().get() > 0) {
				// Scheint kein wirklicher Fehler zu sein. Kessel läuft auch wenn hier was drin steht. Rausnehmen?
				// Die angezeigten Codes stehen nicht in der Fehlerliste. Vermutlich einfach nur das, was am Display
				// angezeigt wird.
				statusMessage = statusMessage + "Fehleranzeigecode im Display Kessel 1: " + getErrorCodeDisplayKessel1().get() + ", ";
			}
		}


		// Set Heater interface STATUS channel
		if (this.connectionAlive == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		} else if (heaterRunning) {
			this._setHeaterState(HeaterState.HEATING.getValue());
		} else if (heaterReadySignal) {
			this._setHeaterState(HeaterState.STANDBY.getValue());
		}
		else {
			// If the code gets to here, the state is undefined.
			this._setHeaterState(HeaterState.UNDEFINED.getValue());
		}

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


		if (this.printInfoToLog) {
			this.logInfo(this.log, "--Buderus Kessel--");
			this.logInfo(this.log, "Heater STATE channel: " + getHeaterState());
			this.logInfo(this.log, "Heater flow temperature: " + getFlowTemperature() + " d°C");
			this.logInfo(this.log, "Heater maximum flow temperature: " + getMaximumFlowTempKessel1().get() + " d°C");
			this.logInfo(this.log, "Heater flow temperature change speed: " + getIR8002FlowTempChangeSpeed().get() + " dK/min");
			this.logInfo(this.log, "Heater return temperature: " + getReturnTemperature() + " d°C");
			this.logInfo(this.log, "Heater effective power percent: " + getEffectiveHeatingPowerPercent() + " %");
			this.logInfo(this.log, "Heater minimum power percent: " + getMinimumPowerPercentKessel1().get() + " %");
			this.logInfo(this.log, "Heater set point flow temp: " + getRequestedTemperatureSetPointKessel1().get() + " d°C");
			this.logInfo(this.log, "Heater set point power percent: " + getRequestedPowerPercentSetPointKessel1().get() + " %");
			this.logInfo(this.log, "Heater pressure: " + getPressureKessel1().get() + " dBar");
			this.logInfo(this.log, "Heater number of startups: " + getNumberOfStartsKessel1().get());
			this.logInfo(this.log, "Heater running time: " + getRunningTimeKessel1().get() + " minutes");
			this.logInfo(this.log, "Register 402: " + getRunPermissionChannel().value().get());
			this.logInfo(this.log, "Heater status message: " + getStatusMessageChannel().value().get());
			this.logInfo(this.log, "");
		}

	}
}
