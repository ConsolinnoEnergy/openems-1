package io.openems.edge.heater.chp.kwenergy;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.heater.api.Chp;
import io.openems.edge.heater.chp.kwenergy.api.ChpKwEnergySmartblock;
import io.openems.edge.heater.chp.kwenergy.api.ControlMode;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.exceptionalstate.api.ExceptionalState;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandler;
import io.openems.edge.exceptionalstate.api.ExceptionalStateHandlerImpl;
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


@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Chp.KwEnergySmartblock",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

/**
 * This module reads the most important variables available via Modbus from a KW Energy Smartblock CHP and maps them to
 * OpenEMS channels. The module is written to be used with the Heater interface methods.
 * When setEnableSignal() from the Heater interface is set to true with no other parameters like setPointPowerPercent()
 * specified, the CHP will turn on with default settings. The default settings are configurable in the config.
 * The CHP can be controlled with setSetPointPowerPercent() or setSetPointElectricPower().
 * setSetPointTemperature() and related methods are not supported by this CHP.
 */
public class ChpKwEnergySmartblockImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		ExceptionalState, ChpKwEnergySmartblock {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager cpm;

	private final Logger log = LoggerFactory.getLogger(ChpKwEnergySmartblockImpl.class);
	private boolean debug;
	private boolean componentEnabled;
	private LocalDateTime connectionTimestamp;
	private boolean connectionAlive = false;
	private boolean readOnly = false;
	private boolean startupStateChecked = false;
	private int maxChpPower;
	private int lastReceivedHandshake = 0;

	private boolean turnOnChp;
	private boolean useEnableSignal;
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
	public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbusBridgeId());
		this.componentEnabled = config.enabled();
		this.debug = config.debug();
		this.connectionTimestamp = LocalDateTime.now().minusMinutes(5);    // Initialize with past time value so connection test is negative at start.
		this.maxChpPower = config.maxChpPower();

		this.readOnly = config.readOnly();
		this.startupStateChecked = false;
		if (this.readOnly == false) {
			this.turnOnChp = config.turnOnChp();
			TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
			this.useEnableSignal = config.useEnableSignalChannel();
			if (this.useEnableSignal) {
				String timerIdEnableSignal;
				if (config.enableSignalTimerIsCyclesNotSeconds()) {
					timerIdEnableSignal = "TimerByCycles";
				} else {
					timerIdEnableSignal = "TimerByTime";
				}
				timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, timerIdEnableSignal, config.waitTimeEnableSignal());
				this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
			}
			this.useExceptionalState = config.useExceptionalState();
			if (this.useExceptionalState) {
				String timerIdEnableSignal;
				if (config.exceptionalStateTimerIsCyclesNotSeconds()) {
					timerIdEnableSignal = "TimerByCycles";
				} else {
					timerIdEnableSignal = "TimerByTime";
				}
				timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerIdEnableSignal, config.waitTimeExceptionalState());
				this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
			}
			switch (config.controlMode()) {
				case "powerPercent":
					this.setControlMode(ControlMode.POWER_PERCENT.getValue());
					this.setHeatingPowerPercentSetpoint(config.defaultSetPointPowerPercent());
					break;
				case "power":
					this.setControlMode(ControlMode.POWER.getValue());
					double calculatedPercent = 1.0 * config.defaultSetPointElectricPower() / this.maxChpPower;
					this.setHeatingPowerPercentSetpoint(calculatedPercent);
					break;
				case "consumption":
					this.setControlMode(ControlMode.CONSUMPTION.getValue());
					break;
			}
			this.getControlModeChannel().nextProcessImage();    // So ’value’ field of channel is filled immediately.

		}
		if (this.componentEnabled == false) {
			_setHeaterState(HeaterState.OFF.getValue());
		}
	}

	@Deactivate
	public void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		ModbusProtocol protocol = new ModbusProtocol(this,
				// Holding register read.
				new FC3ReadRegistersTask(0, Priority.HIGH,
						// Use SignedWordElement when the number can be negative. Signed 16bit maps every number >32767
						// to negative. That means if the value you read is positive and <32767, there is no difference
						// between signed and unsigned.
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
						//m(ChpKwEnergySmartblockChannel.ChannelId.HR109_COMMAND_BITS_1_to_16, new UnsignedWordElement(109),
						//		ElementToChannelConverter.DIRECT_1_TO_1)
						// No read for SET_POINT_POWER_PERCENT and SET_POINT_POWER, since those channels immediately
						// copy setNextWrite to setNextValue.
				),
				new FC3ReadRegistersTask(112, Priority.HIGH,
						m(ChpKwEnergySmartblock.ChannelId.HR112_NETZBEZUGSWERT, new UnsignedWordElement(112),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)
				)
				/*
				new FC3ReadRegistersTask(119, Priority.HIGH,
						m(ChpKwEnergySmartblockChannel.ChannelId.HR119_HANDSHAKE_IN, new UnsignedWordElement(119),
								ElementToChannelConverter.DIRECT_1_TO_1)
				) */

		);
		if (this.readOnly == false) {
			protocol.addTasks(
					// Holding register write.
					// Modbus write tasks take the "setNextWriteValue" value of a channel and send them to the device.
					// Modbus read tasks put values in the "setNextValue" field, which get automatically transferred to the
					// "value" field of the channel. By default, the "setNextWriteValue" field is NOT copied to the
					// "setNextValue" and "value" field. In essence, this makes "setNextWriteValue" and "setNextValue"/"value"
					// two separate channels.
					// That means: Modbus read tasks will not overwrite any "setNextWriteValue" values. You do not have to
					// watch the order in which you call read and write tasks.
					// Also: if you do not add a Modbus read task for a write channel, any "setNextWriteValue" values will
					// not be transferred to the "value" field of the channel, unless you add code that does that.
					new FC16WriteRegistersTask(109,
							m(ChpKwEnergySmartblock.ChannelId.HR109_COMMAND_BITS_1_to_16, new UnsignedWordElement(109),
									ElementToChannelConverter.DIRECT_1_TO_1),
							// A Modbus read commands reads everything from start address to finish address. If there is a
							// gap, you must place a dummy element to fill the gap or end the read command there and start
							// with a new read where you want to continue.
							new DummyRegisterElement(110),
							m(Heater.ChannelId.SET_POINT_HEATING_POWER_PERCENT, new UnsignedWordElement(111),
									ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
							m(ChpKwEnergySmartblock.ChannelId.HR112_NETZBEZUGSWERT, new UnsignedWordElement(112),
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
		if (this.componentEnabled) {
			switch (event.getTopic()) {
				case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
					channelmapping();
					break;
			}
		}
	}

	// Put values in channels that are not directly Modbus read values but derivatives.
	protected void channelmapping() {

		// Pass effective electric power to Chp interface channel.
		if (getModbusEffectiveElectricPower().isDefined()) {
			double powerInKiloWatt = getModbusEffectiveElectricPower().get() / 10;
			_setEffectiveElectricPower(powerInKiloWatt);
		}

		// Parse error bits 1 to 16.
		String errorMessage = "";
		String warningMessage = "";
		int errorBits1to16 = 0;
		if (getErrorBits1to16().isDefined()) {
			errorBits1to16 = getErrorBits1to16().get();
		}
		boolean chpWarning = (errorBits1to16 & 0b01) == 0b01;
		if (chpWarning) {
			warningMessage = "Warning signal active. ";
		}
		boolean chpError = (errorBits1to16 & 0b010) == 0b010;
		this._setChpError(chpError);
		if (chpError) {
			errorMessage = "Error signal active. ";
		}
		boolean chpMaintenanceNeeded = (errorBits1to16 & 0b0100) == 0b0100;
		if (chpMaintenanceNeeded) {
			warningMessage = warningMessage + "Maintenance needed. ";
		}
		boolean chpEmergencyShutdown = (errorBits1to16 & 0b0100000) == 0b0100000;
		if (chpEmergencyShutdown) {
			errorMessage = errorMessage + "Emergency shutdown active. ";
		}
		boolean chpCoolantLow = (errorBits1to16 & 0b01000000) == 0b01000000;
		if (chpMaintenanceNeeded) {
			warningMessage = warningMessage + "Coolant low. ";
		}
		boolean chpLeaking = (errorBits1to16 & 0b0100000000) == 0b0100000000;
		if (chpLeaking) {
			warningMessage = warningMessage + "CHP is leaking. ";
		}

		// Parse status bits 1 to 16.
		int statusBits1to16 = 0;
		boolean statusBits1to16received = false;
		if (getStatusBits1to16().isDefined()) {
			statusBits1to16 = getStatusBits1to16().get();
			statusBits1to16received = true;
		}
		boolean chpAutomaticMode = (statusBits1to16 & 0b01) == 0b01;
		boolean chpRunSignalFromModbus = (statusBits1to16 & 0b010) == 0b010;
		boolean chpManualMode = (statusBits1to16 & 0b0100) == 0b0100;
		boolean chpReadySignal = (statusBits1to16 & 0b01000) == 0b01000;
		boolean chpRunSignalRegistered = (statusBits1to16 & 0b010000) == 0b010000;
		this._setChpRelease(chpRunSignalRegistered);
		boolean chpStartingUp = (statusBits1to16 & 0b0100000) == 0b0100000;
		boolean chpEngineRunning = (statusBits1to16 & 0b010000000) == 0b010000000;
		this._setChpEngineRunning(chpEngineRunning);

		// Parse status bits 65 to 80.
		int statusBits65to80 = 0;
		if (getStatusBits65to80().isDefined()) {
			statusBits65to80 = getStatusBits65to80().get();
		}
		boolean chpBetriebsart0FestwertAktiv = (statusBits65to80 & 0b01000000000000) == 0b01000000000000;
		boolean chpBetriebsart1GleitwertAktiv = (statusBits65to80 & 0b010000000000000) == 0b010000000000000;
		boolean chpBetriebsart2NetzbezugAktiv = (statusBits65to80 & 0b0100000000000000) == 0b0100000000000000;

		// Parse operating mode.
		int operatingModeCode = -1;
		if (getOperatingMode().isDefined()) {
			operatingModeCode = getStatusBits65to80().get();
		}
		String operatingMode;
		/* These codes are from the manual, but apparently they are wrong. Observed codes:
           2052 = 0b0100000000100  (mode ’power percent' "Freigabe durch Extern – Gleitwert", standby)
           41028 = 0b01010000001000100 (mode ’power percent' "Freigabe durch Extern – Gleitwert", running)
           This does not fit at all. */
		switch (operatingModeCode) {
			case 0:
				operatingMode = "Freigabe durch Puffer - Festwert";
				break;
			case 1:
				operatingMode = "Freigabe durch Puffer - Gleitwert";
				break;
			case 2:
				operatingMode = "Freigabe durch Netzbezug - Netzbezugssollwert";
				break;
			case 3:
				operatingMode = "Freigabe durch Puffer - Netzbezugssollwert";
				break;
			case 4:
				operatingMode = "Freigabe durch Extern - Netzbezugssollwert";
				break;
			case 5:
				operatingMode = "Reiner Notstrombetrieb";
				break;
			case 6:
				operatingMode = "Master-/Slavebetrieb";
				break;
			case 7:
				operatingMode = "Freigabe durch Extern – Festwert";
				break;
			case 8:
				operatingMode = "Freigabe durch Extern – Gleitwert";
				break;
			// case 9 does not exist.
			case 10:
				operatingMode = "keine Betriebsart";
				break;
			default:
				operatingMode = "Undefined (code " + operatingModeCode + ")";
				break;
		}

		int receivedHandshakeCounter = 1;

		// Only send write commands when readOnly is false. All write commands are in this if statement.
		if (this.readOnly == false) {

			// Handshake. Get value, send it back. If handshake is not sent, no writing of commands is possible.
			if (getHandshakeOut().isDefined()) {
				receivedHandshakeCounter = getHandshakeOut().get();    // Get receivedHandshakeCounter value.
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
					log.warn("Couldn't write in Channel " + e.getMessage());
				}
			}
			if (ChronoUnit.SECONDS.between(this.connectionTimestamp, LocalDateTime.now()) >= 30) {    // No heart beat match for 30 seconds means connection is dead.
				this.connectionAlive = false;
			}

			// Determine state of Chp before sending commands. Don't send commands if state is undetermined.
			boolean readyForCommands = this.connectionAlive && statusBits1to16received;

			if (this.useEnableSignal || this.useExceptionalState) {

				// Handle EnableSignal.
				if (this.useEnableSignal) {
					this.turnOnChp = this.enableSignalHandler.deviceShouldBeHeating(this);
				}

				// Handle ExceptionalState. ExceptionalState overwrites isEnabledSignal().
				int exceptionalStateValue = 0;
				boolean exceptionalStateActive = false;
				if (this.useExceptionalState) {
					exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
					if (exceptionalStateActive) {
						exceptionalStateValue = this.getExceptionalStateValue();
						if (exceptionalStateValue <= 0) {
							// Turn off Chp when ExceptionalStateValue = 0.
							this.turnOnChp = false;
						} else {
							// When ExceptionalStateValue is between 0 and 100, set Chp to this PowerPercentage.
							this.turnOnChp = true;
							if (exceptionalStateValue > 100) {
								exceptionalStateValue = 100;
							}
						}
					}
				}

				if (readyForCommands) {
					// If the component has just been started, it will most likely take a few cycles before a controller
					// sends an EnableSignal (assuming the CHP should be running). Since no EnableSignal means ’turn off the
					// CHP’, the component will always turn off the CHP during the first few cycles. If the CHP is already
					// on, this would turn the CHP off and on again, which is bad for the lifetime. A scenario where this
					// would happen is if the component or OpenEMS is restarted while the CHP is running.
					// To avoid that, check the CHP status at the startup of the component. If it is on, the component sends
					// the EnableSignal to itself once to keep the CHP on until the timer runs out. This gives any
					// controllers enough time to send the EnableSignal themselves.
					if (this.startupStateChecked == false) {
						this.startupStateChecked = true;
						this.turnOnChp = chpRunSignalRegistered || chpEngineRunning;  // Probably one of them is sufficient.
						if (this.turnOnChp) {
							try {
								this.getEnableSignalChannel().setNextWriteValue(true);
							} catch (OpenemsError.OpenemsNamedException e) {
								e.printStackTrace();
							}
						}
					}
				}

			}

			if (readyForCommands) {

				// Map EffectiveElectricPowerSetpoint channel from Chp interface.
				if (getElectricPowerSetpointChannel().getNextWriteValue().isPresent()) {
					int setpointValue = getElectricPowerSetpointChannel().getNextWriteValue().get();
					if (setpointValue > this.maxChpPower) {
						setpointValue = this.maxChpPower;
					}
					if (setpointValue < this.maxChpPower / 2) {
						setpointValue = this.maxChpPower / 2;
					}
					double calculatedPercent = 1.0 *  getElectricPowerSetpointChannel().getNextWriteValue().get() / this.maxChpPower;
					try {
						this.setHeatingPowerPercentSetpoint(calculatedPercent);
						_setElectricPowerSetpoint(setpointValue);
					} catch (OpenemsError.OpenemsNamedException e) {
						e.printStackTrace();
					}
				}

				// Send command bits, based on settings. ’turnOnChp’ can be set to true by config, EnableSignal or ExceptionalState.
				int commandBits1to16 = 0;
				if (this.turnOnChp) {

					// Command bits:
					// 0 - Steuerung über Bussystem (muss für Steuerung immer 1 sein)
					// 1 - Fehler-Quittierung (darf nur kurzzeitig angelegt werden)
					// 2 - Startanforderung (Stop bei 0)
					// 3 - Betriebsart - Startanfoderung durch Netzbezug
					// 4 - Betriebsart - Startanfoderung durch Puffer
					// 5 - Betriebsart - Regelung durch Netzbezugssollwert
					// 6 - Betriebsart - Regelung durch Gleitwert
					// 7 - Gleitwert kommt über Bus
					// 8 - Netzleistungswert kommt über Bus
					// 9 - Automatikbetrieb BHKW (Flanke)

					ControlMode controlMode = this.getControlMode().asEnum();
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
				// At this point commandBits1to16 have been set. Now send them.
				try {
					setCommandBits1to16(commandBits1to16);
				} catch (OpenemsError.OpenemsNamedException e) {
					log.warn("Couldn't write in Channel " + e.getMessage());
				}
			}
		} else {
			// Heartbeat is not used in readOnly mode. Use this to determine connectionAlive instead.
			this.connectionAlive = statusBits1to16received;
		}

		// Set Heater interface STATUS channel
		if (this.connectionAlive == false) {
			_setHeaterState(HeaterState.OFF.getValue());
		} else if (chpEngineRunning) {
			_setHeaterState(HeaterState.HEATING.getValue());
		} else if (chpStartingUp) {
			_setHeaterState(HeaterState.STARTING_UP_OR_PREHEAT.getValue());
		} else if (chpReadySignal) {
			_setHeaterState(HeaterState.STANDBY.getValue());
		} else {
			// If the code gets to here, the state is undefined.
			_setHeaterState(HeaterState.UNDEFINED.getValue());
		}

		// Build status message.
		String statusMessage = "";
		if (chpError) {
			statusMessage = statusMessage + "Störung BHKW-Anlage, ";
		}
		if (chpWarning) {
			statusMessage = statusMessage + "Warnung BHKW-Anlage, ";
		}
		if (chpMaintenanceNeeded) {
			statusMessage = statusMessage + "Wartungsaufruf BHKW, ";
		}
		if (chpEmergencyShutdown) {
			statusMessage = statusMessage + "Notabschaltung, ";
		}
		if (chpCoolantLow) {
			statusMessage = statusMessage + "Kühlwasserstand min, ";
		}
		if (chpLeaking) {
			statusMessage = statusMessage + "Leckage BHKW-Anlage, ";
		}
		if (chpAutomaticMode) {
			statusMessage = statusMessage + "Automatikbetrieb BHKW, ";
		}
		if (chpRunSignalFromModbus) {
			statusMessage = statusMessage + "Freigabe BHKW durch externes Signal, ";
		}
		if (chpManualMode) {
			statusMessage = statusMessage + "Handbetrieb BHKW, ";
		}
		if (chpReadySignal) {
			statusMessage = statusMessage + "BHKW betriebsbereit, ";
		}
		if (chpRunSignalRegistered) {
			statusMessage = statusMessage + "Anforderung steht an, ";
		}
		if (chpStartingUp) {
			statusMessage = statusMessage + "Start aktiv, ";
		}
		if (chpEngineRunning) {
			statusMessage = statusMessage + "Motor läuft, ";
		}
		if (chpBetriebsart0FestwertAktiv) {
			statusMessage = statusMessage + "Betriebsart 0 - Festwert aktiv, ";
		}
		if (chpBetriebsart1GleitwertAktiv) {
			statusMessage = statusMessage + "Betriebsart 0 - Gleitwert aktiv, ";
		}
		if (chpBetriebsart2NetzbezugAktiv) {
			statusMessage = statusMessage + "Betriebsart 0 - Netzbezug aktiv, ";
		}
		if (statusMessage.length() > 0) {
			statusMessage = statusMessage.substring(0, statusMessage.length() - 2) + ".";
		}
		_setStatusMessage(statusMessage);

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

		if (debug) {
			this.logInfo(this.log, "--CHP KW Energy Smartblock--");
			this.logInfo(this.log, "Engine rpm: " + this.getEngineRpm());
			this.logInfo(this.log, "Engine temperature: " + this.getEngineTemperature());
			this.logInfo(this.log, "Effective electrical power: " + this.getEffectiveElectricPower() + " of max "
					+ this.maxChpPower + " kW (" + (1.0 * this.getEffectiveElectricPower().get() / this.maxChpPower) + "%)");
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
			this.logInfo(this.log, "Operating mode: " + operatingMode);
			this.logInfo(this.log, "Control mode OpenEMS: " + this.getControlMode().asEnum().getName());
			this.logInfo(this.log, "Received handshake counter: " + receivedHandshakeCounter);
			this.logInfo(this.log, "Status message: " + this.getStatusMessage().get());
			this.logInfo(this.log, "");
		}

	}
}
