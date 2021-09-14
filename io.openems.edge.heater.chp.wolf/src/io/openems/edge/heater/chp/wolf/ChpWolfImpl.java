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

@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Chp.Wolf",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

/**
 * This module reads all variables available via Modbus from a Wolf CHP and maps them to OpenEMS
 * channels. WriteChannels can be used to send commands to the CHP via "setNextWriteValue" method.
 */
public class ChpWolfImpl extends AbstractOpenemsModbusComponent implements OpenemsComponent, EventHandler,
		ExceptionalState, ChpWolfChannel {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	protected ComponentManager cpm;

	private final Logger log = LoggerFactory.getLogger(ChpWolfImpl.class);
	private int testcounter = 0;
	private boolean debug;
	private int commandCycler = 0;

	private boolean componentEnabled;
	private boolean turnOnChp;
	private boolean readOnly = false;
	private boolean startupStateChecked = false;
	private int chpMaxElectricPower;

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

	public ChpWolfImpl() {
		super(OpenemsComponent.ChannelId.values(),
				ChpWolfChannel.ChannelId.values(),
				Chp.ChannelId.values(),
				Heater.ChannelId.values(),
				ExceptionalState.ChannelId.values());
	}


	@Activate
	public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
		this.componentEnabled = config.enabled();
		this.debug = config.debug();
		this.chpMaxElectricPower = config.chpMaxElectricPower();

		this.readOnly = config.readOnly();
		this.startupStateChecked = false;
		if (this.readOnly == false) {
			this.setElectricPowerSetpoint(config.defaultSetPointElectricPower());
			this.turnOnChp = config.turnOnChp();
			TimerHandler timer = new TimerHandlerImpl(super.id(), this.cpm);
			this.useEnableSignal = config.useEnableSignalChannel();
			if (this.useEnableSignal) {
				String timerTypeEnableSignal;
				if (config.enableSignalTimerIsCyclesNotSeconds()) {
					timerTypeEnableSignal = "TimerByCycles";
				} else {
					timerTypeEnableSignal = "TimerByTime";
				}
				timer.addOneIdentifier(ENABLE_SIGNAL_IDENTIFIER, timerTypeEnableSignal, config.waitTimeEnableSignal());
				this.enableSignalHandler = new EnableSignalHandlerImpl(timer, ENABLE_SIGNAL_IDENTIFIER);
			}
			this.useExceptionalState = config.useExceptionalState();
			if (this.useExceptionalState) {
				String timerTypeExceptionalState;
				if (config.exceptionalStateTimerIsCyclesNotSeconds()) {
					timerTypeExceptionalState = "TimerByCycles";
				} else {
					timerTypeExceptionalState = "TimerByTime";
				}
				timer.addOneIdentifier(EXCEPTIONAL_STATE_IDENTIFIER, timerTypeExceptionalState, config.waitTimeExceptionalState());
				this.exceptionalStateHandler = new ExceptionalStateHandlerImpl(timer, EXCEPTIONAL_STATE_IDENTIFIER);
			}
		}
		if (this.componentEnabled == false) {
			this._setHeaterState(HeaterState.OFF.getValue());
		}
	}


	@Deactivate
	public void deactivate() {
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
						m(ChpWolfChannel.ChannelId.HR32_BUFFERTANK_TEMP_UPPER, new UnsignedWordElement(0),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolfChannel.ChannelId.HR33_BUFFERTANK_TEMP_MIDDLE, new UnsignedWordElement(1),
								ElementToChannelConverter.DIRECT_1_TO_1),
						m(ChpWolfChannel.ChannelId.HR34_BUFFERTANK_TEMP_LOWER, new UnsignedWordElement(2),
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
		if (this.componentEnabled && event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)) {
			this.channelmapping();
		}
	}

	// Put values in channels that are not directly Modbus read values but derivatives.
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
				warnMessage = warnMessage + "Kühlwasserdruck minimum Gasmotor, ";
			}
			if ((statusBits40003 & 0b01000000) == 0b01000000) {
				warnMessage = warnMessage + "Tank Ölvorlage auf Minimum, ";
			}
			if ((statusBits40003 & 0b010000000) == 0b010000000) {
				warnMessage = warnMessage + "Schmieröldruck Gasmotor minimum, ";
			}
			if ((statusBits40003 & 0b0100000000) == 0b0100000000) {
				warnMessage = warnMessage + "Generatorschutz ausgelöst, ";
			}
			if ((statusBits40003 & 0b01000000000) == 0b01000000000) {
				warnMessage = warnMessage + "NA-Schutz Extern ausgelöst, ";
			}
			if ((statusBits40003 & 0b010000000000) == 0b010000000000) {
				warnMessage = warnMessage + "Erdgasdruck an der Gasregelstrecke zu gering, ";
			}
			if ((statusBits40003 & 0b0100000000000) == 0) {    // Handbuch sagt hier 0=ein, also vermutlich Störung ist wenn hier 0 ist.
				warnMessage = warnMessage + "Durchflußwächter Kühlwasserkreis Gasmotor misst keinen Fluß, ";
			}
			if ((statusBits40003 & 0b01000000000000) == 0b01000000000000) {
				warnMessage = warnMessage + "Sicherheitstempaturbegrenzer Gasmotor geschaltet, ";
			}
			if ((statusBits40003 & 0b010000000000000) == 0b010000000000000) {
				warnMessage = warnMessage + "Motorschutz Pumpe Kühlwasser ausgelöst, ";
			}
			if ((statusBits40003 & 0b0100000000000000) == 0b0100000000000000) {
				warnMessage = warnMessage + "Motorschutz Pumpe Heizung zum Verteiler ausgelöst, ";
			}
			if ((statusBits40003 & 0b01000000000000000) == 0b01000000000000000) {
				warnMessage = warnMessage + "Sicherungsautomat Lüfter Schallhaube ausgelöst, ";
			}

			statusBits40012 = getStatusBits40012().get();
			if ((statusBits40012 & 0b010000000000000) == 0b010000000000000) {
				warnMessage = warnMessage + "Meldung Wartungintervall erreicht, ";
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
		
		// Parse status by checking engine rpm.
		boolean readyForCommands = false;
		boolean chpEngineRunning = false;
		if (getRpm().isDefined()) {
			readyForCommands = true;
			if (getRpm().get() > 10) {
				chpEngineRunning = true;
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

		// Only send write commands when readOnly is false. All write commands are in this if statement.
		if (this.readOnly == false && readyForCommands) {

			if (this.useEnableSignal) {
				this.turnOnChp = this.enableSignalHandler.deviceShouldBeHeating(this);

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
					this.turnOnChp = chpEngineRunning;
					if (this.turnOnChp) {
						try {
							this.getEnableSignalChannel().setNextWriteValue(true);
						} catch (OpenemsError.OpenemsNamedException e) {
							this.log.warn("Couldn't write in Channel " + e.getMessage());
						}
					}
				}
			}

			// Handle ExceptionalState. ExceptionalState overwrites EnableSignal.
			if (this.useExceptionalState) {
				boolean exceptionalStateActive = this.exceptionalStateHandler.exceptionalStateActive(this);
				if (exceptionalStateActive) {
					int exceptionalStateValue = this.getExceptionalStateValue();
					if (exceptionalStateValue <= 0) {
						// Turn off Chp when ExceptionalStateValue = 0.
						this.turnOnChp = false;
					} else {
						// ToDo: Granularity of electric power setpoint is very coarse. The Wolf GTK 4 has 4 kW electric
						//  power. Mosbus power setpoint is an int with unit kW. Usually a chp can only go as low as 50%
						//  of maximum power. That means the GTK 4 has only 3 possible setpoints: 2 kW, 3 kW and 4 kW.
						//  Check if that is really the case or if manual is wrong about power setpoint unit.

						// When ExceptionalStateValue is between 0 and 100, set Chp to this PowerPercentage.
						this.turnOnChp = true;
						if (exceptionalStateValue > 100) {
							exceptionalStateValue = 100;
						}
						int electricPowerSetpoint = (int)Math.round(this.chpMaxElectricPower * exceptionalStateValue / 100.0);
						try {
							this.setElectricPowerSetpoint(electricPowerSetpoint);
						} catch (OpenemsNamedException e) {
							this.log.warn("Couldn't write in Channel " + e.getMessage());
						}
					}
				}
			}

			// Write bits mapping.
			// This chp has an unusual way of handling write commands. Instead of mapping one command to one
			// register, four commands are mapped to three registers that need to be set simultaneously.
			// HR6358 - always 2.
			// HR6359 - the value you want to write.
			// HR6360 - code deciding which command it is.
			//			35 = Sollwert elektrische Leistung in kW
			//			36 = Sollwert Einspeisemanagement (optional)
			//			37 = Reserve
			//			38 = on/off, send 1 for on, 0 for off.
			//
			// You can only send one command per cycle (need to write all three registers for one command)
			this.commandCycler++;
			switch (this.commandCycler) {
				case 1:
					int writeValueCase1 = 0;
					if (this.turnOnChp) {
						writeValueCase1 = 1;
					}
					try {
						setWriteBits1(2);
						setWriteBits2(writeValueCase1);
						setWriteBits3(38);
					} catch (OpenemsNamedException e) {
						this.logError(this.log, "Error setting next write value: " + e);
					}
					break;
				case 2:
					Optional<Double> electricPowerSetpoint = this.getElectricPowerSetpointChannel().getNextWriteValueAndReset();
					if (electricPowerSetpoint.isPresent()) {
						int writeValue = (int)Math.round(electricPowerSetpoint.get());
						// Update channel.
						_setElectricPowerSetpoint(writeValue);
						try {
							setWriteBits1(2);
							setWriteBits2(writeValue);
							setWriteBits3(35);
						} catch (OpenemsNamedException e) {
							this.logError(this.log, "Error setting next write value: " + e);
						}
					}
					break;
				case 3:
					Optional<Integer> einspeisemenagement = getEinspeisemanagementSetpointChannel().getNextWriteValueAndReset();
					if (einspeisemenagement.isPresent()) {
						int writeValue = einspeisemenagement.get();
						// Update channel.
						getEinspeisemanagementSetpointChannel().setNextValue(writeValue);
						try {
							setWriteBits1(2);
							setWriteBits2(writeValue);
							setWriteBits3(36);
						} catch (OpenemsNamedException e) {
							this.logError(this.log, "Error setting next write value: " + e);
						}
					}
					break;
				default:
					this.commandCycler = 0;
					Optional<Integer> reserve = getReserveSetpointChannel().getNextWriteValueAndReset();
					if (reserve.isPresent()) {
						int writeValue = reserve.get();
						// Update channel.
						getReserveSetpointChannel().setNextValue(writeValue);
						try {
							setWriteBits1(2);
							setWriteBits2(writeValue);
							setWriteBits3(37);
						} catch (OpenemsNamedException e) {
							this.logError(this.log, "Error setting next write value: " + e);
						}
					}
			}
		}

		

		if (debug) {
			this.logInfo(this.log, "--Status Bits 40003--");
			this.logInfo(this.log, "0 - RM Ge.Schalter - Rückmeldung Generatorschalter geschlossen = " + (((statusBits40003 & 1) == 1) ? 1 : 0));
			this.logInfo(this.log, "1 - RM Ne.Schalter - Rückmeldung Netzparallelbetrieb möglich = " + (((statusBits40003 & 2) == 2) ? 1 : 0));
			this.logInfo(this.log, "2 - Fernstart - Start-Stopp Eingang = " + (((statusBits40003 & 4) == 4) ? 1 : 0));
			this.logInfo(this.log, "3 - Not stop - Meldung Not-Aus gedrückt = " + (((statusBits40003 & 8) == 8) ? 1 : 0));
			this.logInfo(this.log, "4 - Stellung Auto - Automatik Fernstart möglich = " + (((statusBits40003 & 16) == 16) ? 1 : 0));
			this.logInfo(this.log, "5 - Wasserdruck - Kühlwasserdruck minimum Gasmotor = " + (((statusBits40003 & 32) == 32) ? 1 : 0));
			this.logInfo(this.log, "6 - Ölvorlage - Tank Ölvorlage auf Minimum = " + (((statusBits40003 & 64) == 64) ? 1 : 0));
			this.logInfo(this.log, "7 - Oeldruck min - Schmieröldruck Gasmotor minimum = " + (((statusBits40003 & 128) == 128) ? 1 : 0));
			this.logInfo(this.log, "8 - Gen.Schutz - Generatorschutz ausgelöst = " + (((statusBits40003 & 256) == 256) ? 1 : 0));
			this.logInfo(this.log, "9 - NA-Schutz Extern - NA-Schutz Extern ausgelöst = " + (((statusBits40003 & 512) == 512) ? 1 : 0));
			this.logInfo(this.log, "10 - Erdgasdruck min - Erdgasdruck an der Gasregelstrecke zu gering = " + (((statusBits40003 & 1024) == 1024) ? 1 : 0));
			this.logInfo(this.log, "11 - Durchfluß - Durchflußwächter Kühlwasserkreis Gasmotor geschaltet = " + (((statusBits40003 & 2048) == 2048) ? 1 : 0));
			this.logInfo(this.log, "12 - STB Motor - Sicherheitstempaturbegrenzer Gasmotor geschaltet = " + (((statusBits40003 & 4096) == 4096) ? 1 : 0));
			this.logInfo(this.log, "13 - Stör P Motor - Motorschutz Pumpe Kühlwasser ausgelöst = " + (((statusBits40003 & 8192) == 8192) ? 1 : 0));
			this.logInfo(this.log, "14 - Stör P Heizung - Motorschutz Pumpe Heizung zum Verteiler ausgelöst = " + (((statusBits40003 & 16384) == 16384) ? 1 : 0));
			this.logInfo(this.log, "15 - Stör Lüfter - Sicherungsautomat Lüfter Schallhaube ausgelöst = " + (((statusBits40003 & 32768) == 32768) ? 1 : 0));
			this.logInfo(this.log, "");
			this.logInfo(this.log, "--Status Bits 40012--");
			this.logInfo(this.log, "0 - Starter - Anlasser eingeschaltet = " + (((statusBits40012 & 1) == 1) ? 1 : 0));
			this.logInfo(this.log, "1 - Kessel - Freigabe Kessel = " + (((statusBits40012 & 2) == 2) ? 1 : 0));
			this.logInfo(this.log, "2 - Gasventile - Freigabe Gasventile = " + (((statusBits40012 & 4) == 4) ? 1 : 0));
			this.logInfo(this.log, "3 - Gasventile - Freigabe Gasventile = " + (((statusBits40012 & 8) == 8) ? 1 : 0));
			this.logInfo(this.log, "4 - GLS Aus/Ein - Generatorschalter Ein / AUS = " + (((statusBits40012 & 16) == 16) ? 1 : 0));
			this.logInfo(this.log, "5 - Speicher - Freigabe Speicherentladepumpe = " + (((statusBits40012 & 32) == 32) ? 1 : 0));
			this.logInfo(this.log, "6 - Pumpen+Lüfter - Pumpe Motor Ein, Heizung Ein, Ladeluft Ein, Lüfter Schallhaube Ein = " + (((statusBits40012 & 64) == 64) ? 1 : 0));
			this.logInfo(this.log, "7 - Zuendung - Freigabe Zündung = " + (((statusBits40012 & 128) == 128) ? 1 : 0));
			this.logInfo(this.log, "8 - Reserve = " + (((statusBits40012 & 256) == 256) ? 1 : 0));
			this.logInfo(this.log, "9 - Bereit - Meldung Bereit = " + (((statusBits40012 & 512) == 512) ? 1 : 0));
			this.logInfo(this.log, "10 - Umluft - Anforderung Umluftklappe öffnen = " + (((statusBits40012 & 1024) == 1024) ? 1 : 0));
			this.logInfo(this.log, "11 - Störung = " + (((statusBits40012 & 2048) == 2048) ? 1 : 0));
			this.logInfo(this.log, "12 - Pumpe Ölvorlage - Anforderung Pumpe Ölvorlage = " + (((statusBits40012 & 4096) == 4096) ? 1 : 0));
			this.logInfo(this.log, "13 - Service Zeit - Meldung Wartungintervall erreicht = " + (((statusBits40012 & 8192) == 8192) ? 1 : 0));
			this.logInfo(this.log, "14 - res. = " + (((statusBits40012 & 16384) == 16384) ? 1 : 0));
			this.logInfo(this.log, "15 - res. = " + (((statusBits40012 & 32768) == 32768) ? 1 : 0));
			this.logInfo(this.log, "");
			this.logInfo(this.log, "Vorlauf Temperatur: " + getFlowTemperature());
			this.logInfo(this.log, "Rücklauf Temperatur: " + getReturnTemperature());
			this.logInfo(this.log, "Pufferspeicher Temperatur oben: " + (getBufferTankTempUpper().orElse(0) / 10.0) + "°C");
			this.logInfo(this.log, "Pufferspeicher Temperatur mitte: " + (getBufferTankTempMiddle().orElse(0) / 10.0) + "°C");
			this.logInfo(this.log, "Pufferspeicher Temperatur unten: " + (getBufferTankTempLower().orElse(0) / 10.0) + "°C");
			this.logInfo(this.log, "Elektrische Leistung: " + getEffectiveElectricPower());
			this.logInfo(this.log, "Motor Drehzeahl: " + getRpm().get() + " rpm");
			this.logInfo(this.log, "Laufzeit: " + getRuntime().get() + " h");
			this.logInfo(this.log, "Anzahl der Starts: " + getEngineStarts().get());
			this.logInfo(this.log, "Erzeugte elektrische Arbeit gesamt: " + getElectricalWork().get() + " kWh");
			this.logInfo(this.log, "");
			this.logInfo(this.log, "--Schreibbare Parameter--");
			this.logInfo(this.log, "Sollwert elektrische Leistung in kW: " + getElectricPowerSetpointChannel().value().get());
			this.logInfo(this.log, "On / Off: " + getEnableSignal());
			this.logInfo(this.log, "");
		}

	}
}
